package io.github.francescolv.mine.gametest;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import io.github.francescolv.mine.agent.AgentManager;
import io.github.francescolv.mine.blocks.ThonnyKeys;
import io.github.francescolv.mine.device.BoardBlockEntity;
import io.github.francescolv.mine.device.BoardBlocks;
import io.github.francescolv.mine.device.BoardReceiverBlock;
import io.github.francescolv.mine.device.BoardSenderBlock;
import io.github.francescolv.mine.device.Devices;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Boards on the students' PCs: what MINE's client reads from the serial port reaches the server
 * as {@code key:value}, and the student's Agent reads it with {@code mine.device}.
 */
public class DeviceGameTest {

	/** Where the Agent appears: the student stands north of it, facing south (+Z). */
	private static final BlockPos AGENT = new BlockPos(2, 1, 1);

	/** Values are whole numbers, limited, or texts; keys are checked; one student's values are theirs. */
	@GameTest
	public void valuesFromTheBoard(GameTestHelper ctx) {
		ServerPlayer student = ctx.makeMockServerPlayerInLevel();
		ServerPlayer other = ctx.makeMockServerPlayerInLevel();

		ctx.assertTrue(Devices.received(student, "Luce", " 120 "), "A valid line was dropped");
		ctx.assertValueEqual(Devices.read(student.getUUID(), "luce"), 120, "luce (keys become lower case)");
		Devices.received(student, "temperatura", "-5");
		ctx.assertValueEqual(Devices.read(student.getUUID(), "temperatura"), -5, "a negative number");
		Devices.received(student, "accelerazione", "99999");
		ctx.assertValueEqual(Devices.read(student.getUUID(), "accelerazione"), Devices.MAX, "a number beyond the limit");
		Devices.received(student, "messaggio", " ciao a tutti ");
		ctx.assertValueEqual(Devices.read(student.getUUID(), "messaggio"), "ciao a tutti", "a text");

		ctx.assertFalse(Devices.received(student, "pulsante a", "1"), "A key with a space was taken");
		ctx.assertFalse(Devices.received(student, "città", "1"), "A key with an accent was taken");
		ctx.assertFalse(Devices.received(student, "", "1"), "An empty key was taken");
		ctx.assertTrue(Devices.read(other.getUUID(), "luce") == null, "Another student got the value");
		ctx.assertTrue(Devices.read(student.getUUID(), "nessuno") == null, "A key never sent has a value");

		ctx.assertFalse(Devices.send(ctx.getLevel().getServer(), student.getUUID(), "cuore", "1"),
				"Sent to a board that isn't connected");
		Devices.connected(student, "/dev/ttyACM0");
		ctx.assertTrue(Devices.isConnected(student.getUUID()), "The board is not connected");
		ctx.assertTrue(Devices.send(ctx.getLevel().getServer(), student.getUUID(), "Cuore", "1"), "Not sent");
		ctx.assertFalse(Devices.send(ctx.getLevel().getServer(), student.getUUID(), "non valida", "1"), "Sent with a bad key");
		Devices.connected(student, "");
		ctx.assertFalse(Devices.isConnected(student.getUUID()), "Still connected");
		ctx.succeed();
	}

	/** At most {@link Devices#PER_SECOND} values a second from one student: a board that floods is cut. */
	@GameTest
	public void tooManyValuesAreDropped(GameTestHelper ctx) {
		ServerPlayer student = ctx.makeMockServerPlayerInLevel();
		int taken = 0;
		for (int i = 0; i < Devices.PER_SECOND + 10; i++) {
			if (Devices.received(student, "conta", Integer.toString(i))) {
				taken++;
			}
		}
		// The second may turn during the loop: never more than two seconds' worth.
		ctx.assertTrue(taken >= Devices.PER_SECOND && taken < Devices.PER_SECOND + 10, "Taken: " + taken);
		ctx.succeed();
	}

