package io.github.francescolv.mine.blocks;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.agent.AgentCommand;
import io.github.francescolv.mine.agent.AgentManager;
import io.github.francescolv.mine.agent.PythonBridge;
import io.github.francescolv.mine.device.Devices;
import io.github.francescolv.mine.lesson.Lesson;
import io.github.francescolv.mine.lesson.LessonManager;
import io.github.francescolv.mine.lesson.LessonProgress;
import io.github.francescolv.mine.permission.Permissions;
import io.github.francescolv.mine.text.ServerLanguage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * MINE's block editor: a web page served by the class server, opened in the students' browser
 * (Blockly, no internet needed). Students put blocks together, see the Lua they make, and send
 * the program to their Agent, or run it straight away. The lessons check the world, so blocks
 * and Lua are the same to them.
 */
public final class BlockEditor {

	public static final int PORT = 25580;
	/** A program's name on the Agent: what students type to run it. */
	static final Pattern NAME = Pattern.compile("[a-z0-9_]{1,24}");
	private static final int MAX_PROGRAM = 64 * 1024;
	private static final int MAX_REQUEST = 512 * 1024;
	private static final Pattern FILE = Pattern.compile("(?:[a-z0-9_-]+/)*[a-z0-9_.-]+");
	private static final Gson GSON = new Gson();
	/** The version of the editor's files, for the browser's cache: MINE's, and when the server started. */
	private static final String FILES_TAG = "\"" + MineMod.VERSION + "-" + System.currentTimeMillis() + "\"";
	private static final Map<String, String> TYPES = Map.ofEntries(
			Map.entry("html", "text/html; charset=utf-8"),
			Map.entry("js", "text/javascript; charset=utf-8"),
			Map.entry("css", "text/css; charset=utf-8"),
			Map.entry("svg", "image/svg+xml"),
			Map.entry("png", "image/png"),
			Map.entry("gif", "image/gif"),
			Map.entry("mp3", "audio/mpeg"),
			Map.entry("cur", "image/x-icon"),
			Map.entry("txt", "text/plain; charset=utf-8"),
			Map.entry("mjs", "text/javascript; charset=utf-8"),
			Map.entry("wasm", "application/wasm"));

	private static HttpServer http;

	private BlockEditor() {
	}

	public static void register() {
		BlockSessions.register();
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			// A server students join: not the test server, which has no port.
			// Thonny's codes, for the editor's threads.
			ThonnyKeys.get(server);
			if (server.isDedicatedServer() && server.getPort() > 0) {
				start(server, PORT);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> stop());
	}

