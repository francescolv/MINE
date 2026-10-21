package io.github.francescolv.mine.lesson;

import com.mojang.serialization.MapCodec;
import io.github.francescolv.mine.agent.AgentProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
			Scenes.ironDoor(blocks, door, ahead);
			blocks.put(agent.relative(left), Blocks.STONE.defaultBlockState());
			blocks.put(agent.relative(left, 2), lever(left));
			blocks.put(agent.relative(right), Blocks.STONE.defaultBlockState());
			blocks.put(agent.relative(right, 2), lever(right));
			if (!Scenes.fits(world, blocks, List.of(door))) {
				return new NoRoom();
			}
			Scenes.place(world, blocks);
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
			return Scenes.lit(world, lamp());
		}

		boolean open() {
			return Scenes.open(world, door());
		}

		/** Puts the levers (left, right) on or off. */
		void pull(boolean[] levers) {
			pull(true, levers[0]);
			pull(false, levers[1]);
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
			return Scenes.untouched(world, blocks);
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
					"Not yet: MINE needs room around Byte for the lamp, the door and the levers, and solid ground in front. Go to an open space and type /mine byte recall: your Byte comes to you.");
		}
	}

	/** Done when the lamp behind the Agent is on when the program ends. */
	record Lamp() implements Check {

		static final MapCodec<Lamp> CODEC = MapCodec.unit(Lamp::new);

		@Override
		public Object prepare(ServerLevel world, BlockPos start, Direction ahead) {
			return Scene.build(world, start, ahead);
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
	 * Tested live: MINE pulls the levers in turn ({@link #moves}, left and right), and after each
	 * move the scene must be {@link #right}.
	 */
	interface LeverTest extends Check {

		List<boolean[]> moves();

		boolean right(Scene scene, boolean[] levers);

		/** What went wrong with the levers as they were. */
		Component failed(boolean[] levers);

		/**
		 * The levers are set the first way at once; then, after each look, MINE sets them the next
		 * way and gives the program time to answer.
		 */
		@Override
		default Object prepare(ServerLevel world, BlockPos start, Direction ahead) {
			if (!(Scene.build(world, start, ahead) instanceof Scene scene)) {
				return new NoRoom();
			}
			List<boolean[]> moves = moves();
			scene.pull(moves.getFirst());
			LiveTest test = new LiveTest(scene, SETTLE_MS);
			for (int i = 0; i < moves.size(); i++) {
				boolean[] levers = moves.get(i);
				boolean[] then = i + 1 < moves.size() ? moves.get(i + 1) : null;
				test.then(() -> right(scene, levers), () -> failed(levers), then == null ? null : () -> scene.pull(then), STEP_MS);
			}
			return test;
		}

		@Override
		default boolean watch(AgentProgram program, @Nullable Object memo) {
			// No room for the scene: stop at once, so the student is told instead of waiting.
			return memo instanceof NoRoom || memo instanceof LiveTest test && test.tick();
		}

		@Override
		default boolean passed(AgentProgram program, @Nullable Object memo) {
			return memo instanceof LiveTest test && test.passed;
		}

		@Override
		default @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (memo instanceof NoRoom noRoom) {
				return noRoom.text();
			}
			if (memo instanceof LiveTest test && test.failure != null) {
				return test.failure;
			}
			return endedEarly();
		}
	}

	/**
	 * MINE pulls the left lever on, off and on again, and each time the door in front of the
	 * Agent must follow it, open with the lever on and closed with it off.
	 */
	record LeverDoor() implements LeverTest {

		static final MapCodec<LeverDoor> CODEC = MapCodec.unit(LeverDoor::new);

		@Override
		public List<boolean[]> moves() {
			return List.of(new boolean[] { false, false }, new boolean[] { true, false }, new boolean[] { false, false },
					new boolean[] { true, false });
		}

		@Override
		public boolean right(Scene scene, boolean[] levers) {
			return scene.open() == levers[0];
		}

		@Override
		public Component failed(boolean[] levers) {
			return levers[0]
					? Component.translatableWithFallback("mine.lesson.check.door_closed",
							"Not yet: MINE put the lever on, but the door stayed closed.")
					: Component.translatableWithFallback("mine.lesson.check.door_open",
							"Not yet: MINE put the lever off, but the door stayed open.");
		}
	}

	/**
	 * MINE tries the two levers in all four ways, and the lamp behind the Agent must be on only
	 * when both are on (an AND gate).
	 */
	record LeverAnd() implements LeverTest {

		static final MapCodec<LeverAnd> CODEC = MapCodec.unit(LeverAnd::new);

		@Override
		public List<boolean[]> moves() {
			return List.of(new boolean[] { false, false }, new boolean[] { true, false }, new boolean[] { false, true },
					new boolean[] { true, true }, new boolean[] { false, false });
		}

		@Override
		public boolean right(Scene scene, boolean[] levers) {
			return scene.lit() == (levers[0] && levers[1]);
		}

		@Override
		public Component failed(boolean[] levers) {
			return Component.translatableWithFallback("mine.lesson.check.and_gate",
					"Not yet: with the left lever %s and the right one %s, the lamp was %s. It must be on only when both levers are on.",
					Checks.onOff(levers[0]), Checks.onOff(levers[1]), Checks.onOff(!(levers[0] && levers[1])));
		}
	}

	/** The program ended by itself before MINE had tried the levers. */
	private static Component endedEarly() {
		return Component.translatableWithFallback("mine.lesson.check.listen",
				"Not yet: your program ended before MINE could try the levers. Keep it listening with a loop that never ends (while true do ... end, or \"repeat while true\").");
	}
}
