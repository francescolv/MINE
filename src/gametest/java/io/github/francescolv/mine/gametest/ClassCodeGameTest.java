package io.github.francescolv.mine.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.francescolv.mine.classroom.ClassCode;
import io.github.francescolv.mine.classroom.ClassDay;
import io.github.francescolv.mine.classroom.JoinGate;
import io.github.francescolv.mine.classroom.VersionCheck;
import io.github.francescolv.mine.world.ClassWorlds;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.world.level.gamerules.GameRules;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * The class code: four symbols, a limit to wrong tries, and the login check that lets students
 * who aren't on the whitelist in to type it, or turns them away and puts them on the waiting list.
 */
public class ClassCodeGameTest {

	@GameTest
	public void theClassCode(GameTestHelper ctx) {
		ClassCode.close();
		ctx.assertTrue(ClassCode.current() == null, "No code at first");
		List<Integer> code = ClassCode.open();
		ctx.assertTrue(code.size() == ClassCode.LENGTH && new HashSet<>(code).size() == ClassCode.LENGTH,
				"Four different symbols: " + code);
		ctx.assertTrue(code.stream().allMatch(symbol -> symbol >= 0 && symbol < ClassCode.SYMBOLS.size()), "Known symbols");
		ctx.assertTrue(code.equals(ClassCode.current()), "The open code");

		UUID alex = UUID.randomUUID();
		List<Integer> wrong = new ArrayList<>(code.reversed());
		ctx.assertTrue(ClassCode.check(alex, wrong).result() == ClassCode.Result.WRONG, "A wrong code");
		ctx.assertTrue(ClassCode.triesLeft(alex) == ClassCode.MAX_TRIES - 1, "One try less");
		ctx.assertTrue(ClassCode.check(alex, code).result() == ClassCode.Result.RIGHT, "The right code");

		UUID guesser = UUID.randomUUID();
		ClassCode.Result last = null;
		for (int i = 0; i < ClassCode.MAX_TRIES; i++) {
			last = ClassCode.check(guesser, wrong).result();
		}
		ctx.assertTrue(last == ClassCode.Result.TOO_MANY, "Too many wrong codes");
		ctx.assertTrue(ClassCode.check(guesser, code).result() == ClassCode.Result.TOO_MANY, "No more tries, also with the right code");

		ClassCode.close();
		ctx.assertTrue(ClassCode.check(alex, code).result() == ClassCode.Result.CLOSED, "A closed code lets nobody in");
		ctx.succeed();
	}

	/**
	 * Each world has its own code, different from the others open: the code a student types
	 * takes them to its world. Closing one code leaves the others open.
	 */
	@GameTest(maxTicks = 100)
	public void aCodeForEachWorld(GameTestHelper ctx) {
		MinecraftServer server = ctx.getLevel().getServer();
		String world = "code" + Long.toString(System.nanoTime() % 1_000_000_000L, 36);
		ClassWorlds.create(server, world, ClassWorlds.Kind.FLAT, null);
		ClassCode.close();
		List<Integer> main = ClassCode.open();
		List<Integer> theirs = ClassCode.open(world);
		ctx.assertFalse(main.equals(theirs), "Two different codes");
		ctx.assertTrue(ClassCode.openWorlds().equals(java.util.stream.Stream.of(ClassWorlds.MAIN, world).sorted().toList()),
				"Both open: " + ClassCode.openWorlds());
		UUID student = UUID.randomUUID();
		ClassCode.Check check = ClassCode.check(student, theirs);
		ctx.assertTrue(check.result() == ClassCode.Result.RIGHT && world.equals(check.world()), "Their world's code: " + check);
		ctx.assertTrue(ClassWorlds.MAIN.equals(ClassCode.check(student, main).world()), "The main world's code");
		ctx.assertTrue(ClassCode.close(world) && ClassCode.current(world) == null && ClassCode.current() != null,
				"Closing one leaves the other");

		// Let in with their world's code: they arrive in it.
		ServerPlayer player = ctx.makeMockServerPlayerInLevel();
		GameProfile profile = new GameProfile(player.getUUID(), player.getName().getString());
		ClassCode.admit(server, profile, world);
		JoinGate.arrived(server, player);
		ctx.succeedWhen(() -> {
			ctx.assertTrue(player.level() == ClassWorlds.level(server, world), "In their class world");
			server.getPlayerList().getWhiteList().remove(new UserWhiteListEntry(new NameAndId(profile)));
			ClassCode.close();
			ClassWorlds.remove(server, world);
		});
	}

