package io.github.francescolv.mine.agent;

import io.github.francescolv.mine.MineMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent module logic: each student has at most one Agent (a CC: Tweaked turtle).
 */
public final class AgentManager {

	public static final String CC_MOD_ID = "computercraft";
	/** Whether CC: Tweaked is installed: without it, there are no Agents. */
	public static final boolean CC_LOADED = FabricLoader.getInstance().isModLoaded(CC_MOD_ID);
	private static final Identifier TURTLE_ID = Identifier.fromNamespaceAndPath(CC_MOD_ID, "turtle_normal");
	/** How often, in ticks, we save the position of running Agents (5 seconds). */
	private static final int SYNC_INTERVAL = 100;

	public enum Result {
		SPAWNED,
		REPLACED,
		RECALLED,
		BLOCKED,
		NO_AGENT,
		NO_CC,
		/** Program started by {@code /mine agent run}. */
		STARTED,
		/** The Agent is off (never opened, or shut down). */
		OFF,
		/** The Agent is already running a program, or has the editor open. */
		BUSY,
		/** A program was sent to the Agent: it is on its disk when its shell is ready. */
		SENT,
		/** The Agent was off: it is turning on, and the program starts when it is ready. */
		STARTING
	}

	/** How long a program waits for an Agent that is turning on. */
	private static final int START_TICKS = 10 * 20;
	/** Programs waiting for their Agent to turn on: student → program, ticks left. */
	private static final Map<UUID, Starting> STARTING = new ConcurrentHashMap<>();

	private record Starting(String program, int ticksLeft) {
	}

	/** Programs sent to students' Agents, waiting for their shell to be ready: student → name → program. */
	private static final Map<UUID, Map<String, String>> INBOX = new ConcurrentHashMap<>();

	private AgentManager() {
	}

