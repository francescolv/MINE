package io.github.francescolv.mine.npc;

import io.github.francescolv.mine.lesson.Lesson;
import io.github.francescolv.mine.lesson.LessonProgress;
import io.github.francescolv.mine.lesson.Lessons;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Who a page of an NPC's dialogue is for, written on its first lines:
 *
 * <pre>
 * &#64;lesson loops       students doing the lesson "loops"
 * &#64;lesson loops 2     students at step 2 of "loops" (or a range: 2-3)
 * &#64;done loops         students who have completed "loops"
 * &#64;lesson none        students not doing any lesson
 * </pre>
 *
 * More lines mean "any of them". Pages without them are for everybody. So an NPC can follow
 * the lesson: a hint for each step, congratulations at the end.
 */
public record NpcCondition(Kind kind, String lesson, int fromStep, int toStep) {

	public enum Kind {
		/** Doing the lesson, at a step between {@code fromStep} and {@code toStep} (1-based; 0 = any). */
		LESSON,
		/** Has completed the lesson. */
		DONE,
		/** Not doing any lesson: never started one, or the current one is completed. */
		NONE
	}

	/** A page: who it is for (nobody listed = everybody), what it says, and the condition lines MINE didn't understand. */
	public record Page(List<NpcCondition> conditions, String body, List<String> errors) {

		public boolean forEverybody() {
			return conditions.isEmpty() && errors.isEmpty();
		}

		/** Whether the student may read it; a page with only mistaken conditions is for nobody. */
		public boolean matches(MinecraftServer server, UUID student) {
			return forEverybody() || conditions.stream().anyMatch(condition -> condition.matches(server, student));
		}
	}

	private static final Pattern LESSON = Pattern.compile("@lesson\\s+([a-z0-9_./-]+)(?:\\s+(\\d+)(?:\\s*-\\s*(\\d+))?)?", Pattern.CASE_INSENSITIVE);
	private static final Pattern DONE = Pattern.compile("@done\\s+([a-z0-9_./-]+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern NONE = Pattern.compile("@lesson\\s+none", Pattern.CASE_INSENSITIVE);

	/** Splits a page into its condition lines (the first ones starting with {@code @}) and the rest. */
	public static Page parse(String page) {
		List<NpcCondition> conditions = new ArrayList<>();
		List<String> errors = new ArrayList<>();
		String[] lines = page.split("\n", -1);
		int i = 0;
		for (; i < lines.length && lines[i].strip().startsWith("@"); i++) {
			String line = lines[i].strip();
			NpcCondition condition = parseLine(line);
			if (condition == null) {
				errors.add(line);
			} else {
				conditions.add(condition);
			}
		}
		String body = String.join("\n", List.of(lines).subList(i, lines.length));
		return new Page(List.copyOf(conditions), i == 0 ? body : body.strip(), List.copyOf(errors));
	}

	@Nullable
	private static NpcCondition parseLine(String line) {
		if (NONE.matcher(line).matches()) {
			return new NpcCondition(Kind.NONE, "", 0, 0);
		}
		Matcher lesson = LESSON.matcher(line);
		if (lesson.matches()) {
			int from = lesson.group(2) == null ? 0 : Integer.parseInt(lesson.group(2));
			int to = lesson.group(3) == null ? from : Integer.parseInt(lesson.group(3));
			return from > to ? null : new NpcCondition(Kind.LESSON, lesson.group(1).toLowerCase(), from, to);
		}
		Matcher done = DONE.matcher(line);
		if (done.matches()) {
			return new NpcCondition(Kind.DONE, done.group(1).toLowerCase(), 0, 0);
		}
		return null;
	}

	public boolean matches(MinecraftServer server, UUID student) {
		LessonProgress progress = LessonProgress.get(server);
		LessonProgress.Entry entry = progress.get(student);
		boolean doing = entry != null && entry.status() == LessonProgress.Status.IN_PROGRESS;
		return switch (kind) {
			case NONE -> !doing;
			case DONE -> progress.hasCompleted(student, lesson);
			case LESSON -> doing && entry.lesson().equals(lesson)
					&& (fromStep == 0 || (entry.step() + 1 >= fromStep && entry.step() + 1 <= toStep));
		};
	}

	/** What is wrong with the condition, e.g. a lesson that doesn't exist; {@code null} if nothing. */
	@Nullable
	public Component problem() {
		if (kind == Kind.NONE) {
			return null;
		}
		Lesson found = Lessons.get(lesson);
		if (found == null) {
			return Component.translatableWithFallback("mine.npc.condition.no_lesson", "there's no lesson \"%s\"", lesson);
		}
		if (toStep > found.steps().size()) {
			return Component.translatableWithFallback("mine.npc.condition.no_step", "\"%s\" has only %s steps",
					lesson, found.steps().size());
		}
		return null;
	}

	/** For the teacher, on top of the page: who it is for. */
	public MutableComponent describe() {
		return switch (kind) {
			case NONE -> Component.translatableWithFallback("mine.npc.condition.none", "not doing a lesson");
			case DONE -> Component.translatableWithFallback("mine.npc.condition.done", "completed %s", lesson);
			case LESSON -> fromStep == 0
					? Component.translatableWithFallback("mine.npc.condition.lesson", "doing %s", lesson)
					: fromStep == toStep
					? Component.translatableWithFallback("mine.npc.condition.step", "%s, step %s", lesson, fromStep)
					: Component.translatableWithFallback("mine.npc.condition.steps", "%s, steps %s-%s", lesson, fromStep, toStep);
		};
	}
}