	/**
	 * The Agent's program waits for the board's button, then reads and sends values: what the
	 * blocks of the Board category make (mine.device.wait, read, send, connected).
	 */
	@GameTest(maxTicks = LessonGameTest.TICKS)
	public void theAgentListensToTheBoard(GameTestHelper ctx) {
		ServerPlayer student = ctx.makeMockServerPlayerInLevel();
		ServerPlayer other = ctx.makeMockServerPlayerInLevel();
		BlockPos feet = ctx.absolutePos(AGENT.north());
		student.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0f, 0.0f);
		ctx.assertTrue(AgentManager.spawn(student) == AgentManager.Result.SPAWNED, "Agent not created");
		TurtleBlockEntity turtle = ctx.getBlockEntity(AGENT, TurtleBlockEntity.class);
		ServerComputer computer = turtle.createServerComputer();
		Path dir = ServerContext.get(ctx.getLevel().getServer()).storageDir()
				.resolve("computer").resolve(Integer.toString(computer.getID()));
		Path out = dir.resolve("out");
		try {
			Files.createDirectories(dir);
			Files.deleteIfExists(out);
			Files.writeString(dir.resolve("button.lua"), """
					local key, value = mine.device.wait("Pulsante_A")
					turtle.forward()
					local result = {
					  key = key, value = value,
					  luce = mine.device.read("luce"), nessuno = mine.device.read("nessuno"),
					  sent = mine.device.send("cuore", 1), bad = mine.device.send("non valida", 1),
					  connected = mine.device.connected(),
					}
					local file = fs.open("out", "w")
					for name, v in pairs(result) do file.writeLine(name .. "=" .. tostring(v)) end
					file.close()
					""");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		computer.turnOn();
		Devices.connected(student, "/dev/ttyACM0");
		Devices.received(student, "luce", "40000");

		BlockPos moved = AGENT.south();
		int[] phase = { 0 };
		int[] ticks = { 0 };
		ctx.onEachTick(() -> {
			switch (phase[0]) {
				case 0 -> {
					if (AgentManager.run(student, "button") == AgentManager.Result.STARTED) {
						phase[0] = 1;
					}
				}
				case 1 -> {
					// Another student's button and another key: the Agent keeps waiting.
					Devices.received(other, "pulsante_a", "1");
					Devices.received(student, "pulsante_b", "1");
					if (++ticks[0] > 40) {
						ctx.assertBlockPresent(Blocks.AIR, moved);
						phase[0] = 2;
					}
				}
				case 2 -> {
					// Its own button, every tick until the program has started waiting.
					Devices.received(student, "pulsante_a", "1");
					if (Files.exists(out)) {
						phase[0] = 3;
					}
				}
				default -> {
				}
			}
		});
		ctx.succeedWhen(() -> {
			ctx.assertTrue(phase[0] == 3, "The program hasn't finished");
			String result;
			try {
				result = Files.readString(out);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			for (String expected : new String[] { "key=pulsante_a", "value=1", "luce=" + Devices.MAX, "nessuno=0",
					"sent=true", "bad=false", "connected=true" }) {
				ctx.assertTrue(result.lines().anyMatch(expected::equals), "Expected " + expected + " in:\n" + result);
			}
			ctx.assertFalse(ctx.getBlockState(moved).isAir(), "The Agent didn't move");
		});
	}

	/**
	 * The board receiver gives redstone when the student's board says its key: a lamp next to it
	 * lights up with 1, goes off with 0; another student's board doesn't touch it.
	 */
	@GameTest
	public void theReceiverFollowsTheBoard(GameTestHelper ctx) {
		ServerPlayer student = ctx.makeMockServerPlayerInLevel();
		ServerPlayer other = ctx.makeMockServerPlayerInLevel();
		BlockPos receiver = new BlockPos(1, 1, 1);
		BlockPos lamp = receiver.east();
		ctx.setBlock(receiver, BoardBlocks.RECEIVER);
		ctx.setBlock(lamp, Blocks.REDSTONE_LAMP);
		BoardBlockEntity entity = ctx.getBlockEntity(receiver, BoardBlockEntity.class);
		entity.configure(student.getUUID(), "student", "pulsante", false);

		Devices.received(other, "pulsante", "1");
		ctx.assertBlockProperty(receiver, BoardReceiverBlock.POWER, 0);
		Devices.received(student, "pulsante", "200");
		ctx.assertBlockProperty(receiver, BoardReceiverBlock.POWER, 15);
		ctx.assertBlockProperty(lamp, RedstoneLampBlock.LIT, true);
		Devices.received(student, "pulsante", "7");
		ctx.assertBlockProperty(receiver, BoardReceiverBlock.POWER, 7);
		Devices.received(student, "altro", "0");
		ctx.assertBlockProperty(receiver, BoardReceiverBlock.POWER, 7);
		Devices.received(student, "pulsante", "off");
		ctx.assertBlockProperty(receiver, BoardReceiverBlock.POWER, 0);
		// The lamp goes off a few ticks later, as with any redstone.
		ctx.succeedWhen(() -> ctx.assertBlockProperty(lamp, RedstoneLampBlock.LIT, false));
	}

	/** Like a button: the board only says "pressed", and the receiver stays on for a second. */
	@GameTest(maxTicks = 40)
	public void theReceiverCanWorkLikeAButton(GameTestHelper ctx) {
		ServerPlayer student = ctx.makeMockServerPlayerInLevel();
		BlockPos receiver = new BlockPos(1, 1, 1);
		ctx.setBlock(receiver, BoardBlocks.RECEIVER);
		ctx.getBlockEntity(receiver, BoardBlockEntity.class).configure(student.getUUID(), "student", "pulsante_a", true);
		Devices.received(student, "pulsante_a", "1");
		ctx.assertBlockProperty(receiver, BoardReceiverBlock.POWER, 15);
		ctx.runAfterDelay(BoardReceiverBlock.PULSE_TICKS / 2, () -> ctx.assertBlockProperty(receiver, BoardReceiverBlock.POWER, 15));
		ctx.runAfterDelay(BoardReceiverBlock.PULSE_TICKS + 2, () -> {
			ctx.assertBlockProperty(receiver, BoardReceiverBlock.POWER, 0);
			ctx.succeed();
		});
	}

	/** The board sender tells the student's board the redstone it gets, every time it changes. */
	@GameTest
	public void theSenderTellsTheBoard(GameTestHelper ctx) {
		ServerPlayer student = ctx.makeMockServerPlayerInLevel();
		Devices.connected(student, "/dev/ttyACM0");
		BlockPos sender = new BlockPos(1, 1, 1);
		BlockPos power = sender.west();
		ctx.setBlock(sender, BoardBlocks.SENDER);
		ctx.getBlockEntity(sender, BoardBlockEntity.class).configure(student.getUUID(), "student", "led", false);
		ctx.setBlock(power, Blocks.REDSTONE_BLOCK);
		ctx.assertValueEqual(Devices.lastSent(student.getUUID(), "led"), "15", "led after the redstone block");
		ctx.assertBlockProperty(sender, BoardSenderBlock.POWERED, true);
		ctx.setBlock(power, Blocks.AIR);
		ctx.assertValueEqual(Devices.lastSent(student.getUUID(), "led"), "0", "led after removing it");
		ctx.assertBlockProperty(sender, BoardSenderBlock.POWERED, false);
		Devices.connected(student, "");
		ctx.succeed();
	}

	/**
	 * A board on Wi-Fi (Pico 2 W, mine_wifi.py) asks the server with the student's code: what it
	 * says reaches the student as over a cable, and it gets what was sent to it, also before.
	 */
	@GameTest(maxTicks = LessonGameTest.TICKS)
	public void aBoardOnWifi(GameTestHelper ctx) {
		MinecraftServer server = ctx.getLevel().getServer();
		String base = "http://127.0.0.1:" + BlockEditorGameTest.editor(server) + "/api/device?code=";
		ServerPlayer student = ctx.makeMockServerPlayerInLevel();
		String key = ThonnyKeys.get(server).keyFor(student.getUUID());
		// Sent before the board was there: it gets it when it first asks.
		Devices.send(server, student.getUUID(), "wifi_led", "1");

		CompletableFuture<List<String>> answers = CompletableFuture.supplyAsync(() -> {
			HttpClient http = HttpClient.newHttpClient();
			List<String> bodies = new ArrayList<>();
			try {
				bodies.add(post(http, base + key, "{\"values\": [\"wifi_temperatura:21\"]}"));
				server.submit(() -> Devices.send(server, student.getUUID(), "wifi_led", "0")).get();
				bodies.add(post(http, base + key, "{\"values\": []}"));
				bodies.add(post(http, base + "nessuno", "{\"values\": []}"));
			} catch (Exception e) {
				bodies.add(e.toString());
			}
			return bodies;
		});
		ctx.succeedWhen(() -> {
			ctx.assertTrue(answers.isDone(), "Waiting for the board");
			List<String> bodies = answers.join();
			ctx.assertTrue(bodies.size() == 3, "Answers: " + bodies);
			ctx.assertTrue(bodies.get(0).startsWith("200 ") && bodies.get(0).contains("wifi_led:1"), "First ask: " + bodies.get(0));
			ctx.assertTrue(bodies.get(1).startsWith("200 ") && bodies.get(1).contains("wifi_led:0")
					&& !bodies.get(1).contains("wifi_led:1"), "Second ask: " + bodies.get(1));
			ctx.assertTrue(bodies.get(2).startsWith("403 "), "A wrong code: " + bodies.get(2));
			ctx.assertValueEqual(Devices.read(student.getUUID(), "wifi_temperatura"), 21, "The value from the board");
			ctx.assertTrue(Devices.isConnected(student.getUUID()), "The board on Wi-Fi is not connected");
			Devices.connected(student, "");
		});
	}

	private static String post(HttpClient http, String url, String json) throws IOException, InterruptedException {
		HttpResponse<String> response = http.send(GameTests.post(url, json), HttpResponse.BodyHandlers.ofString());
		return response.statusCode() + " " + response.body();
	}
}
