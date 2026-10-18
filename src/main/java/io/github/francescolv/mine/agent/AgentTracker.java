package io.github.francescolv.mine.agent;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.component.ComputerComponents;
import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeData;
import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.device.Devices;
import io.github.francescolv.mine.text.ServerLanguage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of running turtles, to know where an Agent is after it has moved.
 *
 * <p>This is the only class that depends on the CC: Tweaked API: it must only be loaded when
 * the mod is installed, and its public signatures use Minecraft types only.
 */
final class AgentTracker {

	/** Running turtles, by computer ID. Lua APIs are started and stopped on the computer thread. */
	private static final Map<Integer, MineLuaApi> LIVE = new ConcurrentHashMap<>();

	record Located(Level world, BlockPos pos) {
	}

	private AgentTracker() {
	}

	static void register() {
		ComputerCraftAPI.registerAPIFactory(system -> {
			ITurtleAccess turtle = system.getComponent(ComputerComponents.TURTLE);
			if (turtle == null) {
				return null;
			}
			return new MineLuaApi(system, turtle);
		});
	}

	/**
	 * The {@code mine} Lua API, available on turtles only. Its lifecycle (startup/shutdown)
	 * tells us which turtles are running and where.
	 *
	 * <p>Functions starting with {@code _} are used by {@code rom/autorun/mine.lua} for
	 * {@code /mine agent run} and to track programs: they are not meant for students.
	 */
	public static final class MineLuaApi implements ILuaAPI {

		/** Who receives {@code mine.say} messages: only nearby players, so 30 Agents don't flood the chat. */
		static final double SAY_RADIUS = 32;
		static final int SAY_MAX_LENGTH = 256;

		private final IComputerSystem system;
		private final int id;
		private final ITurtleAccess turtle;
		/** Is the shell waiting for a command? Written by the computer thread, read by the server thread. */
		private volatile boolean idle;
		/** The kit was offered while the shell wasn't waiting for a command. */
		private boolean kitOffered;
		/** MINE stopped the running program (a lesson had seen enough): it ended well, not by an error. */
		private volatile boolean stoppedByMine;
		/** When MINE last asked the program to stop, while it hasn't: -1 if it didn't. */
		private volatile long stopAskedAt = -1;
		/** The program running now, if it is tracked. Only touched on the server thread. */
		@Nullable
		private AgentProgram program;

		MineLuaApi(IComputerSystem system, ITurtleAccess turtle) {
			this.system = system;
			this.id = system.getID();
			this.turtle = turtle;
		}

		@Override
		public String[] getNames() {
			return new String[] { "mine" };
		}

		/** {@code mine.version()}: the MINE version installed on the server. */
		@LuaFunction
		public final String version() {
			return MineMod.VERSION;
		}

		/**
		 * {@code mine.say(message)}: the Agent writes in chat, signed with its label.
		 * Players within {@value #SAY_RADIUS} blocks can read it.
		 */
		@LuaFunction(mainThread = true)
		public final void say(Coerced<String> message) {
			// Coerced: like tostring() in Lua, so mine.say(42) works too.
			String text = message.value();
			if (text.length() > SAY_MAX_LENGTH) {
				text = text.substring(0, SAY_MAX_LENGTH) + "…";
			}
			Component line = Component.literal("[").append(AgentLabels.shown(system.getLabel())).append("] ")
					.withStyle(ChatFormatting.AQUA).append(Component.literal(text).withStyle(ChatFormatting.RESET));

			ServerLevel world = (ServerLevel) turtle.getLevel();
			BlockPos pos = turtle.getPosition();
			for (ServerPlayer player : world.players()) {
				if (player.distanceToSqr(Vec3.atCenterOf(pos)) <= SAY_RADIUS * SAY_RADIUS) {
					player.sendSystemMessage(line);
				}
			}
			if (program != null) {
				program.said(text);
			}
		}

