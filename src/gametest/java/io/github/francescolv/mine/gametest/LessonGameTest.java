package io.github.francescolv.mine.gametest;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import io.github.francescolv.mine.agent.AgentManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs the lesson solutions (in {@code docs/lessons}) inside a real Agent and checks the
 * result in the world: if a lesson stops working, CI notices. Every translation of a lesson
 * has its own solutions (e.g. {@code solutions/it/}), and they are all tested.
 */
public class LessonGameTest {

	/**
	 * Generous: the test server runs ticks as fast as it can, while CC: Tweaked computers run in
	 * real time, so a few hundred ticks can be less than a second. Tests end as soon as they pass.
	 */
	static final int TICKS = 6000;

	static final Path LESSONS = Path.of(System.getProperty("mine.lessons", "../../docs/lessons"));

	/**
	 * Runs the solution and saves "ok" or the Lua error to {@code outcome.txt}. It writes a
	 * temporary file first and then renames it, so the test never reads a half-written file.
	 */
	private static final String WRAPPER = """
			local ok, err = pcall(dofile, "/solution.lua")
			local f = fs.open("/outcome.tmp", "w")
			f.write(ok and "ok" or tostring(err))
			f.close()
			fs.move("/outcome.tmp", "/outcome.txt")
			""";

	/** Where the Agent appears: the student stands north of it, facing south (+Z). */
	private static final BlockPos AGENT = new BlockPos(2, 1, 1);

	/** A 16×16×16 empty structure, for lessons that build further away than {@code EMPTY_STRUCTURE} allows. */
	static final String LAB = "mine-gametest:lab";

	/** In the lab: room to the south for a row of 10, and to the west for floors (which turn right). */
	static final BlockPos LAB_AGENT = new BlockPos(8, 1, 2);

	/** An Agent with the solution in place and its computer on; {@code dir} is its disk. */
	private record Run(ServerComputer computer, Path dir) {

