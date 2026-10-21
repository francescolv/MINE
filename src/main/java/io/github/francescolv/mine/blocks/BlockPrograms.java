package io.github.francescolv.mine.blocks;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The students' programs made with blocks, as Blockly saves them, kept in the world
 * ({@code data/mine/blocks.dat}): the editor shows them again after a restart, and the teacher
 * can look at them. Their Python programs are kept the same way, as source, in
 * {@code data/mine/python.dat} ({@link #python}).
 */
public final class BlockPrograms extends SavedData {

	/** Programs a student can keep; saving one more forgets the oldest. */
	static final int MAX_PROGRAMS = 30;
	/** A program as Blockly saves it: big enough for any lesson. */
	static final int MAX_WORKSPACE = 256 * 1024;

	private static final String KEY = "blocks";
	private static final SavedDataType<BlockPrograms> TYPE = DataFormat.saved(KEY, BlockPrograms::new, BlockPrograms::fromNbt, BlockPrograms::save);
	private static final SavedDataType<BlockPrograms> PYTHON = DataFormat.saved("python", BlockPrograms::new, BlockPrograms::fromNbt, BlockPrograms::save);

	/** Student → their name (for the teacher), their programs (name → workspace, oldest first) and the last one. */
	private final Map<UUID, Student> students = new HashMap<>();

	record Student(String name, LinkedHashMap<String, String> programs, String last) {
	}

	public static BlockPrograms get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	/** The students' Python programs: the source in place of the workspace. */
	public static BlockPrograms python(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(PYTHON);
	}

	/** Keeps {@code workspace} as the student's program {@code program}, and their last one. */
	void put(UUID student, String studentName, String program, String workspace) {
		Student old = students.get(student);
		LinkedHashMap<String, String> programs = old == null ? new LinkedHashMap<>() : old.programs();
		programs.remove(program);
		programs.put(program, workspace.length() > MAX_WORKSPACE ? "" : workspace);
		while (programs.size() > MAX_PROGRAMS) {
			programs.remove(programs.keySet().iterator().next());
		}
		students.put(student, new Student(studentName, programs, program));
		setDirty();
	}

	/**
	 * Gives the student programs (e.g. a lesson's broken programs to fix), replacing theirs with
	 * the same names; the program they were working on stays the one the editor opens.
	 */
	public void give(UUID student, String studentName, Map<String, String> programs) {
		if (programs.isEmpty()) {
			return;
		}
		Student old = students.get(student);
		String last = old == null ? "" : old.last();
		programs.forEach((program, workspace) -> put(student, studentName, program, workspace));
		Student now = students.get(student);
		students.put(student, new Student(now.name(), now.programs(), last));
	}

	/** One of the student's programs, as Blockly saved it; {@code null} if they have none by that name. */
	@Nullable
	public String program(UUID student, String program) {
		Student found = students.get(student);
		return found == null ? null : found.programs().get(program);
	}

	@Nullable
	Student student(UUID student) {
		return students.get(student);
	}

	/** Students with programs, for the teacher. */
	Map<UUID, Student> all() {
		return Map.copyOf(students);
	}

	/** A program as saved: its name and its blocks. */
	private record SavedProgram(String name, String workspace) {
		static final Codec<SavedProgram> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("Name").orElse("").forGetter(SavedProgram::name),
				Codec.STRING.fieldOf("Workspace").orElse("").forGetter(SavedProgram::workspace)
		).apply(instance, SavedProgram::new));
	}

	/** A student's programs as saved, oldest first. */
	private record SavedStudent(UUID player, String name, String last, List<SavedProgram> programs) {
		static final Codec<SavedStudent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("Player").forGetter(SavedStudent::player),
				Codec.STRING.fieldOf("Name").orElse("").forGetter(SavedStudent::name),
				Codec.STRING.fieldOf("Last").orElse("").forGetter(SavedStudent::last),
				DataFormat.lenientList(SavedProgram.CODEC, "program").fieldOf("Programs").orElse(List.of()).forGetter(SavedStudent::programs)
		).apply(instance, SavedStudent::new));
	}

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		DataFormat.writeList(nbt, "Students", SavedStudent.CODEC, students.entrySet().stream().map(e -> new SavedStudent(e.getKey(),
				e.getValue().name(), e.getValue().last(), e.getValue().programs().entrySet().stream()
						.map(program -> new SavedProgram(program.getKey(), program.getValue())).toList())).toList());
		return nbt;
	}

	public static BlockPrograms fromNbt(CompoundTag nbt) {
		BlockPrograms store = new BlockPrograms();
		for (SavedStudent saved : DataFormat.readList(nbt, "Students", SavedStudent.CODEC, "student's programs")) {
			LinkedHashMap<String, String> programs = new LinkedHashMap<>();
			saved.programs().forEach(program -> programs.put(program.name(), program.workspace()));
			store.students.put(saved.player(), new Student(saved.name(), programs, saved.last()));
		}
		return store;
	}

	/** The names of a student's programs, newest first. */
	static List<String> names(Student student) {
		List<String> names = new ArrayList<>(student.programs().keySet());
		Collections.reverse(names);
		return names;
	}
}
