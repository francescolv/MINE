package io.github.francescolv.mine;

import com.fazecast.jSerialComm.SerialPort;
import io.github.francescolv.mine.device.DevicePayloads;
import io.github.francescolv.mine.device.Devices;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The board plugged into the student's PC (micro:bit, Arduino, Pico…), read by MINE on its serial
 * port: one line {@code key:value} is one value for the server, which hands it to the student's
 * Agent. What the Agent sends back is written on the port the same way.
 */
final class ClientDevices {

	static final int BAUD = 115200;
	private static final int LOG_SIZE = 12;

	/** A line in MINE's window: from the board, to the board, or a note. */
	record Line(Kind kind, String text) {
	}

	enum Kind { IN, OUT, IGNORED, NOTE }

	@Nullable
	private static volatile SerialPort port;
	@Nullable
	private static volatile String portName;
	private static final ArrayDeque<Line> LOG = new ArrayDeque<>();
	/** The key the board sent last, to show how the Agent reads it. */
	@Nullable
	private static volatile String lastKey;
	/**
	 * The program on the board, as it says when it starts ({@code programma:name}): flashing a new
	 * program doesn't change anything on the serial port, so this is how the student sees it arrived.
	 */
	@Nullable
	private static volatile BoardProgram program;

	record BoardProgram(String name, LocalTime since) {
	}

	/** The keys a board's program says its name with, at its start. */
	private static final List<String> PROGRAM_KEYS = List.of("programma", "program");