		String outcome() {
			try {
				Path file = dir.resolve("outcome.txt");
				return Files.exists(file) ? Files.readString(file) : null;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private static Run start(GameTestHelper ctx, String lesson, String solution, ItemStack firstSlot) {
		return start(ctx, AGENT, lesson, solution, firstSlot);
	}

	private static Run start(GameTestHelper ctx, BlockPos agent, String lesson, String solution, ItemStack firstSlot) {
		ServerPlayer player = ctx.makeMockServerPlayerInLevel();
		BlockPos student = ctx.absolutePos(agent.north());
		player.snapTo(student.getX() + 0.5, student.getY(), student.getZ() + 0.5, 0.0f, 0.0f);
		ctx.assertTrue(AgentManager.spawn(player) == AgentManager.Result.SPAWNED, "Agent non creato");

		TurtleBlockEntity turtle = ctx.getBlockEntity(agent, TurtleBlockEntity.class);
		turtle.getAccess().getInventory().setItem(0, firstSlot);
		ServerComputer computer = turtle.createServerComputer();

		Path dir = ServerContext.get(ctx.getLevel().getServer()).storageDir()
				.resolve("computer").resolve(Integer.toString(computer.getID()));
		try {
			Files.createDirectories(dir);
			Files.copy(LESSONS.resolve(lesson).resolve("solutions").resolve(solution), dir.resolve("solution.lua"));
			Files.writeString(dir.resolve("startup.lua"), WRAPPER, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		computer.turnOn();
		return new Run(computer, dir);
	}

	/** Waits for the solution to finish without Lua errors, then runs the checks on the world. */
	private static void whenFinished(GameTestHelper ctx, Run run, Runnable checks) {
		boolean[] done = { false };
		ctx.failIfEver(() -> {
			String outcome = done[0] ? null : run.outcome();
			if (outcome == null) {
				return;
			}
			done[0] = true;
			ctx.assertTrue(outcome.equals("ok"), "Lua error in the solution: " + outcome);
			checks.run();
			ctx.succeed();
		});
	}

	private static void expectAgentAt(GameTestHelper ctx, BlockPos pos, Direction facing) {
		ctx.assertBlockPresent(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("computercraft", "turtle_normal")), pos);
		ctx.assertTrue(ctx.getBlockState(pos).getValue(BlockStateProperties.HORIZONTAL_FACING) == facing,
				"The Agent should have faced " + facing);
	}

	private static void hello(GameTestHelper ctx, String solution) {
		Run run = start(ctx, "01-first-steps", solution, ItemStack.EMPTY);
		whenFinished(ctx, run, () -> {
		});
	}

	private static void roundtrip(GameTestHelper ctx, String solution) {
		Run run = start(ctx, "01-first-steps", solution, ItemStack.EMPTY);
		whenFinished(ctx, run, () -> {
			expectAgentAt(ctx, AGENT, Direction.SOUTH);
		});
	}

	private static void tower(GameTestHelper ctx, String solution) {
		Run run = start(ctx, "01-first-steps", solution, new ItemStack(Items.OAK_PLANKS, 3));
		whenFinished(ctx, run, () -> {
			for (int y = 0; y < 3; y++) {
				ctx.assertBlockPresent(Blocks.OAK_PLANKS, AGENT.south().above(y));
			}
			ctx.assertBlockPresent(Blocks.AIR, AGENT.south().above(3));
			expectAgentAt(ctx, AGENT, Direction.SOUTH);
		});
	}

	/** Every block from {@code from} to {@code to} (relative, both included) is {@code block}. */
	static void expectBox(GameTestHelper ctx, Block block, BlockPos from, BlockPos to) {
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			ctx.assertBlockPresent(block, pos);
		}
	}

	/** Builds with a full stack of planks; {@code from}/{@code to} is what it must have built. */
	private static void build(GameTestHelper ctx, String solution, BlockPos from, BlockPos to) {
		build(ctx, "02-loops", solution, from, to);
	}

	private static void build(GameTestHelper ctx, String lesson, String solution, BlockPos from, BlockPos to) {
		Run run = start(ctx, LAB_AGENT, lesson, solution, new ItemStack(Items.OAK_PLANKS, 64));
		whenFinished(ctx, run, () -> expectBox(ctx, Blocks.OAK_PLANKS, from, to));
	}

	@GameTest(maxTicks = TICKS)
	public void lesson1Hello(GameTestHelper ctx) {
		hello(ctx, "hello.lua");
	}

	@GameTest(maxTicks = TICKS)
	public void lesson1Roundtrip(GameTestHelper ctx) {
		roundtrip(ctx, "roundtrip.lua");
	}

	@GameTest(maxTicks = TICKS)
	public void lesson1Tower(GameTestHelper ctx) {
		tower(ctx, "tower.lua");
	}

	@GameTest(maxTicks = TICKS)
	public void lesson1HelloItalian(GameTestHelper ctx) {
		hello(ctx, "it/saluta.lua");
	}

	@GameTest(maxTicks = TICKS)
	public void lesson1RoundtripItalian(GameTestHelper ctx) {
		roundtrip(ctx, "it/andata.lua");
	}

	@GameTest(maxTicks = TICKS)
	public void lesson1TowerItalian(GameTestHelper ctx) {
		tower(ctx, "it/torre.lua");
	}

	// Lesson 2: the row goes south from the Agent, floors and cubes then grow west (turning right).

	@GameTest(structure = LAB, maxTicks = TICKS)
	public void lesson2Row(GameTestHelper ctx) {
		build(ctx, "row.lua", LAB_AGENT, LAB_AGENT.south(9));
	}

	@GameTest(structure = LAB, maxTicks = TICKS)
	public void lesson2Floor(GameTestHelper ctx) {
		build(ctx, "floor.lua", LAB_AGENT.west(3), LAB_AGENT.south(4));
	}

	@GameTest(structure = LAB, maxTicks = TICKS)
	public void lesson2Cube(GameTestHelper ctx) {
		build(ctx, "cube.lua", LAB_AGENT.west(2), LAB_AGENT.south(2).above(2));
	}

	@GameTest(structure = LAB, maxTicks = TICKS)
	public void lesson2RowItalian(GameTestHelper ctx) {
		build(ctx, "it/fila.lua", LAB_AGENT, LAB_AGENT.south(9));
	}

	@GameTest(structure = LAB, maxTicks = TICKS)
	public void lesson2FloorItalian(GameTestHelper ctx) {
		build(ctx, "it/pavimento.lua", LAB_AGENT.west(3), LAB_AGENT.south(4));
	}

	@GameTest(structure = LAB, maxTicks = TICKS)
	public void lesson2CubeItalian(GameTestHelper ctx) {
		build(ctx, "it/cubo.lua", LAB_AGENT.west(2), LAB_AGENT.south(2).above(2));
	}

	// Lesson 3: the broken programs, fixed. That they are really broken is in MissionGameTest.

	@GameTest(structure = LAB, maxTicks = TICKS)
	public void lesson3Row(GameTestHelper ctx) {
		build(ctx, "03-debugging", "broken_row.lua", LAB_AGENT, LAB_AGENT.south(5));
	}

	@GameTest(structure = LAB, maxTicks = TICKS)
	public void lesson3Floor(GameTestHelper ctx) {
		build(ctx, "03-debugging", "broken_floor.lua", LAB_AGENT.west(2), LAB_AGENT.south(3));
	}

	@GameTest(structure = LAB, maxTicks = TICKS)
	public void lesson3Square(GameTestHelper ctx) {
		build(ctx, "03-debugging", "broken_square.lua", LAB_AGENT.west(3), LAB_AGENT.south(3));
	}

	@GameTest(structure = LAB, maxTicks = TICKS)
	public void lesson3RowItalian(GameTestHelper ctx) {
		build(ctx, "03-debugging", "it/fila_rotta.lua", LAB_AGENT, LAB_AGENT.south(5));
	}

	@GameTest(structure = LAB, maxTicks = TICKS)
	public void lesson3FloorItalian(GameTestHelper ctx) {
		build(ctx, "03-debugging", "it/pavimento_rotto.lua", LAB_AGENT.west(2), LAB_AGENT.south(3));
	}

	@GameTest(structure = LAB, maxTicks = TICKS)
	public void lesson3SquareItalian(GameTestHelper ctx) {
		build(ctx, "03-debugging", "it/quadrato_rotto.lua", LAB_AGENT.west(3), LAB_AGENT.south(3));
	}
}
