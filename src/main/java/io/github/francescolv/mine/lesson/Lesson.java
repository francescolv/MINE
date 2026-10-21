package io.github.francescolv.mine.lesson;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/**
 * A lesson with missions: a sequence of steps, each one checked by the server when one of the
 * student's programs ends. Lessons are data, read from {@code data/<namespace>/mine_lesson/<id>.json}
 * in datapacks (MINE's own are in its jar), so teachers can write new ones without Java.
 *
 * <p>Texts live in the language files under {@code mine.lesson.<lesson>.*}; the English strings
 * in the lesson file are the fallback when there is no translation, which is also the case for
 * lessons from other datapacks.
 *
 * <p>A lesson can also give the student programs, copied onto their Agent when the lesson starts
 * (e.g. broken programs to fix): {@code data/<namespace>/mine_lesson_files/<id>/<language>/<name>.lua}.
 * {@code files} maps each language (e.g. {@code it_it}) to its programs, by file name. Programs
 * made with blocks ({@code <name>.json}, as MINE's block editor saves them) go among the student's
 * block programs instead: {@code blocks} maps each language to them, by program name.
 */
public record Lesson(String id, int order, String title, String reflection, List<Step> steps,
		Map<String, Map<String, String>> files, Map<String, Map<String, String>> blocks) {

	/** Programs are offered in this language when there are none in the student's. */
	static final String DEFAULT_LANGUAGE = "en_us";

	/**
	 * One step: what to do ({@code goal}), what to say when a program doesn't do it yet
	 * ({@code notYet}), hints revealed one at a time, and the check that decides. The block
	 * editor shows {@code blockGoal} and {@code blockHints} instead, when the step has them:
	 * the same help, in the blocks' words; the Python editor {@code pythonGoal} and
	 * {@code pythonHints}.
	 */
	public record Step(String id, String goal, String notYet, List<String> hints, Check check, Optional<Give> give,
			Optional<String> blockGoal, List<String> blockHints, Optional<String> pythonGoal, List<String> pythonHints) {

		static final Codec<Step> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.string(1, 32).fieldOf("id").forGetter(Step::id),
				Codec.STRING.fieldOf("goal").forGetter(Step::goal),
				Codec.STRING.fieldOf("not_yet").forGetter(Step::notYet),
				Codec.STRING.listOf().optionalFieldOf("hints", List.of()).forGetter(Step::hints),
				Checks.CODEC.fieldOf("check").forGetter(Step::check),
				Give.CODEC.optionalFieldOf("give").forGetter(Step::give),
				Codec.STRING.optionalFieldOf("block_goal").forGetter(Step::blockGoal),
				Codec.STRING.listOf().optionalFieldOf("block_hints", List.of()).forGetter(Step::blockHints),
				Codec.STRING.optionalFieldOf("python_goal").forGetter(Step::pythonGoal),
				Codec.STRING.listOf().optionalFieldOf("python_hints", List.of()).forGetter(Step::pythonHints)
		).apply(instance, Step::new));
	}

	/**
	 * Items MINE puts in the Agent's highlighted slot when a step begins, so the student doesn't
	 * have to fetch them: {@code count} of them, or a random number up to {@code max_count}.
	 */
	public record Give(Item item, int count, int maxCount) {

		static final Codec<Give> CODEC = RecordCodecBuilder.<Give>create(instance -> instance.group(
				BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Give::item),
				Codec.intRange(1, 64).fieldOf("count").forGetter(Give::count),
				Codec.intRange(1, 64).optionalFieldOf("max_count", 0).forGetter(Give::maxCount)
		).apply(instance, Give::new)).validate(give -> give.maxCount() == 0 || give.maxCount() >= give.count()
				? DataResult.success(give) : DataResult.error(() -> "max_count is smaller than count"));
	}

	/** The contents of a lesson file; the id comes from the file name. */
	static Codec<Lesson> codec(String id) {
		return RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.optionalFieldOf("order", 1000).forGetter(Lesson::order),
				Codec.STRING.fieldOf("title").forGetter(Lesson::title),
				Codec.STRING.fieldOf("reflection").forGetter(Lesson::reflection),
				Codec.list(Step.CODEC, 1, 32).fieldOf("steps").forGetter(Lesson::steps)
		).apply(instance, (order, title, reflection, steps) -> new Lesson(id, order, title, reflection, steps, Map.of(), Map.of())));
	}

	Lesson withFiles(Map<String, Map<String, String>> files, Map<String, Map<String, String>> blocks) {
		return new Lesson(id, order, title, reflection, steps, files, blocks);
	}

	/**
	 * The programs to copy onto the Agent of a student whose game is in {@code language}: those
	 * in that language, otherwise English, otherwise whichever language the lesson has.
	 */
	public Map<String, String> filesFor(String language) {
		return inLanguage(files, language);
	}

	/** The block programs to give a student whose game is in {@code language}, chosen as {@link #filesFor}. */
	public Map<String, String> blocksFor(String language) {
		return inLanguage(blocks, language);
	}

	private static Map<String, String> inLanguage(Map<String, Map<String, String>> byLanguage, String language) {
		Map<String, String> found = byLanguage.get(language.toLowerCase());
		if (found == null) {
			found = byLanguage.get(DEFAULT_LANGUAGE);
		}
		if (found == null) {
			found = byLanguage.values().stream().findFirst().orElse(Map.of());
		}
		return found;
	}

	public Component titleText() {
		return text("title", title);
	}

	public Component reflectionText() {
		return text("reflection", reflection);
	}

	public Component goalText(int step) {
		Step s = steps.get(step);
		return text(s.id() + ".goal", s.goal());
	}

	/** The language a student writes in: hints and goals have their own words in each, when the step has them. */
	public enum Language { LUA, BLOCKS, PYTHON }

	/** The goal in {@code language}'s words, or the plain goal. */
	public Component goalText(int step, Language language) {
		Step s = steps.get(step);
		return switch (language) {
			case BLOCKS -> s.blockGoal().map(goal -> text(s.id() + ".block_goal", goal)).orElseGet(() -> goalText(step));
			case PYTHON -> s.pythonGoal().map(goal -> text(s.id() + ".python_goal", goal)).orElseGet(() -> goalText(step));
			case LUA -> goalText(step);
		};
	}

	/** How many hints a step has in {@code language}'s words (Lua's when it has none of its own). */
	public int hintCount(int step, Language language) {
		Step s = steps.get(step);
		return switch (language) {
			case BLOCKS -> !s.blockHints().isEmpty() ? s.blockHints().size() : s.hints().size();
			case PYTHON -> !s.pythonHints().isEmpty() ? s.pythonHints().size() : s.hints().size();
			case LUA -> s.hints().size();
		};
	}

	public Component notYetText(int step) {
		Step s = steps.get(step);
		return text(s.id() + ".not_yet", s.notYet());
	}

	/** Hint number {@code n} (from 0) of a step. */
	public Component hintText(int step, int n) {
		Step s = steps.get(step);
		return text(s.id() + ".hint." + (n + 1), s.hints().get(n));
	}

	/** Hint number {@code n} (from 0) of a step, in {@code language}'s words if it has them. */
	public Component hintText(int step, int n, Language language) {
		Step s = steps.get(step);
		return switch (language) {
			case BLOCKS -> !s.blockHints().isEmpty() ? text(s.id() + ".block_hint." + (n + 1), s.blockHints().get(n)) : hintText(step, n);
			case PYTHON -> !s.pythonHints().isEmpty() ? text(s.id() + ".python_hint." + (n + 1), s.pythonHints().get(n)) : hintText(step, n);
			case LUA -> hintText(step, n);
		};
	}

	private Component text(String key, String fallback) {
		return Component.translatableWithFallback("mine.lesson." + id + "." + key, fallback);
	}
}