	private ClientDevices() {
	}

	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(DevicePayloads.Open.ID, (payload, context) ->
				context.client().gui.setScreen(new DeviceScreen()));
		ClientPlayNetworking.registerGlobalReceiver(DevicePayloads.Send.ID, (payload, context) ->
				write(payload.key(), payload.value()));
		ClientPlayNetworking.registerGlobalReceiver(DevicePayloads.BlockOpen.ID, (payload, context) ->
				context.client().gui.setScreen(new BoardBlockScreen(payload)));
		// A board belongs to one server: leaving it closes the port.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> disconnect(false));
	}

	/** A serial port the student can choose. */
	record Choice(String name, String description, boolean usb) {
	}

	/** The serial ports of this PC, USB ones (boards) first; Linux's built-in ttyS ports are left out. */
	static List<Choice> ports() {
		List<Choice> choices = new ArrayList<>();
		try {
			for (SerialPort candidate : SerialPort.getCommPorts()) {
				String name = candidate.getSystemPortName();
				if (name.startsWith("ttyS")) {
					continue;
				}
				choices.add(new Choice(candidate.getSystemPortPath(), candidate.getDescriptivePortName(), candidate.getVendorID() != -1));
			}
		} catch (Throwable e) {
			MineMod.LOGGER.warn("[MINE] Can't list the serial ports: {}", e.toString());
		}
		choices.sort(Comparator.comparing((Choice choice) -> !choice.usb()).thenComparing(Choice::name));
		return choices;
	}

	@Nullable
	static String connectedPort() {
		return portName;
	}

	static List<Line> log() {
		synchronized (LOG) {
			return List.copyOf(LOG);
		}
	}

	@Nullable
	static String lastKey() {
		return lastKey;
	}

	private static void note(Kind kind, String text) {
		synchronized (LOG) {
			LOG.addLast(new Line(kind, text));
			while (LOG.size() > LOG_SIZE) {
				LOG.removeFirst();
			}
		}
	}

	/** Opens {@code path} and starts reading it. Returns an error to show, or {@code null}. */
	@Nullable
	static String connect(String path) {
		disconnect(false);
		SerialPort opened;
		try {
			opened = SerialPort.getCommPort(path);
			opened.setComPortParameters(BAUD, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
			opened.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 200, 0);
			if (!opened.openPort()) {
				// On Linux, almost always: the user isn't in the dialout group.
				return "open_failed";
			}
		} catch (Throwable e) {
			MineMod.LOGGER.warn("[MINE] Can't open the serial port {}: {}", path, e.toString());
			return "open_failed";
		}
		port = opened;
		portName = path;
		synchronized (LOG) {
			LOG.clear();
		}
		MineMod.LOGGER.info("[MINE] Board connected on {}", path);
		send(new DevicePayloads.Status(path));
		Thread reader = new Thread(() -> read(opened), "MINE board reader");
		reader.setDaemon(true);
		reader.start();
		return null;
	}

	/** Closes the port; {@code tell}: the server is told the board is gone (it forgets by itself when we leave). */
	static void disconnect(boolean tell) {
		SerialPort open = port;
		port = null;
		portName = null;
		lastKey = null;
		program = null;
		if (open != null) {
			open.closePort();
			MineMod.LOGGER.info("[MINE] Board disconnected");
			if (tell) {
				send(new DevicePayloads.Status(""));
			}
		}
	}

	private static void read(SerialPort from) {
		// A break restarts a micro:bit (its USB chip resets the board), as opening the port restarts
		// an Arduino Uno: the board's program starts again and says its name (programma:name).
		from.setBreak();
		sleep(100);
		from.clearBreak();
		byte[] buffer = new byte[256];
		ByteArrayOutputStream line = new ByteArrayOutputStream();
		// The first line may begin with noise (a board restarting) or be cut (it was already
		// writing): what comes before its key is dropped. Not the whole line: a board that doesn't
		// restart (Uno R4, Pico) hands over what it wrote at its start, programma:name first.
		boolean firstLine = true;
		long second = -1;
		int inSecond = 0;
		while (port == from) {
			int count = from.readBytes(buffer, buffer.length);
			if (count < 0) {
				// Unplugged.
				if (port == from) {
					note(Kind.NOTE, "unplugged");
					Minecraft.getInstance().execute(() -> {
						if (port == from) {
							disconnect(true);
						}
					});
				}
				return;
			}
			for (int i = 0; i < count; i++) {
				byte b = buffer[i];
				if (b == '\n' || b == '\r') {
					String text = line.toString(StandardCharsets.UTF_8).strip();
					line.reset();
					if (firstLine) {
						text = text.replaceFirst("^[^A-Za-z0-9_]+", "");
					}
					if (text.isEmpty()) {
						continue;
					}
					firstLine = false;
					long now = System.currentTimeMillis() / 1000;
					if (now != second) {
						second = now;
						inSecond = 0;
					}
					if (++inSecond <= Devices.PER_SECOND) {
						received(text);
					}
				} else if (line.size() < Devices.MAX_LINE) {
					line.write(b);
				}
			}
		}
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static void received(String text) {
		int colon = text.indexOf(':');
		String key = colon < 0 ? "" : text.substring(0, colon).strip().toLowerCase(Locale.ROOT);
		if (colon <= 0 || key.length() > Devices.MAX_KEY || !key.matches("[a-z0-9_]+")) {
			note(Kind.IGNORED, text);
			return;
		}
		String value = text.substring(colon + 1).strip();
		note(Kind.IN, key + ":" + value);
		if (PROGRAM_KEYS.contains(key)) {
			program = new BoardProgram(value, LocalTime.now().withNano(0));
			Minecraft client = Minecraft.getInstance();
			client.execute(() -> {
				// The window says it in its own place; with it closed, above the hotbar.
				if (!(client.gui.screen() instanceof DeviceScreen) && client.player != null) {
					client.gui.hud.setOverlayMessage(Component.translatableWithFallback("mine.device.program_started",
							"Your board is running %s", value).withStyle(ChatFormatting.AQUA), false);
				}
			});
		} else {
			lastKey = key;
		}
		send(new DevicePayloads.Value(key, value));
	}

	@Nullable
	static BoardProgram program() {
		return program;
	}

	private static void write(String key, String value) {
		SerialPort open = port;
		if (open == null) {
			return;
		}
		byte[] bytes = (key + ":" + value + "\n").getBytes(StandardCharsets.UTF_8);
		open.writeBytes(bytes, bytes.length);
		note(Kind.OUT, key + ":" + value);
	}

	private static void send(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
		Minecraft.getInstance().execute(() -> {
			if (Minecraft.getInstance().getConnection() != null && ClientPlayNetworking.canSend(payload.type())) {
				ClientPlayNetworking.send(payload);
			}
		});
	}

	/** For the window's list: the description, or the port alone. */
	static String label(Choice choice) {
		String description = choice.description();
		String file = choice.name().substring(choice.name().lastIndexOf('/') + 1);
		return description == null || description.isBlank() || Arrays.asList(file, choice.name()).contains(description)
				? file : description + " (" + file + ")";
	}
}
