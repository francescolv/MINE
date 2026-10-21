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
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lesson 15, the farm: nested loops and conditions, like Minecraft Education's farming lessons
 * with the Agent. Each time a program starts, MINE digs a field in the ground in front of Byte:
 * {@code width} rows (to its right) of {@code length} farmland (ahead), with a water channel on
 * its left to keep it wet. Byte flies over the crops, one block above them, as farming turtles do:
 * it plants with {@code byte.placeDown()} and harvests with {@code byte.digDown()}. Then the
 * ground goes back as it was.
 */
final class FarmChecks {

	/** A young crop: never ripe during a program, and never as young as a crop just planted. */
	private static final int YOUNG_MIN = 2;
	private static final int YOUNG_MAX = 4;

	private FarmChecks() {
	}

	/**
	 * {@code harvest}: the field is planted, some crops ripe (in new places every time), the others
	 * young; Byte harvests the ripe ones and plants again, leaving the young ones. Otherwise the
	 * field is empty, and Byte sows it all. {@code count}: Byte also says how many it harvested.
	 */
	record Farm(int width, int length, boolean harvest, boolean count) implements Check {

		static final MapCodec<Farm> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.intRange(1, 4).fieldOf("width").forGetter(Farm::width),
				Codec.intRange(1, 8).fieldOf("length").forGetter(Farm::length),
				Codec.BOOL.optionalFieldOf("harvest", false).forGetter(Farm::harvest),
				Codec.BOOL.optionalFieldOf("count", false).forGetter(Farm::count)
		).apply(instance, Farm::new));

		@Override
		public Object prepare(ServerLevel world, BlockPos start, Direction ahead) {
			return Field.dig(world, start, ahead, width, length, harvest);
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			if (!(memo instanceof Field field)) {
				return false;
			}
			if (!harvest) {
				return field.sown() == field.crops().size();
			}
			return field.harvested() == field.ripe().size() && field.youngCut() == 0
					&& (!count || Checks.numbersSaid(program).contains(field.ripe().size()));
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (memo instanceof Checks.NoRoom noRoom) {
				return noRoom.text();
			}
			if (!(memo instanceof Field field)) {
				return null;
			}
			if (!harvest) {
				return Component.translatableWithFallback("mine.lesson.check.farm_sown",
						"Not yet: %s of the %s farmland blocks have seeds. Byte flies one block above the field (byte.up() first) and plants with byte.placeDown().",
						field.sown(), field.crops().size());
			}
			if (field.harvested() < field.ripe().size() || field.youngCut() > 0) {
				return Component.translatableWithFallback("mine.lesson.check.farm_harvest",
						"Not yet: there were %s ripe crops: Byte harvested and planted again %s, and cut %s young ones.",
						field.ripe().size(), field.harvested(), field.youngCut());
			}
			List<Integer> said = Checks.numbersSaid(program);
			return Component.translatableWithFallback("mine.lesson.check.farm_count",
					"Not yet: Byte harvested %s crops, but said %s. Count them with a variable.",
					field.ripe().size(), said.isEmpty() ? "-" : said.getLast().toString());
		}

		@Override
		public void ended(AgentProgram program, @Nullable Object memo, boolean passed) {
			if (memo instanceof Field field) {
				field.remove();
			}
		}

	}

	/**
	 * The field: where crops grow ({@code crops}, at Byte's starting level, on farmland just
	 * below), which of them MINE made ripe, and the ground it changed, to put back.
	 */
	record Field(ServerLevel world, List<BlockPos> crops, List<BlockPos> ripe, Map<BlockPos, BlockState> ground)
			implements Checks.Challenge {

		static Object dig(ServerLevel world, BlockPos start, Direction ahead, int width, int length, boolean planted) {
			Direction right = ahead.getClockWise();
			List<BlockPos> crops = new ArrayList<>();
			List<BlockPos> water = new ArrayList<>();
			for (int forward = 1; forward <= length; forward++) {
				for (int across = -1; across < width; across++) {
					BlockPos pos = start.relative(ahead, forward).relative(right, across);
					(across < 0 ? water : crops).add(pos);
				}
			}
			// The ground under the field and the channel, free room for the crops and for Byte above them.
			List<BlockPos> all = new ArrayList<>(crops);
			all.addAll(water);
			for (BlockPos pos : all) {
				if (!world.getBlockState(pos.below()).isSolidRender() || !world.getBlockState(pos).canBeReplaced()
						|| !world.getBlockState(pos.above()).canBeReplaced()) {
					return new Checks.NoRoom(length + 1);
				}
			}
			if (!world.getBlockState(start.above()).canBeReplaced()) {
				return new Checks.NoRoom(length + 1);
			}
			Map<BlockPos, BlockState> ground = new LinkedHashMap<>();
			for (BlockPos pos : water) {
				ground.put(pos.below(), world.getBlockState(pos.below()));
				world.setBlock(pos.below(), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
			}
			for (BlockPos pos : crops) {
				ground.put(pos.below(), world.getBlockState(pos.below()));
				world.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE),
						Block.UPDATE_ALL);
			}
			List<BlockPos> ripe = new ArrayList<>();
			if (planted) {
				RandomSource random = world.getRandom();
				// At least one ripe and one young (a field of one: just ripe), in new places every time.
				List<BlockPos> shuffled = new ArrayList<>(crops);
				java.util.Collections.shuffle(shuffled, new java.util.Random(random.nextLong()));
				int howMany = crops.size() < 2 ? crops.size() : 1 + random.nextInt(crops.size() - 1);
				ripe.addAll(shuffled.subList(0, howMany));
				for (BlockPos pos : crops) {
					int age = ripe.contains(pos) ? CropBlock.MAX_AGE : YOUNG_MIN + random.nextInt(YOUNG_MAX - YOUNG_MIN + 1);
					world.setBlock(pos, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, age), Block.UPDATE_ALL);
				}
			}
			return new Field(world, crops, ripe, ground);
		}

		private BlockState crop(BlockPos pos) {
			return world.getBlockState(pos);
		}

		/** Crops planted (any age) where there is one. */
		int sown() {
			return (int) crops.stream().filter(pos -> crop(pos).getBlock() instanceof CropBlock).count();
		}

		/** Ripe crops harvested and planted again: young now, as a seed just planted. */
		int harvested() {
			return (int) ripe.stream().filter(pos -> crop(pos).getBlock() instanceof CropBlock
					&& crop(pos).getValue(CropBlock.AGE) < YOUNG_MIN).count();
		}

		/** Young crops Byte cut (gone, or planted again). */
		int youngCut() {
			return (int) crops.stream().filter(pos -> !ripe.contains(pos)).filter(pos -> !(crop(pos).getBlock() instanceof CropBlock)
					|| crop(pos).getValue(CropBlock.AGE) < YOUNG_MIN).count();
		}

		@Override
		public boolean untouched() {
			return crops.stream().allMatch(pos -> ripe.contains(pos) ? crop(pos).is(Blocks.WHEAT) && crop(pos).getValue(CropBlock.AGE) == CropBlock.MAX_AGE
					: ripe.isEmpty() ? crop(pos).isAir() : crop(pos).is(Blocks.WHEAT));
		}

		@Override
		public void remove() {
			crops.forEach(pos -> world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
			ground.forEach((pos, state) -> world.setBlock(pos, state, Block.UPDATE_ALL));
		}
	}
}
