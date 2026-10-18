package io.github.francescolv.mine.agent;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * An Agent's label, the computer label CC: Tweaked keeps: {@code "Alex's Byte"}. Byte is the
 * Agent's name. The label is stored in English, and shown in each reader's language with
 * {@link #shown}: on the name tag, in the chat ({@code mine.say}) and in the terminal's welcome.
 * Agents made before Byte had its name are labelled {@code "Alex's Byte"}: shown the same.
 */
public final class AgentLabels {

	private static final String SUFFIX = "'s Byte";
	private static final String OLD_SUFFIX = "'s Byte";

	private AgentLabels() {
	}

	public static String of(String student) {
		return student + SUFFIX;
	}

	/** The student a label names, or {@code null} if it isn't a MINE label (a turtle someone labelled). */
	@Nullable
	public static String student(@Nullable String label) {
		if (label == null) {
			return null;
		}
		for (String suffix : new String[] { SUFFIX, OLD_SUFFIX }) {
			if (label.endsWith(suffix) && label.length() > suffix.length()) {
				return label.substring(0, label.length() - suffix.length());
			}
		}
		return null;
	}

	/** The label as its readers see it: "Byte di Alex" in Italian; other labels as they are. */
	public static Component shown(@Nullable String label) {
		String student = student(label);
		if (student != null) {
			return Component.translatableWithFallback("mine.byte.tag", "%s's Byte", student);
		}
		return Component.literal(label != null ? label : "Turtle");
	}
}
