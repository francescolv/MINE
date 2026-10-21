package io.github.francescolv.mine.classroom;

import com.mojang.authlib.GameProfile;
import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.lab.Lab;
import io.github.francescolv.mine.world.ClassWorlds;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * How a student who isn't on the whitelist gets into the class. Minecraft would turn them away;
 * MINE instead:
 * <ol>
 * <li>while the class code is open, lets them through the whitelist check
 * ({@link #passWhitelist}), and then, before they reach the world, asks their MINE for the code:
 * a window with the symbols. The right code puts them on the whitelist and into the world whose
 * code it is (their class world, or the main one), on a lab lane if that world has the lab;</li>
 * <li>otherwise turns them away with a message that says what to do, and tells the teachers, who
 * can let them in with a click.</li>
 * </ol>
 */
public final class JoinGate {

	/** Let through the whitelist check: they must type the code before they reach the world. */
	private static final Set<UUID> NEED_CODE = ConcurrentHashMap.newKeySet();
	/** Typing the code now: their connection waits for it. */
	private static final Set<UUID> TYPING = ConcurrentHashMap.newKeySet();
	private static final String NOT_WHITELISTED = "multiplayer.disconnect.not_whitelisted";

	private JoinGate() {
	}

	public static void register() {
		ClassCodePayloads.register();
		ServerConfigurationConnectionEvents.CONFIGURE.register(JoinGate::configure);
		ServerConfigurationConnectionEvents.DISCONNECT.register((handler, server) -> TYPING.remove(profile(handler).id()));
		ServerConfigurationNetworking.registerGlobalReceiver(ClassCodePayloads.Answer.ID, (payload, context) ->
				context.server().execute(() -> answer(context.server(), context.packetListener(), payload.symbols())));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> arrived(server, handler.getPlayer()));
	}

	/**
	 * Called by Minecraft's login check instead of {@link PlayerList#isWhiteListed}: lets
	 * students who aren't on the whitelist through while the class code is open.
	 */
	public static boolean passWhitelist(PlayerList players, NameAndId player) {
		if (players.isWhiteListed(player)) {
			return true;
		}
		if (ClassCode.anyOpen()) {
			NEED_CODE.add(player.id());
			return true;
		}
		return false;
	}

	/** Called with what the login check answered: Minecraft's "not white-listed" says what to do instead. */
	@Nullable
	public static Component refused(MinecraftServer server, NameAndId player, @Nullable Component reason) {
		if (reason != null && reason.getContents() instanceof TranslatableContents text && NOT_WHITELISTED.equals(text.getKey())) {
			ClassCode.tried(server, new GameProfile(player.id(), player.name()));
			return ClassTexts.notInClass();
		}
		return reason;
	}

	private static void configure(ServerConfigurationPacketListenerImpl handler, MinecraftServer server) {
		GameProfile profile = profile(handler);
		if (!NEED_CODE.remove(profile.id()) || server.getPlayerList().isWhiteListed(new NameAndId(profile))) {
			return;
		}
		ClassCode.tried(server, profile);
		if (ClassCode.triesLeft(profile.id()) == 0) {
			handler.disconnect(ClassTexts.tooMany());
		} else {
			// VersionCheck already turned away games without MINE: this one can type the code.
			TYPING.add(profile.id());
			handler.addTask(new TypeCode(ClassCode.triesLeft(profile.id())));
		}
	}

	private static void answer(MinecraftServer server, ServerConfigurationPacketListenerImpl handler, List<Integer> symbols) {
		GameProfile profile = profile(handler);
		if (!TYPING.contains(profile.id())) {
			return;
		}
		if (symbols.isEmpty()) {
			TYPING.remove(profile.id());
			handler.disconnect(ClassTexts.gaveUp());
			return;
		}
		ClassCode.Check check = ClassCode.check(profile.id(), symbols);
		switch (check.result()) {
			case RIGHT -> {
				TYPING.remove(profile.id());
				ClassCode.admit(server, profile, check.world());
				ClassCode.tellTeachers(server, ClassTexts.joined(profile.name(), check.world()));
				handler.completeTask(TypeCode.KEY);
			}
			case WRONG -> {
				MineMod.LOGGER.info("[MINE] {} typed a wrong class code ({} tries left)", profile.name(), ClassCode.triesLeft(profile.id()));
				ServerConfigurationNetworking.send(handler, new ClassCodePayloads.Ask(ClassCode.triesLeft(profile.id()), true));
			}
			case TOO_MANY -> {
				TYPING.remove(profile.id());
				MineMod.LOGGER.info("[MINE] {} typed too many wrong class codes", profile.name());
				ClassCode.tellTeachers(server, ClassTexts.gaveUpTrying(profile.name()));
				handler.disconnect(ClassTexts.tooMany());
			}
			case CLOSED -> {
				TYPING.remove(profile.id());
				handler.disconnect(ClassTexts.closed());
			}
		}
	}

	/**
	 * A student who just joined the class with the code goes to the world whose code it was. If
	 * the lab is there, they get a lane and arrive in front of the guide at its entrance.
	 */
	public static void arrived(MinecraftServer server, ServerPlayer player) {
		TYPING.remove(player.getUUID());
		String world = ClassCode.arrived(player.getUUID());
		if (world == null) {
			return;
		}
		server.execute(() -> {
			ServerLevel level = ClassWorlds.level(server, world);
			ClassWorlds.World classWorld = ClassWorlds.get(server).world(world);
			if (level != null && classWorld != null) {
				ClassWorlds.goTo(player, level, classWorld);
			}
			Lab lab = level == null ? Lab.get(server) : Lab.get(level);
			if (lab.exists() && lab.laneOf(player.getUUID()) == null) {
				lab.welcome(player);
			}
		});
	}

	private static GameProfile profile(ServerConfigurationPacketListenerImpl handler) {
		return handler.getOwner();
	}

	/** Joining waits here until the student types the right code. */
	private record TypeCode(int triesLeft) implements ConfigurationTask {
		static final Type KEY = new Type(MineMod.MOD_ID + ":class_code");

		@Override
		public void start(Consumer<Packet<?>> sender) {
			sender.accept(ServerConfigurationNetworking.createClientboundPacket(new ClassCodePayloads.Ask(triesLeft, false)));
		}

		@Override
		public Type type() {
			return KEY;
		}
	}
}
