package io.github.francescolv.mine.lesson;

import com.mojang.serialization.MapCodec;
import io.github.francescolv.mine.agent.AgentProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Lesson 6, the Agent and redstone. MINE builds one scene around the Agent for the whole lesson:
 * a redstone lamp behind it, an iron door in front of it, and on each side a stone post with a
 * lever on its outer face (so the levers power the posts, which the Agent feels, and not the
 * lamp). The lamp is behind, not on top: lit by the Agent, it powers the blocks around it, and
 * the door would open with it. The steps that listen to the levers are tested live: MINE pulls the levers while the
 * program runs and looks at what the Agent does, then stops the program.
 */
final class RedstoneChecks {

	/**
	 * Before MINE first looks, while the program gets going, and between pulling a lever and
	 * looking at the door or the lamp: real time, as the Agent's computer runs, not ticks.
	 */
	private static final long SETTLE_MS = 2000;
	private static final long STEP_MS = 1500;

	private RedstoneChecks() {
	}

	/** The scene around the Agent at {@code agent}, facing {@code ahead}. */
	record Scene(ServerLevel world, BlockPos agent, Direction ahead, Map<BlockPos, BlockState> blocks) implements Checks.Challenge {

		BlockPos lamp() {
			return agent.relative(ahead.getOpposite());
		}

		BlockPos door() {
			return agent.relative(ahead);
		}

		BlockPos post(boolean left) {
			return agent.relative(left ? ahead.getCounterClockWise() : ahead.getClockWise());
		}

		BlockPos lever(boolean left) {
			return post(left).relative(left ? ahead.getCounterClockWise() : ahead.getClockWise());
		}

		/** Builds the scene, keeping what is already there of it; or says it needs more room. */
		static Object build(ServerLevel world, BlockPos agent, Direction ahead) {
			Direction left = ahead.getCounterClockWise();
			Direction right = ahead.getClockWise();
			BlockPos door = agent.relative(ahead);
			Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
			blocks.put(agent.relative(ahead.getOpposite()), Blocks.REDSTONE_LAMP.defaultBlockState());
			blocks.put(door, Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, ahead)
					.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
			blocks.put(door.above(), Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, ahead)
					.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
			blocks.put(agent.relative(left), Blocks.STONE.defaultBlockState());
			blocks.put(agent.relative(left, 2), lever(left));
			blocks.put(agent.relative(right), Blocks.STONE.defaultBlockState());
			blocks.put(agent.relative(right, 2), lever(right));
			if (!world.getBlockState(door.below()).isFaceSturdy(world, door.below(), Direction.UP)) {
				return new NoRoom();
			}
			for (Map.Entry<BlockPos, BlockState> block : blocks.entrySet()) {
				BlockState there = world.getBlockState(block.getKey());
				if (!there.is(block.getValue().getBlock()) && !there.canBeReplaced()) {
					return new NoRoom();
				}
			}
			blocks.forEach((pos, state) -> {
				if (!world.getBlockState(pos).is(state.getBlock())) {
					world.setBlock(pos, state, Block.UPDATE_ALL);
				}
			});
			Scene scene = new Scene(world, agent, ahead, blocks);
			scene.pull(true, false);
			scene.pull(false, false);
			return scene;
		}

		private static BlockState lever(Direction outwards) {
			return Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
					.setValue(LeverBlock.FACING, outwards).setValue(LeverBlock.POWERED, false);
		}

		boolean lit() {
			BlockState lamp = world.getBlockState(lamp());
			return lamp.is(Blocks.REDSTONE_LAMP) && lamp.getValue(RedstoneLampBlock.LIT);
		}

		boolean open() {
			BlockState door = world.getBlockState(door());
			return door.is(Blocks.IRON_DOOR) && door.getValue(DoorBlock.OPEN);
		}

		/** Puts a lever on or off, as a player pulling it would. */
		void pull(boolean left, boolean on) {
			BlockPos pos = lever(left);
			BlockState lever = world.getBlockState(pos);
			if (lever.is(Blocks.LEVER) && lever.getValue(LeverBlock.POWERED) != on) {
				world.setBlock(pos, lever.setValue(LeverBlock.POWERED, on), Block.UPDATE_ALL);
				world.updateNeighborsAt(pos, Blocks.LEVER);
				world.updateNeighborsAt(post(left), Blocks.LEVER);
			}
		}

		@Override
		public boolean untouched() {
			return blocks.entrySet().stream().allMatch(block -> world.getBlockState(block.getKey()).is(block.getValue().getBlock()));
		}

		@Override
		public void remove() {
			// Levers first (they hang on the posts), the top of the door before its bottom.
			List.of(lever(true), lever(false), post(true), post(false), door().above(), door(), lamp()).forEach(pos -> {
				if (blocks.containsKey(pos) && world.getBlockState(pos).is(blocks.get(pos).getBlock())) {
					world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				}
			});
		}
	}

	/** There's no room for the scene around the Agent. */
	private record NoRoom() {

		Component text() {
			return Component.translatableWithFallback("mine.lesson.check.redstone_room",
					"Not yet: MINE needs room around Byte for the lamp, the door and the levers, and solid ground in front. Move it to an open space with /mine byte recall.");
		}
	}

	/**
	 * A live test: MINE sets the levers one way after the other while the program runs, and after
	 * each move looks whether the Agent did what it should. {@code moves} are the levers' positions
	 * (left, right), the first one set when the program starts.
	 */
	private static final class Probe implements Checks.Challenge {

		final Scene scene;
		final List<boolean[]> moves;
		final BiPredicate<Scene, boolean[]> right;
		/** When MINE looks next; the clock starts with the program (the probe may be built before). */
		long next = -1;
		int move;
		boolean passed;
		@Nullable
		boolean[] failedAt;