	public static void register() {
		if (!CC_LOADED) {
			MineMod.LOGGER.warn("[MINE] CC: Tweaked not found: Byte module is disabled.");
			return;
		}
		AgentTracker.register();
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			AgentTracker.tick();
			if (!STARTING.isEmpty()) {
				startWaiting(server);
			}
			if (server.getTickCount() % SYNC_INTERVAL == 0) {
				syncPositions(server);
			}
		});
	}

	/** Creates the Agent in front of the student, removing the old one if there was one. */
	public static Result spawn(ServerPlayer player) {
		Block turtle = BuiltInRegistries.BLOCK.getValue(TURTLE_ID);
		if (!CC_LOADED || turtle == Blocks.AIR) {
			return Result.NO_CC;
		}

		ServerLevel world = player.level();
		Direction facing = player.getDirection();
		BlockPos target = targetFor(player);

		AgentRegistry registry = AgentRegistry.get(world.getServer());
		UUID uuid = player.getUUID();
		// The new Agent gets the old one's computer, and so its disk: the student's programs stay,
		// also when the old Agent was broken.
		AgentRegistry.Entry previous = registry.get(uuid);
		int computerId = previous == null ? -1 : previous.computerId();
		AgentTracker.Located old = locate(world.getServer(), registry, uuid);
		boolean oldIsInTheWay = old != null && old.world() == world && old.pos().equals(target);

		if (!oldIsInTheWay && !world.getBlockState(target).canBeReplaced()) {
			return Result.BLOCKED;
		}
		if (old != null) {
			old.world().removeBlock(old.pos(), false);
		}

		// Stored in English; MINE shows it in each player's language ("Byte di Alex"): see AgentLabels.
		String label = AgentLabels.of(player.getName().getString());
		place(world, target, facing, turtle, label, player, computerId);
		if (computerId >= 0 && !keepComputer(world, target, computerId)) {
			computerId = -1;
		}
		registry.put(uuid, new AgentRegistry.Entry(world.dimension(), target, label, computerId));

		MineMod.LOGGER.info("[MINE] Byte {} for {} at {}", old != null ? "replaced" : "created",
				player.getName().getString(), target.toShortString());
		return old != null ? Result.REPLACED : Result.SPAWNED;
	}

	/** Where an Agent is and which way it faces. */
	public record Placement(ServerLevel world, BlockPos pos, Direction facing) {
	}

	/** Where the student's Agent is now, or {@code null} if they don't have one. */
	@Nullable
	public static Placement placement(ServerPlayer player) {
		if (!CC_LOADED) {
			return null;
		}
		AgentTracker.Located located = locate(player.level().getServer(), AgentRegistry.get(player.level().getServer()), player.getUUID());
		if (located == null || !(located.world() instanceof ServerLevel world)) {
			return null;
		}
		BlockState state = world.getBlockState(located.pos());
		Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
		return new Placement(world, located.pos(), facing);
	}

	/** Brings the Agent back in front of the student, keeping its programs and inventory. */
	public static Result recall(ServerPlayer player) {
		if (!CC_LOADED) {
			return Result.NO_CC;
		}

		ServerLevel world = player.level();
		AgentRegistry registry = AgentRegistry.get(world.getServer());
		UUID uuid = player.getUUID();
		AgentTracker.Located old = locate(world.getServer(), registry, uuid);
		if (old == null) {
			return Result.NO_AGENT;
		}

		AgentRegistry.Entry entry = registry.get(uuid);
		if (entry.computerId() < 0) {
			// Never turned on: no programs and it has never moved, so we can simply recreate it.
			return spawn(player) == Result.BLOCKED ? Result.BLOCKED : Result.RECALLED;
		}

		Direction facing = player.getDirection();
		BlockPos target = targetFor(player);
		boolean there = old.world() == world && old.pos().equals(target);
		if (!there && !world.getBlockState(target).canBeReplaced()) {
			return Result.BLOCKED;
		}
		if (!AgentTracker.teleport(entry.computerId(), world, target, facing)) {
			if (there) {
				// Off: it stays where it is, as it was.
				return Result.RECALLED;
			}
			// Off (e.g. after a restart): MINE can't move it, but makes it again in front of the
			// student, with its computer (its programs) and what it carries.
			return respawn(player, old) ? Result.RECALLED : Result.BLOCKED;
		}
		registry.put(uuid, entry.withPosition(world.dimension(), target));
		MineMod.LOGGER.info("[MINE] Byte of {} recalled to {}", player.getName().getString(), target.toShortString());
		return Result.RECALLED;
	}

	/** Starts one of the Agent's programs, as if the student had typed it in its terminal. */
	public static Result run(ServerPlayer player, String program) {
		if (!CC_LOADED) {
			return Result.NO_CC;
		}

		OptionalInt computer = computer(player);
		if (computer.isEmpty()) {
			return Result.NO_AGENT;
		}
		int id = computer.getAsInt();
		return id < 0 ? Result.OFF : AgentTracker.run(id, program);
	}

	/** What to do with a program sent to the Agent. */
	public enum Delivery {
		/** Written on its disk, announced in its terminal. */
		OFFER,
		/** Written and started now. */
		RUN,
		/** Kept until it is started (with {@link #run}): written silently, just before. */
		HOLD
	}

	/**
	 * Sends {@code program} to the student's Agent, as the file {@code name} (e.g. from MINE's
	 * block editor). The Agent writes it on its disk when its shell is ready for a command: now,
	 * or after the program it is running.
	 */
	public static Result send(ServerPlayer player, String name, String program, Delivery delivery) {
		if (!CC_LOADED) {
			return Result.NO_CC;
		}
		OptionalInt computer = computer(player);
		if (computer.isEmpty()) {
			return Result.NO_AGENT;
		}
		INBOX.computeIfAbsent(player.getUUID(), uuid -> new ConcurrentHashMap<>()).put(name, program);
		int id = computer.getAsInt();
		if (delivery == Delivery.RUN) {
			// The shell picks up what was sent before running it.
			return runOrStart(player, name);
		}
		if (id < 0 || AgentTracker.locate(id) == null) {
			// Off: turned on, its shell picks up what was sent when it is ready.
			return powerOn(player) ? Result.SENT : Result.OFF;
		}
		if (delivery == Delivery.OFFER) {
			AgentTracker.offerKit(id);
		}
		return Result.SENT;
	}

	/**
	 * Turns the student's Agent on, if it is off (e.g. after the server restarted): as opening it
	 * would. Returns whether it is on now. Uses CC: Tweaked's own classes, which have no API for
	 * this: if they change, MINE says to open the Agent instead.
	 */
	public static boolean powerOn(ServerPlayer player) {
		if (!CC_LOADED) {
			return false;
		}
		AgentTracker.Located agent = locate(player.level().getServer(), AgentRegistry.get(player.level().getServer()), player.getUUID());
		if (agent == null) {
			return false;
		}
		try {
			if (agent.world().getBlockEntity(agent.pos()) instanceof dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity computer) {
				var running = computer.createServerComputer();
				if (!running.isOn()) {
					running.turnOn();
					MineMod.LOGGER.info("[MINE] Byte of {} turned on", player.getName().getString());
				}
				return true;
			}
		} catch (LinkageError e) {
			MineMod.LOGGER.warn("[MINE] Can't turn Bytes on with this CC: Tweaked ({}): students open them instead", e.toString());
		}
		return false;
	}

	/**
	 * Makes the student's Agent again in front of them, as {@link #spawn} (same computer, so same
	 * programs), with what the old one carried: for an Agent that is off, which can't be moved.
	 */
	private static boolean respawn(ServerPlayer player, AgentTracker.Located old) {
		List<ItemStack> carried = new ArrayList<>();
		try {
			if (old.world().getBlockEntity(old.pos()) instanceof dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity turtle) {
				var inventory = turtle.getAccess().getInventory();
				for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
					carried.add(inventory.getItem(slot).copy());
				}
			}
		} catch (LinkageError e) {
			MineMod.LOGGER.warn("[MINE] Can't read what a Byte carries with this CC: Tweaked ({})", e.toString());
		}
		Result result = spawn(player);
		if (result != Result.SPAWNED && result != Result.REPLACED) {
			return false;
		}
		Placement now = placement(player);
		try {
			if (now != null && now.world().getBlockEntity(now.pos()) instanceof dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity turtle) {
				var inventory = turtle.getAccess().getInventory();
				for (int slot = 0; slot < carried.size() && slot < inventory.getContainerSize(); slot++) {
					inventory.setItem(slot, carried.get(slot));
				}
				inventory.setChanged();
			}
		} catch (LinkageError e) {
			MineMod.LOGGER.warn("[MINE] Can't give a Byte back what it carried with this CC: Tweaked ({})", e.toString());
		}
		return true;
	}

	static void ran(MinecraftServer server, UUID student, String command) {
		if (!command.isBlank()) {
			AgentRegistry.get(server).ran(student, command.strip());
		}
	}

	/**
	 * The last program the student's Agent ran (with its arguments), or {@code null}: the key MINE
	 * gives in the game runs it again. Kept in the world, so it survives a restart of the server.
	 */
	@Nullable
	public static String lastRun(MinecraftServer server, UUID student) {
		return AgentRegistry.get(server).lastRun(student);
	}

	/**
	 * The student's board said {@code key:value}: their Agent gets a {@code mine_device} event, if
	 * it is on. Called on the server thread.
	 */
	public static void deviceEvent(MinecraftServer server, UUID student, String key, Object value) {
		if (!CC_LOADED) {
			return;
		}
		AgentRegistry.Entry entry = AgentRegistry.get(server).get(student);
		if (entry != null && entry.computerId() >= 0) {
			AgentTracker.queueEvent(entry.computerId(), "mine_device", key, value);
		}
	}

	/** What was sent to a student's Agent and not yet written on its disk, taken out of the inbox. */
	static Map<String, String> takeSent(UUID owner) {
		Map<String, String> sent = INBOX.remove(owner);
		return sent == null ? Map.of() : sent;
	}

	/**
	 * Starts one of the Agent's programs like {@link #run}, but an Agent that is off is turned on
	 * first ({@link Result#STARTING}): the program starts when it is ready.
	 */
	public static Result runOrStart(ServerPlayer player, String program) {
		Result result = run(player, program);
		if (result == Result.OFF && powerOn(player)) {
			STARTING.put(player.getUUID(), new Starting(program, START_TICKS));
			return Result.STARTING;
		}
		return result;
	}

	private static void startWaiting(MinecraftServer server) {
		STARTING.forEach((uuid, starting) -> {
			ServerPlayer player = server.getPlayerList().getPlayer(uuid);
			Result result = player == null ? Result.NO_AGENT : run(player, starting.program());
			// An Agent turning on is off, then busy starting its shell, then ready.
			if ((result == Result.OFF || result == Result.BUSY) && starting.ticksLeft() > 0) {
				STARTING.put(uuid, new Starting(starting.program(), starting.ticksLeft() - 1));
				return;
			}
			STARTING.remove(uuid);
			if (player != null && result != Result.STARTED) {
				player.sendSystemMessage(Component.translatableWithFallback("mine.agent.run.not_ready",
						"Your Byte didn't get ready to run %s: open it (right-click) and try again.", starting.program())
						.withStyle(ChatFormatting.RED));
			}
		});
	}

	/**
	 * Tells the student's Agent, if it is on and waiting for a command, to pick up its
	 * {@link AgentKit} now. Otherwise it picks it up the next time its shell is ready.
	 */
	public static void offerKit(ServerPlayer player) {
		if (!CC_LOADED) {
			return;
		}
		int id = computer(player).orElse(-1);
		if (id >= 0) {
			AgentTracker.offerKit(id);
		}
	}

	/**
	 * The student who owns the running turtle {@code computerId}, if it is their Agent; otherwise
	 * {@code null}. Learns the computer ID of an Agent that was just turned on for the first time.
	 */
	@Nullable
	static UUID ownerOf(MinecraftServer server, int computerId, Level world, BlockPos pos) {
		AgentRegistry registry = AgentRegistry.get(server);
		for (Map.Entry<UUID, AgentRegistry.Entry> e : registry.all().entrySet()) {
			AgentRegistry.Entry entry = e.getValue();
			if (entry.computerId() == computerId) {
				return e.getKey();
			}
			if (entry.computerId() < 0 && entry.dimension() == world.dimension() && entry.pos().equals(pos)) {
				registry.put(e.getKey(), entry.withComputerId(computerId));
				return e.getKey();
			}
		}
		return null;
	}

	/** The block in front of the student's feet, where the Agent appears. */
	public static BlockPos targetFor(ServerPlayer player) {
		return player.blockPosition().relative(player.getDirection());
	}

	/**
	 * Where the student's Agent is now, or {@code null} if it no longer exists
	 * (broken, or never created). Updates the registry along the way.
	 */
	@Nullable
	/** The computer of the student's Agent ({@code -1}: never turned on), or empty if they have none. */
	private static OptionalInt computer(ServerPlayer player) {
		AgentRegistry registry = AgentRegistry.get(player.level().getServer());
		if (locate(player.level().getServer(), registry, player.getUUID()) == null) {
			return OptionalInt.empty();
		}
		return OptionalInt.of(registry.get(player.getUUID()).computerId());
	}

	private static AgentTracker.Located locate(MinecraftServer server, AgentRegistry registry, UUID player) {
		AgentRegistry.Entry entry = registry.get(player);
		if (entry == null) {
			return null;
		}

		if (entry.computerId() >= 0) {
			AgentTracker.Located live = AgentTracker.locate(entry.computerId());
			if (live != null && isAgentAt(live.world(), live.pos(), entry.label())) {
				registry.put(player, entry.withPosition(live.world().dimension(), live.pos()));
				return live;
			}
		}

		ServerLevel world = server.getLevel(entry.dimension());
		if (world == null || !isAgentAt(world, entry.pos(), entry.label())) {
			registry.remove(player);
			return null;
		}
		if (entry.computerId() < 0) {
			int id = AgentTracker.idAt(world, entry.pos());
			if (id >= 0) {
				registry.put(player, entry.withComputerId(id));
			}
		}
		return new AgentTracker.Located(world, entry.pos());
	}

	/** Periodically saves where running Agents are, so their position survives restarts. */
	private static void syncPositions(MinecraftServer server) {
		AgentRegistry registry = AgentRegistry.get(server);
		for (Map.Entry<UUID, AgentRegistry.Entry> e : Map.copyOf(registry.all()).entrySet()) {
			AgentRegistry.Entry entry = e.getValue();
			int id = entry.computerId();
			if (id < 0) {
				ServerLevel world = server.getLevel(entry.dimension());
				id = world == null ? -1 : AgentTracker.idAt(world, entry.pos());
				if (id < 0) {
					continue;
				}
				entry = entry.withComputerId(id);
			}
			AgentTracker.Located live = AgentTracker.locate(id);
			if (live != null) {
				entry = entry.withPosition(live.world().dimension(), live.pos());
			}
			registry.put(e.getKey(), entry);
		}
	}

	/** Is there a turtle with this Agent's label? (the student may have broken it) */
	private static boolean isAgentAt(Level world, BlockPos pos, String label) {
		BlockState state = world.getBlockState(pos);
		Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		if (!id.getNamespace().equals(CC_MOD_ID) || !id.getPath().startsWith("turtle")) {
			return false;
		}
		return world.getBlockEntity(pos) instanceof Nameable named
				&& named.getCustomName() != null
				&& label.equals(named.getCustomName().getString());
	}

	/** Gives the Agent just placed at {@code pos} the computer {@code id}; {@code false} if it can't. */
	private static boolean keepComputer(ServerLevel world, BlockPos pos, int id) {
		try {
			if (world.getBlockEntity(pos) instanceof dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity computer) {
				computer.setComputerID(id);
				return true;
			}
		} catch (LinkageError e) {
			MineMod.LOGGER.warn("[MINE] Can't give a new Byte its old computer with this CC: Tweaked ({})", e.toString());
		}
		return false;
	}

	/**
	 * Places a turtle as a player would, labelled; with {@code computerId} (if not -1) it is that
	 * computer, as a turtle picked up and placed again: its disk, its programs. The id goes on the
	 * item, so CC: Tweaked starts that computer, not a new one.
	 */
	private static void place(ServerLevel world, BlockPos pos, Direction facing, Block turtle, String label,
			ServerPlayer player, int computerId) {
		ItemStack stack = new ItemStack(turtle.asItem());
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(label));
		AgentTracker.equipPickaxe(stack, world.getServer());
		if (computerId >= 0) {
			try {
				stack.set(dan200.computercraft.shared.ModRegistry.DataComponents.COMPUTER_ID.get(),
						new dan200.computercraft.shared.util.NonNegativeId.Computer(computerId));
			} catch (LinkageError e) {
				MineMod.LOGGER.warn("[MINE] Can't give a new Byte its old computer with this CC: Tweaked ({})", e.toString());
			}
		}

		BlockState state = turtle.defaultBlockState();
		if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		}

		// Same sequence as BlockItem#place, so that CC: Tweaked assigns owner and label.
		world.setBlock(pos, state, Block.UPDATE_ALL);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null) {
			blockEntity.applyComponentsFromItemStack(stack);
			blockEntity.setChanged();
		}
		turtle.setPlacedBy(world, pos, state, player, stack);
	}
}
