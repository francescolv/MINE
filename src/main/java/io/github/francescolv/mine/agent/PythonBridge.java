package io.github.francescolv.mine.agent;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.francescolv.mine.MineMod;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Python on Byte: the student's Python program runs in their browser (MINE's editor), and each
 * call ({@code byte.forward()}, {@code redstone.getInput("left")}…) is run on Byte by a small Lua
 * program, the bridge, which is a program of the student like any other: lessons check it,
 * Ctrl+T and MINE stop it.
 *
 * <p>The browser asks the server ({@link #call}); the server queues a {@code mine_py} event on
 * Byte; the bridge runs the function and answers ({@code mine._pyReply}). One call at a time per
 * student: the browser waits for each answer. Thread-safe: calls wait on the editor's threads,
 * answers come from Byte's computer thread.
 */
public final class PythonBridge {

	/** How long the first call waits for the bridge to start: the student goes back to the game, 3-2-1. */
	private static final long START_SECONDS = 180;

	private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
	private static final Map<String, Session> BY_TOKEN = new ConcurrentHashMap<>();

	/** One run of a Python program: from Run in the editor to its end. */
	private static final class Session {
		final UUID student;
		final String token = UUID.randomUUID().toString();
		/** Byte's computer, once the bridge has started on it. */
		final CompletableFuture<Integer> ready = new CompletableFuture<>();
		final Map<Integer, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
		final AtomicInteger next = new AtomicInteger();
		volatile boolean ended;
		volatile int computer = -1;

		Session(UUID student) {
			this.student = student;
		}

		void end() {
			ended = true;
			BY_TOKEN.remove(token);
			SESSIONS.remove(student, this);
			ready.completeExceptionally(new IllegalStateException("ended"));
			pending.values().forEach(call -> call.complete(stopped()));
			pending.clear();
		}
	}

	private PythonBridge() {
	}

	/** The bridge's Lua, sent to Byte as the program {@code name}. */
	public static String bridge(String name) {
		try (InputStream in = PythonBridge.class.getResourceAsStream("/web/python/bridge.lua")) {
			return new String(Objects.requireNonNull(in, "bridge.lua").readAllBytes(), StandardCharsets.UTF_8)
					.replace("%NAME%", name);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** Run was pressed: a new run, which waits for the bridge. An older run of the student ends. */
	public static void open(UUID student) {
		Session old = SESSIONS.put(student, new Session(student));
		if (old != null) {
			old.end();
		}
	}

	/**
	 * A call from the student's Python program: {@code function} (e.g. {@code "byte.forward"}) with
	 * its arguments as a JSON array. Blocks until Byte answers: {@code {"ok": true, "values": [...]}},
	 * {@code {"ok": false, "error": "..."}}, or {@code {"stopped": true}} when the bridge ended.
	 * Not on the server thread.
	 */
	public static JsonObject call(MinecraftServer server, UUID student, String function, String arguments) {
		Session session = SESSIONS.get(student);
		if (session == null) {
			return stopped();
		}
		int computer;
		try {
			computer = session.ready.get(START_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			session.end();
			return error("Byte didn't start the program: run it again, and go back to Minecraft.");
		} catch (Exception e) {
			return stopped();
		}
		int id = session.next.incrementAndGet();
		CompletableFuture<JsonObject> answer = new CompletableFuture<>();
		session.pending.put(id, answer);
		if (session.ended) {
			return stopped();
		}
		AgentTracker.queueEvent(computer, "mine_py", id, function, arguments);
		try {
			return answer.get();
		} catch (Exception e) {
			return stopped();
		} finally {
			session.pending.remove(id);
		}
	}

	/** The Python program ended: by itself ({@code error} empty) or with an error, which Byte shows. */
	public static void finish(UUID student, String error) {
		Session session = SESSIONS.get(student);
		if (session == null) {
			return;
		}
		// The bridge may still be starting: it gets the end when it's ready.
		session.ready.thenAccept(computer -> AgentTracker.queueEvent(computer, "mine_py", 0, "end", error));
	}

	/** Stop was pressed in the editor: Byte stops, the Python program too. */
	public static void stop(UUID student) {
		Session session = SESSIONS.get(student);
		if (session != null) {
			session.ready.thenAccept(AgentTracker::stop);
			session.end();
		}
	}

	/** {@code mine._pyReady()}: the bridge started on Byte {@code computer}; its token, or null if no run waits. */
	@Nullable
	static String ready(@Nullable UUID student, int computer) {
		Session session = student == null ? null : SESSIONS.get(student);
		if (session == null || session.ended || session.computer >= 0) {
			return null;
		}
		BY_TOKEN.put(session.token, session);
		session.computer = computer;
		MineMod.LOGGER.info("[MINE] Python bridge started on Byte {}", computer);
		return session.token;
	}

	/**
	 * {@code mine._pyListening(token)}: the bridge is about to wait for calls. Only now the calls
	 * waiting are sent: an event that arrives during {@code mine._pyReady()} (a call on the main
	 * thread) is thrown away by CC: Tweaked.
	 */
	static void listening(String token) {
		Session session = BY_TOKEN.get(token);
		if (session != null && session.computer >= 0) {
			session.ready.complete(session.computer);
		}
	}

	/** {@code mine._pyReply(token, id, ok, values)}: Byte's answer to a call. */
	static void reply(String token, int id, boolean ok, String values) {
		Session session = BY_TOKEN.get(token);
		CompletableFuture<JsonObject> call = session == null ? null : session.pending.get(id);
		if (call == null) {
			return;
		}
		JsonArray array = new JsonArray();
		try {
			JsonElement parsed = JsonParser.parseString(values);
			// CC: Tweaked writes an empty table as {}: no values.
			if (parsed.isJsonArray()) {
				array = parsed.getAsJsonArray();
			}
		} catch (RuntimeException e) {
			ok = false;
			array.add("Byte's answer can't be read");
		}
		JsonObject answer = new JsonObject();
		answer.addProperty("ok", ok);
		if (ok) {
			answer.add("values", array);
		} else {
			answer.addProperty("error", array.isEmpty() || !array.get(0).isJsonPrimitive() ? "error" : array.get(0).getAsString());
		}
		call.complete(answer);
	}

	/** {@code mine._pyEnd(token)}: the bridge ended (finished, stopped, an error). */
	static void ended(String token) {
		Session session = BY_TOKEN.get(token);
		if (session != null) {
			session.end();
		}
	}

	/**
	 * A program on Byte {@code computer} ended: a run whose bridge was there ends too, whatever
	 * ended it, so its waiting calls are answered (stopped) instead of waiting forever.
	 */
	public static void programEnded(int computer) {
		for (Session session : List.copyOf(SESSIONS.values())) {
			if (session.computer == computer) {
				session.end();
			}
		}
	}

	/** The student left: their run ends. */
	public static void forget(UUID student) {
		Session session = SESSIONS.get(student);
		if (session != null) {
			session.end();
		}
	}

	private static JsonObject stopped() {
		JsonObject answer = new JsonObject();
		answer.addProperty("stopped", true);
		return answer;
	}

	private static JsonObject error(String message) {
		JsonObject answer = new JsonObject();
		answer.addProperty("ok", false);
		answer.addProperty("error", message);
		return answer;
	}
}