		/**
		 * {@code mine.challenge([n])}: what the student's lesson step asks this time, e.g. the side
		 * of a square, chosen when the program started; with {@code n}, only its {@code n}-th value
		 * (from 1), for the blocks. Nothing ({@code nil}) when the step doesn't ask anything, or
		 * outside a program.
		 */
		@LuaFunction(mainThread = true)
		public final Object[] challenge(Optional<Integer> n) {
			AgentProgram running = program;
			Object[] values = running == null ? new Object[0] : running.challenge().toArray();
			if (n.isEmpty()) {
				return values;
			}
			int index = n.get() - 1;
			return index >= 0 && index < values.length ? new Object[] { values[index] } : new Object[0];
		}

		/**
		 * {@code mine._deviceRead(key)}: the last value the owner's board gave for {@code key}
		 * (a number or a text), or {@code nil}. Not on the main thread: events are not lost.
		 */
		@LuaFunction
		public final @Nullable Object _deviceRead(String key) {
			GameProfile profile = turtle.getOwningPlayer();
			return profile == null ? null : Devices.read(profile.id(), key);
		}

		/** {@code mine._deviceSend(key, value)}: sends {@code key:value} to the owner's board; false if none is connected. */
		@LuaFunction
		public final boolean _deviceSend(String key, Coerced<String> value) {
			GameProfile profile = turtle.getOwningPlayer();
			return profile != null && Devices.send(((ServerLevel) turtle.getLevel()).getServer(), profile.id(), key, value.value());
		}

		/** {@code mine._deviceConnected()}: whether the owner has a board connected in MINE. */
		@LuaFunction
		public final boolean _deviceConnected() {
			GameProfile profile = turtle.getOwningPlayer();
			return profile != null && Devices.isConnected(profile.id());
		}

		/** {@code mine.owner()}: the name of the student who owns the Agent, or {@code nil}. */
		@LuaFunction
		public final @Nullable String owner() {
			GameProfile profile = turtle.getOwningPlayer();
			return profile == null ? null : profile.name();
		}

		/**
		 * {@code mine._idle(free)}: the shell starts or stops waiting for a command. When it starts,
		 * returns whether its {@link AgentKit} was offered while it wasn't listening.
		 */
		@LuaFunction
		public final synchronized boolean _idle(boolean free) {
			idle = free;
			boolean missed = free && kitOffered;
			if (free) {
				kitOffered = false;
			}
			return missed;
		}

		/** Asks the shell to pick up its {@link AgentKit}: now if it is waiting, else when it next does. */
		synchronized void offerKit() {
			if (idle) {
				system.queueEvent("mine_kit");
			} else {
				kitOffered = true;
			}
		}

		/**
		 * {@code mine._inbox()}: programs sent to this Agent (from MINE's block editor) to write on
		 * its disk, and the lines announcing them; nothing if there are none.
		 */
		@LuaFunction(mainThread = true)
		public final Object[] _inbox() {
			ServerLevel world = (ServerLevel) turtle.getLevel();
			UUID owner = AgentManager.ownerOf(world.getServer(), id, world, turtle.getPosition());
			Map<String, String> sent = owner == null ? Map.of() : AgentManager.takeSent(owner);
			if (sent.isEmpty()) {
				return new Object[0];
			}
			ServerPlayer player = world.getServer().getPlayerList().getPlayer(owner);
			String language = player != null ? player.clientInformation().language() : "en_us";
			List<Component> lines = sent.keySet().stream().<Component>map(name -> Component.translatableWithFallback(
					"mine.blocks.received", "From the blocks: %s (type %s to run it)", name, name)).toList();
			return new Object[] { sent, render(lines, language) };
		}