	@GameTest
	public void sameMineOnServerAndComputers(GameTestHelper ctx) {
		ctx.assertTrue(VersionCheck.problem("0.2.0", "0.2.0") == null, "The same version joins");
		ctx.assertTrue(key(VersionCheck.problem("0.2.0", "0.1.0")).equals("mine.version.different"), "A different version is told what to import");
		ctx.assertTrue(key(VersionCheck.problem("0.2.0", null)).equals("mine.version.old"), "A MINE too old to answer");
		ctx.succeed();
	}

	private static String key(Component text) {
		return text != null && text.getContents() instanceof TranslatableContents content ? content.getKey() : "";
	}

	@GameTest
	public void alwaysMidday(GameTestHelper ctx) {
		MinecraftServer server = ctx.getLevel().getServer();
		GameRules rules = server.getGameRules();
		rules.set(GameRules.ADVANCE_TIME, true, server);
		ClassDay.forget(server);
		ctx.assertTrue(ClassDay.apply(server), "Done the first time");
		ctx.assertFalse(rules.get(GameRules.ADVANCE_TIME), "No night");
		ctx.assertFalse(rules.get(GameRules.ADVANCE_WEATHER), "No rain");
		ctx.assertFalse(server.overworld().isRaining(), "A clear sky");

		// Once per world: then the teacher decides.
		rules.set(GameRules.ADVANCE_TIME, true, server);
		ctx.assertFalse(ClassDay.apply(server), "Not done again");
		ctx.assertTrue(rules.get(GameRules.ADVANCE_TIME), "The teacher's choice stays");
		rules.set(GameRules.ADVANCE_TIME, false, server);
		ctx.succeed();
	}

	@GameTest
	public void joiningTheClass(GameTestHelper ctx) {
		MinecraftServer server = ctx.getLevel().getServer();
		PlayerList players = server.getPlayerList();
		boolean whitelist = players.isUsingWhitelist();
		server.setUsingWhitelist(true);
		GameProfile visitor = new GameProfile(UUID.randomUUID(), "Visitor" + (System.nanoTime() % 1000));
		InetSocketAddress address = new InetSocketAddress("127.0.0.1", 50000);
		try {
			// No code open: turned away with a message that says what to do, and waiting for the teacher.
			ClassCode.close();
			Component refused = players.canPlayerLogin(address, new NameAndId(visitor));
			ctx.assertTrue(refused != null && refused.getContents() instanceof TranslatableContents text
					&& text.getKey().equals("mine.class.not_in_class"), "Not in the class: " + refused);
			ctx.assertTrue(ClassCode.waiting(visitor.name()) != null, "On the waiting list");

			// Code open: through the whitelist check, to type it.
			ClassCode.open();
			ctx.assertTrue(JoinGate.passWhitelist(players, new NameAndId(visitor)), "Let through to type the code");
			Component open = players.canPlayerLogin(address, new NameAndId(visitor));
			ctx.assertTrue(open == null || !(open.getContents() instanceof TranslatableContents text
					&& text.getKey().equals("mine.class.not_in_class")), "Not turned away while the code is open: " + open);
			ClassCode.close();

			// The teacher lets them in by name.
			CommandSourceStack console = server.createCommandSourceStack();
			execute(ctx, console, "mine class waiting", true);
			execute(ctx, console, "mine class admit Nobody", false);
			execute(ctx, console, "mine class admit " + visitor.name().toLowerCase(), true);
			ctx.assertTrue(players.getWhiteList().isWhiteListed(new NameAndId(visitor)), "On the whitelist");
			ctx.assertTrue(ClassCode.waiting(visitor.name()) == null, "No longer waiting");
			ctx.assertTrue(players.canPlayerLogin(address, new NameAndId(visitor)) == null || players.isWhiteListed(new NameAndId(visitor)), "In the class");

			execute(ctx, console, "mine class code", true);
			ctx.assertTrue(ClassCode.current() != null, "/mine class code opens one");
			List<Integer> code = ClassCode.current();
			execute(ctx, console, "mine class code", true);
			ctx.assertTrue(code.equals(ClassCode.current()), "Asked again: the same code");
			execute(ctx, console, "mine class close", true);
			execute(ctx, console, "mine class close", false);
		} finally {
			players.getWhiteList().remove(new UserWhiteListEntry(new NameAndId(visitor)));
			server.setUsingWhitelist(whitelist);
			ClassCode.close();
		}
		ctx.succeed();
	}

	private static void execute(GameTestHelper ctx, CommandSourceStack source, String command, boolean succeeds) {
		int result;
		try {
			result = ctx.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
		} catch (CommandSyntaxException e) {
			io.github.francescolv.mine.MineMod.LOGGER.info("[MINE test] /{} failed: {}", command, e.getMessage());
			result = 0;
		}
		ctx.assertTrue((result > 0) == succeeds, "/" + command + (succeeds ? " should succeed" : " should fail"));
	}
}
