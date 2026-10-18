package io.github.francescolv.mine.classroom;

import com.mojang.authlib.GameProfile;
import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.permission.Permissions;
import io.github.francescolv.mine.world.ClassWorlds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The class code, like Minecraft Education's join code: four symbols the teacher shows and the
 * students click to join the class. Only students who aren't on the whitelist need it: with the
 * right code they are added to it, so next time they come in straight away.
 *
 * <p>Each world has its own code, as each class has its world: the code a student types decides
 * where they arrive (a class world, or the main world, {@link ClassWorlds#MAIN}). Several can be
 * open at once, all different. A code is open for a lesson ({@link #OPEN_FOR}) or until the
 * teacher closes it, and a server restart closes it. Each student has {@link #MAX_TRIES} wrong
 * answers, counted again when a new code opens.
 *
 * <p>Students who try to join without being in the class wait in a list ({@link #waiting()}),
 * where the teacher can let them in by name: they are told about each one.
 */
public final class ClassCode {

	/** Things every child can name and tell apart. Their order is the protocol: only add at the end. */
	public static final List<Item> SYMBOLS = List.of(
			Items.APPLE, Items.CAKE, Items.CARROT, Items.COOKIE,
			Items.DIAMOND, Items.EMERALD, Items.FEATHER, Items.BONE,
			Items.EGG, Items.PUMPKIN, Items.BOOK, Items.GOLD_INGOT);
	public static final int LENGTH = 4;
	public static final int MAX_TRIES = 10;
	public static final Duration OPEN_FOR = Duration.ofHours(2);
	/** One notice per student and minute, also if they keep trying. */
	private static final Duration NOTICE_EVERY = Duration.ofMinutes(1);

	private static final Random RANDOM = new SecureRandom();

	public enum Result { RIGHT, WRONG, TOO_MANY, CLOSED }

	/** What a student's answer was, and for {@link Result#RIGHT}, the world whose code it is. */
	public record Check(Result result, @Nullable String world) {
	}

	private record Open(List<Integer> code, Instant closesAt) {
	}

	public record Waiting(GameProfile profile, Instant since) {
	}

	/** World name → its open code. */
	private static final Map<String, Open> CODES = new ConcurrentHashMap<>();
	/** Wrong answers per student, since a code last opened. */
	private static final Map<UUID, Integer> WRONG = new ConcurrentHashMap<>();
	/** By name, lower case: who tried to join and isn't in the class yet. */
	private static final Map<String, Waiting> WAITING = new ConcurrentHashMap<>();
	private static final Map<UUID, Instant> NOTIFIED = new ConcurrentHashMap<>();
	/** Let in since the server started, and the world they go to when they arrive. */
	private static final Map<UUID, String> ADMITTED = new ConcurrentHashMap<>();

	private ClassCode() {
	}

	/**
	 * A symbol's name, in the reader's language. Without making an item: there are none yet while
	 * a student joins.
	 */
	public static Component name(int symbol) {
		return Component.translatable(SYMBOLS.get(symbol).getDescriptionId());
	}

	/** Opens a new code for the main world: {@link #open(String)}. */
	public static List<Integer> open() {
		return open(ClassWorlds.MAIN);
	}

	/** Opens a new code for {@code world}, replacing its old one; different from the other open codes. */
	public static synchronized List<Integer> open(String world) {
		List<Integer> code;
		do {
			List<Integer> symbols = new ArrayList<>();
			for (int i = 0; i < SYMBOLS.size(); i++) {
				symbols.add(i);
			}
			Collections.shuffle(symbols, RANDOM);
			code = List.copyOf(symbols.subList(0, LENGTH));
		} while (worldOf(code) != null && !world.equals(worldOf(code)));
		CODES.put(world, new Open(code, Instant.now().plus(OPEN_FOR)));
		WRONG.clear();
		return code;
	}

	/** Closes every code. */
	public static synchronized void close() {
		CODES.clear();
		WRONG.clear();
	}

	/** Closes the code of {@code world}; {@code false} if it had none. */
	public static synchronized boolean close(String world) {
		return CODES.remove(world) != null;
	}

	/** The main world's open code: {@link #current(String)}. */
	@Nullable
	public static List<Integer> current() {
		return current(ClassWorlds.MAIN);
	}

	/** The open code of {@code world}, or {@code null}: none, closed, or its time is over. */
	@Nullable
	public static synchronized List<Integer> current(String world) {
		expire();
		Open open = CODES.get(world);
		return open == null ? null : open.code();
	}

	/** The worlds with an open code, by name. */
	public static synchronized List<String> openWorlds() {
		expire();
		return CODES.keySet().stream().sorted().toList();
	}

	public static synchronized boolean anyOpen() {
		expire();
		return !CODES.isEmpty();
	}

	public static Instant closesAt(String world) {
		Open open = CODES.get(world);
		return open == null ? Instant.EPOCH : open.closesAt();
	}

	private static void expire() {
		Instant now = Instant.now();
		CODES.entrySet().removeIf(entry -> {
			if (now.isAfter(entry.getValue().closesAt())) {
				MineMod.LOGGER.info("[MINE] The class code of {} closed by itself after {} hours", entry.getKey(), OPEN_FOR.toHours());
				return true;
			}
			return false;
		});
	}

	@Nullable
	private static String worldOf(List<Integer> code) {
		for (Map.Entry<String, Open> entry : CODES.entrySet()) {
			if (entry.getValue().code().equals(code)) {
				return entry.getKey();
			}
		}
		return null;
	}

	/** Checks a student's answer against every open code. */
	public static synchronized Check check(UUID student, List<Integer> answer) {
		if (!anyOpen()) {
			return new Check(Result.CLOSED, null);
		}
		if (WRONG.getOrDefault(student, 0) >= MAX_TRIES) {
			return new Check(Result.TOO_MANY, null);
		}
		String world = worldOf(answer);
		if (world != null) {
			return new Check(Result.RIGHT, world);
		}
		return new Check(WRONG.merge(student, 1, Integer::sum) >= MAX_TRIES ? Result.TOO_MANY : Result.WRONG, null);
	}

	public static int triesLeft(UUID student) {
		return Math.max(0, MAX_TRIES - WRONG.getOrDefault(student, 0));
	}

	/** Puts the student on the whitelist: in the class from now on. They arrive in {@code world}. */
	public static void admit(MinecraftServer server, GameProfile profile, String world) {
		server.getPlayerList().getWhiteList().add(new UserWhiteListEntry(new NameAndId(profile)));
		WAITING.remove(profile.name().toLowerCase());
		NOTIFIED.remove(profile.id());
		ADMITTED.put(profile.id(), world);
		MineMod.LOGGER.info("[MINE] {} is in the class (whitelist), world {}", profile.name(), world);
	}

	/**
	 * The world a student let in since the server started goes to, once, when they arrive;
	 * {@code null} for everybody else.
	 */
	@Nullable
	static String arrived(UUID student) {
		return ADMITTED.remove(student);
	}

	/** A student tried to join without being in the class: they wait, and teachers are told. */
	public static void tried(MinecraftServer server, GameProfile profile) {
		WAITING.putIfAbsent(profile.name().toLowerCase(), new Waiting(profile, Instant.now()));
		Instant now = Instant.now();
		Instant last = NOTIFIED.get(profile.id());
		if (last != null && now.isBefore(last.plus(NOTICE_EVERY))) {
			return;
		}
		NOTIFIED.put(profile.id(), now);
		String name = profile.name();
		MineMod.LOGGER.info("[MINE] {} is trying to join and isn't in the class: /mine class admit {}", name, name);
		tellTeachers(server, ClassTexts.trying(name, anyOpen()));
	}

	public static List<Waiting> waiting() {
		return WAITING.values().stream().sorted(Comparator.comparing(Waiting::since)).toList();
	}

	@Nullable
	public static Waiting waiting(String name) {
		return WAITING.get(name.toLowerCase());
	}

	/** The code in words, e.g. "Apple · Cake · Diamond · Bone": to say it aloud, or for the console. */
	public static MutableComponent describe(List<Integer> symbols) {
		MutableComponent text = Component.empty();
		for (int i = 0; i < symbols.size(); i++) {
			if (i > 0) {
				text.append(" · ");
			}
			text.append(name(symbols.get(i)));
		}
		return text;
	}

	static void tellTeachers(MinecraftServer server, Component message) {
		server.getPlayerList().getPlayers().stream()
				.filter(Permissions::isTeacher)
				.forEach(teacher -> teacher.sendSystemMessage(message));
	}
}
