package io.github.francescolv.mine.npc;

import io.github.francescolv.mine.DataFormat;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		ListTag list = new ListTag();
		for (Question question : questions.values()) {
			CompoundTag tag = new CompoundTag();
			tag.store("Npc", UUIDUtil.CODEC, question.npc());
			tag.putString("NpcName", question.npcName());
			tag.putInt("Page", question.page());
			tag.putInt("Index", question.index());
			tag.putString("Question", question.text());
			tag.putBoolean("Poll", question.poll());
			ListTag answers = new ListTag();
			question.answers().forEach((student, answer) -> {
				CompoundTag entry = new CompoundTag();
				entry.store("Player", UUIDUtil.CODEC, student);
				entry.putString("Name", answer.name());
				entry.putInt("Option", answer.option());
				entry.putString("Text", answer.text());
				entry.putBoolean("Right", answer.right());
				entry.putInt("Tries", answer.tries());
				entry.putBoolean("Solved", answer.solved());
				entry.putLong("Time", answer.time());
				answers.add(entry);
			});
			tag.put("Answers", answers);
			list.add(tag);
		}
		nbt.put("Questions", list);
		return nbt;
	}

	public static QuizResults fromNbt(CompoundTag nbt) {
		QuizResults results = new QuizResults();
		ListTag list = nbt.getListOrEmpty("Questions");
		for (int i = 0; i < list.size(); i++) {
			CompoundTag tag = list.getCompoundOrEmpty(i);
			Map<UUID, Answer> answers = new HashMap<>();
			ListTag answerList = tag.getListOrEmpty("Answers");
			for (int j = 0; j < answerList.size(); j++) {
				CompoundTag entry = answerList.getCompoundOrEmpty(j);
				answers.put(entry.read("Player", UUIDUtil.CODEC).orElseThrow(), new Answer(entry.getStringOr("Name", ""), entry.getIntOr("Option", 0),
						entry.getStringOr("Text", ""), entry.getBooleanOr("Right", false), entry.getIntOr("Tries", 0),
						entry.getBooleanOr("Solved", false), entry.getLongOr("Time", 0L)));
			}
			Question question = new Question(tag.read("Npc", UUIDUtil.CODEC).orElseThrow(), tag.getStringOr("NpcName", ""), tag.getIntOr("Page", 0),
					tag.getIntOr("Index", 0), tag.getStringOr("Question", ""), tag.getBooleanOr("Poll", false), answers);
			results.questions.put(key(question.npc(), question.text()), question);
		}
		return results;
	}
}
