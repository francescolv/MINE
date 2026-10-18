package io.github.francescolv.mine.lesson;

import io.github.francescolv.mine.agent.AgentProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/** Decides whether a program completed a lesson step, by looking at what the Agent really did. */
@FunctionalInterface
public interface Check {

	/**
	 * Called when the program starts, before its first instruction: returns whatever {@link #passed}
	 * needs to remember. May also prepare the world, e.g. build a challenge in front of the Agent.
	 */
	@Nullable
	default Object start(AgentProgram program) {
		return null;
	}

	/**
	 * Called every tick while the program runs, for checks that test it live (e.g. MINE pulls a
	 * lever and looks at what the Agent does). Returns {@code true} once it has seen enough: MINE
	 * then stops the program, which would otherwise listen forever, as if it had ended by itself.
	 */
	default boolean watch(AgentProgram program, @Nullable Object memo) {
		return false;
	}

	/** Called when the program has ended without errors. */
	boolean passed(AgentProgram program, @Nullable Object memo);

	/** Why the step isn't done yet, when the check can tell; {@code null} for the step's usual message. */
	@Nullable
	default Component whyNot(AgentProgram program, @Nullable Object memo) {
		return null;
	}

	/**
	 * Builds the step's challenge in the world in front of {@code pos}, if the check has one, and
	 * returns what {@link #start} would. Used to show the challenge as soon as the step begins:
	 * the first program then uses it, instead of a new one. {@code null} if there is no challenge.
	 */
	@Nullable
	default Object prepare(ServerLevel world, BlockPos pos, Direction facing) {
		return null;
	}

	/**
	 * Called instead of {@link #start} when the program takes on the challenge {@link #prepare}
	 * built as the step began: e.g. to tell it, through {@code mine.challenge()}, what that one asks.
	 */
	default void reuse(AgentProgram program, @Nullable Object memo) {
	}

	/**
	 * Called last, whatever happened (even if the program stopped with an error): e.g. to take
	 * away what {@link #start} built when the step wasn't passed, so the student can try again.
	 */
	default void ended(AgentProgram program, @Nullable Object memo, boolean passed) {
	}
}
