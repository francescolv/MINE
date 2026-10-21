package io.github.francescolv.mine.lesson;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.francescolv.mine.agent.AgentProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lesson 11, the treasure hunt: coordinates and variables, like Minecraft Education's lessons on
 * coordinates. Each time a program starts, MINE buries gold in the ground near the Agent, at
 * places counted from where it starts: so many blocks forward, so many to its right. The program
 * reads them with {@code mine.challenge()}: forward and right of the first treasure, then of the
 * second... (with blocks, {@code mine.challenge(n)} gives the n-th number).
 */
final class TreasureChecks {

	/** How far the treasures can be: forward from 1, to the right from 0 (in the lab, inside the lane). */
	private static final int MAX_FORWARD = 6;
	private static final int MAX_RIGHT = 4;

	private TreasureChecks() {
	}

	/**
	 * {@code count} treasures. Without {@code dig}, Byte must stop right on top of the (only) one;
	 * with it, dig them all up from above ({@code byte.digDown()}), and with {@code home} come back
	 * where it started.
	 */
	record Treasure(int count, boolean dig, boolean home) implements Check {

		static final MapCodec<Treasure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.intRange(1, 5).fieldOf("count").forGetter(Treasure::count),
				Codec.BOOL.optionalFieldOf("dig", false).forGetter(Treasure::dig),
				Codec.BOOL.optionalFieldOf("home", false).forGetter(Treasure::home)
		).apply(instance, Treasure::new));

		@Override
		public Object start(AgentProgram program) {
			Object memo = prepare(program.world(), program.startPos(), program.startFacing());
			reuse(program, memo);
			return memo;
		}

		@Override
		public Object prepare(ServerLevel world, BlockPos start, Direction ahead) {
			return Buried.bury(world, start, ahead, count);
		}

		@Override
		public void reuse(AgentProgram program, @Nullable Object memo) {
			if (memo instanceof Buried buried) {
				program.setChallenge(buried.numbers().toArray());
			}
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			if (!(memo instanceof Buried buried)) {
				return false;
			}
			if (!dig) {
				return program.pos().equals(buried.spots().getFirst());
			}
			return buried.left() == 0 && (!home || program.pos().equals(program.startPos()));
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (memo instanceof Checks.NoRoom noRoom) {
				return noRoom.text();
			}
			if (!(memo instanceof Buried buried)) {
				return null;
			}
			int[] at = buried.local(program.pos());
			if (!dig) {
				return Component.translatableWithFallback("mine.lesson.check.treasure_where",
						"Not yet: the treasure was %s forward and %s to the right; Byte stopped %s forward and %s to the right.",
						buried.numbers().get(0), buried.numbers().get(1), at[0], at[1]);
			}
			if (buried.left() > 0) {
				return Component.translatableWithFallback("mine.lesson.check.treasure_left",
						"Not yet: %s of %s treasures are still in the ground. Byte digs one standing right on top of it: byte.digDown().",
						buried.left(), count);
			}
			return Component.translatableWithFallback("mine.lesson.check.treasure_home",
					"Not yet: all the treasure is dug up, but Byte isn't home: it's %s forward and %s to the right of where it started.",
					at[0], at[1]);
		}

		/** The ground goes back as it was, passed or not, for the next try. */
		@Override
		public void ended(AgentProgram program, @Nullable Object memo, boolean passed) {
			if (memo instanceof Buried buried) {
				buried.remove();
			}
		}
	}

	/**
	 * The gold MINE buried: in the ground under each spot (where Byte stands to dig it), and what
	 * the ground was before, to put it back. {@code numbers}: forward and right of each spot.
	 */
	record Buried(ServerLevel world, BlockPos start, Direction ahead, List<BlockPos> spots, List<Integer> numbers,
			Map<BlockPos, BlockState> ground) implements Checks.Challenge {

		static Object bury(ServerLevel world, BlockPos start, Direction ahead, int count) {
			Direction right = ahead.getClockWise();
			// Room to walk: Byte's level over the whole area, and solid ground under it to hide gold in.
			for (int forward = 0; forward <= MAX_FORWARD; forward++) {
				for (int across = 0; across <= MAX_RIGHT; across++) {
					BlockPos pos = start.relative(ahead, forward).relative(right, across);
					if (!pos.equals(start) && !world.getBlockState(pos).canBeReplaced()
							|| !world.getBlockState(pos.below()).isSolidRender()) {
						return new Checks.NoRoom(MAX_FORWARD + 1);
					}
				}
			}
			RandomSource random = world.getRandom();
			List<BlockPos> spots = new ArrayList<>();
			List<Integer> numbers = new ArrayList<>();
			while (spots.size() < count) {
				int forward = 1 + random.nextInt(MAX_FORWARD);
				int across = random.nextInt(MAX_RIGHT + 1);
				BlockPos spot = start.relative(ahead, forward).relative(right, across);
				if (!spots.contains(spot)) {
					spots.add(spot);
					numbers.add(forward);
					numbers.add(across);
				}
			}
			Map<BlockPos, BlockState> ground = new LinkedHashMap<>();
			for (BlockPos spot : spots) {
				ground.put(spot.below(), world.getBlockState(spot.below()));
				world.setBlock(spot.below(), Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
			}
			return new Buried(world, start, ahead, spots, numbers, ground);
		}

		/** How many treasures are still in the ground. */
		int left() {
			return (int) ground.keySet().stream().filter(pos -> world.getBlockState(pos).is(Blocks.GOLD_BLOCK)).count();
		}

		/** {@code pos} counted from the start: blocks forward, blocks to the right. */
		int[] local(BlockPos pos) {
			BlockPos offset = pos.subtract(start);
			Direction right = ahead.getClockWise();
			return new int[] {
					offset.getX() * ahead.getStepX() + offset.getZ() * ahead.getStepZ(),
					offset.getX() * right.getStepX() + offset.getZ() * right.getStepZ() };
		}

		@Override
		public boolean untouched() {
			return left() == ground.size();
		}

		@Override
		public void remove() {
			ground.forEach((pos, state) -> world.setBlock(pos, state, Block.UPDATE_ALL));
		}
	}
}
