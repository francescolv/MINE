package io.github.francescolv.mine.npc;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * NPCs and boards described in datapacks, for lessons that come with their own characters and
 * signs: {@code data/<namespace>/mine_npc/<id>.json} and {@code data/<namespace>/mine_board/<id>.json}.
 * Their texts are in the files themselves, one list per language (or a single list):
 *
 * <pre>
 * { "name": "Ada", "pages": { "en_us": ["Hi!", "..."], "it_it": ["Ciao!", "..."] } }
 * { "size": 2, "text": { "en_us": ["# Loops", "..."], "it_it": ["# Cicli", "..."] } }
 * </pre>
 *
 * Read again on {@code /reload}. The format is described in {@code docs/guide/npc.md}.
 */
public final class Templates {

	/** Texts in this language are used when there are none in the reader's. */
	public static final String DEFAULT_LANGUAGE = "en_us";

	/** Language → lines (or pages); a plain list is the same text for every language. */
	private static final Codec<Map<String, List<String>>> BY_LANGUAGE = Codec.either(
					Codec.STRING.listOf(), Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()))
			.xmap(either -> either.map(list -> Map.of(DEFAULT_LANGUAGE, list), Function.identity()),
					Either::<List<String>, Map<String, List<String>>>right)
			.validate(byLanguage -> byLanguage.isEmpty()
					? DataResult.error(() -> "no texts") : DataResult.success(byLanguage));

	public record NpcTemplate(Identifier id, String name, Map<String, List<String>> pages) {

		static Codec<NpcTemplate> codec(Identifier id) {
			return RecordCodecBuilder.create(instance -> instance.group(
					Codec.string(1, 64).fieldOf("name").forGetter(NpcTemplate::name),
					BY_LANGUAGE.fieldOf("pages").forGetter(NpcTemplate::pages)
			).apply(instance, (name, pages) -> new NpcTemplate(id, name, pages)));
		}

		/** The dialogue in {@code language}, or in English, or in whichever language the file has. */
		public List<String> pages(String language) {
			return inLanguage(pages, language);
		}
	}

	public record BoardTemplate(Identifier id, float size, Map<String, List<String>> text) {

		static Codec<BoardTemplate> codec(Identifier id) {
			return RecordCodecBuilder.create(instance -> instance.group(
					Codec.floatRange(0.5f, 8).optionalFieldOf("size", 2f).forGetter(BoardTemplate::size),
					BY_LANGUAGE.fieldOf("text").forGetter(BoardTemplate::text)
			).apply(instance, (size, text) -> new BoardTemplate(id, size, text)));
		}

		/** The board's lines in {@code language}, or in English, or in whichever language the file has. */
		public List<String> text(String language) {
			return inLanguage(text, language);
		}
	}

	private static final FileToIdConverter NPCS = FileToIdConverter.json("mine_npc");
	private static final FileToIdConverter BOARDS = FileToIdConverter.json("mine_board");

	private static volatile Map<Identifier, NpcTemplate> npcs = Map.of();
	private static volatile Map<Identifier, BoardTemplate> boards = Map.of();

	private Templates() {
	}

	public static void register() {
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public Identifier getFabricId() {
				return Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "templates");
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				npcs = load(manager, NPCS, NpcTemplate::codec, "NPC");
				boards = load(manager, BOARDS, BoardTemplate::codec, "board");
			}
		});
	}

	@Nullable
	public static NpcTemplate npc(Identifier id) {
		return npcs.get(id);
	}

	@Nullable
	public static BoardTemplate board(Identifier id) {
		return boards.get(id);
	}

	public static Iterable<Identifier> npcIds() {
		return npcs.keySet();
	}

	public static Iterable<Identifier> boardIds() {
		return boards.keySet();
	}

	static <T> T inLanguage(Map<String, T> byLanguage, String language) {
		T text = byLanguage.get(language.toLowerCase());
		if (text == null) {
			text = byLanguage.get(DEFAULT_LANGUAGE);
		}
		return text != null ? text : byLanguage.values().iterator().next();
	}

	/** A broken file is skipped with a warning in the server log, as for lessons. */
	private static <T> Map<Identifier, T> load(ResourceManager manager, FileToIdConverter finder,
			Function<Identifier, Codec<T>> codec, String what) {
		Map<Identifier, T> loaded = new TreeMap<>();
		for (Map.Entry<Identifier, Resource> file : finder.listMatchingResources(manager).entrySet()) {
			Identifier id = finder.fileToId(file.getKey());
			try (Reader reader = file.getValue().openAsReader()) {
				JsonElement json = JsonParser.parseReader(reader);
				String tooNew = DataFormat.tooNew(json);
				if (tooNew != null) {
					MineMod.LOGGER.warn("[MINE] Skipping {} {}: {}", what, id, tooNew);
					continue;
				}
				codec.apply(id).parse(JsonOps.INSTANCE, json)
						.ifSuccess(template -> loaded.put(id, template))
						.ifError(error -> MineMod.LOGGER.warn("[MINE] Skipping {} {}: {}", what, id, error.message()));
			} catch (Exception e) {
				MineMod.LOGGER.warn("[MINE] Skipping {} {}: {}", what, id, e.getMessage());
			}
		}
		MineMod.LOGGER.info("[MINE] Loaded {} {}s: {}", loaded.size(), what, loaded.keySet());
		return Map.copyOf(loaded);
	}
}