		/** {@code mine._report(outcome, program)}: how a program started from chat ended. */
		@LuaFunction(mainThread = true)
		public final void _report(String outcome, String program) {
			GameProfile profile = turtle.getOwningPlayer();
			ServerPlayer owner = profile == null ? null
					: ((ServerLevel) turtle.getLevel()).getServer().getPlayerList().getPlayer(profile.id());
			if (owner == null) {
				return;
			}
			owner.sendSystemMessage(switch (outcome) {
				// Not green: finished isn't done, the lesson says that (also "not yet").
				case "ok" -> Component.translatableWithFallback("mine.agent.run.ok",
						"Your Byte has finished %s.", program).withStyle(ChatFormatting.GRAY);
				case "missing" -> Component.translatableWithFallback("mine.agent.run.missing",
						"Your Byte has no program called %s. To see its programs, type ls in its terminal.",
						program).withStyle(ChatFormatting.RED);
				default -> Component.translatableWithFallback("mine.agent.run.error",
						"%s stopped because of an error: open your Byte to read it.", program).withStyle(ChatFormatting.RED);
			});
		}

		/**
		 * {@code mine._started(command)}: one of the student's programs is starting, as typed (with
		 * its arguments). Only counts on the Agent of a student: other turtles are not tracked.
		 */
		@LuaFunction(mainThread = true)
		public final void _started(Optional<String> command) {
			ServerLevel world = (ServerLevel) turtle.getLevel();
			UUID owner = AgentManager.ownerOf(world.getServer(), id, world, turtle.getPosition());
			program = owner == null ? null
					: new AgentProgram(owner, id, world, turtle.getPosition(), turtle.getDirection(),
							turtle.getInventory().getItem(turtle.getSelectedSlot()).getCount());
			if (program != null) {
				command.ifPresent(typed -> AgentManager.ran(world.getServer(), owner, typed));
				program.start();
			}
		}

		/**
		 * {@code mine._finished(ok)}: the program started by {@code _started} has ended. Returns
		 * the lines to write in the terminal (e.g. how the lesson step went), in the owner's language.
		 */
		@LuaFunction(mainThread = true)
		public final List<String> _finished(boolean ok) {
			AgentProgram finished = program;
			program = null;
			stopAskedAt = -1;
			PythonBridge.programEnded(id);
			if (finished == null || turtle.getLevel() != finished.world()) {
				return List.of();
			}
			Container inventory = turtle.getInventory();
			int selected = turtle.getSelectedSlot();
			int others = 0;
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				if (slot != selected) {
					others += inventory.getItem(slot).getCount();
				}
			}
			finished.finish(turtle.getPosition(), turtle.getDirection(), ok,
					inventory.getItem(selected).getCount(), others);

			ServerPlayer owner = finished.world().getServer().getPlayerList().getPlayer(finished.owner());
			String language = owner != null ? owner.clientInformation().language() : "en_us";
			return finished.told().stream().map(line -> ServerLanguage.render(line, language)).toList();
		}

		/**
		 * {@code mine._prepare()}: the shell is ready for a command. Gives the Agent its pickaxe if
		 * it has no tools, and whatever the {@link AgentKit} says: returns
		 * {@code filesToken, files, filesLines, lines}, or nothing if this is not a student's Agent.
		 */
		@LuaFunction(mainThread = true)
		public final Object[] _prepare() {
			ServerLevel world = (ServerLevel) turtle.getLevel();
			UUID owner = AgentManager.ownerOf(world.getServer(), id, world, turtle.getPosition());
			if (owner == null) {
				return new Object[0];
			}
			if (turtle.getUpgrade(TurtleSide.LEFT) == null && turtle.getUpgrade(TurtleSide.RIGHT) == null) {
				pickaxe(world.getServer()).ifPresent(pickaxe -> turtle.setUpgrade(TurtleSide.LEFT, pickaxe));
			}
			ServerPlayer player = world.getServer().getPlayerList().getPlayer(owner);
			String language = player != null ? player.clientInformation().language() : "en_us";
			AgentKit.Kit kit = AgentKit.prepare(world.getServer(), owner, language, turtle.getInventory(), turtle.getSelectedSlot());
			if (kit == null) {
				return new Object[0];
			}
			return new Object[] { kit.filesToken(), kit.files(), render(kit.filesLines(), language), render(kit.lines(), language) };
		}

