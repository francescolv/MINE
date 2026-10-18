package io.github.francescolv.mine.lesson;

import io.github.francescolv.mine.DataFormat;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Each student's lesson progress, and their answers to the lessons' reflection questions, saved
 * in the world ({@code data/mine/lessons.dat}). Answers are kept for every lesson, also when the
 * student moves on to another one or their progress is reset: they are the trace of their thinking.
 * So are the lessons each student has completed.
 */
public final class LessonProgress extends SavedData {

	private static final String KEY = "lessons";
	private static final SavedDataType<LessonProgress> TYPE = DataFormat.saved(KEY, LessonProgress::new, LessonProgress::fromNbt, LessonProgress::save);

	public enum Status {
		NOT_STARTED,
		IN_PROGRESS,
		COMPLETED
	}

	/**
	 * Progress in one lesson: the current step, how many of its hints were shown, when the
	 * lesson was started and completed (epoch milliseconds, 0 = not yet), and the last step whose
	 * items the Agent has received ({@code given}, -1 = none yet).
	 */
	public record Entry(String lesson, int step, int hints, long startedAt, long completedAt, int given) {

		public Entry(String lesson, int step, int hints, long startedAt, long completedAt) {
			this(lesson, step, hints, startedAt, completedAt, -1);
		}

		public Status status() {
			return completedAt > 0 ? Status.COMPLETED : Status.IN_PROGRESS;
		}

		Entry nextStep() {
			return new Entry(lesson, step + 1, 0, startedAt, completedAt, given);
		}

		Entry completed(long time) {
			return new Entry(lesson, step + 1, 0, startedAt, time, given);
		}

		Entry withHints(int shown) {
			return new Entry(lesson, step, shown, startedAt, completedAt, given);
		}

		Entry withGiven(int givenStep) {
			return new Entry(lesson, step, hints, startedAt, completedAt, givenStep);
		}
	}

	/** An answer to a lesson's reflection question; the name is kept for students who are offline. */
	public record Answer(String name, String text, long time) {
	}

	private final Map<UUID, Entry> students = new HashMap<>();
	/** Student → lesson → their latest answer. */
	private final Map<UUID, Map<String, Answer>> answers = new HashMap<>();
	/** Student → the lessons they have completed, also before the current one. */
	private final Map<UUID, Set<String>> completed = new HashMap<>();

	public static LessonProgress get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	/** The student's progress, or {@code null} if they haven't started a lesson. */
	@Nullable
	public Entry get(UUID student) {
		return students.get(student);
	}

	public Status status(UUID student) {
		Entry entry = students.get(student);
		return entry == null ? Status.NOT_STARTED : entry.status();
	}

	public void put(UUID student, Entry entry) {
		if (!entry.equals(students.put(student, entry))) {
			setDirty();
		}
		if (entry.status() == Status.COMPLETED) {
			completed.computeIfAbsent(student, uuid -> new HashSet<>()).add(entry.lesson());
		}
	}

	/**
	 * Whether the student has ever completed the lesson. Worlds from before this was kept count
	 * the current lesson if completed, and the lessons whose reflection they answered (only
	 * possible at the end).
	 */
	public boolean hasCompleted(UUID student, String lesson) {
		Entry entry = students.get(student);
		return completed.getOrDefault(student, Set.of()).contains(lesson)
				|| (entry != null && entry.lesson().equals(lesson) && entry.status() == Status.COMPLETED)
				|| answer(student, lesson) != null;
	}

	public void remove(UUID student) {
		if (students.remove(student) != null) {
			setDirty();
		}
	}

	public void putAnswer(UUID student, String lesson, Answer answer) {
		answers.computeIfAbsent(student, uuid -> new HashMap<>()).put(lesson, answer);
		setDirty();
	}

	@Nullable
	public Answer answer(UUID student, String lesson) {
		Map<String, Answer> byLesson = answers.get(student);
		return byLesson == null ? null : byLesson.get(lesson);
	}

	/** One student's answers, by lesson. */
	public Map<String, Answer> answersOf(UUID student) {
		return Map.copyOf(answers.getOrDefault(student, Map.of()));
	}

	/** Everyone's answers to one lesson, sorted by name. */
	public List<Answer> answers(String lesson) {
		return answers.values().stream()
				.map(byLesson -> byLesson.get(lesson))
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(Answer::name, String.CASE_INSENSITIVE_ORDER))
				.toList();
	}

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		ListTag list = new ListTag();
		students.forEach((student, entry) -> {
			CompoundTag tag = new CompoundTag();
			tag.store("Player", UUIDUtil.CODEC, student);
			tag.putString("Lesson", entry.lesson());
			tag.putInt("Step", entry.step());
			tag.putInt("Hints", entry.hints());
			tag.putLong("StartedAt", entry.startedAt());
			tag.putLong("CompletedAt", entry.completedAt());
			tag.putInt("Given", entry.given());
			list.add(tag);
		});
		nbt.put("Students", list);

		ListTag answerList = new ListTag();
		answers.forEach((student, byLesson) -> byLesson.forEach((lesson, answer) -> {
			CompoundTag tag = new CompoundTag();
			tag.store("Player", UUIDUtil.CODEC, student);
			tag.putString("Lesson", lesson);
			tag.putString("Name", answer.name());
			tag.putString("Text", answer.text());
			tag.putLong("Time", answer.time());
			answerList.add(tag);
		}));
		nbt.put("Answers", answerList);

		ListTag completedList = new ListTag();
		completed.forEach((student, lessons) -> lessons.forEach(lesson -> {
			CompoundTag tag = new CompoundTag();
			tag.store("Player", UUIDUtil.CODEC, student);
			tag.putString("Lesson", lesson);
			completedList.add(tag);
		}));
		nbt.put("Completed", completedList);
		return nbt;
	}

	public static LessonProgress fromNbt(CompoundTag nbt) {
		LessonProgress progress = new LessonProgress();
		ListTag list = nbt.getListOrEmpty("Students");
		for (int i = 0; i < list.size(); i++) {
			CompoundTag tag = list.getCompoundOrEmpty(i);
			progress.students.put(tag.read("Player", UUIDUtil.CODEC).orElseThrow(), new Entry(tag.getStringOr("Lesson", ""), tag.getIntOr("Step", 0),
					tag.getIntOr("Hints", 0), tag.getLongOr("StartedAt", 0L), tag.getLongOr("CompletedAt", 0L),
					tag.contains("Given") ? tag.getIntOr("Given", 0) : -1));
		}
		ListTag answerList = nbt.getListOrEmpty("Answers");
		for (int i = 0; i < answerList.size(); i++) {
			CompoundTag tag = answerList.getCompoundOrEmpty(i);
			progress.answers.computeIfAbsent(tag.read("Player", UUIDUtil.CODEC).orElseThrow(), uuid -> new HashMap<>()).put(tag.getStringOr("Lesson", ""),
					new Answer(tag.getStringOr("Name", ""), tag.getStringOr("Text", ""), tag.getLongOr("Time", 0L)));
		}
		ListTag completedList = nbt.getListOrEmpty("Completed");
		for (int i = 0; i < completedList.size(); i++) {
			CompoundTag tag = completedList.getCompoundOrEmpty(i);
			progress.completed.computeIfAbsent(tag.read("Player", UUIDUtil.CODEC).orElseThrow(), uuid -> new HashSet<>()).add(tag.getStringOr("Lesson", ""));
		}
		return progress;
	}
}
