package io.github.francescolv.mine.gametest;

import io.github.francescolv.mine.blocks.BlockEditor;
import io.github.francescolv.mine.blocks.BlockSessions;
import io.github.francescolv.mine.lesson.LessonManager;
import io.github.francescolv.mine.lesson.LessonProgress;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.players.ServerOpListEntry;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The block editor as a browser sees it: the page, Blockly's files, the student's session through
 * their personal code, and what sending a program answers. Requests are answered on the server
 * thread, so they are made in the background and checked tick after tick.
 */
public class BlockEditorGameTest {

	@GameTest(maxTicks = LessonGameTest.TICKS)
	public void theBlockEditorInTheBrowser(GameTestHelper ctx) {
		int port = editor(ctx.getLevel().getServer());
		ctx.assertTrue(port > 0, "The editor started");
		ServerPlayer student = ctx.makeMockServerPlayerInLevel();
		String code = BlockSessions.codeFor(student.getUUID());
		ctx.assertTrue(code.equals(BlockSessions.codeFor(student.getUUID())), "The same code until the student leaves");

		HttpClient http = HttpClient.newHttpClient();
		String base = "http://127.0.0.1:" + port;
		CompletableFuture<HttpResponse<String>> page = get(http, base + "/");
		CompletableFuture<HttpResponse<String>> italian = get(http, base + "/blockly/msg/it.js");
		CompletableFuture<HttpResponse<String>> missing = get(http, base + "/secret.txt");
		CompletableFuture<HttpResponse<String>> stranger = get(http, base + "/api/session?code=nobody");
		CompletableFuture<HttpResponse<String>> session = get(http, base + "/api/session?code=" + code);
		CompletableFuture<HttpResponse<String>> badName = post(http, base + "/api/send?code=" + code,
				"{\"name\": \"My Program\", \"lua\": \"turtle.forward()\", \"workspace\": \"{}\", \"run\": false}");
		CompletableFuture<HttpResponse<String>> noAgent = post(http, base + "/api/send?code=" + code,
				"{\"name\": \"square\", \"lua\": \"turtle.forward()\", \"workspace\": \"{}\", \"run\": true}");
		List<CompletableFuture<HttpResponse<String>>> all = List.of(page, italian, missing, stranger, session, badName, noAgent);

		ctx.succeedWhen(() -> {
			ctx.assertTrue(all.stream().allMatch(CompletableFuture::isDone), "Waiting for the answers");
			ctx.assertTrue(page.join().statusCode() == 200 && page.join().body().contains("blockly_compressed.js"), "The page");
			ctx.assertTrue(italian.join().statusCode() == 200 && italian.join().body().contains("Blockly.Msg"), "Blockly in Italian");
			ctx.assertTrue(missing.join().statusCode() == 404, "Only the editor's files");
			ctx.assertTrue(stranger.join().statusCode() == 403, "A code that is nobody's");
			ctx.assertTrue(session.join().statusCode() == 200 && session.join().body().contains(student.getName().getString()),
					"The student's session: " + session.join().body());
			ctx.assertTrue(badName.join().body().contains("\"ok\":false") && badName.join().body().contains("small letters"),
					"A name the Agent can't run: " + badName.join().body());
			ctx.assertTrue(noAgent.join().body().contains("\"ok\":false") && noAgent.join().body().contains("no Byte"),
					"No Agent yet: " + noAgent.join().body());
			BlockSessions.forget(student.getUUID());
		});
	}

