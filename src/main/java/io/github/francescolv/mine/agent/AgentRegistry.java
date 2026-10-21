package io.github.francescolv.mine.agent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.francescolv.mine.DataFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

	/** A student's Byte as saved. */
	private record Saved(UUID player, ResourceKey<Level> dimension, long pos, String label, int computerId, Optional<String> lastRun) {
		static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("Player").forGetter(Saved::player),
				Level.RESOURCE_KEY_CODEC.fieldOf("Dimension").forGetter(Saved::dimension),
				Codec.LONG.fieldOf("Pos").orElse(0L).forGetter(Saved::pos),
				Codec.STRING.fieldOf("Label").orElse("").forGetter(Saved::label),
				Codec.INT.fieldOf("ComputerId").orElse(0).forGetter(Saved::computerId),
				Codec.STRING.optionalFieldOf("LastRun").forGetter(Saved::lastRun)
		).apply(instance, Saved::new));
	}

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		DataFormat.writeList(nbt, "Bytes", Saved.CODEC, agents.entrySet().stream().map(e -> new Saved(e.getKey(), e.getValue().dimension(),
				e.getValue().pos().asLong(), e.getValue().label(), e.getValue().computerId(), Optional.ofNullable(lastRun.get(e.getKey())))).toList());
		return nbt;
	}

	/** The registry as saved in {@code nbt}. */
	public static AgentRegistry fromNbt(CompoundTag nbt) {
		AgentRegistry registry = new AgentRegistry();
		for (Saved saved : DataFormat.readList(nbt, "Bytes", Saved.CODEC, "Byte")) {
			registry.agents.put(saved.player(), new Entry(saved.dimension(), BlockPos.of(saved.pos()), saved.label(), saved.computerId()));
			saved.lastRun().ifPresent(command -> registry.lastRun.put(saved.player(), command));
		}
		return registry;
	}
}
