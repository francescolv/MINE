package io.github.francescolv.mine.lesson;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.francescolv.mine.agent.AgentProgram;
import io.github.francescolv.mine.device.Devices;
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
import java.util.List;

/**
 * Lesson 9, data from the board: MINE plays the student's board, sending {@code count} numbers
 * on {@code key} (between {@code min} and {@code max}, new ones each time), and the program
 * keeps them in a list:
 * <ul>
 * <li>{@code chart}: Byte builds a bar chart, one column per number, the first right in front of
 * where Byte starts and the next ones each a block further to its right. A column is as tall as
 * its number, or scaled: {@code number * height // max} when {@code height} is given; then Byte
 * goes back where it started (in the lab, the lanes are 8 blocks apart);</li>
 * <li>{@code chart_max}: Byte says the largest number and its position (from 1).</li>
 * </ul>
 * The program ends by itself; MINE looks at what it built or said then.
 */
final class ChartChecks {

	/** Ticks between two numbers, as a board writing twice a second. */
	private static final int EVERY = 10;

	private ChartChecks() {
	}

	/** One run: the numbers MINE sends, and how many it has sent. */
	private static final class Run {
		final ServerLevel world;
		final BlockPos start;
		final Direction ahead;
		final List<Integer> values = new ArrayList<>();
		int sent;
		long next = -1;
		long settled;

		Run(ServerLevel world, BlockPos start, Direction ahead) {
			this.world = world;
			this.start = start;
			this.ahead = ahead;
		}

		/** Where column {@code i} (from 0) stands, on the ground level of Byte's start. */
		BlockPos column(int i) {
			return start.relative(ahead).relative(ahead.getClockWise(), i);
		}

		/** How tall column {@code i} is: solid blocks from the bottom up. */
		int height(int i, int max) {
			int h = 0;
			while (h <= max + 1 && !world.getBlockState(column(i).above(h)).isAir()) {
				h++;
			}
			return h;
		}
	}

	/** What both checks share: the numbers, sent while the program runs. */
	private interface Played extends Check {

		String key();

		int count();

		int min();

		int max();

		@Override
		default Object start(AgentProgram program) {
			Run run = new Run(program.world(), program.startPos(), program.startFacing());
			RandomSource random = program.world().getRandom();
			for (int i = 0; i < count(); i++) {
				run.values.add(min() + random.nextInt(max() - min() + 1));
			}
			clear(run);
			return run;
		}

		/** Takes away a chart from a previous try: only the blocks where columns go. */
		default void clear(Run run) {
		}

		@Override
		default boolean watch(AgentProgram program, @Nullable Object memo) {
			if (!(memo instanceof Run run) || run.sent >= run.values.size()) {
				return false;
			}
			long now = run.world.getGameTime();
			if (run.next < 0) {
				// As for the live checks: the program gets going first.
				run.next = now + LiveTest.GAME_SETTLE_TICKS;
				run.settled = System.currentTimeMillis() + LiveTest.GAME_SETTLE_MS;
			}
			if (now >= run.next && System.currentTimeMillis() >= run.settled) {
				Devices.play(run.world.getServer(), program.owner(), key(), Integer.toString(run.values.get(run.sent)));
				run.sent++;
				run.next = now + EVERY;
			}
			return false;
		}

		/** The program ended before MINE had sent all the numbers. */
		@Nullable
		default Component tooSoon(Run run) {
			return run.sent < run.values.size()
					? Component.translatableWithFallback("mine.lesson.check.chart_too_soon",
							"Not yet: your program ended before the board had sent its %s numbers. Wait for each one: mine.device.wait(\"%s\").",
							run.values.size(), key())
					: null;
		}
	}

	/** A bar chart of the numbers, as tall as each, or scaled to {@code height}. */
	record Chart(String key, int count, int min, int max, int height) implements Played {

