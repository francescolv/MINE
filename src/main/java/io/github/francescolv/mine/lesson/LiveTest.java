package io.github.francescolv.mine.lesson;

import io.github.francescolv.mine.agent.AgentProgram;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A live test in real time, for programs that answer through Python (each call goes to the
 * server and back): MINE does something (plays the board, pulls a lever), waits, looks, and so on.
 * Like {@link SensorChecks.Timeline}, but its waits are in milliseconds, not game ticks, which a
 * test server runs much faster than real time.
 */
final class LiveTest implements Checks.Challenge {

	/** Before the first beat: the program gets going (Python loads, Byte starts). */
	static final long SETTLE_MS = 3000;
	/** How long a program has to answer something MINE did. */
	static final long ANSWER_MS = 1500;

	private record Beat(@Nullable BooleanSupplier ok, @Nullable Supplier<Component> why, @Nullable Runnable act, long ms) {
	}

	private final Checks.Challenge scene;
	private final List<Beat> beats = new ArrayList<>();
	private long next = -1;
	private int beat;
	boolean passed;
	@Nullable
	Component failure;

	LiveTest(Checks.Challenge scene) {
		this.scene = scene;
	}

	/** Nothing built in the world: the test only plays the board and looks at what Byte tells it. */
	LiveTest() {
		this(NOTHING);
	}

	/** First checks {@code ok} (failing with {@code why}), then does {@code act}, then waits {@code ms}. */
	LiveTest then(@Nullable BooleanSupplier ok, @Nullable Supplier<Component> why, @Nullable Runnable act, long ms) {
		beats.add(new Beat(ok, why, act, ms));
		return this;
	}

	/** Does {@code act} and gives the program time to answer. */
	LiveTest act(Runnable act) {
		return then(null, null, act, ANSWER_MS);
	}

	/** Checks {@code ok}, failing with {@code why}. */
	LiveTest expect(BooleanSupplier ok, Supplier<Component> why) {
		return then(ok, why, null, 0);
	}

	Checks.Challenge scene() {
		return scene;
	}

	/** One tick of the test; {@code true} when it is over, passed or not. */
	boolean tick() {
		long now = System.currentTimeMillis();
		if (next < 0) {
			next = now + SETTLE_MS;
		}
		while (!passed && failure == null && now >= next) {
			Beat current = beats.get(beat);
			if (current.ok() != null && !current.ok().getAsBoolean()) {
				failure = current.why().get();
				break;
			}
			if (current.act() != null) {
				current.act().run();
			}
			next = now + current.ms();
			if (++beat == beats.size()) {
				passed = true;
			}
		}
		return passed || failure != null;
	}

	@Override
	public boolean untouched() {
		return scene.untouched();
	}

	@Override
	public void remove() {
		scene.remove();
	}

	private static final Checks.Challenge NOTHING = new Checks.Challenge() {
		@Override
		public boolean untouched() {
			return true;
		}

		@Override
		public void remove() {
		}
	};

	/**
	 * What the live checks share: a test made when the program starts, run while it runs (MINE
	 * then stops it), passed if every beat was.
	 */
	abstract static class Check implements io.github.francescolv.mine.lesson.Check {

		/** The test for this program; or, when there's no room for its scene, what to say instead. */
		abstract Object test(AgentProgram program);

		@Override
		public Object start(AgentProgram program) {
			return test(program);
		}

		@Override
		public boolean watch(AgentProgram program, @Nullable Object memo) {
			return !(memo instanceof LiveTest test) || test.tick();
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return memo instanceof LiveTest test && test.passed;
		}

		@Override
		public void ended(AgentProgram program, @Nullable Object memo, boolean passed) {
			if (memo instanceof LiveTest test) {
				test.remove();
			}
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (memo instanceof LiveTest test && test.failure != null) {
				return test.failure;
			}
			if (memo instanceof Component text) {
				return text;
			}
			return Component.translatableWithFallback("mine.lesson.check.keep_listening_python",
					"Not yet: your program ended before MINE could try it. Keep it listening: while True:");
		}
	}
}
