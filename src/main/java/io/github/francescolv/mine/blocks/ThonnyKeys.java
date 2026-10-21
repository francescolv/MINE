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

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Each student's code for Python on their PC (Thonny): short, easy to type, and the same every
 * day, kept in the world ({@code data/mine/thonny.dat}). Thonny's mine.py asks for it once and
 * remembers it in the student's folder. It only works while the student is in the game, as the
 * editor's links.
 */
public final class ThonnyKeys extends SavedData {

	/** No 0/O, 1/I/L: read from the screen and typed by hand. */
	private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
	private static final int LENGTH = 6;
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final SavedDataType<ThonnyKeys> TYPE = DataFormat.saved("thonny", ThonnyKeys::new, ThonnyKeys::fromNbt, ThonnyKeys::save);

	/** Code → student, for the editor's threads (which mustn't touch the world's saved data). */
	private static final Map<String, UUID> BY_KEY = new ConcurrentHashMap<>();

	private final Map<UUID, String> keys = new HashMap<>();

	public static ThonnyKeys get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	/** The student's code, made the first time. Server thread. */
	public String keyFor(UUID student) {
		String key = keys.get(student);
		if (key == null) {
			do {
				StringBuilder made = new StringBuilder();
				for (int i = 0; i < LENGTH; i++) {
					made.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
				}
				key = made.toString();
			} while (BY_KEY.containsKey(key));
			keys.put(student, key);
			BY_KEY.put(key, student);
			setDirty();
		}
		return key;
	}

	/** Who the code stands for, or {@code null}; any thread (the store must have been loaded). */
	@Nullable
	public static UUID student(@Nullable String key) {
		return key == null ? null : BY_KEY.get(key.strip().toUpperCase(Locale.ROOT));
	}

	/** A student's key as saved. */
	private record Saved(UUID player, String key) {
		static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("Player").forGetter(Saved::player),
				Codec.STRING.fieldOf("Key").orElse("").forGetter(Saved::key)
		).apply(instance, Saved::new));
	}

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		DataFormat.writeList(nbt, "Keys", Saved.CODEC, keys.entrySet().stream().map(e -> new Saved(e.getKey(), e.getValue())).toList());
		return nbt;
	}

	public static ThonnyKeys fromNbt(CompoundTag nbt) {
		ThonnyKeys store = new ThonnyKeys();
		for (Saved saved : DataFormat.readList(nbt, "Keys", Saved.CODEC, "Thonny key")) {
			store.keys.put(saved.player(), saved.key());
			BY_KEY.put(saved.key(), saved.player());
		}
		return store;
	}
}