		static final MapCodec<Chart> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.string(1, 32).fieldOf("key").forGetter(Chart::key),
				Codec.intRange(1, 10).fieldOf("count").forGetter(Chart::count),
				Codec.intRange(0, 32767).fieldOf("min").forGetter(Chart::min),
				Codec.intRange(1, 32767).fieldOf("max").forGetter(Chart::max),
				Codec.intRange(0, 12).optionalFieldOf("height", 0).forGetter(Chart::height)
		).apply(instance, Chart::new));

		/** How tall the column of {@code value} must be. */
		int expected(int value) {
			return height > 0 ? value * height / max : value;
		}

		int tallest() {
			return height > 0 ? height : max;
		}

		@Override
		public void clear(Run run) {
			for (int i = 0; i < count; i++) {
				for (int h = 0; h <= tallest() + 1; h++) {
					BlockPos pos = run.column(i).above(h);
					BlockState state = run.world.getBlockState(pos);
					if (state.is(Blocks.OAK_PLANKS)) {
						run.world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return memo instanceof Run run && tooSoon(run) == null && wrongColumn(run) < 0 && back(program, run);
		}

		/** Byte back where it started, facing the same way: the next chart has room in the lane. */
		private static boolean back(AgentProgram program, Run run) {
			return program.pos().equals(run.start) && program.facing() == run.ahead;
		}

		private int wrongColumn(Run run) {
			for (int i = 0; i < run.values.size(); i++) {
				if (run.height(i, tallest()) != expected(run.values.get(i))) {
					return i;
				}
			}
			return -1;
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (!(memo instanceof Run run)) {
				return null;
			}
			Component early = tooSoon(run);
			if (early != null) {
				return early;
			}
			int wrong = wrongColumn(run);
			if (wrong < 0) {
				return back(program, run) ? null : Component.translatableWithFallback("mine.lesson.check.chart_back",
						"Not yet: the chart is right! Now bring Byte back where it started, facing the same way, so the next chart has room.");
			}
			int value = run.values.get(wrong);
			return height > 0
					? Component.translatableWithFallback("mine.lesson.check.chart_scaled",
							"Not yet: the board sent %s, and column %s should be %s tall (%s × %s // %s), but it's %s.",
							value, wrong + 1, expected(value), value, height, max, run.height(wrong, tallest()))
					: Component.translatableWithFallback("mine.lesson.check.chart_column",
							"Not yet: the board sent %s, but column %s is %s tall. The numbers were %s.",
							value, wrong + 1, run.height(wrong, tallest()), run.values.toString());
		}
	}

	/** Byte says the largest number and its position, from 1. */
	record Largest(String key, int count, int min, int max) implements Played {

		static final MapCodec<Largest> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.string(1, 32).fieldOf("key").forGetter(Largest::key),
				Codec.intRange(2, 10).fieldOf("count").forGetter(Largest::count),
				Codec.intRange(0, 32767).fieldOf("min").forGetter(Largest::min),
				Codec.intRange(1, 32767).fieldOf("max").forGetter(Largest::max)
		).apply(instance, Largest::new));

		@Override
		public Object start(AgentProgram program) {
			Run run = (Run) Played.super.start(program);
			// One largest number, so its position is clear.
			int top = run.values.indexOf(run.values.stream().max(Integer::compare).orElse(0));
			for (int i = 0; i < run.values.size(); i++) {
				if (i != top && run.values.get(i).equals(run.values.get(top))) {
					run.values.set(i, Math.max(min, run.values.get(i) - 1));
				}
			}
			return run;
		}

		private static int largest(Run run) {
			return run.values.stream().max(Integer::compare).orElse(0);
		}

		private static boolean said(AgentProgram program, int number, int position) {
			for (String message : program.messages()) {
				List<Integer> numbers = Checks.numbers(message);
				if (numbers.contains(number) && numbers.contains(position)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return memo instanceof Run run && tooSoon(run) == null
					&& said(program, largest(run), run.values.indexOf(largest(run)) + 1);
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (!(memo instanceof Run run)) {
				return null;
			}
			Component early = tooSoon(run);
			return early != null ? early : Component.translatableWithFallback("mine.lesson.check.chart_max",
					"Not yet: the numbers were %s. Byte must say the largest one, %s, and its position, %s.",
					run.values.toString(), largest(run), run.values.indexOf(largest(run)) + 1);
		}
	}
}
