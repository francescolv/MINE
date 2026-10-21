package io.github.francescolv.mine.gametest;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.blocks.BlockPrograms;
import io.github.francescolv.mine.blocks.BlockSessions;
import io.github.francescolv.mine.blocks.ThonnyKeys;
import io.github.francescolv.mine.lesson.LessonManager;
import io.github.francescolv.mine.lesson.LessonProgress;
import io.github.francescolv.mine.lesson.Lessons;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Python on Byte, as the editor's page drives it: Run sends the bridge to Byte and starts it, then
 * each call of the Python program is a request that waits for Byte's answer. Here the test plays
 * the browser (MicroPython itself is tried in Node and in the browser).
 */
public class PythonGameTest {

	/** Where Byte appears: the student stands north of it, facing south (+Z). */
	private static final BlockPos BYTE = new BlockPos(2, 1, 1);

	@GameTest(maxTicks = LessonGameTest.TICKS)
	public void pythonCallsRunOnByte(GameTestHelper ctx) {
		int port = BlockEditorGameTest.editor(ctx.getLevel().getServer());
		ServerPlayer student = ctx.makeMockServerPlayerInLevel();
		ServerComputer computer = GameTests.spawnAgent(ctx, student, BYTE).createServerComputer();
		computer.turnOn();
		LessonManager.start(student, Lessons.get("first_steps"));
		String base = "http://127.0.0.1:" + port;
		String code = BlockSessions.codeFor(student.getUUID());
		HttpClient http = HttpClient.newHttpClient();

		List<String> answers = new ArrayList<>();
		long[] perCall = { 0 };
		CompletableFuture<?>[] browser = { null };
		CompletableFuture<HttpResponse<String>>[] run = new CompletableFuture[1];
		int[] ticks = { 0 };
		ctx.onEachTick(() -> {
			ticks[0]++;
			// Byte boots, then Run, again until Byte is waiting for a command.
			if (browser[0] == null && ticks[0] % 20 == 0 && (run[0] == null || run[0].isDone())) {
				if (run[0] != null && run[0].join().body().contains("\"ok\":true")) {
					browser[0] = CompletableFuture.runAsync(() -> {
						answers.add(call(http, base, code, "byte.forward", "[]"));
						long start = System.nanoTime();
						for (int i = 0; i < 10; i++) {
							answers.add(call(http, base, code, "redstone.getInput", "[\"left\"]"));
						}
						perCall[0] = (System.nanoTime() - start) / 10_000_000;
						answers.add(call(http, base, code, "os.execute", "[\"rm\"]"));
						answers.add(call(http, base, code, "byte._hidden", "[]"));
						answers.add(call(http, base, code, "mine.say", "[\"Ciao da Python\"]"));
						post(http, base + "/api/py/end?code=" + code, "{\"error\": \"\"}");
						// After the end, Byte no longer answers: the program is told it stopped.
						answers.add(call(http, base, code, "byte.forward", "[]"));
					});
				} else {
					run[0] = http.sendAsync(GameTests.post(base + "/api/py/run?code=" + code, "{\"name\": \"prova\"}"),
							HttpResponse.BodyHandlers.ofString());
				}
			}
		});
		ctx.succeedWhen(() -> {
			ctx.assertTrue(browser[0] != null && browser[0].isDone(), "Waiting for the program");
			browser[0].join();
			ctx.assertTrue(answers.get(0).equals("{\"ok\":true,\"values\":[true]}"), "byte.forward(): " + answers.get(0));
			ctx.assertTrue(answers.get(1).equals("{\"ok\":true,\"values\":[false]}"), "redstone.getInput: " + answers.get(1));
			ctx.assertTrue(answers.get(11).contains("one of Byte"), "os.execute: " + answers.get(11));
			ctx.assertTrue(answers.get(12).contains("one of Byte"), "a hidden function: " + answers.get(12));
			ctx.assertTrue(answers.get(13).contains("\"ok\":true"), "mine.say: " + answers.get(13));
			ctx.assertTrue(answers.get(14).equals("{\"stopped\":true}"), "after the end: " + answers.get(14));
			ctx.assertTrue(ctx.getBlockEntity(BYTE.south(), TurtleBlockEntity.class) != null, "Byte moved forward");
			LessonProgress.Entry entry = LessonProgress.get(ctx.getLevel().getServer()).get(student.getUUID());
			ctx.assertTrue(entry != null && entry.step() == 1, "Lesson 1's first step, done in Python: " + entry);
			MineMod.LOGGER.info("[MINE test] Python: {} ms per call to Byte", perCall[0]);
			BlockSessions.forget(student.getUUID());
		});
	}

