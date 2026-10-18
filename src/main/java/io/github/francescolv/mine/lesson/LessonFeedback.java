package io.github.francescolv.mine.lesson;

import io.github.francescolv.mine.PlayerSounds;
import io.github.francescolv.mine.agent.AgentProgram;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

/**
 * What the student sees and hears during a lesson: chat, titles, action bar and sounds only,
 * so it works without a resource pack and on old school PCs. How a program went is also written
 * in the Agent's terminal, which hides the chat while it is open. Colours: aqua for information,
 * gold for hints, green for success, light purple for reflection.
 */
final class LessonFeedback {

	private LessonFeedback() {
	}

	static void started(ServerPlayer student, Lesson lesson) {
		title(student, lesson.titleText().copy().withStyle(ChatFormatting.AQUA), stepCounter(lesson, 0));
		student.sendSystemMessage(Component.translatableWithFallback("mine.lesson.started", "Lesson started: %s",
				lesson.titleText()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
		goal(student, lesson, 0);
	}

	/** The student may be offline: the news still goes to the Agent's terminal. */
	static void stepDone(AgentProgram program, @Nullable ServerPlayer student, Lesson lesson, int next) {
		program.tell(stepDoneText());
		program.tell(goalText(lesson, next));
		if (student == null) {
			return;
		}
		sound(student, SoundEvents.PLAYER_LEVELUP);
		title(student, stepDoneText().withStyle(ChatFormatting.GREEN), stepCounter(lesson, next));
		student.sendSystemMessage(stepDoneText().withStyle(ChatFormatting.GREEN));
		goal(student, lesson, next);
	}

	static void completed(AgentProgram program, @Nullable ServerPlayer student, Lesson lesson) {
		program.tell(completedText());
		program.tell(reflectionText(lesson));
		program.tell(Component.translatableWithFallback("mine.lesson.answer_prompt_terminal",
				"Answer in the chat with /mine lesson answer followed by your answer."));
		if (student == null) {
			return;
		}
		sound(student, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);
		title(student, completedText().withStyle(ChatFormatting.GREEN), lesson.titleText());
		student.sendSystemMessage(completedText().withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
		student.sendSystemMessage(reflectionText(lesson).withStyle(ChatFormatting.LIGHT_PURPLE));
		student.sendSystemMessage(answerPrompt());
	}

	/** How to answer the reflection question, with the command ready to be written in chat. */
	static MutableComponent answerPrompt() {
		return Component.translatableWithFallback("mine.lesson.answer_prompt", "Write your answer in the chat: %s",
				Component.literal("/mine lesson answer …").withStyle(style -> style
						.withUnderlined(true)
						.withClickEvent(new ClickEvent.SuggestCommand("/mine lesson answer "))
						.withHoverEvent(new HoverEvent.ShowText(Component.translatableWithFallback("mine.lesson.hint_click", "Click to write it in chat")))))
				.withStyle(ChatFormatting.LIGHT_PURPLE);
	}

	static void notYet(AgentProgram program, @Nullable ServerPlayer student, Component why) {
		program.tell(why);
		if (student != null) {
			// Also in the chat: the bar above the hotbar is gone in a few seconds, and cuts long lines.
			student.sendOverlayMessage(why.copy().withStyle(ChatFormatting.GOLD));
			student.sendSystemMessage(why.copy().withStyle(ChatFormatting.GOLD));
		}
	}

	/** The goal of a step, with a clickable reminder of the hint command. */
	static void goal(ServerPlayer student, Lesson lesson, int step) {
		student.sendSystemMessage(goalText(lesson, step).withStyle(ChatFormatting.AQUA));
		student.sendSystemMessage(hintReminder());
	}

	static MutableComponent hintReminder() {
		return Component.translatableWithFallback("mine.lesson.hint_reminder", "Need a hand? Type %s",
				Component.literal("/mine lesson hint").withStyle(style -> style
						.withUnderlined(true)
						.withClickEvent(new ClickEvent.SuggestCommand("/mine lesson hint"))
						.withHoverEvent(new HoverEvent.ShowText(Component.translatableWithFallback("mine.lesson.hint_click", "Click to write it in chat")))))
				.withStyle(ChatFormatting.GOLD);
	}

	private static MutableComponent goalText(Lesson lesson, int step) {
		return Component.translatableWithFallback("mine.lesson.goal", "Step %s/%s: %s",
				step + 1, lesson.steps().size(), lesson.goalText(step));
	}

	private static MutableComponent stepDoneText() {
		return Component.translatableWithFallback("mine.lesson.step_done", "Step complete!");
	}

	private static MutableComponent completedText() {
		return Component.translatableWithFallback("mine.lesson.completed", "Mission complete!");
	}

	private static MutableComponent reflectionText(Lesson lesson) {
		return Component.translatableWithFallback("mine.lesson.reflection", "Think about it: %s", lesson.reflectionText());
	}

	private static Component stepCounter(Lesson lesson, int step) {
		return Component.translatableWithFallback("mine.lesson.step_counter", "Step %s of %s",
				step + 1, lesson.steps().size());
	}

	private static void title(ServerPlayer student, Component title, Component subtitle) {
		student.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
		student.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		student.connection.send(new ClientboundSetTitleTextPacket(title));
	}

	private static void sound(ServerPlayer student, SoundEvent sound) {
		PlayerSounds.play(student, sound, SoundSource.PLAYERS, 0.6f, 1.0f);
	}
}
