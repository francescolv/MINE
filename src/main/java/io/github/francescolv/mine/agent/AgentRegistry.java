package io.github.francescolv.mine.agent;

import io.github.francescolv.mine.DataFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent "one student → one Byte" registry, saved in the world ({@code data/mine/agents.dat}).
 */
public final class AgentRegistry extends SavedData {

	private static final String KEY = "agents";
	private static final SavedDataType<AgentRegistry> TYPE = DataFormat.saved(KEY, AgentRegistry::new, AgentRegistry::fromNbt, AgentRegistry::save);

	/**
	 * Last known position of the Agent. {@code computerId} is -1 until the turtle has been
	 * turned on for the first time (CC: Tweaked only assigns an ID on startup).
	 */
	public record Entry(ResourceKey<Level> dimension, BlockPos pos, String label, int computerId) {

		Entry withPosition(ResourceKey<Level> newDimension, BlockPos newPos) {
			return new Entry(newDimension, newPos, label, computerId);
		}

		Entry withComputerId(int id) {
			return new Entry(dimension, pos, label, id);
		}
	}

	private final Map<UUID, Entry> agents = new HashMap<>();
	/** The last program each student's Agent ran, as typed: kept with the Agent, also after a restart. */
	private final Map<UUID, String> lastRun = new HashMap<>();

	public static AgentRegistry get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	@Nullable
	public Entry get(UUID player) {
		return agents.get(player);
	}

	public Map<UUID, Entry> all() {
		return agents;
	}

	public void put(UUID player, Entry entry) {
		if (!entry.equals(agents.put(player, entry))) {
			setDirty();
		}
	}

	public void remove(UUID player) {
		if (agents.remove(player) != null) {
			setDirty();
		}
	}

	@Nullable
	public String lastRun(UUID player) {
		return lastRun.get(player);
	}

	public void ran(UUID player, String command) {
		if (!command.equals(lastRun.put(player, command))) {
			setDirty();
		}
	}

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		ListTag list = new ListTag();
		agents.forEach((player, entry) -> {
			CompoundTag tag = new CompoundTag();
			tag.store("Player", UUIDUtil.CODEC, player);
			tag.putString("Dimension", entry.dimension().identifier().toString());
			tag.putLong("Pos", entry.pos().asLong());
			tag.putString("Label", entry.label());
			tag.putInt("ComputerId", entry.computerId());
			if (lastRun.containsKey(player)) {
				tag.putString("LastRun", lastRun.get(player));
			}
			list.add(tag);
		});
		nbt.put("Bytes", list);
		return nbt;
	}

	/** The registry as saved in {@code nbt}. */
	public static AgentRegistry fromNbt(CompoundTag nbt) {
		AgentRegistry registry = new AgentRegistry();
		ListTag list = nbt.getListOrEmpty("Bytes");
		for (int i = 0; i < list.size(); i++) {
			CompoundTag tag = list.getCompoundOrEmpty(i);
			ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse(tag.getStringOr("Dimension", "")));
			UUID player = tag.read("Player", UUIDUtil.CODEC).orElseThrow();
			registry.agents.put(player, new Entry(dimension, BlockPos.of(tag.getLongOr("Pos", 0L)),
					tag.getStringOr("Label", ""), tag.getIntOr("ComputerId", 0)));
			tag.getString("LastRun").ifPresent(command -> registry.lastRun.put(player, command));
		}
		return registry;
	}
}
