package io.github.francescolv.mine.device;

import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.agent.AgentManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Boards (micro:bit, Arduino, Pico…) on the students' PCs. A board writes lines {@code key:value}
 * on its serial port; the student's MINE reads them and sends them here. A board with Wi-Fi (Pico
 * 2 W) instead asks the server itself, every so often, with the student's code ({@link #wifi}). Each value is kept (the
 * Agent reads the last one) and handed to that student's Agent as a {@code mine_device} event:
 * a board only ever reaches the things of the student who plugged it in.
 *
 * <p>Values are whole numbers from {@value #MIN} to {@value #MAX} (beyond, they are limited), or
 * text. Read by the computer threads of the Agents: everything here is thread-safe.
 */
public final class Devices {

	public static final int MAX_KEY = 32;
	public static final int MAX_LINE = 256;
	public static final int MIN = -32768;
	public static final int MAX = 32767;
	/** At most this many values a second from one student; more are dropped. */
	public static final int PER_SECOND = 40;
	/** The "port" of a board on Wi-Fi. */
	public static final String WIFI = "Wi-Fi";
	/** A board on Wi-Fi that hasn't asked for this long is gone. */
	private static final long WIFI_TIMEOUT_MS = 10_000;
	/** Lines waiting for a board on Wi-Fi to ask: at most this many, the oldest are dropped. */
	private static final int OUTBOX = 64;

	private static final Pattern KEY = Pattern.compile("[a-z0-9_]{1," + MAX_KEY + "}");
	private static final Pattern NUMBER = Pattern.compile("[+-]?\\d{1,12}");

	/** The last value of each key, per student: an Integer or a String. */
	private static final Map<UUID, Map<String, Object>> VALUES = new ConcurrentHashMap<>();
	/** The port of each student's connected board. */
	private static final Map<UUID, String> CONNECTED = new ConcurrentHashMap<>();
	/** What was last sent to each student's board, per key: what its LEDs should show now. */
	private static final Map<UUID, Map<String, String>> LAST_SENT = new ConcurrentHashMap<>();
	private static final Map<UUID, Window> RATE = new ConcurrentHashMap<>();
	/** When each board on Wi-Fi last asked, and the lines waiting for it. */
	private static final Map<UUID, Long> WIFI_SEEN = new ConcurrentHashMap<>();
	private static final Map<UUID, Deque<String>> WIFI_OUT = new ConcurrentHashMap<>();

	private static final class Window {
		long second = -1;
		int count;
	}

	private Devices() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(DevicePayloads.Open.ID, DevicePayloads.Open.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(DevicePayloads.Send.ID, DevicePayloads.Send.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DevicePayloads.Status.ID, DevicePayloads.Status.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DevicePayloads.Value.ID, DevicePayloads.Value.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(DevicePayloads.Status.ID, (payload, context) ->
				connected(context.player(), payload.port()));
		ServerPlayNetworking.registerGlobalReceiver(DevicePayloads.Value.ID, (payload, context) ->
				received(context.player(), payload.key(), payload.value()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.getPlayer().getUUID()));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0 && !WIFI_SEEN.isEmpty()) {
				wifiGone(server);
			}
		});
	}

	/**
	 * The student's board on Wi-Fi asked: it says {@code lines} ({@code key:value}), and gets the
	 * lines sent to it since it last asked. The first time, it gets what was last sent for each
	 * key, so its LEDs show what they should. Server thread.
	 */
	public static List<String> wifi(ServerPlayer player, List<String> lines) {
		UUID student = player.getUUID();
		WIFI_SEEN.put(student, System.currentTimeMillis());
		if (!onWifi(student)) {
			Deque<String> out = new ArrayDeque<>();
			LAST_SENT.getOrDefault(student, Map.of()).forEach((key, value) -> out.add(key + ":" + value));
			WIFI_OUT.put(student, out);
			connected(player, WIFI);
		}
		for (String line : lines) {
			int colon = line.indexOf(':');
			if (colon > 0) {
				received(player, line.substring(0, colon), line.substring(colon + 1));
			}
		}
		Deque<String> out = WIFI_OUT.get(student);
		if (out == null) {
			return List.of();
		}
		synchronized (out) {
			List<String> send = List.copyOf(out);
			out.clear();
			return send;
		}
	}

	private static void wifiGone(MinecraftServer server) {
		long now = System.currentTimeMillis();
		WIFI_SEEN.entrySet().removeIf(entry -> {
			if (now - entry.getValue() < WIFI_TIMEOUT_MS) {
				return false;
			}
			WIFI_OUT.remove(entry.getKey());
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player != null && onWifi(entry.getKey())) {
				connected(player, "");
			}
			return true;
		});
	}

	/** The student connected a board on {@code port}, or disconnected it ({@code port} empty). */
	public static void connected(ServerPlayer player, String port) {
		if (port.isEmpty()) {
			if (CONNECTED.remove(player.getUUID()) != null) {
				MineMod.LOGGER.info("[MINE] {} disconnected their board", player.getName().getString());
			}
			return;
		}
		CONNECTED.put(player.getUUID(), port);
		BoardBlocks.connected(player.level().getServer(), player.getUUID());
		MineMod.LOGGER.info("[MINE] {} connected a board on {}", player.getName().getString(), port);
	}

	/** The student's board said {@code key:value}. Returns whether it was taken (not dropped). */
	public static boolean received(ServerPlayer player, String key, String value) {
		String name = normalizeKey(key);
		if (!KEY.matcher(name).matches() || value.length() > MAX_LINE || !allowed(player)) {
			return false;
		}
		play(player.level().getServer(), player.getUUID(), name, value);
		return true;
	}

	/**
	 * {@code key:value} as if the student's board had said it: kept, and handed to their Agent and
	 * their board blocks. Lessons use it to test a program for a board without the board.
	 * Server thread.
	 */
	public static void play(MinecraftServer server, UUID student, String key, String value) {
		Object parsed = parse(value);
		VALUES.computeIfAbsent(student, uuid -> new ConcurrentHashMap<>()).put(key, parsed);
		AgentManager.deviceEvent(server, student, key, parsed);
		BoardBlocks.received(server, student, key, parsed);
	}

	private static boolean allowed(ServerPlayer player) {
		long second = player.level().getServer().getTickCount() / 20;
		Window window = RATE.computeIfAbsent(player.getUUID(), uuid -> new Window());
		synchronized (window) {
			if (window.second != second) {
				window.second = second;
				window.count = 0;
			}
			return ++window.count <= PER_SECOND;
		}
	}

	/** A key as boards and programs may write it: without spaces around, in small letters. */
	private static String normalizeKey(String key) {
		return key.strip().toLowerCase(Locale.ROOT);
	}

	/** Whether {@code key} can be a key: letters a-z, digits and _, at most {@value #MAX_KEY}. */
	public static boolean validKey(String key) {
		return KEY.matcher(key).matches();
	}

	/** A whole number (limited to {@value #MIN}…{@value #MAX}) or else the text, without spaces around. */
	static Object parse(String value) {
		String text = value.strip();
		if (NUMBER.matcher(text).matches()) {
			long number = Long.parseLong(text.startsWith("+") ? text.substring(1) : text);
			return (int) Math.max(MIN, Math.min(MAX, number));
		}
		return text;
	}

	/** The last value the student's board gave for {@code key}, or {@code null}. */
	@Nullable
	public static Object read(UUID student, String key) {
		Map<String, Object> values = VALUES.get(student);
		return values == null ? null : values.get(normalizeKey(key));
	}

	/** Whether the student's board is on Wi-Fi now, not on a cable. */
	public static boolean onWifi(UUID student) {
		return WIFI.equals(CONNECTED.get(student));
	}

	/** Whether the student has a board connected in MINE now. */
	public static boolean isConnected(UUID student) {
		return CONNECTED.containsKey(student);
	}

	/**
	 * Sends {@code key:value} to the student's board. Returns false if the key isn't valid or the
	 * student has no board connected. Thread-safe: the packet leaves from the server thread.
	 */
	public static boolean send(MinecraftServer server, UUID student, String key, String value) {
		String name = normalizeKey(key);
		String text = value.replace('\n', ' ').replace('\r', ' ');
		if (!KEY.matcher(name).matches() || text.length() > MAX_LINE) {
			return false;
		}
		// Kept even without a board: lessons check what the Agent would have told it.
		LAST_SENT.computeIfAbsent(student, uuid -> new ConcurrentHashMap<>()).put(name, text);
		if (!isConnected(student)) {
			return false;
		}
		Deque<String> out = onWifi(student) ? WIFI_OUT.get(student) : null;
		if (out != null) {
			synchronized (out) {
				if (out.size() >= OUTBOX) {
					out.removeFirst();
				}
				out.add(name + ":" + text);
			}
			return true;
		}
		server.execute(() -> {
			ServerPlayer player = server.getPlayerList().getPlayer(student);
			if (player != null && ServerPlayNetworking.canSend(player, DevicePayloads.Send.ID)) {
				ServerPlayNetworking.send(player, new DevicePayloads.Send(name, text));
			}
		});
		return true;
	}

	/** What was last sent to the student's board for {@code key} (even with no board connected), or {@code null}. */
	@Nullable
	public static String lastSent(UUID student, String key) {
		Map<String, String> sent = LAST_SENT.get(student);
		return sent == null ? null : sent.get(key);
	}

	/** Can this student's game read boards (MINE installed)? */
	public static boolean canOpen(ServerPlayer player) {
		return ServerPlayNetworking.canSend(player, DevicePayloads.Open.ID);
	}

	static void forget(UUID student) {
		VALUES.remove(student);
		CONNECTED.remove(student);
		RATE.remove(student);
		LAST_SENT.remove(student);
		WIFI_SEEN.remove(student);
		WIFI_OUT.remove(student);
	}
}
