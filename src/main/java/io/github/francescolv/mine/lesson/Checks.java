package io.github.francescolv.mine.lesson;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.francescolv.mine.agent.AgentProgram;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The checks lessons are built from. Each one looks at the world, never at the student's code.
 * In a lesson file a check is written as its type plus its parameters, e.g.
 * {@code {"type": "tower", "height": 3}}.
 */
final class Checks {

	/** The Agent spoke in chat with {@code mine.say}. */
	record Said() implements Check {

		static final MapCodec<Said> CODEC = MapCodec.unit(Said::new);

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return program.says() > 0;
		}
	}

	/**
	 * The Agent went at least {@code distance} blocks away and came back to where it started,
	 * facing the same way. The distance rules out an empty program.
	 */
	record Roundtrip(int distance) implements Check {

		static final MapCodec<Roundtrip> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.intRange(1, 64).fieldOf("distance").forGetter(Roundtrip::distance)
		).apply(instance, Roundtrip::new));

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return program.maxDistance() >= distance
					&& program.pos().equals(program.startPos())
					&& program.facing() == program.startFacing();
		}
	}

	/**
	 * There is now a column of {@code height} blocks right in front of where the Agent started,
	 * where there was free space before, and the Agent is back at its starting height.
	 */
	record Tower(int height) implements Check {

		static final MapCodec<Tower> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.intRange(1, 64).fieldOf("height").forGetter(Tower::height)
		).apply(instance, Tower::new));

		@Override
		public Object start(AgentProgram program) {
			BlockPos base = program.startPos().relative(program.startFacing());
			for (int y = 0; y < height; y++) {
				if (!program.world().getBlockState(base.above(y)).canBeReplaced()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object wasFree) {
			return Boolean.TRUE.equals(wasFree) && program.pos().getY() == program.startPos().getY()
					&& built(program);
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object wasFree) {
			if (!Boolean.TRUE.equals(wasFree) || built(program)) {
				return null;
			}
			return outOfBlocks(program, height);
		}

		private boolean built(AgentProgram program) {
			BlockPos base = program.startPos().relative(program.startFacing());
			for (int y = 0; y < height; y++) {
				BlockPos pos = base.above(y);
				if (pos.equals(program.pos()) || program.world().getBlockState(pos).canBeReplaced()) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * The program built a solid box of blocks, anywhere near where the Agent started: a row, a
	 * floor, a wall, a cube... {@code length} and {@code width} are the two horizontal sides, in
	 * either direction; {@code height} is vertical. Only blocks placed where there was free space
	 * count, so whatever was already there doesn't help.
	 */
	record Build(int length, int width, int height) implements Check {

		static final MapCodec<Build> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.intRange(1, 16).fieldOf("length").forGetter(Build::length),
				Codec.intRange(1, 16).optionalFieldOf("width", 1).forGetter(Build::width),
				Codec.intRange(1, 16).optionalFieldOf("height", 1).forGetter(Build::height)
		).apply(instance, Build::new));

		/** Free space around the Agent's starting point, when the program started. */
		private record Area(BlockPos min, int sizeX, int sizeY, int sizeZ, boolean[] free) {

			boolean wasFree(BlockPos pos) {
				return free[index(pos)];
			}

			int index(BlockPos pos) {
				int x = pos.getX() - min.getX();
				int y = pos.getY() - min.getY();
				int z = pos.getZ() - min.getZ();
				return (x * sizeY + y) * sizeZ + z;
			}

			Iterable<BlockPos> positions() {
				return BlockPos.betweenClosed(min, min.offset(sizeX - 1, sizeY - 1, sizeZ - 1));
			}
		}

		/** A group of touching new blocks, its lowest corner and the box around it. */
		private record Shape(Set<BlockPos> positions, BlockPos min, int sizeX, int sizeY, int sizeZ) {

			int blocks() {
				return positions.size();
			}
		}

		/** How far from the start the shape may be: its longest side plus some room to move around. */
		private int reach() {
			return Math.max(length, width) + 2;
		}

		@Override
		public Object start(AgentProgram program) {
			int reach = reach();
			BlockPos min = program.startPos().offset(-reach, -height - 2, -reach);
			Area area = new Area(min, 2 * reach + 1, 2 * height + 5, 2 * reach + 1,
					new boolean[(2 * reach + 1) * (2 * height + 5) * (2 * reach + 1)]);
			for (BlockPos pos : area.positions()) {
				// The Agent itself stands on its starting point, which is free for building.
				area.free()[area.index(pos)] = pos.equals(program.startPos())
						|| program.world().getBlockState(pos).canBeReplaced();
			}
			return area;
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return shapes(program, memo).stream().anyMatch(this::matches);
		}

		/**
		 * The Agent ran out of blocks, or built something of the wrong size: says how big it is,
		 * so the student can compare it with the goal (this is where area and volume come in).
		 */
		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			return measure(program, shapes(program, memo), length * width * height);
		}

		@Nullable
		private static Shape biggest(List<Shape> shapes) {
			Shape biggest = null;
			for (Shape shape : shapes) {
				if (biggest == null || shape.blocks() > biggest.blocks()) {
					biggest = shape;
				}
			}
			return biggest;
		}

		/** Out of blocks, or the size of the biggest thing built; {@code null} if nothing was built. */
		private static @Nullable Component measure(AgentProgram program, List<Shape> shapes, int needed) {
			Shape biggest = biggest(shapes);
			// Nothing built at all, e.g. a warm-up program: the step's usual message says it best.
			if (biggest == null) {
				return null;
			}
			Component empty = biggest.blocks() < needed ? outOfBlocks(program, needed) : null;
			if (empty != null) {
				return empty;
			}
			return Component.translatableWithFallback("mine.lesson.check.wrong_shape",
					"Not yet: your Byte built %s blocks: the build is %s long, %s wide and %s tall. Compare it with the goal: what's different?",
					biggest.blocks(), Math.max(biggest.sizeX(), biggest.sizeZ()),
					Math.min(biggest.sizeX(), biggest.sizeZ()), biggest.sizeY());
		}

		private boolean matches(Shape shape) {
			boolean sidesMatch = (shape.sizeX() == length && shape.sizeZ() == width)
					|| (shape.sizeX() == width && shape.sizeZ() == length);
			return sidesMatch && shape.sizeY() == height && shape.blocks() == length * width * height;
		}

		/** The shapes found at the end of a program, in the game tick they were looked for: passed() and whyNot() both ask. */
		private static final Map<Area, List<Shape>> SEEN = new java.util.WeakHashMap<>();
		private static long seenAt = -1;

		/** The new blocks near the start, split into groups of blocks that touch each other. */
		private List<Shape> shapes(AgentProgram program, @Nullable Object memo) {
			if (!(memo instanceof Area area)) {
				return List.of();
			}
			long now = program.world().getGameTime();
			if (now != seenAt) {
				SEEN.clear();
				seenAt = now;
			}
			return SEEN.computeIfAbsent(area, built -> findShapes(program, built));
		}

		private static List<Shape> findShapes(AgentProgram program, Area area) {
			Set<BlockPos> placed = new HashSet<>();
			for (BlockPos pos : area.positions()) {
				if (area.wasFree(pos) && !pos.equals(program.pos())
						&& !program.world().getBlockState(pos).canBeReplaced()) {
					placed.add(pos.immutable());
				}
			}
			List<Shape> shapes = new ArrayList<>();
			while (!placed.isEmpty()) {
				BlockPos first = placed.iterator().next();
				placed.remove(first);
				Deque<BlockPos> todo = new ArrayDeque<>(List.of(first));
				Set<BlockPos> positions = new HashSet<>();
				BlockPos.MutableBlockPos min = first.mutable();
				BlockPos.MutableBlockPos max = first.mutable();
				while (!todo.isEmpty()) {
					BlockPos pos = todo.poll();
					positions.add(pos);
					min.set(Math.min(min.getX(), pos.getX()), Math.min(min.getY(), pos.getY()), Math.min(min.getZ(), pos.getZ()));
					max.set(Math.max(max.getX(), pos.getX()), Math.max(max.getY(), pos.getY()), Math.max(max.getZ(), pos.getZ()));
					for (Direction side : Direction.values()) {
						BlockPos next = pos.relative(side);
						if (placed.remove(next)) {
							todo.add(next);
						}
					}
				}
				shapes.add(new Shape(positions, min.immutable(), max.getX() - min.getX() + 1,
						max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1));
			}
			return shapes;
		}
	}

	/**
	 * Blocks MINE placed to build a challenge in front of the Agent, so they can be taken away
	 * again. Only placed if every position involved is free: MINE never replaces what's there.
	 */
	record Course(ServerLevel world, Map<BlockPos, Block> blocks) {

		/** Builds {@code blocks}, or returns {@code null} if they or {@code room} aren't all free. */
		static @Nullable Course build(ServerLevel world, Map<BlockPos, Block> blocks, List<BlockPos> room) {
			for (BlockPos pos : blocks.keySet()) {
				if (!world.getBlockState(pos).canBeReplaced()) {
					return null;
				}
			}
			for (BlockPos pos : room) {
				if (!world.getBlockState(pos).canBeReplaced()) {
					return null;
				}
			}
			blocks.forEach((pos, block) -> world.setBlockAndUpdate(pos, block.defaultBlockState()));
			return new Course(world, blocks);
		}

		boolean intact(BlockPos pos) {
			return world.getBlockState(pos).is(blocks.get(pos));
		}

		/** Whether all its blocks are still there. */
		boolean untouched() {
			return blocks.keySet().stream().allMatch(this::intact);
		}

		/** Takes away the blocks of the course that are still there. */
		void remove() {
			blocks.keySet().stream().filter(this::intact).forEach(pos -> world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
		}
	}

	/** A challenge MINE built in the world, as remembered by a check. */
	interface Challenge {

		/** Whether nobody has touched it yet. */
		boolean untouched();

		/** Takes away what is left of it. */
		void remove();
	}

	/** There wasn't enough free space to build the challenge: {@code needed} blocks in front. */
	record NoRoom(int needed) {

		Component text() {
			return Component.translatableWithFallback("mine.lesson.check.no_room",
					"Not yet: MINE needs %s free blocks in front of Byte to build the challenge. Go to an open space and type /mine byte recall: your Byte comes to you.",
					needed);
		}
	}

	/** A brick wall MINE built across the Agent's way, {@code distance} blocks ahead. */
	private record Wall(Course course, BlockPos center, int distance) implements Challenge {

		/**
		 * Builds a wall 3 blocks wide at a random distance ahead of {@code start}, with free
		 * space to reach it, climb over it and land behind it; or says how much room it needs.
		 */
		static Object build(ServerLevel world, BlockPos start, Direction ahead, int minDistance, int maxDistance) {
			Direction side = ahead.getClockWise();
			int distance = minDistance + world.getRandom().nextInt(maxDistance - minDistance + 1);
			BlockPos center = start.relative(ahead, distance);

			Map<BlockPos, Block> wall = new LinkedHashMap<>();
			for (BlockPos pos : List.of(center.relative(side.getOpposite()), center, center.relative(side))) {
				wall.put(pos, Blocks.BRICKS);
			}
			// The way there, over the wall and down behind it.
			List<BlockPos> room = new ArrayList<>();
			for (int i = 1; i <= distance + 1; i++) {
				BlockPos pos = start.relative(ahead, i);
				if (i != distance) {
					room.add(pos);
				}
				room.add(pos.above());
			}
			Course course = Course.build(world, wall, room);
			return course == null ? new NoRoom(maxDistance + 2) : new Wall(course, center, distance);
		}

		@Override
		public boolean untouched() {
			return course.untouched();
		}

		@Override
		public void remove() {
			course.remove();
		}
	}

	/**
	 * Each time a program starts, MINE builds a wall across the Agent's way, between
	 * {@code min_distance} and {@code max_distance} blocks ahead, never at a distance the student
	 * can count on. Done when the Agent has climbed over it and is right behind it, at its
	 * starting height, with the wall untouched. A wall that wasn't passed is taken away.
	 */
	record OverTheWall(int minDistance, int maxDistance) implements Check {

		static final MapCodec<OverTheWall> CODEC = rangeCodec("distance", 1, 12, OverTheWall::new, OverTheWall::minDistance, OverTheWall::maxDistance);

		@Override
		public Object prepare(ServerLevel world, BlockPos start, Direction ahead) {
			return Wall.build(world, start, ahead, minDistance, maxDistance);
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return memo instanceof Wall wall
					&& program.pos().equals(wall.center().relative(program.startFacing()))
					&& wall.untouched();
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (memo instanceof NoRoom noRoom) {
				return noRoom.text();
			}
			if (memo instanceof Wall wall) {
				return Component.translatableWithFallback("mine.lesson.check.wall",
						"Not yet: this time the wall was %s blocks away. Byte has to land right behind it, back on the ground, without breaking it.",
						wall.distance());
			}
			return null;
		}

		@Override
		public void ended(AgentProgram program, @Nullable Object memo, boolean passed) {
			if (!passed && memo instanceof Wall wall) {
				wall.course().remove();
			}
		}
	}

	/**
	 * Each time a program starts, MINE lays a row of {@code length} blocks in front of the Agent,
	 * at its height: stone, with between {@code min_gold} and {@code max_gold} gold blocks in
	 * random places. Done when all the gold has been dug up and all the stone is still there.
	 */
	record GoldRow(int length, int minGold, int maxGold) implements Check {

		static final MapCodec<GoldRow> CODEC = RecordCodecBuilder.<GoldRow>mapCodec(instance -> instance.group(
				Codec.intRange(2, 16).fieldOf("length").forGetter(GoldRow::length),
				Codec.intRange(1, 16).fieldOf("min_gold").forGetter(GoldRow::minGold),
				Codec.intRange(1, 16).fieldOf("max_gold").forGetter(GoldRow::maxGold)
		).apply(instance, GoldRow::new)).validate(check -> check.minGold() <= check.maxGold() && check.maxGold() < check.length()
				? DataResult.success(check) : DataResult.error(() -> "Need min_gold <= max_gold < length"));

		private record Row(Course course, Set<BlockPos> gold) implements Challenge {

			@Override
			public boolean untouched() {
				return course.untouched();
			}

			@Override
			public void remove() {
				course.remove();
			}

			long goldLeft() {
				return gold.stream().filter(course::intact).count();
			}

			long stoneDug() {
				return course.blocks().keySet().stream().filter(pos -> !gold.contains(pos) && !course.intact(pos)).count();
			}
		}

		@Override
		public Object prepare(ServerLevel world, BlockPos start, Direction ahead) {
			List<BlockPos> row = new ArrayList<>();
			List<BlockPos> room = new ArrayList<>(List.of(start.above()));
			for (int i = 1; i <= length; i++) {
				row.add(start.relative(ahead, i));
				room.add(start.relative(ahead, i).above());
			}
			List<BlockPos> shuffled = new ArrayList<>(row);
			Collections.shuffle(shuffled, new java.util.Random(world.getRandom().nextLong()));
			int count = minGold + world.getRandom().nextInt(maxGold - minGold + 1);
			Set<BlockPos> gold = new HashSet<>(shuffled.subList(0, count));

			Map<BlockPos, Block> blocks = new LinkedHashMap<>();
			row.forEach(pos -> blocks.put(pos, gold.contains(pos) ? Blocks.GOLD_BLOCK : Blocks.STONE));
			Course course = Course.build(world, blocks, room);
			return course == null ? new NoRoom(length + 1) : new Row(course, gold);
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return memo instanceof Row row && row.goldLeft() == 0 && row.stoneDug() == 0;
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (memo instanceof NoRoom noRoom) {
				return noRoom.text();
			}
			if (memo instanceof Row row) {
				return Component.translatableWithFallback("mine.lesson.check.gold",
						"Not yet: %s gold blocks are still in the row, and %s stone blocks were dug up.",
						row.goldLeft(), row.stoneDug());
			}
			return null;
		}

		@Override
		public void ended(AgentProgram program, @Nullable Object memo, boolean passed) {
			if (!passed && memo instanceof Row row) {
				row.course().remove();
			}
		}
	}

	/**
	 * A tower right in front of where the Agent started, using every block in its selected slot
	 * (at least {@code min_blocks}, as many as the student likes), and the Agent back on the
	 * ground. The student can't count on a number: the program has to notice when the blocks
	 * run out (turtle.place() returns false) and when it touches the ground.
	 */
	record TowerOfAll(int minBlocks) implements Check {

		static final MapCodec<TowerOfAll> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.intRange(1, 64).optionalFieldOf("min_blocks", 3).forGetter(TowerOfAll::minBlocks)
		).apply(instance, TowerOfAll::new));

		/** How many blocks the Agent had, and whether there was room for a tower that tall. */
		private record Stock(int blocks, boolean room) {
		}

		@Override
		public Object start(AgentProgram program) {
			int blocks = program.startSelectedItems();
			BlockPos base = program.startPos().relative(program.startFacing());
			boolean room = true;
			for (int y = 0; y <= blocks; y++) {
				room &= program.world().getBlockState(base.above(y)).canBeReplaced();
			}
			return new Stock(blocks, room);
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return memo instanceof Stock stock && stock.blocks() >= minBlocks && stock.room()
					&& height(program) == stock.blocks() && program.selectedItems() == 0 && onTheGround(program);
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (!(memo instanceof Stock stock)) {
				return null;
			}
			if (stock.blocks() < minBlocks) {
				return Component.translatableWithFallback("mine.lesson.check.few_blocks",
						"Not yet: before starting, put at least %s blocks in Byte's highlighted slot.", minBlocks);
			}
			if (!stock.room()) {
				return Component.translatableWithFallback("mine.lesson.check.no_room_tower",
						"Not yet: there's no room for a tower %s blocks tall in front of Byte. Go to an open space and type /mine byte recall: your Byte comes to you.",
						stock.blocks());
			}
			int height = height(program);
			if (height != stock.blocks()) {
				return Component.translatableWithFallback("mine.lesson.check.tower_of_all",
						"Not yet: Byte had %s blocks, and the tower is %s tall.", stock.blocks(), height);
			}
			return null;
		}

		/** How tall the column in front of the starting point is (the Agent itself doesn't count). */
		private static int height(AgentProgram program) {
			BlockPos base = program.startPos().relative(program.startFacing());
			int height = 0;
			while (height < 64 && !base.above(height).equals(program.pos())
					&& !program.world().getBlockState(base.above(height)).canBeReplaced()) {
				height++;
			}
			return height;
		}

		/** Not higher than where it started, and standing on something. */
		private static boolean onTheGround(AgentProgram program) {
			return program.pos().getY() <= program.startPos().getY()
					&& !program.world().getBlockState(program.pos().below()).canBeReplaced();
		}
	}

	/**
	 * Like {@link OverTheWall}, MINE builds a wall at a random distance each time a program
	 * starts. Done when the Agent stopped right in front of it and said, with mine.say, how many
	 * steps it took: that number, and no other (saying every number doesn't count).
	 */
	record CountToWall(int minDistance, int maxDistance) implements Check {

		static final MapCodec<CountToWall> CODEC = rangeCodec("distance", 2, 12, CountToWall::new, CountToWall::minDistance, CountToWall::maxDistance);

		@Override
		public Object prepare(ServerLevel world, BlockPos start, Direction ahead) {
			return Wall.build(world, start, ahead, minDistance, maxDistance);
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			if (!(memo instanceof Wall wall) || !atTheWall(program, wall)) {
				return false;
			}
			List<Integer> numbers = numbersSaid(program);
			return !numbers.isEmpty() && numbers.stream().allMatch(n -> n == wall.distance() - 1);
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (memo instanceof NoRoom noRoom) {
				return noRoom.text();
			}
			if (!(memo instanceof Wall wall)) {
				return null;
			}
			if (!atTheWall(program, wall)) {
				return Component.translatableWithFallback("mine.lesson.check.count_reach",
						"Not yet: Byte has to stop right in front of the wall.");
			}
			return Component.translatableWithFallback("mine.lesson.check.count_say",
					"Not yet: this time Byte took %s steps to reach the wall. Its messages had to say that number, and no other.",
					wall.distance() - 1);
		}

		@Override
		public void ended(AgentProgram program, @Nullable Object memo, boolean passed) {
			if (!passed && memo instanceof Wall wall) {
				wall.remove();
			}
		}

		private static boolean atTheWall(AgentProgram program, Wall wall) {
			return program.pos().equals(wall.center().relative(program.startFacing().getOpposite())) && wall.untouched();
		}

	}

	/**
	 * Each time a program starts, MINE chooses the side of a square, between {@code min_side} and
	 * {@code max_side}, and the program reads it with {@code mine.challenge()}. Done when it built
	 * a solid square floor of that size, as {@code build} would check it.
	 */
	record SquareOfChallenge(int minSide, int maxSide) implements Check {

		static final MapCodec<SquareOfChallenge> CODEC = rangeCodec("side", 1, 8, SquareOfChallenge::new, SquareOfChallenge::minSide, SquareOfChallenge::maxSide);

		/** The side MINE asked for, and the square check for that size with its memo. */
		private record Asked(int side, Build build, Object memo) {
		}

		@Override
		public Object start(AgentProgram program) {
			int side = minSide + program.world().getRandom().nextInt(maxSide - minSide + 1);
			program.setChallenge(side);
			Build build = new Build(side, side, 1);
			return new Asked(side, build, build.start(program));
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return memo instanceof Asked asked && asked.build().passed(program, asked.memo());
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (!(memo instanceof Asked asked)) {
				return null;
			}
			Build.Shape biggest = Build.biggest(asked.build().shapes(program, asked.memo()));
			if (biggest == null) {
				return Component.translatableWithFallback("mine.lesson.check.asked_square",
						"Not yet: this time mine.challenge() asked for a square %s × %s.", asked.side(), asked.side());
			}
			return Component.translatableWithFallback("mine.lesson.check.asked_square_built",
					"Not yet: this time MINE asked for a square %s × %s, and your Byte built %s blocks: the build is %s long and %s wide.",
					asked.side(), asked.side(), biggest.blocks(), Math.max(biggest.sizeX(), biggest.sizeZ()),
					Math.min(biggest.sizeX(), biggest.sizeZ()));
		}
	}

	/**
	 * A step pyramid: a solid {@code base} × {@code base} square, then squares 2 blocks smaller
	 * stacked on top and centred, up to a single block (or a 2 × 2 for an even base). Built
	 * anywhere near the start, facing any direction.
	 */
	record Pyramid(int base) implements Check {

		static final MapCodec<Pyramid> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.intRange(2, 9).fieldOf("base").forGetter(Pyramid::base)
		).apply(instance, Pyramid::new));

		private int layers() {
			return (base + 1) / 2;
		}

		/** How many blocks the whole pyramid takes. */
		private int blocks() {
			int blocks = 0;
			for (int side = base; side > 0; side -= 2) {
				blocks += side * side;
			}
			return blocks;
		}

		/** The area and the shapes are the same as a box as big as the pyramid. */
		private Build box() {
			return new Build(base, base, layers());
		}

		@Override
		public Object start(AgentProgram program) {
			return box().start(program);
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return box().shapes(program, memo).stream().anyMatch(this::isPyramid);
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			return Build.measure(program, box().shapes(program, memo), blocks());
		}

		private boolean isPyramid(Build.Shape shape) {
			if (shape.sizeX() != base || shape.sizeZ() != base || shape.sizeY() != layers() || shape.blocks() != blocks()) {
				return false;
			}
			BlockPos min = shape.min();
			for (int layer = 0; layer < layers(); layer++) {
				for (int x = layer; x < base - layer; x++) {
					for (int z = layer; z < base - layer; z++) {
						if (!shape.positions().contains(min.offset(x, layer, z))) {
							return false;
						}
					}
				}
			}
			return true;
		}
	}

	private static final Map<String, MapCodec<? extends Check>> TYPES = Map.ofEntries(
			Map.entry("said", Said.CODEC),
			Map.entry("roundtrip", Roundtrip.CODEC),
			Map.entry("tower", Tower.CODEC),
			Map.entry("build", Build.CODEC),
			Map.entry("over_the_wall", OverTheWall.CODEC),
			Map.entry("gold_row", GoldRow.CODEC),
			Map.entry("tower_of_all", TowerOfAll.CODEC),
			Map.entry("count_to_wall", CountToWall.CODEC),
			Map.entry("square_of_challenge", SquareOfChallenge.CODEC),
			Map.entry("pyramid", Pyramid.CODEC),
			Map.entry("redstone_lamp", RedstoneChecks.Lamp.CODEC),
			Map.entry("lever_door", RedstoneChecks.LeverDoor.CODEC),
			Map.entry("lever_and", RedstoneChecks.LeverAnd.CODEC),
			Map.entry("plate_door", SensorChecks.PlateDoor.CODEC),
			Map.entry("night_lamp", SensorChecks.NightLamp.CODEC),
			Map.entry("button_bridge", SensorChecks.ButtonBridge.CODEC),
			Map.entry("board_door", BoardChecks.BoardDoor.CODEC),
			Map.entry("board_lamp", BoardChecks.BoardLamp.CODEC),
			Map.entry("board_heart", BoardChecks.BoardHeart.CODEC),
			Map.entry("chart", ChartChecks.Chart.CODEC),
			Map.entry("chart_max", ChartChecks.Largest.CODEC),
			Map.entry("maze", MazeChecks.Maze.CODEC),
			Map.entry("treasure", TreasureChecks.Treasure.CODEC),
			Map.entry("greenhouse_average", ConnectedChecks.Average.CODEC),
			Map.entry("greenhouse_alarm", ConnectedChecks.Alarm.CODEC),
			Map.entry("greenhouse_report", ConnectedChecks.Report.CODEC),
			Map.entry("fan", ConnectedChecks.Fan.CODEC),
			Map.entry("crossing", ConnectedChecks.Crossing.CODEC),
			Map.entry("farm", FarmChecks.Farm.CODEC),
			Map.entry("cipher", CipherChecks.Cipher.CODEC));

	/** A check in a lesson file: {@code {"type": "...", ...parameters}}. Lesson files are only read, never written. */
	static final Codec<Check> CODEC = Codec.STRING.partialDispatch("type",
			check -> DataResult.error(() -> "Lesson checks are only read: " + check),
			type -> TYPES.containsKey(type)
					? DataResult.success(TYPES.get(type))
					: DataResult.error(() -> "Unknown check type '" + type + "', known types: " + TYPES.keySet()));

	private Checks() {
	}

	/**
	 * A check with a random number between {@code min_<name>} and {@code max_<name>} (each from
	 * {@code low} to {@code high}, the first not bigger than the second).
	 */
	static <C> MapCodec<C> rangeCodec(String name, int low, int high, BiFunction<Integer, Integer, C> make,
			Function<C, Integer> min, Function<C, Integer> max) {
		return RecordCodecBuilder.<C>mapCodec(instance -> instance.group(
				Codec.intRange(low, high).fieldOf("min_" + name).forGetter(min),
				Codec.intRange(low, high).fieldOf("max_" + name).forGetter(max)
		).apply(instance, make)).validate(check -> min.apply(check) <= max.apply(check)
				? DataResult.success(check) : DataResult.error(() -> "min_" + name + " is bigger than max_" + name));
	}

	/** A whole number in what the Agent says: with its sign, up to 9 digits (longer ones aren't counted). */
	private static final Pattern NUMBER = Pattern.compile("-?\\d+");
	private static final int NUMBER_DIGITS = 9;

	/** The whole numbers in a message, in order. */
	static List<Integer> numbers(String message) {
		List<Integer> numbers = new ArrayList<>();
		Matcher matcher = NUMBER.matcher(message);
		while (matcher.find()) {
			String digits = matcher.group();
			if (digits.length() - (digits.startsWith("-") ? 1 : 0) <= NUMBER_DIGITS) {
				numbers.add(Integer.parseInt(digits));
			}
		}
		return numbers;
	}

	/** The whole numbers in everything the Agent said, in order. */
	static List<Integer> numbersSaid(AgentProgram program) {
		return program.messages().stream().flatMap(message -> numbers(message).stream()).toList();
	}

	/** "on" or "off", for lamps, doors and fans in the checks' messages. */
	static Component onOff(boolean on) {
		return on ? Component.translatableWithFallback("mine.lesson.check.on", "on")
				: Component.translatableWithFallback("mine.lesson.check.off", "off");
	}

	/** For live tests: the program ended before MINE could try it. */
	static Component keepListening() {
		return Component.translatableWithFallback("mine.lesson.check.keep_listening",
				"Not yet: your program ended before MINE could try it. Keep it listening: while true do ... end.");
	}

	/**
	 * An unfinished build with the selected slot empty: the Agent ran out of blocks, often because
	 * they were spread over several slots and turtle.place() only uses one. {@code null} otherwise.
	 */
	private static @Nullable Component outOfBlocks(AgentProgram program, int needed) {
		if (program.selectedItems() > 0) {
			return null;
		}
		return program.otherItems() > 0
				? Component.translatableWithFallback("mine.lesson.check.blocks_elsewhere",
						"Not yet: the highlighted slot ran out of blocks, and Byte only builds with that one. Put at least %s blocks, all in it.", needed)
				: Component.translatableWithFallback("mine.lesson.check.no_blocks",
						"Not yet: Byte ran out of blocks. Put at least %s in the highlighted slot of its inventory.", needed);
	}
}