	/**
	 * A real Python program, run by MicroPython as in the editor (tools/python-run.mjs, in Node):
	 * lesson 1's tower, built from Python, passes the lesson's check.
	 */
	@GameTest(maxTicks = LessonGameTest.TICKS * 4)
	public void aPythonProgramBuildsLessonOnesTower(GameTestHelper ctx) {
		int port = BlockEditorGameTest.editor(ctx.getLevel().getServer());
		ServerPlayer student = ctx.makeMockServerPlayerInLevel();
		TurtleBlockEntity turtle = GameTests.spawnAgent(ctx, student, BYTE);
		turtle.createServerComputer().turnOn();
		turtle.getAccess().getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 3));
		LessonManager.start(student, Lessons.get("first_steps"));
		LessonProgress.get(ctx.getLevel().getServer()).put(student.getUUID(), new LessonProgress.Entry("first_steps", 2, 0, 1, 0));
		String code = BlockSessions.codeFor(student.getUUID());

		CompletableFuture<String> node = CompletableFuture.supplyAsync(() -> {
			try {
				Path program = Files.createTempFile("tower", ".py");
				try (InputStream in = PythonGameTest.class.getResourceAsStream("/python/tower.py")) {
					Files.write(program, in.readAllBytes());
				}
				Path runner = LessonGameTest.LESSONS.resolve("../../tools/python-run.mjs").toAbsolutePath().normalize();
				Process process = new ProcessBuilder("node", runner.toString(), "http://127.0.0.1:" + port, code, "torre",
						program.toString()).redirectErrorStream(true).start();
				String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
				return "exit " + process.waitFor() + "\n" + output;
			} catch (Exception e) {
				return "exit -1\n" + e;
			}
		});
		ctx.succeedWhen(() -> {
			ctx.assertTrue(node.isDone(), "Waiting for Python");
			String output = node.join();
			ctx.assertTrue(output.startsWith("exit 0") && output.contains("done " + MineMod.VERSION), "Python: " + output);
			LessonProgress.Status status = LessonProgress.get(ctx.getLevel().getServer()).status(student.getUUID());
			ctx.assertTrue(status == LessonProgress.Status.COMPLETED, "Lesson 1 completed with a Python tower: " + status);
			BlockSessions.forget(student.getUUID());
		});
	}

	/**
	 * Python on the PC, as with Thonny: the student downloads their mine.py from the editor and
	 * runs a program with the PC's own Python 3, which drives Byte through the same server.
	 */
	@GameTest(maxTicks = LessonGameTest.TICKS * 4)
	public void thonnyDrivesByteFromThePc(GameTestHelper ctx) {
		int port = BlockEditorGameTest.editor(ctx.getLevel().getServer());
		ServerPlayer student = ctx.makeMockServerPlayerInLevel();
		GameTests.spawnAgent(ctx, student, BYTE).createServerComputer().turnOn();
		String base = "http://127.0.0.1:" + port;
		// The student's code for Thonny, the same every day: mine.py remembers it in mine.txt.
		String key = ThonnyKeys.get(ctx.getLevel().getServer()).keyFor(student.getUUID());

		CompletableFuture<String> python = CompletableFuture.supplyAsync(() -> {
			try {
				Path dir = Files.createTempDirectory("thonny");
				HttpResponse<String> module = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
						URI.create(base + "/python/thonny/mine.py")).build(), HttpResponse.BodyHandlers.ofString());
				Files.writeString(dir.resolve("mine.py"), module.body());
				Files.writeString(dir.resolve("mine.txt"), "server=127.0.0.1:" + port + "\ncode=" + key.toLowerCase() + "\n");
				Files.writeString(dir.resolve("prova_pc.py"), """
						from mine import byte
						import mine
						mine.say("Hello from Thonny")
						print("moved", byte.forward())
						""");
				String output = "";
				// Byte may still be starting up: Run again, as a student would.
				for (int attempt = 0; attempt < 40; attempt++) {
					Process process = new ProcessBuilder("python3", "prova_pc.py").directory(dir.toFile())
							.redirectErrorStream(true).start();
					output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
					int exit = process.waitFor();
					if (exit == 0 || !output.contains("busy")) {
						return "exit " + exit + "\n" + output;
					}
					Thread.sleep(500);
				}
				return "exit busy\n" + output;
			} catch (Exception e) {
				return "exit -1\n" + e;
			}
		});
		ctx.succeedWhen(() -> {
			ctx.assertTrue(python.isDone(), "Waiting for Python on the PC");
			String output = python.join();
			ctx.assertTrue(output.startsWith("exit 0") && output.contains("moved True"), "Python 3: " + output);
			ctx.assertTrue(ctx.getBlockEntity(BYTE.south(), TurtleBlockEntity.class) != null, "Byte moved forward");
			ctx.assertTrue(BlockPrograms.python(ctx.getLevel().getServer()).program(student.getUUID(), "prova_pc") != null,
					"The program is kept with the student's Python programs");
			BlockSessions.forget(student.getUUID());
		});
	}

	/**
	 * The Python lessons with their solutions, run by MicroPython as in the editor, each in English
	 * and then again in Italian (in {@code solutions/it}): MINE plays the board (and the world
	 * around Byte), Byte answers. One test, one after the other: mock players share their name (and
	 * so their id, and their Byte) between tests running together. Lesson 9 last: its charts stay.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = 3_000_000)
	public void pythonLessons(GameTestHelper ctx) {
		run(ctx, List.of(
				new Lesson("greenhouse", "12-greenhouse", "average", "alarm", "report", "media", "allarme", "resoconto"),
				new Lesson("control", "13-control", "threshold", "hysteresis", "manual", "soglia", "isteresi", "manuale"),
				new Lesson("crossing", "14-crossing", "close", "reopen", "trains", "chiudi", "riapri", "treni"),
				new Lesson("cipher", "16-cipher", "caesar", "key", "crack", "cesare", "chiave", "forza"),
				new Lesson("charts", "09-charts", "chart", "largest", "light", "grafico", "massimo", "luce")));
	}

	/** A lesson, its folder in {@code docs/lessons}, its three English solutions and then the three Italian ones. */
	private record Lesson(String id, String folder, String... programs) {

		Path solution(int i) {
			Path solutions = LessonGameTest.LESSONS.resolve(folder).resolve("solutions");
			return (i < 3 ? solutions : solutions.resolve("it")).resolve(programs[i] + ".py");
		}
	}

	private static void run(GameTestHelper ctx, List<Lesson> lessons) {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				ctx.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		MinecraftServer server = ctx.getLevel().getServer();
		int port = BlockEditorGameTest.editor(server);
		ServerPlayer student = ctx.makeMockServerPlayerInLevel();
		TurtleBlockEntity turtle = GameTests.spawnAgent(ctx, student, LessonGameTest.LAB_AGENT);
		turtle.createServerComputer().turnOn();
		turtle.getAccess().getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
		String code = BlockSessions.codeFor(student.getUUID());
		Path runner = LessonGameTest.LESSONS.resolve("../../tools/python-run.mjs").toAbsolutePath().normalize();
		String[] current = { "" };

		CompletableFuture<String> run = CompletableFuture.supplyAsync(() -> {
			StringBuilder log = new StringBuilder();
			try {
				for (Lesson lesson : lessons) {
					for (int i = 0; i < 6; i++) {
						if (i % 3 == 0) {
							// A lesson from the start, once Byte has finished the one before (the Python side
							// ends a moment before Byte's program, which MINE then checks).
							for (int wait = 0; wait < 600 && !current[0].isEmpty() && server.submit(() -> LessonProgress.get(server)
									.status(student.getUUID())).join() != LessonProgress.Status.COMPLETED; wait++) {
								Thread.sleep(100);
							}
							if (!current[0].isEmpty() && server.submit(() -> LessonProgress.get(server).status(student.getUUID())).join()
									!= LessonProgress.Status.COMPLETED) {
								log.append("Lesson ").append(current[0]).append(" not completed\n");
								return log.toString();
							}
							current[0] = lesson.id() + (i == 0 ? "" : " (it)");
							server.submit(() -> LessonManager.start(student, Lessons.get(lesson.id()))).join();
						}
						Process process = new ProcessBuilder("node", runner.toString(), "http://127.0.0.1:" + port, code, lesson.programs()[i],
								lesson.solution(i).toString()).redirectErrorStream(true).start();
						String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
						log.append(lesson.programs()[i]).append(" exit ").append(process.waitFor()).append(": ").append(output).append('\n');
						// The next program may need Byte's planks again.
						turtle.getAccess().getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
					}
				}
			} catch (Exception e) {
				log.append(e);
			}
			return log.toString();
		});
		long[] over = { 0 };
		ctx.onEachTick(() -> {
			if (!run.isDone()) {
				return;
			}
			// The programs are over: Byte finishes the last one a moment later, and MINE checks it. Then
			// the lesson is completed, or it won't be: no waiting until the tick limit.
			if (over[0] == 0) {
				over[0] = System.currentTimeMillis();
			}
			LessonProgress.Status status = LessonProgress.get(server).status(student.getUUID());
			if (status != LessonProgress.Status.COMPLETED && System.currentTimeMillis() - over[0] < 20_000) {
				return;
			}
			BlockSessions.forget(student.getUUID());
			String log = run.join();
			if (status == LessonProgress.Status.COMPLETED && !log.contains("not completed")) {
				ctx.succeed();
			} else {
				ctx.fail("Lesson " + current[0] + " with Python: " + status + " " + LessonProgress.get(server).get(student.getUUID())
						+ "\n" + log);
			}
		});
	}

	private static String call(HttpClient http, String base, String code, String function, String arguments) {
		return post(http, base + "/api/py/call?code=" + code, "{\"fn\": \"" + function + "\", \"args\": " + arguments + "}");
	}

	private static String post(HttpClient http, String url, String json) {
		try {
			return http.send(GameTests.post(url, json), HttpResponse.BodyHandlers.ofString()).body();
		} catch (Exception e) {
			return e.toString();
		}
	}

}
