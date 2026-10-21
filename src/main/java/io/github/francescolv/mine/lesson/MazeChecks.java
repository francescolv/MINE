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

/**
 * Lesson checks with a maze, like Minecraft Education's "Maze Algorithms" and "Wall Follower".
 * Each time a program starts, MINE builds a new one in front of the Agent, which stands in its
 * entrance: walls of stone bricks two blocks high, and a gold block in the wall where the way out
 * is. The Agent must reach the gold walking along the corridors only: no climbing, no digging.
 */
final class MazeChecks {


	private MazeChecks() {
	}

	/**
	 * A maze {@code width} cells across (to the Agent's right) and {@code depth} cells ahead.
	 * {@code corridor}: one winding way and no branches, for a first try. {@code say_steps}: at the
	 * gold, the Agent also says how many steps it took, counted by the program.
	 */
	record Maze(int width, int depth, boolean corridor, boolean saySteps) implements Check {

		static final MapCodec<Maze> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.intRange(2, 8).fieldOf("width").forGetter(Maze::width),
				Codec.intRange(2, 8).fieldOf("depth").forGetter(Maze::depth),
				Codec.BOOL.optionalFieldOf("corridor", false).forGetter(Maze::corridor),
				Codec.BOOL.optionalFieldOf("say_steps", false).forGetter(Maze::saySteps)
		).apply(instance, Maze::new));

		@Override
		public Object prepare(ServerLevel world, BlockPos start, Direction ahead) {
			return Built.build(world, start, ahead, width, depth, corridor);
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return memo instanceof Built maze && walkedIt(program, maze) && program.pos().equals(maze.exit())
					&& (!saySteps || Checks.numbersSaid(program).contains(program.steps()));
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (memo instanceof Checks.NoRoom noRoom) {
				return noRoom.text();
			}
			if (!(memo instanceof Built maze)) {
				return null;
			}
			if (program.highest() > program.startPos().getY()) {
				return Component.translatableWithFallback("mine.lesson.check.maze_over",
						"Not yet: Byte went over the walls. In a maze it can only walk along the corridors.");
			}
			if (!maze.untouched()) {
				return Component.translatableWithFallback("mine.lesson.check.maze_broken",
						"Not yet: Byte broke a wall of the maze. Find the way out without digging.");
			}
			if (!program.pos().equals(maze.exit())) {
				return Component.translatableWithFallback("mine.lesson.check.maze_exit",
						"Not yet: Byte stopped %s blocks from the gold. It has to stop right in front of it.",
						program.pos().distManhattan(maze.exit()));
			}
			List<Integer> said = Checks.numbersSaid(program);
			if (saySteps && !said.isEmpty()) {
				return Component.translatableWithFallback("mine.lesson.check.maze_count",
						"Not yet: Byte took %s steps, but said %s. Count every step with a variable.",
						program.steps(), said.getLast());
			}
			return null;
		}

		/** A new maze every time: the one just walked goes, passed or not, to make room for the next. */
		@Override
		public void ended(AgentProgram program, @Nullable Object memo, boolean passed) {
			if (memo instanceof Built maze) {
				maze.remove();
			}
		}

		private static boolean walkedIt(AgentProgram program, Built maze) {
			return program.highest() == program.startPos().getY() && maze.untouched();
		}
	}

	/**
	 * A maze MINE built: its walls and gold, and {@code exit}, the place in front of the gold.
	 *
	 * <p>The maze is a grid of cells; in blocks, cell (x, z) is at (2x + 1, 2z + 1) and the blocks
	 * between cells are walls or passages. The Agent stands in the gap of the first wall, at cell
	 * column 0: the maze spreads to its right and ahead, never behind it, where its student stands
	 * (in the standard lab, it fits in the Agent's lane).
	 */
	record Built(Checks.Course course, BlockPos exit) implements Checks.Challenge {

		private static final int HEIGHT = 2;
		private static final int TRIES = 100;

		static Object build(ServerLevel world, BlockPos start, Direction ahead, int width, int depth, boolean corridor) {
			RandomSource random = world.getRandom();
			Layout layout = corridor ? Layout.corridor(width, depth, random) : Layout.maze(width, depth, random);
			Direction right = ahead.getClockWise();
			Map<BlockPos, Block> blocks = new LinkedHashMap<>();
			List<BlockPos> room = new ArrayList<>();
			for (int bx = 0; bx <= 2 * width; bx++) {
				for (int bz = 0; bz <= 2 * depth; bz++) {
					Local local = new Local(bx, bz);
					BlockPos ground = start.relative(right, bx - 1).relative(ahead, bz);
					for (int y = 0; y < HEIGHT; y++) {
						if (local.equals(layout.gold())) {
							blocks.put(ground.above(y), Blocks.GOLD_BLOCK);
						} else if (!layout.open().contains(local)) {
							blocks.put(ground.above(y), Blocks.STONE_BRICKS);
						} else if (bz > 0) {
							room.add(ground.above(y));
						}
					}
				}
			}
			Checks.Course course = Checks.Course.build(world, blocks, room);
			if (course == null) {
				return new Checks.NoRoom(2 * depth + 1);
			}
			Local exit = layout.exit();
			return new Built(course, start.relative(right, exit.x() - 1).relative(ahead, exit.z()));
		}

		@Override
		public boolean untouched() {
			return course.untouched();
		}

		@Override
		public void remove() {
			course.remove();
		}

		private record Local(int x, int z) {

			Local plus(Local other) {
				return new Local(x + other.x, z + other.z);
			}
		}

		private static final List<Local> SIDES = List.of(new Local(0, 1), new Local(1, 0), new Local(-1, 0), new Local(0, -1));

		/** In blocks: the open places (cells and passages, the entrance too), the gold, and the place in front of it. */
		private record Layout(Set<Local> open, Local gold, Local exit) {

			private static Local block(Local cell) {
				return new Local(2 * cell.x() + 1, 2 * cell.z() + 1);
			}

			private static boolean inside(Local cell, int width, int depth) {
				return cell.x() >= 0 && cell.x() < width && cell.z() >= 0 && cell.z() < depth;
			}

			/** A perfect maze (one way between any two cells), the way out somewhere on its far or side walls. */
			static Layout maze(int width, int depth, RandomSource random) {
				Set<Local> open = new HashSet<>(Set.of(new Local(1, 0)));
				Set<Local> visited = new HashSet<>();
				Deque<Local> path = new ArrayDeque<>(List.of(new Local(0, 0)));
				visited.add(new Local(0, 0));
				open.add(block(new Local(0, 0)));
				while (!path.isEmpty()) {
					Local cell = path.peek();
					List<Local> sides = new ArrayList<>(SIDES);
					Collections.shuffle(sides, new java.util.Random(random.nextLong()));
					Local next = null;
					for (Local side : sides) {
						Local candidate = cell.plus(side);
						if (inside(candidate, width, depth) && !visited.contains(candidate)) {
							next = candidate;
							open.add(block(cell).plus(side));
							break;
						}
					}
					if (next == null) {
						path.pop();
					} else {
						visited.add(next);
						open.add(block(next));
						path.push(next);
					}
				}
				// The way out: a cell on the far row or on a side, never next to the entrance.
				List<Local[]> ways = new ArrayList<>();
				for (int x = 0; x < width; x++) {
					ways.add(new Local[] { new Local(x, depth - 1), new Local(0, 1) });
				}
				for (int z = depth / 2; z < depth; z++) {
					ways.add(new Local[] { new Local(0, z), new Local(-1, 0) });
					ways.add(new Local[] { new Local(width - 1, z), new Local(1, 0) });
				}
				Local[] way = ways.get(random.nextInt(ways.size()));
				return new Layout(open, block(way[0]).plus(way[1]), block(way[0]));
			}

			/**
			 * One corridor that winds from the entrance, at least half the cells long and with at
			 * least two turns: no branches, as only the way itself is dug. The gold closes its end,
			 * in a wall with no corridor behind it, so it can't be seen from earlier on.
			 */
			static Layout corridor(int width, int depth, RandomSource random) {
				int least = Math.max(4, width * depth / 2);
				Layout best = null;
				int bestSize = 0;
				for (int attempt = 0; attempt < TRIES; attempt++) {
					List<Local> walk = new ArrayList<>(List.of(new Local(0, 0)));
					Set<Local> visited = new HashSet<>(walk);
					while (true) {
						Local cell = walk.getLast();
						List<Local> free = new ArrayList<>();
						for (Local side : SIDES) {
							Local next = cell.plus(side);
							if (inside(next, width, depth) && !visited.contains(next)) {
								free.add(next);
							}
						}
						if (free.isEmpty()) {
							break;
						}
						Local next = free.get(random.nextInt(free.size()));
						walk.add(next);
						visited.add(next);
					}
					Layout layout = closed(walk, visited);
					if (layout == null) {
						continue;
					}
					if (walk.size() >= least && turns(walk) >= 2) {
						return layout;
					}
					if (walk.size() > bestSize) {
						best = layout;
						bestSize = walk.size();
					}
				}
				return best != null ? best : closed(List.of(new Local(0, 0)), Set.of(new Local(0, 0)));
			}

			/** The corridor {@code walk} dug out, and the gold at its end; {@code null} if there's nowhere for the gold. */
			private static @Nullable Layout closed(List<Local> walk, Set<Local> visited) {
				Set<Local> open = new HashSet<>(Set.of(new Local(1, 0)));
				for (int i = 0; i < walk.size(); i++) {
					open.add(block(walk.get(i)));
					if (i > 0) {
						Local from = block(walk.get(i - 1));
						Local to = block(walk.get(i));
						open.add(new Local((from.x() + to.x()) / 2, (from.z() + to.z()) / 2));
					}
				}
				Local end = walk.getLast();
				Local before = walk.size() > 1 ? walk.get(walk.size() - 2) : new Local(0, -1);
				Local heading = new Local(end.x() - before.x(), end.z() - before.z());
				// Straight ahead if it can, else to one side.
				for (Local side : List.of(heading, new Local(heading.z(), heading.x()), new Local(-heading.z(), -heading.x()))) {
					Local behind = end.plus(side);
					if (!visited.contains(behind) && !(behind.x() == 0 && behind.z() == -1)) {
						return new Layout(open, block(end).plus(side), block(end));
					}
				}
				return null;
			}

			private static int turns(List<Local> walk) {
				int turns = 0;
				for (int i = 2; i < walk.size(); i++) {
					Local a = walk.get(i - 2);
					Local b = walk.get(i - 1);
					Local c = walk.get(i);
					if (b.x() - a.x() != c.x() - b.x() || b.z() - a.z() != c.z() - b.z()) {
						turns++;
					}
				}
				return turns;
			}
		}
	}
}
