package io.github.francescolv.mine.lesson;

import io.github.francescolv.mine.agent.AgentProgram;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A live test: while the program runs, MINE does something (plays the board, pulls a lever,
 * steps on a plate), waits, looks, and so on, one beat after the other, around a scene it built.
 * <ul>
 * <li>In real time ({@link #LiveTest(Checks.Challenge)}), for programs that answer through
 * Python (each call goes to the server and back), or whose answer MINE only looks at.</li>
 * <li>In game ticks ({@link #inGameTime}), for Lua programs that wait themselves: the Agent's
 * clock (os.sleep, timers) counts game ticks, which a test server runs much faster than real
 * time. The first beat also waits for the program to get going in real time.</li>
 * </ul>
 */
final class LiveTest implements Checks.Challenge {

	/** Before the first beat, in real time: the program gets going (Python loads, Byte starts). */
	static final long SETTLE_MS = 3000;
	/** How long a program has to answer something MINE did, in real time. */
	static final long ANSWER_MS = 1500;
	/** Before the first beat, in game time: both real time (the Agent's computer runs on its own) and game ticks. */
	static final long GAME_SETTLE_MS = 2000;
	static final int GAME_SETTLE_TICKS = 40;

	/**
	 * One moment of the test: first MINE looks ({@code ok}, and if it's wrong, {@code why} tells
	 * the student), then it acts ({@code act}), then it waits {@code pause} before the next one.
	 */
	private record Beat(@Nullable BooleanSupplier ok, @Nullable Supplier<Component> why, @Nullable Runnable act, long pause) {
	}

	/** In game time, the world whose ticks the test counts; {@code null} in real time. */
	@Nullable
	private final ServerLevel world;
	private final Checks.Challenge scene;
	/** Before the first beat: in the test's time, and also in real time. */
	private final long settle;
	private final long settleMs;
	private final List<Beat> beats = new ArrayList<>();
	/** When the next beat is, in the test's time; the clock starts with the program (the test may be made before). */
	private long next = -1;
	private long settled;
	private int beat;
	boolean passed;
	@Nullable
	Component failure;

	private LiveTest(@Nullable ServerLevel world, Checks.Challenge scene, long settle, long settleMs) {
		this.world = world;
		this.scene = scene;
		this.settle = settle;
		this.settleMs = settleMs;
	}

	/** In real time, around {@code scene}, after {@code settleMs}. */
	LiveTest(Checks.Challenge scene, long settleMs) {
		this(null, scene, settleMs, 0);
	}

	LiveTest(Checks.Challenge scene) {
		this(scene, SETTLE_MS);
	}

	/** Nothing built in the world: the test only plays the board and looks at what Byte tells it. */
	LiveTest() {
		this(NOTHING);
	}

	/** In game ticks of {@code world}, around {@code scene}. */
	static LiveTest inGameTime(ServerLevel world, Checks.Challenge scene) {
		return new LiveTest(world, scene, GAME_SETTLE_TICKS, GAME_SETTLE_MS);
	}

	/** First checks {@code ok} (failing with {@code why}), then does {@code act}, then waits {@code pause} (ticks or ms). */
	LiveTest then(@Nullable BooleanSupplier ok, @Nullable Supplier<Component> why, @Nullable Runnable act, long pause) {
		beats.add(new Beat(ok, why, act, pause));
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
		long now = world != null ? world.getGameTime() : System.currentTimeMillis();
		if (next < 0) {
			next = now + settle;
			settled = System.currentTimeMillis() + settleMs;
		}
		while (!passed && failure == null && now >= next && (beat > 0 || System.currentTimeMillis() >= settled)) {
			Beat current = beats.get(beat);
			if (current.ok() != null && !current.ok().getAsBoolean()) {
				failure = current.why().get();
				break;
			}
			if (current.act() != null) {
				current.act().run();
			}
			next = now + current.pause();
			if (++beat == beats.size()) {
				passed = true;
			}
			if (world != null) {
				// In game time, one beat per tick: what MINE did reaches the world first.
				break;
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
	interface Check extends io.github.francescolv.mine.lesson.Check {

		/** The test for this program; or, when there's no room for its scene, what to say instead. */
		Object test(AgentProgram program);

		@Override
		default Object start(AgentProgram program) {
			return test(program);
		}

		@Override
		default boolean watch(AgentProgram program, @Nullable Object memo) {
			return !(memo instanceof LiveTest test) || test.tick();
		}

		@Override
		default boolean passed(AgentProgram program, @Nullable Object memo) {
			return memo instanceof LiveTest test && test.passed;
		}

		@Override
		default void ended(AgentProgram program, @Nullable Object memo, boolean passed) {
			if (memo instanceof LiveTest test) {
				test.remove();
			}
		}

		@Override
		default @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
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
