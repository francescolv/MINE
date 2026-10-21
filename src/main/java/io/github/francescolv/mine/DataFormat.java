package io.github.francescolv.mine;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Version numbers of what MINE reads and writes, so that a future MINE knows what it is reading
 * and an older one says so instead of misreading it:
 * <ul>
 * <li>{@link #FILES}: lessons, NPCs and boards in datapacks, written by teachers
 * ({@code "format": 1}; a file without it is format 1);</li>
 * <li>{@link #SAVED}: what MINE keeps in the world (lessons' progress, the lab, portfolios...),
 * see {@link #saved}.</li>
 * </ul>
 * A new MINE changes a number only when it changes a format in a way the old one can't read, and
 * then reads the old format too.
 */
public final class DataFormat {

	public static final int FILES = 1;
	public static final int SAVED = 1;
	private static final String FILE_KEY = "format";
	private static final String SAVED_KEY = "MineFormat";

	/** What MINE saves in the world, by name: {@code data/mine/<name>.dat}. */
	private static final List<String> SAVED_NAMES = new ArrayList<>();

	private DataFormat() {
	}

	/**
	 * Reads each JSON file {@code finder} finds in the datapacks and hands it to {@code use}. A file
	 * that can't be read, or is in a newer format, is skipped with a warning in the server log
	 * naming it as a {@code what}: a typo in a teacher's file must not stop the others from working.
	 */
	public static void loadJson(ResourceManager manager, FileToIdConverter finder, String what, BiConsumer<Identifier, JsonElement> use) {
		for (Map.Entry<Identifier, Resource> file : finder.listMatchingResources(manager).entrySet()) {
			Identifier id = finder.fileToId(file.getKey());
			try (Reader reader = file.getValue().openAsReader()) {
				JsonElement json = JsonParser.parseReader(reader);
				String tooNew = tooNew(json);
				if (tooNew != null) {
					MineMod.LOGGER.warn("[MINE] Skipping {} {}: {}", what, id, tooNew);
					continue;
				}
				use.accept(id, json);
			} catch (Exception e) {
				MineMod.LOGGER.warn("[MINE] Skipping {} {}: {}", what, id, e.getMessage());
			}
		}
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTING.register(DataFormat::moveOldFiles);
	}

	/**
	 * How MINE keeps {@code name} in the world, in {@code data/mine/<name>.dat}, marked with its
	 * format. Minecraft's command storage fixes are used when Minecraft updates the file: they
	 * leave data they don't know alone, as MINE's.
	 */
	public static <T extends SavedData> SavedDataType<T> saved(String name, Supplier<T> empty,
			Function<CompoundTag, T> read, Function<T, CompoundTag> write) {
		SAVED_NAMES.add(name);
		return new SavedDataType<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, name), empty,
				CompoundTag.CODEC.xmap(nbt -> {
					check(nbt, name);
					return read.apply(nbt);
				}, state -> write(write.apply(state))),
				DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
	}

	/** MINE for Minecraft 1.21 kept {@code data/mine_<name>.dat}: moved to {@code data/mine/<name>.dat}. */
	private static void moveOldFiles(MinecraftServer server) {
		Path data = server.getWorldPath(LevelResource.DATA);
		for (String name : SAVED_NAMES) {
			Path old = data.resolve("mine_" + name + ".dat");
			Path now = data.resolve(MineMod.MOD_ID).resolve(name + ".dat");
			if (Files.exists(old) && !Files.exists(now)) {
				try {
					Files.createDirectories(now.getParent());
					Files.move(old, now);
					MineMod.LOGGER.info("[MINE] Moved {} to {}", old.getFileName(), data.relativize(now));
				} catch (IOException e) {
					MineMod.LOGGER.error("[MINE] Couldn't move {} to {}: {}", old, now, e.getMessage());
				}
			}
		}
	}

	/** Why this MINE can't read a datapack file, or {@code null} if it can. */
	@Nullable
	public static String tooNew(JsonElement json) {
		if (json.isJsonObject() && json.getAsJsonObject().has(FILE_KEY)) {
			int format = json.getAsJsonObject().get(FILE_KEY).getAsInt();
			if (format > FILES) {
				return "made for a newer MINE (format " + format + ", this MINE " + MineMod.VERSION + " reads up to " + FILES + "): update MINE";
			}
		}
		return null;
	}

	/**
	 * The entries of the list {@code key} in saved data, each read with {@code codec}. An entry that
	 * can't be read is left out, with a warning naming it as one of {@code what}: the others stay.
	 */
	public static <T> List<T> readList(CompoundTag nbt, String key, Codec<T> codec, String what) {
		List<T> entries = new ArrayList<>();
		ListTag list = nbt.getListOrEmpty(key);
		for (int i = 0; i < list.size(); i++) {
			int index = i;
			codec.parse(NbtOps.INSTANCE, list.get(i))
					.ifSuccess(entries::add)
					.ifError(error -> MineMod.LOGGER.warn("[MINE] Skipping {} number {} of the saved data: {}", what, index + 1, error.message()));
		}
		return entries;
	}

	/**
	 * A list in saved data, inside an entry of another list: as {@link #readList}, an element that
	 * can't be read is left out, with a warning naming it as one of {@code what}.
	 */
	public static <T> Codec<List<T>> lenientList(Codec<T> codec, String what) {
		return Codec.PASSTHROUGH.listOf().xmap(elements -> {
			List<T> entries = new ArrayList<>();
			for (Dynamic<?> element : elements) {
				codec.parse(element)
						.ifSuccess(entries::add)
						.ifError(error -> MineMod.LOGGER.warn("[MINE] Skipping a {} of the saved data: {}", what, error.message()));
			}
			return entries;
		}, entries -> entries.stream().<Dynamic<?>>map(entry -> new Dynamic<>(NbtOps.INSTANCE,
				codec.encodeStart(NbtOps.INSTANCE, entry).getOrThrow())).toList());
	}

	/** Saves {@code entries} as the list {@code key}, each written with {@code codec}. */
	public static <T> void writeList(CompoundTag nbt, String key, Codec<T> codec, Iterable<T> entries) {
		ListTag list = new ListTag();
		for (T entry : entries) {
			list.add(codec.encodeStart(NbtOps.INSTANCE, entry).getOrThrow());
		}
		nbt.put(key, list);
	}

	/** Marks saved data with its format. */
	public static CompoundTag write(CompoundTag nbt) {
		nbt.putInt(SAVED_KEY, SAVED);
		return nbt;
	}

	/** Saved data from a newer MINE (the world was opened with it, then MINE went back): said in the log. */
	public static void check(CompoundTag nbt, String what) {
		int format = nbt.getIntOr(SAVED_KEY, 1);
		if (format > SAVED) {
			MineMod.LOGGER.warn("[MINE] {} was saved by a newer MINE (format {}, this MINE {} reads up to {}): some of it may be lost. Update MINE.",
					what, format, MineMod.VERSION, SAVED);
		}
	}
}