	/** Starts serving the editor on {@code port} (0: any free one); returns the port, or -1. */
	public static synchronized int start(MinecraftServer server, int port) {
		stop();
		try {
			http = HttpServer.create(new InetSocketAddress(port), 0);
		} catch (IOException e) {
			MineMod.LOGGER.warn("[MINE] The block editor can't use port {}: {}", port, e.getMessage());
			return -1;
		}
		http.createContext("/", exchange -> {
			try (exchange) {
				handle(server, exchange);
			} catch (RuntimeException e) {
				MineMod.LOGGER.warn("[MINE] Block editor: {}", e.toString());
			}
		});
		// Virtual threads: a Python call waits for Byte (a move, os.sleep, a redstone change) without
		// holding one of a few threads, for every student at once.
		http.setExecutor(Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("MINE block editor ", 0).factory()));
		http.start();
		MineMod.LOGGER.info("[MINE] Block editor on port {}: students open it with /mine blocks", http.getAddress().getPort());
		return http.getAddress().getPort();
	}

	public static synchronized void stop() {
		if (http != null) {
			http.stop(0);
			http = null;
		}
	}

	/** The port the editor is served on, or -1 if it isn't. */
	public static synchronized int port() {
		return http == null ? -1 : http.getAddress().getPort();
	}

	private static void handle(MinecraftServer server, HttpExchange exchange) throws IOException {
		URI uri = exchange.getRequestURI();
		String path = uri.getPath();
		switch (path) {
			case "/", "/index.html" -> file(exchange, "editor/index.html");
			case "/api/session" -> session(server, exchange, code(uri), python(uri));
			case "/api/send" -> send(server, exchange, code(uri));
			case "/api/hint" -> hint(server, exchange, code(uri), python(uri));
			case "/api/program" -> program(server, exchange, code(uri), parameter(uri, "student"), parameter(uri, "name"), python(uri));
			case "/python", "/python.html" -> file(exchange, "python/python.html");
			case "/api/py/run" -> pythonRun(server, exchange, code(uri));
			case "/api/py/call" -> pythonCall(server, exchange, code(uri));
			case "/api/py/end" -> pythonEnd(server, exchange, code(uri), false);
			case "/api/py/stop" -> pythonEnd(server, exchange, code(uri), true);
			case "/api/py/save" -> pythonSave(server, exchange, code(uri));
			case "/api/device" -> device(server, exchange, code(uri));
			default -> {
				String name = path.startsWith("/editor/") || path.startsWith("/blockly/") || path.startsWith("/python/")
						|| path.startsWith("/codemirror/") ? path.substring(1) : null;
				if (name != null && FILE.matcher(name).matches() && !name.contains("..")) {
					file(exchange, name);
				} else {
					reply(exchange, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
				}
			}
		}
	}

	/** Whether the page asking is the Python editor ({@code kind=python}): its programs and its words. */
	private static boolean python(URI uri) {
		return "python".equals(parameter(uri, "kind"));
	}

	private static BlockPrograms store(MinecraftServer server, boolean python) {
		return python ? BlockPrograms.python(server) : BlockPrograms.get(server);
	}

	@Nullable
	private static String code(URI uri) {
		return parameter(uri, "code");
	}

	@Nullable
	private static String parameter(URI uri, String name) {
		String query = uri.getRawQuery();
		if (query == null) {
			return null;
		}
		for (String part : query.split("&")) {
			if (part.startsWith(name + "=")) {
				return URLDecoder.decode(part.substring(name.length() + 1), StandardCharsets.UTF_8);
			}
		}
		return null;
	}

	private static void file(HttpExchange exchange, String name) throws IOException {
		try (InputStream in = BlockEditor.class.getResourceAsStream("/web/" + name)) {
			if (in == null) {
				reply(exchange, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
				return;
			}
			// The editor's files only change with MINE (or a restart, while developing it): the
			// browser keeps them, and asks each time whether they are still the same.
			exchange.getResponseHeaders().set("Cache-Control", "no-cache");
			exchange.getResponseHeaders().set("ETag", FILES_TAG);
			if (FILES_TAG.equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
				exchange.sendResponseHeaders(304, -1);
				return;
			}
			String type = TYPES.getOrDefault(name.substring(name.lastIndexOf('.') + 1), "application/octet-stream");
			reply(exchange, 200, type, in.readAllBytes());
		}
	}

	/** Who the student is, their language and their last program. */
	private static void session(MinecraftServer server, HttpExchange exchange, @Nullable String code, boolean python) throws IOException {
		JsonObject answer = onServer(server, () -> {
			ServerPlayer player = player(server, code);
			if (player == null) {
				return null;
			}
			JsonObject json = new JsonObject();
			json.addProperty("player", player.getName().getString());
			json.addProperty("language", player.clientInformation().language().startsWith("it") ? "it" : "en");
			json.addProperty("id", player.getUUID().toString());
			json.addProperty("thonny", ThonnyKeys.get(server).keyFor(player.getUUID()));
			BlockPrograms programs = store(server, python);
			BlockPrograms.Student mine = programs.student(player.getUUID());
			JsonArray names = new JsonArray();
			if (mine != null) {
				json.addProperty("name", mine.last());
				json.addProperty("workspace", mine.programs().getOrDefault(mine.last(), ""));
				BlockPrograms.names(mine).forEach(names::add);
			}
			json.add("programs", names);
			json.add("lesson", lesson(player, python ? Lesson.Language.PYTHON : Lesson.Language.BLOCKS));
			// Teachers can open their students' programs, to help them.
			if (Permissions.isTeacher(player)) {
				JsonArray students = new JsonArray();
				programs.all().entrySet().stream()
						.filter(entry -> !entry.getKey().equals(player.getUUID()))
						.sorted(Comparator.comparing(entry -> entry.getValue().name(), String.CASE_INSENSITIVE_ORDER))
						.forEach(entry -> {
							JsonObject student = new JsonObject();
							student.addProperty("id", entry.getKey().toString());
							student.addProperty("name", entry.getValue().name());
							JsonArray theirs = new JsonArray();
							BlockPrograms.names(entry.getValue()).forEach(theirs::add);
							student.add("programs", theirs);
							students.add(student);
						});
				json.add("students", students);
			}
			return json;
		});
		json(exchange, answer);
	}

	/**
	 * The student's lesson, as the editor shows it: title, step, goal and the hints revealed so
	 * far, in {@code language}'s words; {@code null} if they have no lesson.
	 */
	@Nullable
	static JsonObject lesson(ServerPlayer player, Lesson.Language language) {
		UUID uuid = player.getUUID();
		Lesson lesson = LessonManager.current(player.level().getServer(), uuid);
		LessonProgress.Entry entry = LessonProgress.get(player.level().getServer()).get(uuid);
		if (lesson == null || entry == null) {
			return null;
		}
		JsonObject json = new JsonObject();
		json.addProperty("title", ServerLanguage.render(lesson.titleText(), player));
		json.addProperty("steps", lesson.steps().size());
		if (entry.status() == LessonProgress.Status.COMPLETED) {
			json.addProperty("completed", true);
			json.addProperty("reflection", ServerLanguage.render(lesson.reflectionText(), player));
			return json;
		}
		int step = entry.step();
		json.addProperty("step", step + 1);
		json.addProperty("goal", ServerLanguage.render(lesson.goalText(step, language), player));
		int total = lesson.hintCount(step, language);
		JsonArray hints = new JsonArray();
		for (int n = 0; n < Math.min(entry.hints(), total); n++) {
			hints.add(ServerLanguage.render(lesson.hintText(step, n, language), player));
		}
		json.add("hints", hints);
		json.addProperty("more", entry.hints() < total);
		return json;
	}

	/** Reveals the next hint of the student's step; answers with their lesson, as {@link #lesson}. */
	private static void hint(MinecraftServer server, HttpExchange exchange, @Nullable String code, boolean python) throws IOException {
		if (!"POST".equals(exchange.getRequestMethod())) {
			reply(exchange, 405, "text/plain; charset=utf-8", new byte[0]);
			return;
		}
		json(exchange, onServer(server, () -> {
			ServerPlayer player = player(server, code);
			if (player == null) {
				return null;
			}
			Lesson.Language language = python ? Lesson.Language.PYTHON : Lesson.Language.BLOCKS;
			LessonManager.nextHint(player, language);
			JsonObject answer = new JsonObject();
			answer.add("lesson", lesson(player, language));
			return answer;
		}));
	}

	/** One saved program: the player's own, or (for a teacher) a student's. */
	private static void program(MinecraftServer server, HttpExchange exchange, @Nullable String code,
			@Nullable String student, @Nullable String name, boolean python) throws IOException {
		json(exchange, onServer(server, () -> {
			ServerPlayer player = player(server, code);
			if (player == null) {
				return null;
			}
			UUID owner;
			try {
				owner = student == null || student.isEmpty() ? player.getUUID() : UUID.fromString(student);
			} catch (IllegalArgumentException e) {
				owner = null;
			}
			JsonObject answer = new JsonObject();
			BlockPrograms.Student theirs = owner == null ? null : store(server, python).student(owner);
			boolean allowed = owner != null && (owner.equals(player.getUUID()) || Permissions.isTeacher(player));
			if (allowed && theirs != null && name != null && theirs.programs().containsKey(name)) {
				answer.addProperty("name", name);
				answer.addProperty("owner", theirs.name());
				answer.addProperty("workspace", theirs.programs().get(name));
			}
			return answer;
		}));
	}

	/** The program to send to the student's Agent: {@code name}, {@code lua}, {@code workspace}, {@code run}. */
	private static void send(MinecraftServer server, HttpExchange exchange, @Nullable String code) throws IOException {
		if (!"POST".equals(exchange.getRequestMethod())) {
			reply(exchange, 405, "text/plain; charset=utf-8", new byte[0]);
			return;
		}
		JsonObject sent = body(exchange);
		if (sent == null) {
			reply(exchange, 400, "text/plain; charset=utf-8", new byte[0]);
			return;
		}
		json(exchange, onServer(server, () -> {
			ServerPlayer player = player(server, code);
			return player == null ? null : send(player, string(sent, "name"), string(sent, "lua"), string(sent, "workspace"),
					sent.has("run") && sent.get("run").getAsBoolean());
		}));
	}

	/** What happens when a student sends a program: the answer to show in the editor. */
	static JsonObject send(ServerPlayer player, String name, String lua, String workspace, boolean run) {
		if (NAME.matcher(name).matches() && lua.length() <= MAX_PROGRAM) {
			BlockPrograms.get(player.level().getServer()).put(player.getUUID(), player.getName().getString(), name, workspace);
		}
		return deliver(player, name, lua, run);
	}

	/** A program for the student's Byte, from the blocks or the Python bridge: the answer to show in the editor. */
	private static JsonObject deliver(ServerPlayer player, String name, String lua, boolean run) {
		JsonObject answer = new JsonObject();
		Component message;
		boolean ok = false;
		if (!NAME.matcher(name).matches()) {
			message = Component.translatableWithFallback("mine.blocks.bad_name",
					"The program's name may only have small letters, digits and _ (at most 24 characters).");
		} else if (lua.length() > MAX_PROGRAM) {
			message = Component.translatableWithFallback("mine.blocks.too_big", "The program is too big.");
		} else {
			// Run: the student is looking at the browser, not at the Agent. Their MINE starts the
			// program when they are back in the game, after a countdown.
			boolean later = run && ServerPlayNetworking.canSend(player, BlocksCommand.RunWhenBack.ID);
			AgentManager.Result result = AgentManager.send(player, name, lua,
					later ? AgentManager.Delivery.HOLD : run ? AgentManager.Delivery.RUN : AgentManager.Delivery.OFFER);
			ok = result == AgentManager.Result.SENT || result == AgentManager.Result.STARTED || result == AgentManager.Result.STARTING;
			if (later && result == AgentManager.Result.SENT) {
				ServerPlayNetworking.send(player, new BlocksCommand.RunWhenBack(name));
			}
			message = later && result == AgentManager.Result.SENT
					? Component.translatableWithFallback("mine.blocks.go_back", "Go back to Minecraft: %s starts as soon as you are there.", name)
					: switch (result) {
				case SENT -> Component.translatableWithFallback("mine.blocks.sent", "Sent to your Byte: type %s in its terminal to run it.", name);
				case STARTED, STARTING -> Component.translatableWithFallback("mine.blocks.started", "Your Byte is running %s.", name);
				case BUSY -> Component.translatableWithFallback("mine.blocks.busy",
						"Sent, but your Byte is busy: stop it by holding Ctrl+T for a second in its terminal, then run %s.", name);
				case OFF -> Component.translatableWithFallback("mine.blocks.off",
						"Sent: open your Byte (right-click it) to turn it on, then run %s.", name);
				default -> noAgent();
			};
			MineMod.LOGGER.info("[MINE] {} sent the program {} from the blocks ({})", player.getName().getString(), name,
					result.name().toLowerCase());
		}
		answer.addProperty("ok", ok);
		answer.addProperty("message", ServerLanguage.render(message, player));
		return answer;
	}

	private static Component noAgent() {
		return Component.translatableWithFallback("mine.blocks.no_agent", "You have no Byte yet: in the game, type /mine byte.");
	}

	@Nullable
	private static JsonObject body(HttpExchange exchange) throws IOException {
		if (!"POST".equals(exchange.getRequestMethod())) {
			return null;
		}
		byte[] body = exchange.getRequestBody().readNBytes(MAX_REQUEST + 1);
		try {
			return body.length > MAX_REQUEST ? null : JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (RuntimeException e) {
			return null;
		}
	}

	/** Run, for a Python program: Byte gets the bridge (as the program {@code name}) and starts it as for blocks. */
	private static void pythonRun(MinecraftServer server, HttpExchange exchange, @Nullable String code) throws IOException {
		JsonObject request = body(exchange);
		if (request == null) {
			reply(exchange, 400, "text/plain; charset=utf-8", new byte[0]);
			return;
		}
		UUID student = who(code);
		json(exchange, student == null ? null : onServer(server, () -> {
			ServerPlayer player = server.getPlayerList().getPlayer(student);
			if (player == null) {
				// Thonny's code is the student's for good, but Byte only listens while they play.
				JsonObject answer = new JsonObject();
				answer.addProperty("ok", false);
				answer.addProperty("reason", "offline");
				return answer;
			}
			String name = string(request, "name");
			String source = string(request, "source");
			if (NAME.matcher(name).matches()) {
				PythonBridge.open(player.getUUID());
				if (!source.isEmpty() && source.length() <= MAX_PROGRAM) {
					BlockPrograms.python(server).put(player.getUUID(), player.getName().getString(), name, source);
				}
			}
			return deliver(player, name, NAME.matcher(name).matches() ? PythonBridge.bridge(name) : "", true);
		}));
	}

	/** Save, for a Python program: kept with the student's Python programs, not sent to Byte. */
	private static void pythonSave(MinecraftServer server, HttpExchange exchange, @Nullable String code) throws IOException {
		JsonObject request = body(exchange);
		if (request == null) {
			reply(exchange, 400, "text/plain; charset=utf-8", new byte[0]);
			return;
		}
		json(exchange, onServer(server, () -> {
			ServerPlayer player = player(server, code);
			if (player == null) {
				return null;
			}
			String name = string(request, "name");
			String source = string(request, "source");
			JsonObject answer = new JsonObject();
			boolean ok = NAME.matcher(name).matches() && source.length() <= MAX_PROGRAM;
			if (ok) {
				BlockPrograms.python(server).put(player.getUUID(), player.getName().getString(), name, source);
			}
			answer.addProperty("ok", ok);
			answer.addProperty("message", ServerLanguage.render(ok
					? Component.translatableWithFallback("mine.python.saved", "Saved.")
					: Component.translatableWithFallback("mine.blocks.bad_name",
							"The program's name may only have small letters, digits and _ (at most 24 characters)."), player));
			return answer;
		}));
	}

	/**
	 * A board on Wi-Fi (a Pico 2 W) asks, with the student's code for Thonny: {@code values}, the
	 * lines {@code key:value} it says, as over a cable; {@code send}, the lines for it. Only while
	 * the student is in the game ({@code reason: offline} otherwise), as for Thonny.
	 */
	private static void device(MinecraftServer server, HttpExchange exchange, @Nullable String code) throws IOException {
		JsonObject request = body(exchange);
		UUID student = who(code);
		if (request == null || student == null) {
			json(exchange, null);
			return;
		}
		List<String> values = new ArrayList<>();
		if (request.has("values") && request.get("values").isJsonArray()) {
			request.getAsJsonArray("values").forEach(value -> {
				if (value.isJsonPrimitive() && values.size() < Devices.PER_SECOND) {
					values.add(value.getAsString());
				}
			});
		}
		json(exchange, onServer(server, () -> {
			ServerPlayer player = server.getPlayerList().getPlayer(student);
			JsonObject answer = new JsonObject();
			if (player == null) {
				answer.addProperty("reason", "offline");
				return answer;
			}
			JsonArray send = new JsonArray();
			Devices.wifi(player, values).forEach(send::add);
			answer.add("send", send);
			return answer;
		}));
	}

	/** One call of the Python program, answered by Byte: this request waits for it, off the server thread. */
	private static void pythonCall(MinecraftServer server, HttpExchange exchange, @Nullable String code) throws IOException {
		JsonObject request = body(exchange);
		UUID student = who(code);
		if (request == null || student == null) {
			json(exchange, null);
			return;
		}
		String arguments = request.has("args") && request.get("args").isJsonArray() ? request.get("args").toString() : "[]";
		json(exchange, PythonBridge.call(server, student, string(request, "fn"), arguments));
	}

	/** The Python program ended ({@code error}: why, or empty), or Stop was pressed. */
	private static void pythonEnd(MinecraftServer server, HttpExchange exchange, @Nullable String code, boolean stop) throws IOException {
		JsonObject request = body(exchange);
		UUID student = who(code);
		if (request == null || student == null) {
			json(exchange, null);
			return;
		}
		if (stop) {
			PythonBridge.stop(student);
		} else {
			PythonBridge.finish(student, string(request, "error"));
		}
		JsonObject answer = new JsonObject();
		answer.addProperty("ok", true);
		json(exchange, answer);
	}

	/**
	 * The student is back in the game and starts {@code name}, or with the key ({@code name}
	 * empty) the last program their Agent ran, typed in its terminal, from the chat or from the
	 * blocks; else the last one sent from the blocks. Called on the server thread.
	 */
	static void go(ServerPlayer player, String name) {
		BlockPrograms.Student saved = BlockPrograms.get(player.level().getServer()).student(player.getUUID());
		String last = AgentManager.lastRun(player.level().getServer(), player.getUUID());
		String program = !name.isEmpty() ? name : last != null ? last : saved != null ? saved.last() : "";
		if (program.isBlank() || (!name.isEmpty() && !NAME.matcher(name).matches())) {
			player.sendSystemMessage(Component.translatableWithFallback("mine.blocks.nothing_to_run",
					"Run a program on your Byte first (in its terminal, or from the blocks with /mine blocks): then R runs it again.")
					.withStyle(ChatFormatting.GOLD));
			return;
		}
		AgentManager.Result result = AgentManager.runOrStart(player, program);
		MineMod.LOGGER.info("[MINE] {} ran {} from the blocks ({})", player.getName().getString(), program, result.name().toLowerCase());
		if (result == AgentManager.Result.BUSY && name.isEmpty()) {
			// The program typed in the terminal is still going (e.g. listening for redstone): nothing to start again.
			player.sendSystemMessage(Component.translatableWithFallback("mine.blocks.still_running",
					"Your Byte is still running its program: watch it! To stop it, hold Ctrl+T for a second in its terminal.")
					.withStyle(ChatFormatting.GOLD));
			return;
		}
		Component problem = switch (result) {
			case STARTED, STARTING -> null;
			case BUSY, OFF -> AgentCommand.message(result);
			default -> noAgent();
		};
		if (problem != null) {
			player.sendSystemMessage(problem.copy().withStyle(ChatFormatting.RED));
		}
	}

	private static String string(JsonObject json, String key) {
		return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : "";
	}

	/**
	 * Who a code stands for: the editor's link code (while the student is in the game) or the
	 * student's code for Thonny ({@link ThonnyKeys}).
	 */
	@Nullable
	static UUID who(@Nullable String code) {
		UUID player = BlockSessions.player(code);
		return player != null ? player : ThonnyKeys.student(code);
	}

	@Nullable
	private static ServerPlayer player(MinecraftServer server, @Nullable String code) {
		UUID player = who(code);
		return player == null ? null : server.getPlayerList().getPlayer(player);
	}

	/** Runs {@code task} on the server thread: the world and the players are only safe there. */
	@Nullable
	private static JsonObject onServer(MinecraftServer server, java.util.function.Supplier<JsonObject> task) {
		try {
			return server.submit(task).get(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			MineMod.LOGGER.warn("[MINE] Block editor: {}", e.toString());
			return null;
		}
	}

	/** {@code null}: the code isn't a student's in the game (they left, or the server restarted). */
	private static void json(HttpExchange exchange, @Nullable JsonObject json) throws IOException {
		if (json == null) {
			reply(exchange, 403, "application/json", "{\"expired\":true}".getBytes(StandardCharsets.UTF_8));
		} else {
			reply(exchange, 200, "application/json", GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
		}
	}

	private static void reply(HttpExchange exchange, int status, String type, byte[] body) throws IOException {
		exchange.getResponseHeaders().set("Content-Type", type);
		// Answers of the API are never kept: they change with the game.
		if (!exchange.getResponseHeaders().containsKey("Cache-Control")) {
			exchange.getResponseHeaders().set("Cache-Control", "no-store");
		}
		exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
		if (body.length > 0) {
			exchange.getResponseBody().write(body);
		}
	}
}