		/**
		 * {@code mine._welcome()}: what the Agent shows when it starts, in place of CraftOS's banner,
		 * in its owner's language: MINE's version and the Agent's name, the student's lesson if they
		 * have one, and where to find help.
		 */
		@LuaFunction(mainThread = true)
		public final List<String> _welcome() {
			ServerLevel world = (ServerLevel) turtle.getLevel();
			UUID owner = AgentManager.ownerOf(world.getServer(), id, world, turtle.getPosition());
			String language = ownerLanguage(world, owner);
			List<Component> lines = new ArrayList<>();
			lines.add(Component.translatableWithFallback("mine.agent.welcome", "MINE %s · %s", MineMod.VERSION,
					AgentLabels.shown(system.getLabel())));
			Component status = owner == null ? null : AgentKit.status(world.getServer(), owner);
			if (status != null) {
				lines.add(status);
			}
			lines.add(Component.translatableWithFallback("mine.agent.welcome_help",
					"ls: programs · help byte · help mine"));
			return render(lines, language);
		}

		/** The owner's game language (e.g. "it_it"), "en_us" while they're offline: mine.lua picks the help pages with it. */
		@LuaFunction(mainThread = true)
		public final String _language() {
			ServerLevel world = (ServerLevel) turtle.getLevel();
			return ownerLanguage(world, AgentManager.ownerOf(world.getServer(), id, world, turtle.getPosition()));
		}

		/**
		 * {@code mine._pyReady()}: the Python bridge started on this Byte; a token for its answers,
		 * or nil if no Python program waits for it (the bridge was run by hand).
		 */
		@LuaFunction(mainThread = true)
		public final @Nullable String _pyReady() {
			ServerLevel world = (ServerLevel) turtle.getLevel();
			return PythonBridge.ready(AgentManager.ownerOf(world.getServer(), id, world, turtle.getPosition()), id);
		}

		/** {@code mine._pyReply(token, id, ok, values)}: the answer to a Python call, values as JSON. */
		@LuaFunction
		public final void _pyReply(String token, int call, boolean ok, String values) {
			PythonBridge.reply(token, call, ok, values);
		}

		/** {@code mine._pyListening(token)}: the bridge waits for calls from now on. */
		@LuaFunction
		public final void _pyListening(String token) {
			PythonBridge.listening(token);
		}

		/** {@code mine._pyEnd(token)}: the bridge ended. */
		@LuaFunction
		public final void _pyEnd(String token) {
			PythonBridge.ended(token);
		}

		private static String ownerLanguage(ServerLevel world, UUID owner) {
			ServerPlayer player = owner == null ? null : world.getServer().getPlayerList().getPlayer(owner);
			return player != null ? player.clientInformation().language() : "en_us";
		}

		private static List<String> render(List<Component> lines, String language) {
			return lines.stream().map(line -> ServerLanguage.render(line, language)).toList();
		}

		/**
		 * Stops the running program, wherever it is waiting, as a success: see {@link #_stoppedByMine}.
		 * The shell turns the {@code mine_stop} event into the end of the program ({@code mine.lua}).
		 */
		void stop() {
			stoppedByMine = true;
			stopAskedAt = System.currentTimeMillis();
			system.queueEvent("mine_stop");
		}

		/**
		 * Asks again, every second, a program that hasn't stopped yet: an event that arrives while
		 * the program waits for a main-thread function (e.g. sending to the board) is lost.
		 */
		void stopAgain() {
			if (stopAskedAt >= 0 && System.currentTimeMillis() - stopAskedAt > 1000) {
				stopAskedAt = System.currentTimeMillis();
				system.queueEvent("mine_stop");
			}
		}

		/** {@code mine._stoppedByMine()}: whether MINE stopped the program that just ended (asked once). */
		@LuaFunction
		public final boolean _stoppedByMine() {
			boolean stopped = stoppedByMine;
			stoppedByMine = false;
			return stopped;
		}

		/** Starts {@code program} if the shell is free; called from the server thread. */
		synchronized boolean requestRun(String program) {
			if (!idle) {
				return false;
			}
			idle = false;
			system.queueEvent("mine_run", program);
			return true;
		}

