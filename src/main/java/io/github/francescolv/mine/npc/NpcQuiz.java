package io.github.francescolv.mine.npc;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A multiple-choice question written on a page of an NPC's dialogue, like a checklist:
 *
 * <pre>
 * How many blocks does the loop place?
 * [ ] 3
 * [x] 4
 * [ ] 5
 * </pre>
 *
 * Lines starting with {@code [ ]} are answers, {@code [x]} marks the right ones (more than one
 * may be right). With no {@code [x]} the question is a poll: every answer is fine, and saved.
 * A page may hold more questions, one after the other: the text after a question's answers is
 * the next question.
 */
public record NpcQuiz(String question, List<Option> options) {

	public record Option(String text, boolean right) {
	}

	/** The questions of a page, in order, and the text after the last one (if any). */
	public record Page(List<NpcQuiz> questions, String after) {
	}

	private static final Pattern OPTION = Pattern.compile("^\\s*\\[([ xX])]\\s*(\\S.*)$");

	/** The questions written on {@code page}; {@code null} if the page has no answers to choose. */
	@Nullable
	public static Page parse(String page) {
		List<NpcQuiz> questions = new ArrayList<>();
		List<String> text = new ArrayList<>();
		List<Option> options = new ArrayList<>();
		for (String line : page.split("\n", -1)) {
			Matcher matcher = OPTION.matcher(line);
			if (matcher.matches()) {
				options.add(new Option(matcher.group(2).strip(), !matcher.group(1).isBlank()));
				continue;
			}
			if (!options.isEmpty()) {
				// A line that isn't an answer, after answers: the question is complete.
				questions.add(new NpcQuiz(String.join("\n", text).strip(), List.copyOf(options)));
				text.clear();
				options.clear();
			}
			text.add(line);
		}
		if (!options.isEmpty()) {
			questions.add(new NpcQuiz(String.join("\n", text).strip(), List.copyOf(options)));
			text.clear();
		}
		return questions.isEmpty() ? null : new Page(List.copyOf(questions), String.join("\n", text).strip());
	}

	/** A poll: no answer is marked as right, so every answer is fine. */
	public boolean isPoll() {
		return options.stream().noneMatch(Option::right);
	}

	/** {@code A}, {@code B}, {@code C}…: how answers are named in the book and in the results. */
	public static String letter(int option) {
		return String.valueOf((char) ('A' + option));
	}
}