		Probe(Scene scene, List<boolean[]> moves, BiPredicate<Scene, boolean[]> right) {
			this.scene = scene;
			this.moves = moves;
			this.right = right;
			set(moves.getFirst());
		}

		private void set(boolean[] levers) {
			scene.pull(true, levers[0]);
			scene.pull(false, levers[1]);
		}

		/** One tick of the test; {@code true} when it is over, passed or not. */
		boolean tick() {
			if (next < 0) {
				next = System.currentTimeMillis() + SETTLE_MS;
			}
			if (passed || failedAt != null || System.currentTimeMillis() < next) {
				return passed || failedAt != null;
			}
			boolean[] levers = moves.get(move);
			if (!right.test(scene, levers)) {
				failedAt = levers;
				return true;
			}
			if (++move == moves.size()) {
				passed = true;
				return true;
			}
			set(moves.get(move));
			next = System.currentTimeMillis() + STEP_MS;
			return false;
		}

		@Override
		public boolean untouched() {
			return scene.untouched();
		}

		@Override
		public void remove() {
			scene.remove();
		}
	}

	private static Object scene(ServerLevel world, BlockPos pos, Direction facing) {
		return Scene.build(world, pos, facing);
	}

	private static Component onOff(boolean on) {
		return on ? Component.translatableWithFallback("mine.lesson.check.on", "on")
				: Component.translatableWithFallback("mine.lesson.check.off", "off");
	}

	/** Done when the lamp behind the Agent is on when the program ends. */
	record Lamp() implements Check {

		static final MapCodec<Lamp> CODEC = MapCodec.unit(Lamp::new);

		@Override
		public Object start(AgentProgram program) {
			return prepare(program.world(), program.startPos(), program.startFacing());
		}

		@Override
		public Object prepare(ServerLevel world, BlockPos start, Direction ahead) {
			return scene(world, start, ahead);
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return memo instanceof Scene scene && scene.lit();
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			return memo instanceof NoRoom noRoom ? noRoom.text() : null;
		}
	}

	/**
	 * Tested live: MINE pulls the left lever on, off and on again, and each time the door in front
	 * of the Agent must follow it, open with the lever on and closed with it off.
	 */
	record LeverDoor() implements Check {

		static final MapCodec<LeverDoor> CODEC = MapCodec.unit(LeverDoor::new);

		@Override
		public Object start(AgentProgram program) {
			return prepare(program.world(), program.startPos(), program.startFacing());
		}

		@Override
		public Object prepare(ServerLevel world, BlockPos start, Direction ahead) {
			Object scene = scene(world, start, ahead);
			return scene instanceof Scene built ? new Probe(built,
					List.of(new boolean[] { false, false }, new boolean[] { true, false }, new boolean[] { false, false },
							new boolean[] { true, false }),
					(at, levers) -> at.open() == levers[0]) : scene;
		}

		@Override
		public boolean watch(AgentProgram program, @Nullable Object memo) {
			// No room for the scene: stop at once, so the student is told instead of waiting.
			return memo instanceof NoRoom || memo instanceof Probe probe && probe.tick();
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return memo instanceof Probe probe && probe.passed;
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (memo instanceof NoRoom noRoom) {
				return noRoom.text();
			}
			if (memo instanceof Probe probe && probe.failedAt != null) {
				return probe.failedAt[0]
						? Component.translatableWithFallback("mine.lesson.check.door_closed",
								"Not yet: MINE put the lever on, but the door stayed closed.")
						: Component.translatableWithFallback("mine.lesson.check.door_open",
								"Not yet: MINE put the lever off, but the door stayed open.");
			}
			return endedEarly();
		}
	}

	/**
	 * Tested live: MINE tries the two levers in all four ways, and the lamp behind the Agent
	 * must be on only when both are on (an AND gate).
	 */
	record LeverAnd() implements Check {

		static final MapCodec<LeverAnd> CODEC = MapCodec.unit(LeverAnd::new);

		@Override
		public Object start(AgentProgram program) {
			return prepare(program.world(), program.startPos(), program.startFacing());
		}

		@Override
		public Object prepare(ServerLevel world, BlockPos start, Direction ahead) {
			Object scene = scene(world, start, ahead);
			return scene instanceof Scene built ? new Probe(built,
					List.of(new boolean[] { false, false }, new boolean[] { true, false }, new boolean[] { false, true },
							new boolean[] { true, true }, new boolean[] { false, false }),
					(at, levers) -> at.lit() == (levers[0] && levers[1])) : scene;
		}

		@Override
		public boolean watch(AgentProgram program, @Nullable Object memo) {
			// No room for the scene: stop at once, so the student is told instead of waiting.
			return memo instanceof NoRoom || memo instanceof Probe probe && probe.tick();
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return memo instanceof Probe probe && probe.passed;
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (memo instanceof NoRoom noRoom) {
				return noRoom.text();
			}
			if (memo instanceof Probe probe && probe.failedAt != null) {
				return Component.translatableWithFallback("mine.lesson.check.and_gate",
						"Not yet: with the left lever %s and the right one %s, the lamp was %s. It must be on only when both levers are on.",
						onOff(probe.failedAt[0]), onOff(probe.failedAt[1]), onOff(!(probe.failedAt[0] && probe.failedAt[1])));
			}
			return endedEarly();
		}
	}

	/** The program ended by itself before MINE had tried the levers. */
	private static Component endedEarly() {
		return Component.translatableWithFallback("mine.lesson.check.listen",
				"Not yet: your program ended before MINE could try the levers. Keep it listening: while true do ... end.");
	}
}
