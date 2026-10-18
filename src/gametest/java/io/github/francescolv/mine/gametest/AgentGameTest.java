package io.github.francescolv.mine.gametest;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import io.github.francescolv.mine.agent.AgentManager;
import io.github.francescolv.mine.agent.AgentRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AgentGameTest {

	/** Looked up at test time: when this class is loaded, CC: Tweaked hasn't registered its blocks yet. */
	private static Block turtle() {
		return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("computercraft", "turtle_normal"));
	}

	/** Yaw 0 = the student faces south (+Z). */
	private static ServerPlayer studentAt(GameTestHelper ctx, BlockPos relative) {
		ServerPlayer player = ctx.makeMockServerPlayerInLevel();
		BlockPos abs = ctx.absolutePos(relative);
		player.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0.0f, 0.0f);
		return player;
	}

	private static void moveTo(GameTestHelper ctx, ServerPlayer player, BlockPos relative) {
		BlockPos abs = ctx.absolutePos(relative);
		player.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0.0f, 0.0f);
	}

	private static void expectResult(GameTestHelper ctx, AgentManager.Result expected, AgentManager.Result actual) {
		ctx.assertTrue(expected == actual, "Expected " + expected + ", got " + actual);
	}

	@GameTest
	public void spawnPlacesLabelledAgentFacingStudent(GameTestHelper ctx) {
		ServerPlayer player = studentAt(ctx, new BlockPos(2, 1, 2));

		expectResult(ctx, AgentManager.Result.SPAWNED, AgentManager.spawn(player));

		BlockPos agent = new BlockPos(2, 1, 3);
		ctx.assertBlockPresent(turtle(), agent);
		ctx.assertTrue(ctx.getBlockState(agent).getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.SOUTH,
				"The Agent must face the same direction as the student");
		Component name = ((Nameable) ctx.getBlockEntity(agent, TurtleBlockEntity.class)).getCustomName();
		String expected = player.getName().getString() + "'s Byte";
		ctx.assertTrue(name != null && expected.equals(name.getString()),
				"Expected label \"" + expected + "\", found " + name);
		ctx.succeed();
	}

	@GameTest
	public void secondSpawnReplacesOldAgent(GameTestHelper ctx) {
		ServerPlayer player = studentAt(ctx, new BlockPos(1, 1, 1));
		expectResult(ctx, AgentManager.Result.SPAWNED, AgentManager.spawn(player));

		moveTo(ctx, player, new BlockPos(5, 1, 1));
		expectResult(ctx, AgentManager.Result.REPLACED, AgentManager.spawn(player));

		ctx.assertBlockPresent(Blocks.AIR, new BlockPos(1, 1, 2));
		ctx.assertBlockPresent(turtle(), new BlockPos(5, 1, 2));
		ctx.succeed();
	}

	@GameTest
	public void spawnFailsWhenBlocked(GameTestHelper ctx) {
		ctx.setBlock(new BlockPos(2, 1, 3), Blocks.STONE);
		ServerPlayer player = studentAt(ctx, new BlockPos(2, 1, 2));

		expectResult(ctx, AgentManager.Result.BLOCKED, AgentManager.spawn(player));

		ctx.assertBlockPresent(Blocks.STONE, new BlockPos(2, 1, 3));
		ctx.succeed();
	}

	@GameTest
	public void recallBringsAgentToStudent(GameTestHelper ctx) {
		ServerPlayer player = studentAt(ctx, new BlockPos(1, 1, 1));
		expectResult(ctx, AgentManager.Result.NO_AGENT, AgentManager.recall(player));
		expectResult(ctx, AgentManager.Result.SPAWNED, AgentManager.spawn(player));

		moveTo(ctx, player, new BlockPos(5, 1, 4));
		expectResult(ctx, AgentManager.Result.RECALLED, AgentManager.recall(player));

		ctx.assertBlockPresent(Blocks.AIR, new BlockPos(1, 1, 2));
		ctx.assertBlockPresent(turtle(), new BlockPos(5, 1, 5));
		ctx.succeed();
	}

	@GameTest
	public void brokenAgentIsForgotten(GameTestHelper ctx) {
		ServerPlayer player = studentAt(ctx, new BlockPos(2, 1, 2));
		expectResult(ctx, AgentManager.Result.SPAWNED, AgentManager.spawn(player));

		// The student breaks the Agent: the next command gives a new one, not a "replaced" one.
		ctx.setBlock(new BlockPos(2, 1, 3), Blocks.AIR);
		expectResult(ctx, AgentManager.Result.NO_AGENT, AgentManager.recall(player));
		expectResult(ctx, AgentManager.Result.SPAWNED, AgentManager.spawn(player));
		ctx.succeed();
	}

	/** A new Agent, also after the old one was broken, gets its computer back: the student's programs stay. */
	@GameTest(maxTicks = LessonGameTest.TICKS)
	public void aNewAgentKeepsTheOldOnesPrograms(GameTestHelper ctx) {
		ServerPlayer player = studentAt(ctx, new BlockPos(2, 1, 2));
		expectResult(ctx, AgentManager.Result.SPAWNED, AgentManager.spawn(player));
		ServerComputer computer = ctx.getBlockEntity(new BlockPos(2, 1, 3), TurtleBlockEntity.class).createServerComputer();
		computer.turnOn();

		boolean[] done = { false };
		ctx.failIfEver(() -> {
			// Asking for a run makes MINE learn which computer the Agent is, once it's on.
			if (done[0] || !computer.isOn() || AgentManager.run(player, "nothing") == AgentManager.Result.OFF) {
				return;
			}
			done[0] = true;
			ctx.setBlock(new BlockPos(2, 1, 3), Blocks.AIR);
			expectResult(ctx, AgentManager.Result.SPAWNED, AgentManager.spawn(player));
			TurtleBlockEntity replaced = ctx.getBlockEntity(new BlockPos(2, 1, 3), TurtleBlockEntity.class);
			int now = replaced.getComputerID();
			ctx.assertTrue(now == computer.getID(), "The new Agent is computer " + now + ", the old one was " + computer.getID());
			// The computer that runs, not only the number on the block.
			int running = replaced.createServerComputer().getID();
			ctx.assertTrue(running == computer.getID(), "The new Agent runs computer " + running + ", the old one was " + computer.getID());
			ctx.succeed();
		});
	}

	/**
	 * An Agent that is off (as after a server restart) comes when recalled too, with its
	 * computer and what it carries.
	 */
	@GameTest(maxTicks = LessonGameTest.TICKS)
	public void anAgentThatIsOffComesWhenRecalled(GameTestHelper ctx) {
		ServerPlayer player = studentAt(ctx, new BlockPos(1, 1, 1));
		expectResult(ctx, AgentManager.Result.SPAWNED, AgentManager.spawn(player));
		TurtleBlockEntity original = ctx.getBlockEntity(new BlockPos(1, 1, 2), TurtleBlockEntity.class);
		ServerComputer computer = original.createServerComputer();
		computer.turnOn();

		int[] stage = { 0 };
		ctx.failIfEver(() -> {
			// On, and known to MINE: then off.
			if (stage[0] == 0 && computer.isOn() && AgentManager.run(player, "nothing") != AgentManager.Result.OFF) {
				original.getAccess().getInventory().setItem(5, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
				computer.shutdown();
				stage[0] = 1;
			} else if (stage[0] == 1 && !computer.isOn() && AgentManager.run(player, "nothing") == AgentManager.Result.OFF) {
				stage[0] = 2;
				moveTo(ctx, player, new BlockPos(5, 1, 4));
				expectResult(ctx, AgentManager.Result.RECALLED, AgentManager.recall(player));
				ctx.assertBlockPresent(Blocks.AIR, new BlockPos(1, 1, 2));
				TurtleBlockEntity moved = ctx.getBlockEntity(new BlockPos(5, 1, 5), TurtleBlockEntity.class);
				ctx.assertTrue(moved.getComputerID() == computer.getID(), "The same computer: its programs");
				ctx.assertTrue(moved.getAccess().getInventory().getItem(5).is(net.minecraft.world.item.Items.DIAMOND), "What it carries");
				ctx.assertTrue(moved.getAccess().getOwningPlayer() != null, "Still the student's");
				moved.createServerComputer().turnOn();
				stage[0] = 3;
			} else if (stage[0] == 3 && AgentManager.run(player, "nothing") != AgentManager.Result.OFF) {
				// Turned on again, MINE knows it: it runs the student's programs.
				ctx.succeed();
			}
		});
	}

	/**
	 * A running Agent that is recalled must stay the same computer, on and "alive":
	 * CC: Tweaked unloads computers that their block doesn't keep alive for 100 ticks.
	 */
	@GameTest(maxTicks = LessonGameTest.TICKS)
	public void recalledRunningAgentKeepsItsComputer(GameTestHelper ctx) {
		ServerPlayer player = studentAt(ctx, new BlockPos(1, 1, 1));
		expectResult(ctx, AgentManager.Result.SPAWNED, AgentManager.spawn(player));

		TurtleBlockEntity original = ctx.getBlockEntity(new BlockPos(1, 1, 2), TurtleBlockEntity.class);
		ServerComputer computer = original.createServerComputer();
		computer.turnOn();

		// Wait for the turtle to boot (and for MINE to see it running), then recall it. Polling, not a
		// fixed delay: the test server runs ticks much faster than CC: Tweaked boots a computer.
		boolean[] recalled = { false };
		ctx.failIfEver(() -> {
			if (recalled[0] || !computer.isOn()) {
				return;
			}
			recalled[0] = true;
			moveTo(ctx, player, new BlockPos(5, 1, 4));
			expectResult(ctx, AgentManager.Result.RECALLED, AgentManager.recall(player));
			ctx.assertBlockPresent(Blocks.AIR, new BlockPos(1, 1, 2));
			ctx.assertBlockPresent(turtle(), new BlockPos(5, 1, 5));

			ctx.runAfterDelay(200, () -> {
				TurtleBlockEntity moved = ctx.getBlockEntity(new BlockPos(5, 1, 5), TurtleBlockEntity.class);
				ServerComputer now = moved.getServerComputer();
				ctx.assertTrue(now != null, "After the recall the turtle no longer has a loaded computer");
				ctx.assertTrue(now == computer, "After the recall the turtle has a different computer (state lost)");
				ctx.assertTrue(now.isOn(), "After the recall the turtle's computer is off");
				ctx.assertTrue(now.checkUsable(player), "After the recall the terminal is not usable");
				ctx.succeed();
			});
		});
	}

	@GameTest
	public void runNeedsAnAgentThatIsOn(GameTestHelper ctx) {
		ServerPlayer player = studentAt(ctx, new BlockPos(2, 1, 2));
		expectResult(ctx, AgentManager.Result.NO_AGENT, AgentManager.run(player, "tower"));
		expectResult(ctx, AgentManager.Result.SPAWNED, AgentManager.spawn(player));
		expectResult(ctx, AgentManager.Result.OFF, AgentManager.run(player, "tower"));
		ctx.succeed();
	}

	/**
	 * {@code /mine agent run}: the program starts as soon as the shell is ready, and meanwhile the Agent is busy.
	 * MINE remembers it as the last program run, for the run key.
	 */
	@GameTest(maxTicks = LessonGameTest.TICKS)
	public void runStartsProgramFromChat(GameTestHelper ctx) {
		ServerPlayer player = studentAt(ctx, new BlockPos(2, 1, 1));
		expectResult(ctx, AgentManager.Result.SPAWNED, AgentManager.spawn(player));

		TurtleBlockEntity turtle = ctx.getBlockEntity(new BlockPos(2, 1, 2), TurtleBlockEntity.class);
		turtle.getAccess().getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
		ServerComputer computer = turtle.createServerComputer();
		Path dir = ServerContext.get(ctx.getLevel().getServer()).storageDir()
				.resolve("computer").resolve(Integer.toString(computer.getID()));
		try {
			Files.createDirectories(dir);
			Files.writeString(dir.resolve("block.lua"), "sleep(1)\nturtle.place()\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		computer.turnOn();

		boolean[] started = { false };
		ctx.failIfEver(() -> {
			if (!started[0]) {
				// While booting it answers OFF or BUSY; as soon as the shell waits for a command, it starts.
				AgentManager.Result result = AgentManager.run(player, "block now");
				if (result == AgentManager.Result.STARTED) {
					started[0] = true;
					expectResult(ctx, AgentManager.Result.BUSY, AgentManager.run(player, "block"));
				}
			} else if (ctx.getBlockState(new BlockPos(2, 1, 3)).is(Blocks.OAK_PLANKS)) {
				// The run key starts it again, with its arguments.
				ctx.assertTrue("block now".equals(AgentManager.lastRun(ctx.getLevel().getServer(), player.getUUID())),
						"The last program run: " + AgentManager.lastRun(ctx.getLevel().getServer(), player.getUUID()));
				// Also after a restart of the server.
				AgentRegistry saved = AgentRegistry.fromNbt(AgentRegistry.get(ctx.getLevel().getServer()).save());
				ctx.assertTrue("block now".equals(saved.lastRun(player.getUUID())), "The last program run is saved: " + saved.lastRun(player.getUUID()));
				ctx.succeed();
			}
		});
	}

	/**
	 * A program started from the chat, the blocks or the run key stops with Ctrl+T (the
	 * "terminate" event), like one typed in the terminal: then the Agent is free again.
	 */
	@GameTest(maxTicks = LessonGameTest.TICKS)
	public void terminateStopsAProgramStartedFromTheChat(GameTestHelper ctx) {
		ServerPlayer player = studentAt(ctx, new BlockPos(2, 1, 1));
		expectResult(ctx, AgentManager.Result.SPAWNED, AgentManager.spawn(player));

		TurtleBlockEntity turtle = ctx.getBlockEntity(new BlockPos(2, 1, 2), TurtleBlockEntity.class);
		ServerComputer computer = turtle.createServerComputer();
		Path dir = ServerContext.get(ctx.getLevel().getServer()).storageDir()
				.resolve("computer").resolve(Integer.toString(computer.getID()));
		try {
			Files.createDirectories(dir);
			Files.writeString(dir.resolve("listen.lua"), "while true do os.pullEvent(\"redstone\") end\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		computer.turnOn();

		int[] state = { 0 };
		int[] wait = { 0 };
		ctx.failIfEver(() -> {
			switch (state[0]) {
				case 0 -> {
					if (AgentManager.run(player, "listen") == AgentManager.Result.STARTED) {
						state[0] = 1;
					}
				}
				case 1 -> {
					// Running and listening: Ctrl+T.
					if (++wait[0] == 40) {
						expectResult(ctx, AgentManager.Result.BUSY, AgentManager.run(player, "listen"));
						computer.queueEvent("terminate");
						state[0] = 2;
					}
				}
				default -> {
					// While the shell gets back to its prompt it is busy; then it is free.
					if (AgentManager.run(player, "listen") == AgentManager.Result.STARTED) {
						ctx.succeed();
					}
				}
			}
		});
	}
}