	/**
	 * Programs are kept in the world: the student finds them again, other students can't open
	 * them, the teacher can.
	 */
	@GameTest(maxTicks = LessonGameTest.TICKS)
	public void blockProgramsAreKept(GameTestHelper ctx) {
		MinecraftServer server = ctx.getLevel().getServer();
		int port = editor(server);
		ServerPlayer alex = ctx.makeMockServerPlayerInLevel();
		ServerPlayer sam = ctx.makeMockServerPlayerInLevel();
		ServerPlayer teacher = ctx.makeMockServerPlayerInLevel();
		server.getPlayerList().getOps().add(new ServerOpListEntry(teacher.nameAndId(), LevelBasedPermissionSet.GAMEMASTER, false));
		String base = "http://127.0.0.1:" + port;
		HttpClient http = HttpClient.newHttpClient();
		String alexCode = BlockSessions.codeFor(alex.getUUID());
		String program = "square" + (System.nanoTime() % 1000);
		CompletableFuture<HttpResponse<String>> sent = post(http, base + "/api/send?code=" + alexCode,
				"{\"name\": \"" + program + "\", \"lua\": \"turtle.forward()\", \"workspace\": \"{\\\"blocks\\\": 1}\", \"run\": false}");
		CompletableFuture<HttpResponse<String>>[] later = new CompletableFuture[4];

		ctx.succeedWhen(() -> {
			ctx.assertTrue(sent.isDone(), "Sending");
			if (later[0] == null) {
				later[0] = get(http, base + "/api/program?code=" + alexCode + "&name=" + program);
				later[1] = get(http, base + "/api/program?code=" + BlockSessions.codeFor(sam.getUUID()) + "&student=" + alex.getUUID() + "&name=" + program);
				later[2] = get(http, base + "/api/program?code=" + BlockSessions.codeFor(teacher.getUUID()) + "&student=" + alex.getUUID() + "&name=" + program);
				later[3] = get(http, base + "/api/session?code=" + BlockSessions.codeFor(teacher.getUUID()));
			}
			ctx.assertTrue(java.util.Arrays.stream(later).allMatch(CompletableFuture::isDone), "Waiting for the answers");
			ctx.assertTrue(later[0].join().body().contains("blocks"), "Alex finds their program: " + later[0].join().body());
			ctx.assertFalse(later[1].join().body().contains("workspace"), "Sam can't open it: " + later[1].join().body());
			ctx.assertTrue(later[2].join().body().contains("blocks"), "The teacher can: " + later[2].join().body());
			ctx.assertTrue(later[3].join().body().contains(program), "The teacher's list of students' programs");
			for (ServerPlayer player : List.of(alex, sam, teacher)) {
				BlockSessions.forget(player.getUUID());
			}
			server.getPlayerList().getOps().remove(teacher.nameAndId());
		});
	}

	/**
	 * The editor shows the student's lesson: the goal and the hints in the blocks' words. Hints
	 * asked in the editor and in the game are counted together.
	 */
	@GameTest(maxTicks = LessonGameTest.TICKS)
	public void theLessonInTheEditor(GameTestHelper ctx) {
		MinecraftServer server = ctx.getLevel().getServer();
		int port = editor(server);
		ServerPlayer student = ctx.makeMockServerPlayerInLevel();
		LessonProgress.get(server).put(student.getUUID(), new LessonProgress.Entry("loops", 0, 0, System.currentTimeMillis(), 0));
		String base = "http://127.0.0.1:" + port;
		String code = BlockSessions.codeFor(student.getUUID());
		HttpClient http = HttpClient.newHttpClient();
		CompletableFuture<HttpResponse<String>> session = get(http, base + "/api/session?code=" + code);
		CompletableFuture<HttpResponse<String>>[] later = new CompletableFuture[2];
		Component[] inGame = new Component[1];

		ctx.succeedWhen(() -> {
			ctx.assertTrue(session.isDone(), "Waiting for the session");
			String first = session.join().body();
			ctx.assertTrue(first.contains("using a repeat block") && first.contains("\"step\":1") && first.contains("\"more\":true"),
					"The step's goal for blocks: " + first);
			if (later[0] == null) {
				later[0] = post(http, base + "/api/hint?code=" + code, "");
			}
			ctx.assertTrue(later[0].isDone(), "Waiting for the hint");
			ctx.assertTrue(later[0].join().body().contains("puts a block under Byte"),
					"The first hint, for blocks: " + later[0].join().body());
			if (inGame[0] == null) {
				inGame[0] = LessonManager.nextHint(student);
				later[1] = get(http, base + "/api/session?code=" + code);
			}
			ctx.assertTrue(inGame[0] != null && inGame[0].getString().contains("2/2"), "In the game, the second hint: " + inGame[0]);
			ctx.assertTrue(later[1].isDone(), "Waiting for the session again");
			String last = later[1].join().body();
			ctx.assertTrue(last.contains("repeats the blocks inside it") && last.contains("\"more\":false"),
					"Both hints, in the blocks' words: " + last);
			LessonProgress.get(server).remove(student.getUUID());
			BlockSessions.forget(student.getUUID());
		});
	}

	/** One editor for the tests, which run side by side; it stops with the test server. */
	static synchronized int editor(MinecraftServer server) {
		return BlockEditor.port() > 0 ? BlockEditor.port() : BlockEditor.start(server, 0);
	}

	private static CompletableFuture<HttpResponse<String>> get(HttpClient http, String url) {
		return http.sendAsync(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());
	}

	private static CompletableFuture<HttpResponse<String>> post(HttpClient http, String url, String json) {
		return http.sendAsync(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
	}
}
