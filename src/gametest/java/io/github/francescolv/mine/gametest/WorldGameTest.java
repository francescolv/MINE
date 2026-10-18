package io.github.francescolv.mine.gametest;

import io.github.francescolv.mine.lab.Lab;
import io.github.francescolv.mine.world.ClassWorlds;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** The class worlds: made while the server runs, flat or from a map, visited and deleted. */
public class WorldGameTest {

	/**
	 * A flat class world is ready at once, for a class (midday, no rain, no monsters); the
	 * teacher takes a student there, and deleting it brings them back to the main world.
	 */
	@GameTest(maxTicks = 200)
	public void aFlatClassWorld(GameTestHelper ctx) {
		MinecraftServer server = ctx.getLevel().getServer();
		String name = unique("flat");
		ctx.assertTrue(ClassWorlds.create(server, name, ClassWorlds.Kind.FLAT, null) == ClassWorlds.Result.CREATED, "Created");
		ctx.assertTrue(ClassWorlds.create(server, name, ClassWorlds.Kind.FLAT, null) == ClassWorlds.Result.EXISTS, "Only once");
		ctx.assertTrue(ClassWorlds.create(server, "Bad Name", ClassWorlds.Kind.FLAT, null) == ClassWorlds.Result.BAD_NAME, "Bad name");
		ctx.assertTrue(ClassWorlds.create(server, ClassWorlds.MAIN, ClassWorlds.Kind.FLAT, null) == ClassWorlds.Result.BAD_NAME,
				"\"main\" is the main world");
		ServerLevel level = ClassWorlds.level(server, name);
		ctx.assertTrue(level != null, "The world is open");
		ClassWorlds.World world = ClassWorlds.get(server).world(name);
		ctx.assertTrue(level.getBlockState(world.spawn().below()).is(Blocks.GRASS_BLOCK),
				"Students arrive on the grass: " + level.getBlockState(world.spawn().below()) + " under " + world.spawn());
		ctx.assertFalse(level.getGameRules().get(GameRules.ADVANCE_TIME), "Always midday");
		ctx.assertFalse(level.getGameRules().get(GameRules.ADVANCE_WEATHER), "No rain");
		ctx.assertTrue(level.getLevelData().getDifficulty() == Difficulty.PEACEFUL, "No monsters");
		ctx.assertTrue(ctx.getLevel().getGameRules().get(GameRules.ADVANCE_WEATHER) != null, "The main world keeps its own rules");

		ServerPlayer student = ctx.makeMockServerPlayerInLevel();
		ClassWorlds.goTo(student, level, world);
		ctx.assertTrue(student.level() == level, "The student is in the class world");
		ctx.assertTrue(student.getRespawnConfig() != null && student.getRespawnConfig().respawnData().dimension().equals(level.dimension()),
				"And comes back there");
		ctx.assertTrue(ClassWorlds.remove(server, name), "Deleted");
		ctx.assertTrue(student.level() == server.overworld(), "Back in the main world");
		ctx.assertTrue(student.getRespawnConfig() == null, "With the main world's spawn");
		ctx.succeedWhen(() -> ctx.assertTrue(server.getLevel(ClassWorlds.key(name)) == null, "The world is gone"));
	}

	/**
	 * The teacher changes a class world's rules during the lesson, at once and only there: free
	 * time brings monsters, night, fire, explosions and fighting; the lesson takes them away.
	 * The rules are kept with the world.
	 */
	@GameTest(maxTicks = 200)
	public void classRulesChangeDuringTheLesson(GameTestHelper ctx) {
		MinecraftServer server = ctx.getLevel().getServer();
		String name = unique("rules");
		ClassWorlds.create(server, name, ClassWorlds.Kind.FLAT, null);
		ServerLevel level = ClassWorlds.level(server, name);
		GameRules rules = level.getGameRules();
		boolean mainPvp = server.overworld().getGameRules().get(GameRules.PVP);
		ctx.assertTrue(level.getLevelData().getDifficulty() == Difficulty.PEACEFUL && !rules.get(GameRules.ADVANCE_TIME)
				&& rules.get(GameRules.PVP) && rules.get(GameRules.TNT_EXPLODES), "A new world: no monsters, always day, the rest allowed");

		ClassWorlds.setRules(server, name, ClassWorlds.FREE);
		ctx.assertTrue(level.getLevelData().getDifficulty() == Difficulty.NORMAL, "Free time: monsters");
		ctx.assertTrue(rules.get(GameRules.ADVANCE_TIME), "Free time: night comes");
		ctx.assertTrue(rules.get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER) > 0, "Free time: fire spreads");

