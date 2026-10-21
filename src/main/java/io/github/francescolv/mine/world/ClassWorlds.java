package io.github.francescolv.mine.world;

import io.github.francescolv.mine.DataFormat;
import io.github.francescolv.mine.Exports;
import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.lab.Lab;
import io.github.francescolv.mine.zone.Zones;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The class worlds, like Minecraft Education's: each class has its own world, made by the teacher
 * while the server runs, flat (for the lab and MINE's lessons) or as a copy of a map. The server's
 * own world stays the teacher's: {@link #MAIN}.
 *
 * <p>A class world is a dimension made with Fantasy ({@code mine:class_<name>}), kept in the
 * server's world folder like the others. Each one has its own {@link Rule class rules}, which the
 * teacher changes during the lesson; it starts with no monsters and no night (and no rain: class
 * worlds have the main world's weather, which a class server keeps clear). Fantasy doesn't open
 * them again by itself after a restart: MINE does, from the list kept here
 * ({@code data/mine/worlds.dat}).
 *
 * <p>Maps are Java worlds the teacher puts in {@code <server>/mine/maps/<name>/}, downloaded or
 * made: a class world made from one is a copy of its overworld, so the map itself never changes
 * and the next class gets it as new. Outside the map there is nothing (void).
 */
public final class ClassWorlds extends SavedData {

	/** What students and teachers type for the server's own world. */
	public static final String MAIN = "main";
	/** World names are typed in commands: short, small letters, digits and _. */
	public static final Pattern NAME = Pattern.compile("[a-z0-9_]{1,16}");
	/** Where teachers put the maps, in the server's folder. */
	public static final String MAPS = "mine/maps";
	/** The parts of a world's folder that are copied from a map: blocks, entities, points of interest. */
	private static final List<String> MAP_PARTS = List.of("region", "entities", "poi");
	/** Midday on the world's clock. */
	private static final int NOON = 6000;

	private static final String KEY = "worlds";
	private static final SavedDataType<ClassWorlds> TYPE = DataFormat.saved(KEY, ClassWorlds::new, ClassWorlds::fromNbt, ClassWorlds::save);

	public enum Kind { FLAT, MAP }

	/**
	 * What the teacher allows in a class world, each one on or off: during the lesson nothing
	 * that gets in the way, at the end free time. Their order is the order they are listed in.
	 */
	public enum Rule {
		/** Hostile mobs (the world's difficulty: normal, or peaceful). */
		MONSTERS,
		/** Day and night (otherwise always midday). */
		NIGHT,
		/** Fire spreading. */
		FIRE,
		/** TNT, and creepers breaking blocks. */
		EXPLOSIONS,
		/** Players hurting each other. */
		PVP;

		public String id() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/** A new class world: no monsters, always day; the rest as in Minecraft. */
	public static final Set<Rule> NEW_WORLD = EnumSet.of(Rule.FIRE, Rule.EXPLOSIONS, Rule.PVP);
	/** "Lesson": nothing that gets in the way. */
	public static final Set<Rule> LESSON = EnumSet.noneOf(Rule.class);
	/** "Free time": everything. */
	public static final Set<Rule> FREE = EnumSet.allOf(Rule.class);

	/** A class world: how it was made, where students arrive, and what is allowed in it. */
	public record World(String name, Kind kind, String map, BlockPos spawn, float yaw, long created, Set<Rule> rules) {

		public World {
			rules = rules.isEmpty() ? EnumSet.noneOf(Rule.class) : EnumSet.copyOf(rules);
		}

		World withSpawn(BlockPos newSpawn, float newYaw) {
			return new World(name, kind, map, newSpawn, newYaw, created, rules);
		}

		World withRules(Set<Rule> newRules) {
			return new World(name, kind, map, spawn, yaw, created, newRules);
		}

		public boolean allows(Rule rule) {
			return rules.contains(rule);
		}
	}

	public enum Result { CREATED, BAD_NAME, EXISTS, NO_MAP, BAD_MAP, FAILED }

	private final Map<String, World> worlds = new TreeMap<>();
	/** The open class worlds' settings: Fantasy reads the difficulty from them, so changing it is live. */
	private static final Map<ResourceKey<Level>, RuntimeLevelConfig> CONFIGS = new java.util.concurrent.ConcurrentHashMap<>();

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ClassWorlds state = get(server);
			for (World world : List.copyOf(state.worlds.values())) {
				try {
					open(server, world);
				} catch (RuntimeException e) {
					MineMod.LOGGER.warn("[MINE] Can't open the class world {}: {}", world.name(), e.toString());
				}
			}
			if (!state.worlds.isEmpty()) {
				MineMod.LOGGER.info("[MINE] Class worlds: {}", state.worlds.keySet());
			}
		});
	}

	public static ClassWorlds get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public static ResourceKey<Level> key(String name) {
		return ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "class_" + name));
	}

	public List<World> all() {
		return List.copyOf(worlds.values());
	}

	@Nullable
	public World world(String name) {
		return worlds.get(name);
	}

	/** The class world {@code level} is, or {@code null} if it's the main world or another one. */
	@Nullable
	public World of(ServerLevel level) {
		for (World world : worlds.values()) {
			if (key(world.name()).equals(level.dimension())) {
				return world;
			}
		}
		return null;
	}

	/** The world called {@code name}: a class world, or the main world for {@link #MAIN}; {@code null} if none. */
	@Nullable
	public static ServerLevel level(MinecraftServer server, String name) {
		if (MAIN.equals(name)) {
			return server.overworld();
		}
		return get(server).worlds.containsKey(name) ? server.getLevel(key(name)) : null;
	}

	/**
	 * Makes a class world: flat, or a copy of the map {@code map} (a folder in {@link #MAPS}).
	 * It's ready as soon as this returns.
	 */
	public static Result create(MinecraftServer server, String name, Kind kind, @Nullable String map) {
		ClassWorlds state = get(server);
		if (!NAME.matcher(name).matches() || MAIN.equals(name)) {
			return Result.BAD_NAME;
		}
		if (state.worlds.containsKey(name) || server.getLevel(key(name)) != null) {
			return Result.EXISTS;
		}
		BlockPos spawn = BlockPos.ZERO;
		float yaw = 0;
		if (kind == Kind.MAP) {
			Path source = map == null || !NAME.matcher(map).matches() ? null : maps(server).resolve(map);
			if (source == null || !Files.isDirectory(source)) {
				return Result.NO_MAP;
			}
			Path overworld = overworldOf(source);
			if (overworld == null) {
				return Result.BAD_MAP;
			}
			Path target = folder(server, name);
			try {
				Exports.deleteFolder(target);
				for (String part : MAP_PARTS) {
					if (Files.isDirectory(overworld.resolve(part))) {
						copyFolder(overworld.resolve(part), target.resolve(part));
					}
				}
			} catch (IOException | UncheckedIOException e) {
				MineMod.LOGGER.warn("[MINE] Can't copy the map {} into the class world {}: {}", map, name, e.toString());
				return Result.FAILED;
			}
			spawn = mapSpawn(source);
		}
		World world = new World(name, kind, kind == Kind.MAP ? map : "", spawn, yaw, System.currentTimeMillis(), NEW_WORLD);
		ServerLevel level;
		try {
			level = open(server, world);
		} catch (RuntimeException e) {
			MineMod.LOGGER.warn("[MINE] Can't make the class world {}: {}", name, e.toString());
			return Result.FAILED;
		}
		// Arrive on the ground: on top of the flat world, or of the map where its spawn is unknown.
		if (kind == Kind.FLAT || spawn.equals(BlockPos.ZERO)) {
			level.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
			int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
			spawn = new BlockPos(spawn.getX(), Math.max(top, level.getMinY() + 1), spawn.getZ());
		}
		state.worlds.put(name, world.withSpawn(spawn, yaw));
		state.setDirty();
		MineMod.LOGGER.info("[MINE] Class world {} made ({}{})", name, kind, kind == Kind.MAP ? " " + map : "");
		return Result.CREATED;
	}

	/**
	 * Deletes a class world and everything in it. Whoever is in it goes back to the main world;
	 * whoever had their spawn there gets the main world's.
	 */
	public static boolean remove(MinecraftServer server, String name) {
		ClassWorlds state = get(server);
		World world = state.worlds.get(name);
		if (world == null) {
			return false;
		}
		ResourceKey<Level> key = key(name);
		ServerLevel level = server.getLevel(key);
		if (level != null) {
			Lab.get(level).forget();
		}
		Zones.removeIn(server, key);
		for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
			if (player.level().dimension().equals(key)) {
				goTo(player, server.overworld(), null);
			}
			ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
			if (respawn != null && respawn.respawnData().dimension().equals(key)) {
				player.setRespawnPosition(null, false);
			}
		}
		Fantasy.get(server).getOrOpenPersistentLevel(key.identifier(), config(server, world)).delete();
		CONFIGS.remove(key);
		state.worlds.remove(name);
		state.setDirty();
		MineMod.LOGGER.info("[MINE] Class world {} deleted", name);
		return true;
	}

	/**
	 * Changes what is allowed in a class world, at once: monsters come or go, the sun starts
	 * moving or goes back to midday.
	 */
	public static void setRules(MinecraftServer server, String name, Set<Rule> rules) {
		ClassWorlds state = get(server);
		World world = state.worlds.get(name);
		ServerLevel level = server.getLevel(key(name));
		if (world == null || level == null) {
			return;
		}
		world = world.withRules(rules);
		state.worlds.put(name, world);
		state.setDirty();
		RuntimeLevelConfig config = CONFIGS.get(level.dimension());
		if (config != null) {
			config.setDifficulty(difficulty(world));
		}
		GameRules gameRules = level.getGameRules();
		gameRules.set(GameRules.ADVANCE_TIME, world.allows(Rule.NIGHT), null);
		gameRules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, fireSpread(world), null);
		gameRules.set(GameRules.TNT_EXPLODES, world.allows(Rule.EXPLOSIONS), null);
		gameRules.set(GameRules.MOB_GRIEFING, world.allows(Rule.EXPLOSIONS), null);
		gameRules.set(GameRules.PVP, world.allows(Rule.PVP), null);
		var clock = server.registryAccess().getOrThrow(WorldClocks.OVERWORLD);
		if (!world.allows(Rule.NIGHT)) {
			level.clockManager().setTotalTicks(clock, NOON);
		}
		level.clockManager().setPaused(clock, !world.allows(Rule.NIGHT));
		MineMod.LOGGER.info("[MINE] Class world {}: allowed {}", name, world.rules());
	}

	/** Students arrive here in the world they're in: where the teacher stands now. */
	public static boolean setSpawn(ServerPlayer teacher) {
		ClassWorlds state = get(teacher.level().getServer());
		World world = state.of(teacher.level());
		if (world == null) {
			return false;
		}
		state.worlds.put(world.name(), world.withSpawn(teacher.blockPosition(), teacher.getYRot()));
		state.setDirty();
		return true;
	}

	/**
	 * Takes the player to {@code level}: to its spawn, which also becomes theirs (they come back
	 * there if they fall or die). {@code world} is the class world, {@code null} for the main one.
	 */
	public static void goTo(ServerPlayer player, ServerLevel level, @Nullable World world) {
		BlockPos pos;
		float yaw;
		if (world == null) {
			LevelData.RespawnData main = level.getServer().getRespawnData();
			pos = main.pos();
			yaw = main.yaw();
			player.setRespawnPosition(null, false);
		} else {
			pos = world.spawn();
			yaw = world.yaw();
			player.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(level.dimension(), pos, yaw, 0), true), false);
		}
		player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.<Relative>of(), yaw, 0, true);
	}

	/** The last list of maps (usable ones, then all), and when it was made: commands suggest them at every key. */
	private static final Map<Boolean, List<String>> MAPS_SEEN = new ConcurrentHashMap<>();
	private static volatile long mapsSeenAt;
	private static final long MAPS_FRESH_MS = 5000;

	/** The maps a class world can be made from: the folders in {@link #MAPS} that hold a Java world. */
	public static List<String> maps(MinecraftServer server, boolean usableOnly) {
		long now = System.currentTimeMillis();
		if (now - mapsSeenAt > MAPS_FRESH_MS) {
			MAPS_SEEN.clear();
			mapsSeenAt = now;
		}
		return MAPS_SEEN.computeIfAbsent(usableOnly, usable -> findMaps(server, usable));
	}

	private static List<String> findMaps(MinecraftServer server, boolean usableOnly) {
		Path dir = maps(server);
		if (!Files.isDirectory(dir)) {
			return List.of();
		}
		try (Stream<Path> children = Files.list(dir)) {
			return children.filter(Files::isDirectory)
					.filter(child -> NAME.matcher(child.getFileName().toString()).matches())
					.filter(child -> !usableOnly || overworldOf(child) != null)
					.map(child -> child.getFileName().toString())
					.sorted()
					.toList();
		} catch (IOException e) {
			return List.of();
		}
	}

	public static Path maps(MinecraftServer server) {
		return server.getServerDirectory().resolve(MAPS);
	}

	private static ServerLevel open(MinecraftServer server, World world) {
		RuntimeLevelConfig config = config(server, world);
		ServerLevel level = Fantasy.get(server).getOrOpenPersistentLevel(key(world.name()).identifier(), config).asLevel();
		CONFIGS.put(level.dimension(), config);
		return level;
	}

	/** How a class world is made, with its rules. */
	private static RuntimeLevelConfig config(MinecraftServer server, World world) {
		return new RuntimeLevelConfig()
				.setDimensionType(BuiltinDimensionTypes.OVERWORLD)
				.setGenerator(world.kind() == Kind.FLAT ? flat(server) : new VoidChunkGenerator(server))
				.setFlat(world.kind() == Kind.FLAT)
				.setSeed(world.created())
				.setDifficulty(difficulty(world))
				.setGameRule(GameRules.ADVANCE_TIME, world.allows(Rule.NIGHT))
				.setGameRule(GameRules.ADVANCE_WEATHER, false)
				.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, fireSpread(world))
				.setGameRule(GameRules.TNT_EXPLODES, world.allows(Rule.EXPLOSIONS))
				.setGameRule(GameRules.MOB_GRIEFING, world.allows(Rule.EXPLOSIONS))
				.setGameRule(GameRules.PVP, world.allows(Rule.PVP))
				// Command blocks don't fill the teachers' chat with what they did.
				.setGameRule(GameRules.COMMAND_BLOCK_OUTPUT, false)
				.setClockTime(WorldClocks.OVERWORLD, NOON, !world.allows(Rule.NIGHT));
	}

	private static Difficulty difficulty(World world) {
		return world.allows(Rule.MONSTERS) ? Difficulty.NORMAL : Difficulty.PEACEFUL;
	}

	/** Minecraft's own distance, or none: fire doesn't spread. */
	private static int fireSpread(World world) {
		return world.allows(Rule.FIRE) ? 128 : 0;
	}

	/** Minecraft's classic flat world (grass on dirt), without villages: room for the lab. */
	private static ChunkGenerator flat(MinecraftServer server) {
		var registries = server.registryAccess();
		FlatLevelGeneratorSettings settings = FlatLevelGeneratorSettings.getDefault(registries.lookupOrThrow(Registries.BIOME),
				registries.lookupOrThrow(Registries.STRUCTURE_SET), registries.lookupOrThrow(Registries.PLACED_FEATURE));
		return new FlatLevelSource(settings.withBiomeAndLayers(settings.getLayersInfo(), Optional.of(HolderSet.empty()), settings.getBiome()));
	}

	private static Path folder(MinecraftServer server, String name) {
		return DimensionType.getStorageFolder(key(name), server.getWorldPath(LevelResource.ROOT));
	}

	/**
	 * The folder with a map's overworld: the map itself for worlds saved before Minecraft 26.1,
	 * {@code dimensions/minecraft/overworld} for newer ones. {@code null} if there are no blocks.
	 */
	@Nullable
	private static Path overworldOf(Path map) {
		for (Path candidate : List.of(map.resolve("dimensions").resolve("minecraft").resolve("overworld"), map)) {
			Path region = candidate.resolve("region");
			if (Files.isDirectory(region)) {
				try (Stream<Path> files = Files.list(region)) {
					if (files.anyMatch(file -> file.getFileName().toString().endsWith(".mca"))) {
						return candidate;
					}
				} catch (IOException e) {
					return null;
				}
			}
		}
		return null;
	}

	/** Where the map's players appear, from its level.dat (old and new format); the centre if unknown. */
	private static BlockPos mapSpawn(Path map) {
		Path levelDat = map.resolve("level.dat");
		if (!Files.isRegularFile(levelDat)) {
			return BlockPos.ZERO;
		}
		try {
			CompoundTag data = NbtIo.readCompressed(levelDat, NbtAccounter.unlimitedHeap()).getCompoundOrEmpty("Data");
			Optional<int[]> pos = data.getCompoundOrEmpty("spawn").getIntArray("pos");
			if (pos.isPresent() && pos.get().length == 3) {
				return new BlockPos(pos.get()[0], pos.get()[1], pos.get()[2]);
			}
			if (data.contains("SpawnX")) {
				return new BlockPos(data.getIntOr("SpawnX", 0), data.getIntOr("SpawnY", 0), data.getIntOr("SpawnZ", 0));
			}
		} catch (IOException | RuntimeException e) {
			MineMod.LOGGER.warn("[MINE] Can't read the spawn of the map {}: {}", map.getFileName(), e.toString());
		}
		return BlockPos.ZERO;
	}

	private static void copyFolder(Path from, Path to) throws IOException {
		try (Stream<Path> files = Files.walk(from)) {
			for (Path file : (Iterable<Path>) files::iterator) {
				Path target = to.resolve(from.relativize(file).toString());
				if (Files.isDirectory(file)) {
					Files.createDirectories(target);
				} else {
					Files.copy(file, target);
				}
			}
		}
	}


	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		ListTag list = new ListTag();
		for (World world : worlds.values()) {
			CompoundTag tag = new CompoundTag();
			tag.putString("Name", world.name());
			tag.putString("Kind", world.kind().name());
			tag.putString("Map", world.map());
			tag.putIntArray("Spawn", new int[] { world.spawn().getX(), world.spawn().getY(), world.spawn().getZ() });
			tag.putFloat("Yaw", world.yaw());
			tag.putLong("Created", world.created());
			ListTag rules = new ListTag();
			world.rules().forEach(rule -> rules.add(net.minecraft.nbt.StringTag.valueOf(rule.name())));
			tag.put("Rules", rules);
			list.add(tag);
		}
		nbt.put("Worlds", list);
		return nbt;
	}

	public static ClassWorlds fromNbt(CompoundTag nbt) {
		ClassWorlds state = new ClassWorlds();
		ListTag list = nbt.getListOrEmpty("Worlds");
		for (int i = 0; i < list.size(); i++) {
			CompoundTag tag = list.getCompoundOrEmpty(i);
			String name = tag.getStringOr("Name", "");
			Kind kind;
			try {
				kind = Kind.valueOf(tag.getStringOr("Kind", Kind.FLAT.name()));
			} catch (IllegalArgumentException e) {
				kind = Kind.FLAT;
			}
			int[] spawn = tag.getIntArray("Spawn").filter(pos -> pos.length == 3).orElse(new int[3]);
			Set<Rule> rules = EnumSet.noneOf(Rule.class);
			if (tag.contains("Rules")) {
				ListTag names = tag.getListOrEmpty("Rules");
				for (int j = 0; j < names.size(); j++) {
					String rule = names.getStringOr(j, "");
					java.util.Arrays.stream(Rule.values()).filter(r -> r.name().equals(rule)).forEach(rules::add);
				}
			} else {
				rules.addAll(NEW_WORLD);
			}
			if (NAME.matcher(name).matches()) {
				state.worlds.put(name, new World(name, kind, tag.getStringOr("Map", ""), new BlockPos(spawn[0], spawn[1], spawn[2]),
						tag.getFloatOr("Yaw", 0), tag.getLongOr("Created", 0), rules));
			}
		}
		return state;
	}
}
