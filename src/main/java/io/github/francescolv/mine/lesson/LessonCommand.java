package io.github.francescolv.mine.lesson;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.francescolv.mine.permission.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * {@code /mine lesson ...}. Students start, follow and restart their own lesson; teachers
 * (permission level 2, like operators) can do it for the whole class with a selector such as
 * {@code @a}, see where everyone is, and reset progress.
 */
public final class LessonCommand {

	private LessonCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("lesson")
				.then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
				.then(Commands.literal("start")
						.then(Commands.argument("lesson", StringArgumentType.word())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
										Lessons.all().stream().map(Lesson::id), builder))
								.executes(ctx -> start(ctx, List.of(ctx.getSource().getPlayerOrException())))
								.then(Commands.argument("students", EntityArgument.players())
										.requires(Permissions::isTeacher)
										.executes(ctx -> start(ctx, students(EntityArgument.getPlayers(ctx, "students")))))))
				.then(Commands.literal("status")
						.executes(ctx -> ownStatus(ctx.getSource()))
						.then(Commands.argument("students", EntityArgument.players())
								.requires(Permissions::isTeacher)
								.executes(ctx -> classStatus(ctx.getSource(), students(EntityArgument.getPlayers(ctx, "students"))))))
				.then(Commands.literal("hint").executes(ctx -> hint(ctx.getSource())))
				.then(Commands.literal("answer")
						.then(Commands.argument("answer", StringArgumentType.greedyString())
								.executes(ctx -> answer(ctx.getSource(), StringArgumentType.getString(ctx, "answer")))))
				.then(Commands.literal("answers")
						.requires(Permissions::isTeacher)
						.then(Commands.argument("lesson", StringArgumentType.word())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
										Lessons.all().stream().map(Lesson::id), builder))
								.executes(ctx -> answers(ctx.getSource(), StringArgumentType.getString(ctx, "lesson")))))
				.then(Commands.literal("restart").executes(ctx -> restart(ctx.getSource())))
				.then(Commands.literal("reset")
						.requires(Permissions::isTeacher)
						.then(Commands.argument("students", EntityArgument.players())
								.executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayers(ctx, "students")))));
	}

	/**
	 * The students among the players a teacher chose: {@code @a} is the class, not the teachers in
	 * it, who'd otherwise get the lesson too. One player named on their own counts, even a teacher.
	 */
	private static Collection<ServerPlayer> students(Collection<ServerPlayer> players) {
		return players.size() == 1 ? players : players.stream().filter(player -> !Permissions.isTeacher(player)).toList();
	}

	private static int list(CommandSourceStack source) {
		for (Lesson lesson : Lessons.all()) {
			source.sendSuccess(() -> Component.translatableWithFallback("mine.lesson.list_entry", "%s — %s (%s steps)",
					Component.literal(lesson.id()).withStyle(ChatFormatting.AQUA), lesson.titleText(), lesson.steps().size()), false);
		}
		return Lessons.all().size();
	}

	private static int start(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> students)
			throws CommandSyntaxException {
		String id = StringArgumentType.getString(ctx, "lesson");
		Lesson lesson = Lessons.get(id);
		if (lesson == null) {
			ctx.getSource().sendFailure(Component.translatableWithFallback("mine.lesson.unknown",
					"There is no lesson called %s. See /mine lesson list.", id));
			return 0;
		}
		for (ServerPlayer student : students) {
			LessonManager.start(student, lesson);
		}
		ServerPlayer self = ctx.getSource().getPlayer();
		if (students.size() != 1 || !students.contains(self)) {
			ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("mine.lesson.started_for",
					"Lesson %s started for %s students.", lesson.titleText(), students.size()), true);
		}
		return students.size();
	}

	private static int ownStatus(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer student = source.getPlayerOrException();
		source.sendSuccess(() -> statusLine(student), false);
		LessonProgress.Entry entry = LessonProgress.get(source.getServer()).get(student.getUUID());
		Lesson lesson = LessonManager.current(entry);
		if (lesson != null && entry.status() == LessonProgress.Status.IN_PROGRESS) {
			LessonFeedback.goal(student, lesson, entry.step());
		}
		return 1;
	}

	private static int classStatus(CommandSourceStack source, Collection<ServerPlayer> students) {
		int done = 0;
		for (ServerPlayer student : students) {
			source.sendSuccess(() -> statusLine(student), false);
			if (LessonProgress.get(source.getServer()).status(student.getUUID()) == LessonProgress.Status.COMPLETED) {
				done++;
				Component answer = answerLine(student);
				if (answer != null) {
					source.sendSuccess(() -> answer, false);
				}
			}
		}
		int completed = done;
		source.sendSuccess(() -> Component.translatableWithFallback("mine.lesson.status_summary",
				"%s of %s students have completed their lesson.", completed, students.size()), false);
		return students.size();
	}

	/** One line per student, e.g. "Alex — First steps with Byte: step 2 of 3". */
	private static Component statusLine(ServerPlayer student) {
		LessonProgress.Entry entry = LessonProgress.get(student.level().getServer()).get(student.getUUID());
		Lesson lesson = LessonManager.current(entry);
		Component name = student.getDisplayName();
		if (lesson == null) {
			return Component.translatableWithFallback("mine.lesson.status_none", "%s: no lesson started", name)
					.withStyle(ChatFormatting.GRAY);
		}
		if (entry.status() == LessonProgress.Status.COMPLETED) {
			return Component.translatableWithFallback("mine.lesson.status_completed", "%s — %s: completed",
					name, lesson.titleText()).withStyle(ChatFormatting.GREEN);
		}
		return Component.translatableWithFallback("mine.lesson.status_step", "%s — %s: step %s of %s",
				name, lesson.titleText(), entry.step() + 1, lesson.steps().size()).withStyle(ChatFormatting.AQUA);
	}

	/** The student's answer to the reflection of the lesson they completed, indented under their line. */
	@Nullable
	private static Component answerLine(ServerPlayer student) {
		Lesson lesson = LessonManager.current(student.level().getServer(), student.getUUID());
		LessonProgress.Answer answer = lesson == null ? null
				: LessonProgress.get(student.level().getServer()).answer(student.getUUID(), lesson.id());
		return answer == null ? null : Component.translatableWithFallback("mine.lesson.status_answer", "   Answer: %s",
				Component.literal(answer.text())).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
	}

	private static int answer(CommandSourceStack source, String text) throws CommandSyntaxException {
		if (!LessonManager.answer(source.getPlayerOrException(), text)) {
			source.sendFailure(Component.translatableWithFallback("mine.lesson.answer_not_yet",
					"The question comes at the end of the lesson: finish it first, then answer."));
			return 0;
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.lesson.answer_saved",
				"Answer saved: your teacher can read it.").withStyle(ChatFormatting.GREEN), false);
		return 1;
	}

	/** Every answer to a lesson's reflection, also from students who are offline. */
	private static int answers(CommandSourceStack source, String id) {
		Lesson lesson = Lessons.get(id);
		if (lesson == null) {
			source.sendFailure(Component.translatableWithFallback("mine.lesson.unknown",
					"There is no lesson called %s. See /mine lesson list.", id));
			return 0;
		}
		List<LessonProgress.Answer> answers = LessonProgress.get(source.getServer()).answers(lesson.id());
		if (answers.isEmpty()) {
			source.sendSuccess(() -> Component.translatableWithFallback("mine.lesson.answers_none",
					"Nobody has answered the question of %s yet.", lesson.titleText()).withStyle(ChatFormatting.GRAY), false);
			return 0;
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.lesson.answers_header", "%s answers to: %s",
				answers.size(), lesson.reflectionText()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
		for (LessonProgress.Answer answer : answers) {
			source.sendSuccess(() -> Component.translatableWithFallback("mine.lesson.answers_entry", "%s: %s",
					Component.literal(answer.name()).withStyle(ChatFormatting.AQUA), Component.literal(answer.text())), false);
		}
		return answers.size();
	}

	private static int hint(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer student = source.getPlayerOrException();
		if (LessonProgress.get(source.getServer()).status(student.getUUID()) != LessonProgress.Status.IN_PROGRESS) {
			source.sendFailure(notStarted());
			return 0;
		}
		Component hint = LessonManager.nextHint(student);
		if (hint == null) {
			source.sendFailure(Component.translatableWithFallback("mine.lesson.no_more_hints",
					"No more hints for this step: ask your teacher!"));
			return 0;
		}
		source.sendSuccess(() -> hint.copy().withStyle(ChatFormatting.GOLD), false);
		return 1;
	}

	private static int restart(CommandSourceStack source) throws CommandSyntaxException {
		if (!LessonManager.restart(source.getPlayerOrException())) {
			source.sendFailure(notStarted());
			return 0;
		}
		return 1;
	}

	private static int reset(CommandSourceStack source, Collection<ServerPlayer> students) {
		students.forEach(LessonManager::reset);
		source.sendSuccess(() -> Component.translatableWithFallback("mine.lesson.reset",
				"Lesson progress reset for %s students.", students.size()), true);
		return students.size();
	}

	private static Component notStarted() {
		return Component.translatableWithFallback("mine.lesson.not_started",
				"You haven't started a lesson. Your teacher will tell you which one, or see /mine lesson list.");
	}
}