		ClassWorlds.setRules(server, name, ClassWorlds.LESSON);
		ctx.assertTrue(level.getLevelData().getDifficulty() == Difficulty.PEACEFUL, "Lesson: no monsters");
		ctx.assertFalse(rules.get(GameRules.ADVANCE_TIME), "Lesson: always day");
		ctx.assertTrue(rules.get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER) == 0, "Lesson: no fire spreading");
		ctx.assertFalse(rules.get(GameRules.TNT_EXPLODES) || rules.get(GameRules.MOB_GRIEFING), "Lesson: no explosions");
		ctx.assertFalse(rules.get(GameRules.PVP), "Lesson: no fighting");
		ctx.assertTrue(server.overworld().getGameRules().get(GameRules.PVP) == mainPvp, "The main world keeps its rules");
		ctx.assertTrue(ClassWorlds.fromNbt(ClassWorlds.get(server).save()).world(name).rules().equals(ClassWorlds.LESSON),
				"The rules are saved with the world");

		ClassWorlds.remove(server, name);
		ctx.succeed();
	}

	/**
	 * Each class world has its own lab, apart from the main world's: built, with lanes for the
	 * students in it, and gone with the world.
	 */
	@GameTest(maxTicks = 200)
	public void aLabInEachClassWorld(GameTestHelper ctx) {
		MinecraftServer server = ctx.getLevel().getServer();
		String a = unique("laba");
		String b = unique("labb");
		ClassWorlds.create(server, a, ClassWorlds.Kind.FLAT, null);
		ClassWorlds.create(server, b, ClassWorlds.Kind.FLAT, null);
		ServerLevel worldA = ClassWorlds.level(server, a);
		ServerLevel worldB = ClassWorlds.level(server, b);
		Lab main = Lab.get(server);
		ctx.assertTrue(Lab.get(worldA) != Lab.get(worldB) && Lab.get(worldA) != main, "Three different labs");
		ctx.assertFalse(Lab.get(worldA).exists(), "A new world has no lab");

		ServerPlayer teacher = ctx.makeMockServerPlayerInLevel();
		ClassWorlds.goTo(teacher, worldA, ClassWorlds.get(server).world(a));
		Lab.get(worldA).build(teacher, 2);
		ctx.assertTrue(Lab.get(worldA).exists() && Lab.get(worldA).world(server) == worldA, "The lab of " + a);
		ctx.assertFalse(Lab.get(worldB).exists(), "Not in " + b);

		ServerPlayer student = ctx.makeMockServerPlayerInLevel();
		ClassWorlds.goTo(student, worldA, ClassWorlds.get(server).world(a));
		ctx.assertTrue(Lab.students(worldA).contains(student) && !Lab.students(worldB).contains(student), "The students in the world");
		ctx.assertTrue(Lab.get(worldA).assign(List.of(student)).isEmpty() && student.level() == worldA, "A lane in their world");

		ClassWorlds.remove(server, a);
		ctx.succeedWhen(() -> {
			ctx.assertTrue(server.getLevel(ClassWorlds.key(a)) == null, "Deleted");
			ClassWorlds.create(server, a, ClassWorlds.Kind.FLAT, null);
			ctx.assertFalse(Lab.get(ClassWorlds.level(server, a)).exists(), "A new world with the same name has no lab");
			ClassWorlds.remove(server, a);
			ClassWorlds.remove(server, b);
		});
	}

	/**
	 * A command block in a class world (no player, a teacher's permissions): its commands are
	 * about the world it is in, as a teacher's would be.
	 */
	@GameTest(maxTicks = 100)
	public void commandBlocksInAClassWorld(GameTestHelper ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		MinecraftServer server = ctx.getLevel().getServer();
		String name = unique("cmd");
		ClassWorlds.create(server, name, ClassWorlds.Kind.FLAT, null);
		ServerLevel level = ClassWorlds.level(server, name);
		var commandBlock = server.createCommandSourceStack().withLevel(level)
				.withPosition(net.minecraft.world.phys.Vec3.atCenterOf(ClassWorlds.get(server).world(name).spawn()))
				.withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.GAMEMASTER);
		var commands = server.getCommands().getDispatcher();
		ctx.assertTrue(commands.execute("mine world rules free", commandBlock) > 0, "Free time from a command block");
		ctx.assertTrue(ClassWorlds.get(server).world(name).rules().equals(ClassWorlds.FREE), "In the command block's world");
		ctx.assertTrue(commands.execute("mine world rules lesson", commandBlock) > 0, "Back to the lesson");
		ctx.assertTrue(commands.execute("mine class code", commandBlock) > 0 && io.github.francescolv.mine.classroom.ClassCode.current(name) != null,
				"The code of the command block's world");
		io.github.francescolv.mine.classroom.ClassCode.close(name);
		ClassWorlds.remove(server, name);
		ctx.succeed();
	}

	/**
	 * A class world made from a map is a copy of it: what was built in the map is there, and
	 * changing the class world doesn't change the map.
	 */
	@GameTest(maxTicks = 400)
	public void aClassWorldFromAMap(GameTestHelper ctx) {
		MinecraftServer server = ctx.getLevel().getServer();
		// A map made here: a flat world with a gold block, saved and copied to the maps.
		String source = unique("src");
		String map = unique("map");
		ctx.assertTrue(ClassWorlds.create(server, source, ClassWorlds.Kind.FLAT, null) == ClassWorlds.Result.CREATED, "Source world");
		ServerLevel original = ClassWorlds.level(server, source);
		BlockPos gold = ClassWorlds.get(server).world(source).spawn().east(2);
		original.setBlockAndUpdate(gold, Blocks.GOLD_BLOCK.defaultBlockState());
		original.save(null, true, false);
		Path mapFolder = ClassWorlds.maps(server).resolve(map);
		copy(DimensionType.getStorageFolder(original.dimension(), server.getWorldPath(LevelResource.ROOT)), mapFolder);
		ctx.assertTrue(ClassWorlds.maps(server, true).contains(map), "The map is listed: " + ClassWorlds.maps(server, false));
		ctx.assertTrue(ClassWorlds.create(server, unique("x"), ClassWorlds.Kind.MAP, "nothing_here") == ClassWorlds.Result.NO_MAP,
				"No such map");

		String name = unique("copy");
		ctx.assertTrue(ClassWorlds.create(server, name, ClassWorlds.Kind.MAP, map) == ClassWorlds.Result.CREATED, "Made from the map");
		ServerLevel copy = ClassWorlds.level(server, name);
		ctx.assertTrue(copy.getBlockState(gold).is(Blocks.GOLD_BLOCK), "The map's gold block is there: " + copy.getBlockState(gold));
		ctx.assertTrue(copy.getBlockState(gold.below()).is(Blocks.GRASS_BLOCK), "And the map's ground");
		ctx.assertTrue(copy.getBlockState(gold.offset(2000, 0, 0)).isAir(), "Outside the map there's nothing");
		copy.setBlockAndUpdate(gold, Blocks.AIR.defaultBlockState());
		ctx.assertTrue(original.getBlockState(gold).is(Blocks.GOLD_BLOCK), "The map doesn't change");

		ClassWorlds.remove(server, source);
		ClassWorlds.remove(server, name);
		delete(mapFolder);
		ctx.succeed();
	}

	private static String unique(String prefix) {
		return prefix + Long.toString(System.nanoTime() % 1_000_000_000L, 36);
	}

	private static void copy(Path from, Path to) {
		try (Stream<Path> files = Files.walk(from)) {
			for (Path file : (Iterable<Path>) files::iterator) {
				Path target = to.resolve(from.relativize(file).toString());
				if (Files.isDirectory(file)) {
					Files.createDirectories(target);
				} else if (!file.getFileName().toString().endsWith(".lock")) {
					Files.copy(file, target);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void delete(Path folder) {
		try (Stream<Path> files = Files.walk(folder)) {
			files.sorted(Comparator.reverseOrder()).forEach(file -> file.toFile().delete());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
