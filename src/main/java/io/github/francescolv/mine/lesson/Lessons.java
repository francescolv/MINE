package io.github.francescolv.mine.lesson;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.francescolv.mine.DataFormat;
import io.github.francescolv.mine.MineMod;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * All the lessons with missions, read from the datapacks: {@code data/<namespace>/mine_lesson/<id>.json},
 * plus the programs they give students in {@code data/<namespace>/mine_lesson_files/<id>/<language>/}
 * ({@code .lua} for the Agent, {@code .json} for the block editor).
 * They are read again on {@code /reload}, so a teacher can try a lesson while writing it.
 * The format is described in {@code docs/guide/lesson-format.md}.
 */
public final class Lessons {

	private static final FileToIdConverter FINDER = FileToIdConverter.json("mine_lesson");
	private static final FileToIdConverter FILES = new FileToIdConverter("mine_lesson_files", ".lua");
	private static final FileToIdConverter BLOCK_FILES = new FileToIdConverter("mine_lesson_files", ".json");

	/** Lesson ids are typed in commands: keep them to letters, digits and underscores. */
	private static final String ID_PATTERN = "[a-z0-9_]+";

	private static volatile List<Lesson> all = List.of();

	private Lessons() {
	}

	public static void register() {
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public Identifier getFabricId() {
				return Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "lessons");
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				all = load(manager);
			}
		});
	}

	/** Every lesson, in the order they should be taught. */
	public static List<Lesson> all() {
		return all;
	}

	@Nullable
	public static Lesson get(String id) {
		for (Lesson lesson : all) {
			if (lesson.id().equals(id)) {
				return lesson;
			}
		}
		return null;
	}

	/**
	 * Reads the lesson files. A broken file is skipped with a warning in the server log: one
	 * typo in a teacher's lesson must not stop the others from working.
	 */
	private static List<Lesson> load(ResourceManager manager) {
		Map<String, Lesson> lessons = new LinkedHashMap<>();
		for (Map.Entry<Identifier, Resource> file : FINDER.listMatchingResources(manager).entrySet()) {
			Identifier fileId = FINDER.fileToId(file.getKey());
			String id = fileId.getPath();
			if (!id.matches(ID_PATTERN)) {
				MineMod.LOGGER.warn("[MINE] Skipping lesson {}: its file name may only use a-z, 0-9 and _", fileId);
				continue;
			}
			try (Reader reader = file.getValue().openAsReader()) {
				JsonElement json = JsonParser.parseReader(reader);
				String tooNew = DataFormat.tooNew(json);
				if (tooNew != null) {
					MineMod.LOGGER.warn("[MINE] Skipping lesson {}: {}", fileId, tooNew);
					continue;
				}
				Lesson.codec(id).parse(JsonOps.INSTANCE, json)
						.ifSuccess(lesson -> {
							if (lessons.put(id, lesson) != null) {
								MineMod.LOGGER.warn("[MINE] Two lessons are called {}: using the one in {}", id, fileId);
							}
						})
						.ifError(error -> MineMod.LOGGER.warn("[MINE] Skipping lesson {}: {}", fileId, error.message()));
			} catch (Exception e) {
				MineMod.LOGGER.warn("[MINE] Skipping lesson {}: {}", fileId, e.getMessage());
			}
		}
		Map<String, Map<String, Map<String, String>>> files = loadFiles(manager, FILES, ".lua");
		Map<String, Map<String, Map<String, String>>> blocks = loadFiles(manager, BLOCK_FILES, "");
		blocks.forEach((id, byLanguage) -> byLanguage.forEach((language, programs) -> programs.entrySet().removeIf(program -> {
			if (program.getKey().length() <= 24 && isJsonObject(program.getValue())) {
				return false;
			}
			MineMod.LOGGER.warn("[MINE] Skipping block program {}/{}/{}: it must be a program saved by MINE's block editor, "
					+ "with a name of up to 24 letters", id, language, program.getKey());
			return true;
		})));
		for (String id : union(files.keySet(), blocks.keySet())) {
			Lesson lesson = lessons.get(id);
			if (lesson == null) {
				MineMod.LOGGER.warn("[MINE] Programs for lesson {}, but there is no such lesson", id);
			} else {
				lessons.put(id, lesson.withFiles(files.getOrDefault(id, Map.of()), blocks.getOrDefault(id, Map.of())));
			}
		}
		List<Lesson> sorted = lessons.values().stream()
				.sorted(Comparator.comparingInt(Lesson::order).thenComparing(Lesson::id))
				.toList();
		MineMod.LOGGER.info("[MINE] Loaded {} lessons: {}", sorted.size(), sorted.stream().map(Lesson::id).toList());
		return sorted;
	}

	private static Set<String> union(Set<String> a, Set<String> b) {
		Set<String> all = new TreeSet<>(a);
		all.addAll(b);
		return all;
	}

	private static boolean isJsonObject(String text) {
		try {
			return JsonParser.parseString(text).isJsonObject();
		} catch (RuntimeException e) {
			return false;
		}
	}

	/** Lesson id → language → name (+ {@code suffix}) → contents, for the files {@code finder} finds. */
	private static Map<String, Map<String, Map<String, String>>> loadFiles(ResourceManager manager, FileToIdConverter finder,
			String suffix) {
		Map<String, Map<String, Map<String, String>>> files = new TreeMap<>();
		for (Map.Entry<Identifier, Resource> file : finder.listMatchingResources(manager).entrySet()) {
			Identifier fileId = file.getKey();
			// <lesson>/<language>/<name>.lua or .json
			String[] parts = finder.fileToId(fileId).getPath().split("/");
			if (parts.length != 3 || !parts[0].matches(ID_PATTERN) || !parts[1].matches(ID_PATTERN)
					|| !parts[2].matches(ID_PATTERN)) {
				MineMod.LOGGER.warn("[MINE] Skipping {}: lesson programs go in mine_lesson_files/<lesson>/<language>/<name>.lua (or .json)", fileId);
				continue;
			}
			try (InputStream in = file.getValue().open()) {
				files.computeIfAbsent(parts[0], id -> new TreeMap<>())
						.computeIfAbsent(parts[1], language -> new TreeMap<>())
						.put(parts[2] + suffix, new String(in.readAllBytes(), StandardCharsets.UTF_8));
			} catch (Exception e) {
				MineMod.LOGGER.warn("[MINE] Skipping {}: {}", fileId, e.getMessage());
			}
		}
		return files;
	}
}
