package io.github.francescolv.mine.permission;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.francescolv.mine.DataFormat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who may do what in MINE. Three roles, like Minecraft Education's host, world builder and student:
 * <ul>
 * <li><b>teachers</b> are operators (permission level 2, like vanilla commands such as /gamemode):
 * they manage the class and can change everything;</li>
 * <li><b>builders</b> are students the teacher allows to build with MINE's tools (NPCs, for now),
 * changing only what they made themselves, without the powers of an operator;</li>
 * <li><b>students</b> are everyone else.</li>
 * </ul>
 * In a single-player world with cheats on, the player is an operator, so a teacher.
 * Builders are saved in the world ({@code data/mine/builders.dat}).
 *
 * <p>A teacher can switch to the <b>student view</b> ({@code /mine view student}) to try lessons
 * and NPCs as students see them: MINE then treats them as a student, until they switch back or
 * the server restarts. Minecraft's own operator powers (e.g. /gamemode) are not MINE's to take.
 */
public final class Permissions extends SavedData {

	public static final int TEACHER = 2;
	/** What operators of {@link #TEACHER} level have: Minecraft's game master commands. */
	public static final Permission TEACHER_PERMISSION = net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER;

	private static final String KEY = "builders";
	private static final SavedDataType<Permissions> TYPE = DataFormat.saved(KEY, Permissions::new, Permissions::fromNbt, Permissions::save);

	/** Teachers who are looking at MINE as a student. Not saved: a restart brings everyone back. */
	private static final Set<UUID> STUDENT_VIEW = ConcurrentHashMap.newKeySet();

	/** Builders and their name, to list them also when they are offline. */
	private final Map<UUID, String> builders = new HashMap<>();

	public static Permissions get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public static boolean isTeacher(ServerPlayer player) {
		return player.permissions().hasPermission(TEACHER_PERMISSION) && !inStudentView(player);
	}

	public static boolean isTeacher(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		return source.permissions().hasPermission(TEACHER_PERMISSION) && (player == null || !inStudentView(player));
	}

	/** Teachers, and students the teacher made builders. */
	public static boolean isBuilder(ServerPlayer player) {
		return isTeacher(player)
				|| (!inStudentView(player) && get(player.level().getServer()).builders.containsKey(player.getUUID()));
	}

	/** Whether the player is a teacher looking at MINE as a student. */
	public static boolean inStudentView(ServerPlayer player) {
		return STUDENT_VIEW.contains(player.getUUID());
	}

	/** Switches the student view on or off; {@code false} if it already was. */
	public static boolean setStudentView(ServerPlayer player, boolean on) {
		return on ? STUDENT_VIEW.add(player.getUUID()) : STUDENT_VIEW.remove(player.getUUID());
	}

	/** For command requirements: the console and command blocks count as teachers. */
	public static boolean isBuilder(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		return isTeacher(source) || (player != null && isBuilder(player));
	}

	/** Makes the player a builder; {@code false} if they already were. */
	public boolean addBuilder(ServerPlayer player) {
		boolean added = builders.put(player.getUUID(), player.getName().getString()) == null;
		setDirty();
		return added;
	}

	/** {@code false} if the player wasn't a builder. */
	public boolean removeBuilder(ServerPlayer player) {
		boolean removed = builders.remove(player.getUUID()) != null;
		if (removed) {
			setDirty();
		}
		return removed;
	}

	/** The builders' names, sorted. */
	public List<String> builderNames() {
		return builders.values().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
	}

	/** A builder as saved. */
	private record Saved(UUID player, String name) {
		static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("Player").forGetter(Saved::player),
				Codec.STRING.fieldOf("Name").orElse("").forGetter(Saved::name)
		).apply(instance, Saved::new));
	}

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		DataFormat.writeList(nbt, "Builders", Saved.CODEC, builders.entrySet().stream().map(e -> new Saved(e.getKey(), e.getValue())).toList());
		return nbt;
	}

	public static Permissions fromNbt(CompoundTag nbt) {
		Permissions permissions = new Permissions();
		DataFormat.readList(nbt, "Builders", Saved.CODEC, "builder").forEach(saved -> permissions.builders.put(saved.player(), saved.name()));
		return permissions;
	}
}
