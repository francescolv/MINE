package io.github.francescolv.mine.agent;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * One run of a student's program on their Agent, seen from the server: where the Agent started,
 * where it went and what it did. Everything here is measured by MINE, never reported by Lua,
 * so other modules (lessons) can trust it.
 *
 * <p>Only programs the student saved count: built-in programs such as {@code lua} or
 * {@code edit} are not tracked (see {@code rom/autorun/mine.lua}). Lives on the server thread.
 */
public final class AgentProgram {

	/** Notified when an Agent program starts and ends, on the server thread. */
	public interface Listener {

		default void started(AgentProgram program) {
		}

		void finished(AgentProgram program);

		/** Every tick while the program runs. */
		default void tick(AgentProgram program) {
		}
	}

	private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
	private static final int MAX_MESSAGES = 32;

	private final UUID owner;
	private final int computerId;
	private final ServerLevel world;
	private final BlockPos startPos;
	private final Direction startFacing;
	private final int startSelectedItems;
	private BlockPos pos;
	private Direction facing;
	private int maxDistance;
	private int highest;
	private int steps;
	private int says;
	/** What the program said with mine.say, the first {@value #MAX_MESSAGES} messages. */
	private final List<String> messages = new ArrayList<>();
	/** What {@code mine.challenge()} returns to this program: chosen by a lesson when it starts. */
	private volatile List<Object> challenge = List.of();
	private boolean succeeded;
	private int selectedItems;
	private int otherItems;
	private final List<Component> terminal = new ArrayList<>();
	@Nullable
	private Object attachment;

	AgentProgram(UUID owner, int computerId, ServerLevel world, BlockPos pos, Direction facing, int selectedItems) {
		this.owner = owner;
		this.computerId = computerId;
		this.world = world;
		this.startPos = pos;
		this.startFacing = facing;
		this.startSelectedItems = selectedItems;
		this.pos = pos;
		this.facing = facing;
		this.highest = pos.getY();
	}

	public static void addListener(Listener listener) {
		LISTENERS.add(listener);
	}

	public static void removeListener(Listener listener) {
		LISTENERS.remove(listener);
	}

	void start() {
		LISTENERS.forEach(listener -> listener.started(this));
	}

	void tick() {
		LISTENERS.forEach(listener -> listener.tick(this));
	}

	/**
	 * Stops the program, e.g. one that listens forever once a lesson has seen what it needed: it
	 * ends as if it had finished by itself.
	 */
	public void stop() {
		AgentTracker.stop(computerId);
	}

	void moved(BlockPos newPos, Direction newFacing) {
		if (!newPos.equals(pos)) {
			steps++;
		}
		highest = Math.max(highest, newPos.getY());
		pos = newPos;
		facing = newFacing;
		maxDistance = Math.max(maxDistance, startPos.distManhattan(newPos));
	}

	void said(String message) {
		says++;
		if (messages.size() < MAX_MESSAGES) {
			messages.add(message);
		}
	}

	void finish(BlockPos endPos, Direction endFacing, boolean ok, int selectedItems, int otherItems) {
		moved(endPos, endFacing);
		succeeded = ok;
		this.selectedItems = selectedItems;
		this.otherItems = otherItems;
		LISTENERS.forEach(listener -> listener.finished(this));
	}

	/** The student who owns the Agent. */
	public UUID owner() {
		return owner;
	}

	public int computerId() {
		return computerId;
	}

	public ServerLevel world() {
		return world;
	}

	public BlockPos startPos() {
		return startPos;
	}

	public Direction startFacing() {
		return startFacing;
	}

	/** Where the Agent is now (at the end, once the program has finished). */
	public BlockPos pos() {
		return pos;
	}

	public Direction facing() {
		return facing;
	}

	/** The farthest the Agent got from its starting point, in blocks (Manhattan distance). */
	public int maxDistance() {
		return maxDistance;
	}

	/** The highest the Agent has been while the program ran. */
	public int highest() {
		return highest;
	}

	/** How many times the Agent moved one block (forward, back, up or down). */
	public int steps() {
		return steps;
	}

	/** How many times the program called {@code mine.say}. */
	public int says() {
		return says;
	}

	/** What the program said with {@code mine.say}, in order (at most {@value #MAX_MESSAGES} messages). */
	public List<String> messages() {
		return messages;
	}

	/**
	 * Sets what {@code mine.challenge()} returns while this program runs, e.g. the size of a
	 * square chosen at random: the program reads it instead of counting on a fixed number.
	 * Only from {@link Listener#started}.
	 */
	public void setChallenge(Object... values) {
		challenge = List.of(values);
	}

	public List<Object> challenge() {
		return challenge;
	}

	/** Whether the program ended without errors. Only meaningful once it has finished. */
	public boolean succeeded() {
		return succeeded;
	}

	/** At the start, how many items were in the Agent's selected slot. */
	public int startSelectedItems() {
		return startSelectedItems;
	}

	/** At the end, how many items are in the Agent's selected slot: the one turtle.place() uses. */
	public int selectedItems() {
		return selectedItems;
	}

	/** At the end, how many items are in the Agent's other slots. */
	public int otherItems() {
		return otherItems;
	}

	/**
	 * Adds a line to write in the Agent's terminal when the program ends: while the terminal is
	 * open the student can't see the chat. Only from {@link Listener#finished}.
	 */
	public void tell(Component line) {
		terminal.add(line);
	}

	/** The lines added with {@link #tell}. */
	public List<Component> told() {
		return terminal;
	}

	/** Free slot for a listener to remember something between {@code started} and {@code finished}. */
	@Nullable
	public Object attachment() {
		return attachment;
	}

	public void setAttachment(@Nullable Object attachment) {
		this.attachment = attachment;
	}
}