		@Override
		public void startup() {
			idle = false;
			LIVE.put(id, this);
		}

		@Override
		public void shutdown() {
			LIVE.remove(id, this);
		}
	}

	/** Follows the Agents that are running a tracked program; called every tick on the server thread. */
	static void tick() {
		for (MineLuaApi api : LIVE.values()) {
			AgentProgram program = api.program;
			if (program != null && api.turtle.getLevel() == program.world()) {
				program.moved(api.turtle.getPosition(), api.turtle.getDirection());
				program.tick();
				api.stopAgain();
			}
		}
	}

	/** Queues an event on the running Agent {@code computerId}; nothing if it is off. */
	static void queueEvent(int computerId, String event, Object... arguments) {
		MineLuaApi api = LIVE.get(computerId);
		if (api != null) {
			api.system.queueEvent(event, arguments);
		}
	}

	/** Stops the program the Agent {@code computerId} is running, as a success. */
	static void stop(int computerId) {
		MineLuaApi api = LIVE.get(computerId);
		if (api != null) {
			api.stop();
		}
	}

	@Nullable
	static Located locate(int computerId) {
		MineLuaApi api = LIVE.get(computerId);
		return api == null ? null : new Located(api.turtle.getLevel(), api.turtle.getPosition());
	}

	/** ID of the running turtle at {@code pos}, or -1. */
	static int idAt(Level world, BlockPos pos) {
		for (Map.Entry<Integer, MineLuaApi> entry : LIVE.entrySet()) {
			ITurtleAccess turtle = entry.getValue().turtle;
			if (turtle.getLevel() == world && turtle.getPosition().equals(pos)) {
				return entry.getKey();
			}
		}
		return -1;
	}

	/** Starts a program on the turtle, as if the student had typed it in the terminal. */
	static AgentManager.Result run(int computerId, String program) {
		MineLuaApi api = LIVE.get(computerId);
		if (api == null) {
			return AgentManager.Result.OFF;
		}
		return api.requestRun(program) ? AgentManager.Result.STARTED : AgentManager.Result.BUSY;
	}

	/** Asks a running turtle to pick up its {@link AgentKit} now, instead of at its next prompt. */
	static void offerKit(int computerId) {
		MineLuaApi api = LIVE.get(computerId);
		if (api != null) {
			api.offerKit();
		}
	}

	/**
	 * Gives the turtle item a diamond pickaxe, so the Agent can dig like the Minecraft Education
	 * one ({@code turtle.dig()}). Does nothing if the upgrade isn't there (e.g. disabled by a datapack).
	 */
	@SuppressWarnings("unchecked")
	static void equipPickaxe(ItemStack stack, MinecraftServer server) {
		var component = (DataComponentType<UpgradeData<ITurtleUpgrade>>) BuiltInRegistries.DATA_COMPONENT_TYPE
				.getValue(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "left_turtle_upgrade"));
		if (component != null) {
			pickaxe(server).ifPresent(pickaxe -> stack.set(component, pickaxe));
		}
	}

	private static Optional<UpgradeData<ITurtleUpgrade>> pickaxe(MinecraftServer server) {
		return server.registryAccess().lookupOrThrow(ITurtleUpgrade.REGISTRY)
				.get(ResourceKey.create(ITurtleUpgrade.REGISTRY, Identifier.withDefaultNamespace("diamond_pickaxe")))
				.map(UpgradeData::ofDefault);
	}

	/** Moves a running turtle, keeping its programs and inventory. */
	static boolean teleport(int computerId, ServerLevel world, BlockPos pos, Direction facing) {
		MineLuaApi api = LIVE.get(computerId);
		ITurtleAccess turtle = api == null ? null : api.turtle;
		// Already there: it only turns.
		boolean there = turtle != null && turtle.getLevel() == world && turtle.getPosition().equals(pos);
		if (turtle == null || !there && !turtle.teleportTo(world, pos)) {
			return false;
		}
		turtle.setDirection(facing);
		return true;
	}
}
