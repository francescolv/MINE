package io.github.francescolv.mine.lesson;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.francescolv.mine.DataFormat;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
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

	/** Whether the student has ever completed the lesson. */
	public boolean hasCompleted(UUID student, String lesson) {
		return completed.getOrDefault(student, Set.of()).contains(lesson);
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

	/** A student's progress as saved. */
	private record SavedEntry(UUID player, String lesson, int step, int hints, long startedAt, long completedAt, int given) {
		static final Codec<SavedEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("Player").forGetter(SavedEntry::player),
				Codec.STRING.fieldOf("Lesson").orElse("").forGetter(SavedEntry::lesson),
				Codec.INT.fieldOf("Step").orElse(0).forGetter(SavedEntry::step),
				Codec.INT.fieldOf("Hints").orElse(0).forGetter(SavedEntry::hints),
				Codec.LONG.fieldOf("StartedAt").orElse(0L).forGetter(SavedEntry::startedAt),
				Codec.LONG.fieldOf("CompletedAt").orElse(0L).forGetter(SavedEntry::completedAt),
				Codec.INT.fieldOf("Given").orElse(-1).forGetter(SavedEntry::given)
		).apply(instance, SavedEntry::new));
	}

	/** A reflection answer as saved. */
	private record SavedAnswer(UUID player, String lesson, String name, String text, long time) {
		static final Codec<SavedAnswer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("Player").forGetter(SavedAnswer::player),
				Codec.STRING.fieldOf("Lesson").orElse("").forGetter(SavedAnswer::lesson),
				Codec.STRING.fieldOf("Name").orElse("").forGetter(SavedAnswer::name),
				Codec.STRING.fieldOf("Text").orElse("").forGetter(SavedAnswer::text),
				Codec.LONG.fieldOf("Time").orElse(0L).forGetter(SavedAnswer::time)
		).apply(instance, SavedAnswer::new));
	}

	/** A lesson a student completed, as saved. */
	private record SavedCompleted(UUID player, String lesson) {
		static final Codec<SavedCompleted> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("Player").forGetter(SavedCompleted::player),
				Codec.STRING.fieldOf("Lesson").orElse("").forGetter(SavedCompleted::lesson)
		).apply(instance, SavedCompleted::new));
	}

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		DataFormat.writeList(nbt, "Students", SavedEntry.CODEC, students.entrySet().stream().map(e -> {
			Entry entry = e.getValue();
			return new SavedEntry(e.getKey(), entry.lesson(), entry.step(), entry.hints(), entry.startedAt(), entry.completedAt(), entry.given());
		}).toList());
		DataFormat.writeList(nbt, "Answers", SavedAnswer.CODEC, answers.entrySet().stream().flatMap(e -> e.getValue().entrySet().stream()
				.map(a -> new SavedAnswer(e.getKey(), a.getKey(), a.getValue().name(), a.getValue().text(), a.getValue().time()))).toList());
		DataFormat.writeList(nbt, "Completed", SavedCompleted.CODEC, completed.entrySet().stream().flatMap(e -> e.getValue().stream()
				.map(lesson -> new SavedCompleted(e.getKey(), lesson))).toList());
		return nbt;
	}

	public static LessonProgress fromNbt(CompoundTag nbt) {
		LessonProgress progress = new LessonProgress();
		for (SavedEntry saved : DataFormat.readList(nbt, "Students", SavedEntry.CODEC, "student's progress")) {
			progress.students.put(saved.player(), new Entry(saved.lesson(), saved.step(), saved.hints(), saved.startedAt(),
					saved.completedAt(), saved.given()));
		}
		for (SavedAnswer saved : DataFormat.readList(nbt, "Answers", SavedAnswer.CODEC, "reflection answer")) {
			progress.answers.computeIfAbsent(saved.player(), uuid -> new HashMap<>()).put(saved.lesson(),
					new Answer(saved.name(), saved.text(), saved.time()));
		}
		for (SavedCompleted saved : DataFormat.readList(nbt, "Completed", SavedCompleted.CODEC, "completed lesson")) {
			progress.completed.computeIfAbsent(saved.player(), uuid -> new HashSet<>()).add(saved.lesson());
		}
		return progress;
	}
}
