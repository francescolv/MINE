package io.github.francescolv.mine.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.francescolv.mine.DataFormat;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Students' answers to the NPCs' quizzes, saved in the world ({@code data/mine/quizzes.dat}) so
 * the teacher can read them later, also for students who are offline and for NPCs far away or
 * removed.
 *
 * <p>A question is known by its NPC and its text: if the teacher rewrites a question, it is a new
 * one, and the answers to the old text stay under it.
 */
public final class QuizResults extends SavedData {

	private static final String KEY = "quizzes";
	private static final SavedDataType<QuizResults> TYPE = DataFormat.saved(KEY, QuizResults::new, QuizResults::fromNbt, QuizResults::save);

	/**
	 * One student's answer: the first one they chose (what they knew), whether it was right, and how
	 * many tries they needed to find a right one (0 = not yet). For polls, their latest choice.
	 */
	public record Answer(String name, int option, String text, boolean right, int tries, boolean solved, long time) {
	}

	/**
	 * A question of an NPC, with everybody's answers. {@code page} and {@code index} (its place
	 * among the questions of the page) are where it was last seen.
	 */
	public record Question(UUID npc, String npcName, int page, int index, String text, boolean poll,
			Map<UUID, Answer> answers) {
	}

	private final Map<String, Question> questions = new LinkedHashMap<>();

	public static QuizResults get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	private static String key(UUID npc, String question) {
		return npc + "\n" + question;
	}

	/** Records a student's choice; returns their answer as it is now saved. */
	public Answer record(UUID npc, String npcName, int page, int index, NpcQuiz quiz, UUID student, String name,
			int option, long time) {
		Question question = questions.compute(key(npc, quiz.question()), (key, old) -> new Question(npc, npcName, page, index,
				quiz.question(), quiz.isPoll(), old == null ? new HashMap<>() : old.answers()));
		NpcQuiz.Option chosen = quiz.options().get(option);
		Answer old = question.answers().get(student);
		Answer now;
		if (quiz.isPoll() || old == null) {
			now = new Answer(name, option, chosen.text(), chosen.right(), 1, chosen.right(), time);
		} else if (old.solved()) {
			now = old;
		} else {
			// The first choice stays: what changes is how many tries it took to get it right.
			now = new Answer(name, old.option(), old.text(), old.right(), old.tries() + 1, chosen.right(), old.time());
		}
		question.answers().put(student, now);
		setDirty();
		return now;
	}

	@Nullable
	public Answer answer(UUID npc, String question, UUID student) {
		Question saved = questions.get(key(npc, question));
		return saved == null ? null : saved.answers().get(student);
	}

	/** The questions of the NPC, in the order they come in its dialogue. */
	public List<Question> of(UUID npc) {
		return questions.values().stream()
				.filter(question -> question.npc().equals(npc))
				.sorted(Comparator.comparingInt(Question::page).thenComparingInt(Question::index))
				.toList();
	}

	/** Every question, by NPC name and then in dialogue order. */
	public List<Question> all() {
		return questions.values().stream()
				.sorted(Comparator.comparing(Question::npcName, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(question -> question.npc().toString())
						.thenComparingInt(Question::page)
						.thenComparingInt(Question::index))
				.toList();
	}

	/** The NPCs with answered questions called {@code name} (more NPCs may have the same name). */
	public List<UUID> npcsNamed(String name) {
		List<UUID> npcs = new ArrayList<>();
		for (Question question : questions.values()) {
			if (question.npcName().equalsIgnoreCase(name) && !npcs.contains(question.npc())) {
				npcs.add(question.npc());
			}
		}
		return npcs;
	}

	/** Names of the NPCs with answered questions, for command suggestions. */
	public List<String> npcNames() {
		return questions.values().stream().map(Question::npcName).distinct().sorted().toList();
	}

	/** Forgets the answers to the NPC's questions, e.g. to use it again with another class. */
	public int clear(UUID npc) {
		int before = questions.size();
		questions.values().removeIf(question -> question.npc().equals(npc));
		if (questions.size() != before) {
			setDirty();
		}
		return before - questions.size();
	}

	/** A student's answer as saved. */
	private record SavedAnswer(UUID player, String name, int option, String text, boolean right, int tries, boolean solved, long time) {
		static final Codec<SavedAnswer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("Player").forGetter(SavedAnswer::player),
				Codec.STRING.fieldOf("Name").orElse("").forGetter(SavedAnswer::name),
				Codec.INT.fieldOf("Option").orElse(0).forGetter(SavedAnswer::option),
				Codec.STRING.fieldOf("Text").orElse("").forGetter(SavedAnswer::text),
				Codec.BOOL.fieldOf("Right").orElse(false).forGetter(SavedAnswer::right),
				Codec.INT.fieldOf("Tries").orElse(0).forGetter(SavedAnswer::tries),
				Codec.BOOL.fieldOf("Solved").orElse(false).forGetter(SavedAnswer::solved),
				Codec.LONG.fieldOf("Time").orElse(0L).forGetter(SavedAnswer::time)
		).apply(instance, SavedAnswer::new));
	}

	/** A question as saved, with its answers. */
	private record SavedQuestion(UUID npc, String npcName, int page, int index, String text, boolean poll, List<SavedAnswer> answers) {
		static final Codec<SavedQuestion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("Npc").forGetter(SavedQuestion::npc),
				Codec.STRING.fieldOf("NpcName").orElse("").forGetter(SavedQuestion::npcName),
				Codec.INT.fieldOf("Page").orElse(0).forGetter(SavedQuestion::page),
				Codec.INT.fieldOf("Index").orElse(0).forGetter(SavedQuestion::index),
				Codec.STRING.fieldOf("Question").orElse("").forGetter(SavedQuestion::text),
				Codec.BOOL.fieldOf("Poll").orElse(false).forGetter(SavedQuestion::poll),
				DataFormat.lenientList(SavedAnswer.CODEC, "quiz answer").fieldOf("Answers").orElse(List.of()).forGetter(SavedQuestion::answers)
		).apply(instance, SavedQuestion::new));
	}

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		DataFormat.writeList(nbt, "Questions", SavedQuestion.CODEC, questions.values().stream().map(question -> new SavedQuestion(
				question.npc(), question.npcName(), question.page(), question.index(), question.text(), question.poll(),
				question.answers().entrySet().stream().map(e -> {
					Answer answer = e.getValue();
					return new SavedAnswer(e.getKey(), answer.name(), answer.option(), answer.text(), answer.right(), answer.tries(),
							answer.solved(), answer.time());
				}).toList())).toList());
		return nbt;
	}

	public static QuizResults fromNbt(CompoundTag nbt) {
		QuizResults results = new QuizResults();
		for (SavedQuestion saved : DataFormat.readList(nbt, "Questions", SavedQuestion.CODEC, "quiz question")) {
			Map<UUID, Answer> answers = new HashMap<>();
			for (SavedAnswer answer : saved.answers()) {
				answers.put(answer.player(), new Answer(answer.name(), answer.option(), answer.text(), answer.right(), answer.tries(),
						answer.solved(), answer.time()));
			}
			Question question = new Question(saved.npc(), saved.npcName(), saved.page(), saved.index(), saved.text(), saved.poll(), answers);
			results.questions.put(key(question.npc(), question.text()), question);
		}
		return results;
	}
}
