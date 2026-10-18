package io.github.francescolv.mine.npc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a page of an NPC's dialogue, as the teacher wrote it in a book, into chat text. Links
 * written like {@code [Start the lesson](/mine lesson start loops)} become buttons: a command
 * (run by the student who clicks, with their own permissions) or a web page ({@code https://...}).
 */
public final class NpcText {

	/** {@code [label](target)}: the target may contain spaces, as commands do, but not a ")". */
	private static final Pattern LINK = Pattern.compile("\\[([^\\]\\n]+)]\\(([^)\\n]+)\\)");

	private NpcText() {
	}

	public static MutableComponent page(String page) {
		MutableComponent text = Component.empty();
		Matcher matcher = LINK.matcher(page);
		int last = 0;
		while (matcher.find()) {
			text.append(Component.literal(page.substring(last, matcher.start())));
			text.append(button(matcher.group(1), matcher.group(2)));
			last = matcher.end();
		}
		text.append(Component.literal(page.substring(last)));
		return text;
	}

	/** For whoever may change the NPC, next to its name: opens the dialogue in MINE's window. */
	public static Component editButton(UUID npc) {
		return Component.translatableWithFallback("mine.npc.edit_button", "[✎ Edit]").withStyle(style -> style
				.withColor(ChatFormatting.DARK_GREEN)
				.withClickEvent(new ClickEvent.RunCommand("/mine npc edit " + npc))
				.withHoverEvent(new HoverEvent.ShowText(Component.translatableWithFallback("mine.npc.edit_hover",
						"Change what this NPC says"))));
	}

	/** For the teacher, on top of a page for some students only: {@code For: loops, step 2 · completed loops}. */
	public static Component label(NpcCondition.Page page) {
		MutableComponent who = Component.empty();
		for (NpcCondition condition : page.conditions()) {
			if (!who.getSiblings().isEmpty()) {
				who.append(" · ");
			}
			who.append(condition.describe());
		}
		for (String error : page.errors()) {
			if (!who.getSiblings().isEmpty()) {
				who.append(" · ");
			}
			who.append(Component.literal("⚠ " + error).withStyle(ChatFormatting.RED));
		}
		return Component.translatableWithFallback("mine.npc.condition.label", "For: %s", who)
				.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
	}

	/** What to write under a question, e.g. whether the reader's answer was right; {@code null} for nothing. */
	@FunctionalInterface
	public interface Notes {

		Notes NONE = (page, question, quiz) -> null;

		@Nullable
		Component at(int page, int question, NpcQuiz quiz);
	}

	/**
	 * A page with questions: each question, then its answers as buttons that run
	 * {@code /mine npc choose <npc> <page> <question> <answer>} for the student who clicks, then
	 * its note, if any.
	 */
	public static MutableComponent quiz(NpcQuiz.Page quiz, UUID npc, int page, Notes notes) {
		MutableComponent text = Component.empty();
		for (int q = 0; q < quiz.questions().size(); q++) {
			NpcQuiz question = quiz.questions().get(q);
			if (q > 0) {
				text.append("\n\n");
			}
			if (!question.question().isEmpty()) {
				text.append(page(question.question())).append("\n");
			}
			for (int i = 0; i < question.options().size(); i++) {
				String command = "/mine npc choose " + npc + " " + page + " " + q + " " + i;
				if (i > 0) {
					text.append("\n");
				}
				text.append(Component.literal(NpcQuiz.letter(i) + ") " + question.options().get(i).text()).withStyle(style -> style
						.withColor(ChatFormatting.BLUE)
						.withClickEvent(new ClickEvent.RunCommand(command))
						.withHoverEvent(new HoverEvent.ShowText(Component.translatableWithFallback("mine.npc.quiz.choose", "Choose this answer")))));
			}
			Component note = notes.at(page, q, question);
			if (note != null) {
				text.append("\n").append(note);
			}
		}
		if (!quiz.after().isEmpty()) {
			text.append("\n\n").append(page(quiz.after()));
		}
		return text;
	}

	private static Component button(String label, String target) {
		ClickEvent click;
		String trimmed = target.strip();
		if (trimmed.startsWith("/")) {
			click = new ClickEvent.RunCommand(trimmed);
		} else if (!trimmed.contains(" ") && (trimmed.startsWith("https://") || trimmed.startsWith("http://"))) {
			click = new ClickEvent.OpenUrl(URI.create(trimmed));
		} else {
			// Not something we can click: leave it as it was written.
			return Component.literal("[" + label + "](" + target + ")");
		}
		return Component.literal("[" + label + "]").withStyle(style -> style
				// Blue: readable on the paper of the book window where dialogues open.
				.withColor(ChatFormatting.BLUE)
				.withUnderlined(true)
				.withClickEvent(click)
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(trimmed))));
	}
}
