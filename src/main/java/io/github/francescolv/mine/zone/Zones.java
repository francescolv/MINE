package io.github.francescolv.mine.zone;

import io.github.francescolv.mine.DataFormat;
import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.permission.Permissions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Zones: named boxes of a world that the teacher marks with two corner blocks: the corners mark
 * the area, as on a map, and the zone goes {@link #BELOW} blocks under them (the ground, holes) and
 * {@link #ABOVE} over them (what is built), or as the teacher says. A zone keeps a
 * snapshot of its blocks, taken when it is made (and again with {@code save}), and the teacher puts
 * it back as it was ({@code restore}): after a lesson, the village is whole again.
 *
 * <p>Agents are left out: a snapshot keeps an Agent's place as air, and a restore doesn't touch
 * the Agents that are there (otherwise it would make copies, or wipe one). MINE's NPCs and boards
 * are entities, and a restore leaves entities alone; only dropped items and experience go.
 *
 * <p>A zone can be {@link Zone#protect protected}: only teachers change its blocks
 * ({@link ZoneProtection}).
 *
 * <p>Zones are kept in {@code data/mine/zones.dat}, their snapshots in {@code <world>/mine/zones/<name>.nbt}.
 */
public final class Zones extends SavedData {

	public static final Pattern NAME = Pattern.compile("[a-z0-9_]{1,24}");
	/** Blocks in a zone at most: a restore is done in one go, so it must stay quick. */
	public static final int MAX_VOLUME = 1_000_000;
	/** The block that marks a zone's corners: easy to see, and not used for building much. */
	public static final net.minecraft.world.item.Item CORNER = Items.GLAZED_TERRACOTTA.magenta();
	private static final String CORNER_TAG = "mine_zone_corner";
	/** Put back without neighbour updates, drops or block side effects: as it was, not as it would react. */
	private static final int RESTORE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
	/** How far a zone goes under its lower corner, unless the teacher says otherwise: the ground. */
	public static final int BELOW = 10;
	/** How far a zone goes over its higher corner, unless the teacher says otherwise: what is built. */
	public static final int ABOVE = 30;
	/** How long the outline shows, in ticks. */
	private static final int OUTLINE_TICKS = 20 * 15;
	/** How long a corner's light column shows while the zone isn't made yet, in ticks. */
	private static final int COLUMN_TICKS = 20 * 60 * 10;
	/** How tall a corner's light column is: seen from far away. */
	private static final int COLUMN_HEIGHT = 24;

	private static final String KEY = "zones";
	private static final SavedDataType<Zones> TYPE = DataFormat.saved(KEY, Zones::new, Zones::fromNbt, Zones::save);

	public record Zone(String name, ResourceKey<Level> dimension, BoundingBox box, long created, boolean protect) {

		Zone withProtect(boolean on) {
			return new Zone(name, dimension, box, created, on);
		}

		public int volume() {
			return box.getXSpan() * box.getYSpan() * box.getZSpan();
		}

		public boolean contains(Level level, BlockPos pos) {
			return level.dimension().equals(dimension) && box.isInside(pos);
		}
	}

	public enum Result { DONE, BAD_NAME, EXISTS, NO_CORNERS, TOO_BIG, NO_ZONE, NO_WORLD, NO_SNAPSHOT, FAILED }

	private final Map<String, Zone> zones = new java.util.TreeMap<>();

	/** The corners a teacher has placed, oldest first (at most two), in the world they're in, and when. */
	private record Corner(ResourceKey<Level> dimension, BlockPos pos, int placed) {
	}

	private static final Map<UUID, List<Corner>> CORNERS = new ConcurrentHashMap<>();
	private record Outline(ServerPlayer player, ResourceKey<Level> dimension, BoundingBox box, int until) {
	}
	private static final Map<UUID, Outline> OUTLINES = new ConcurrentHashMap<>();
	private static int ticks;

	public static void register() {
		// A teacher placing a corner block marks a corner; the block is placed as usual, to be seen.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (level.isClientSide() || !(player instanceof ServerPlayer teacher) || !isCorner(stack) || !Permissions.isTeacher(teacher)) {
				return InteractionResult.PASS;
			}
			BlockPos pos = new BlockPlaceContext(player, hand, stack, hit).getClickedPos();
			corner(teacher, pos);
			return InteractionResult.PASS;
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (++ticks % 10 == 0 && !CORNERS.isEmpty()) {
				// A light column over each corner, for the teacher who placed it: seen from afar.
				CORNERS.forEach((uuid, corners) -> {
					ServerPlayer teacher = server.getPlayerList().getPlayer(uuid);
					if (teacher != null) {
						corners.stream()
								.filter(corner -> ticks - corner.placed() < COLUMN_TICKS && corner.dimension().equals(teacher.level().dimension()))
								.forEach(corner -> drawColumn(teacher, corner.pos()));
					}
				});
			}
			if (ticks % 10 == 0 && !OUTLINES.isEmpty()) {
				OUTLINES.values().removeIf(outline -> {
					if (ticks > outline.until() || outline.player().isRemoved()) {
						return true;
					}
					if (outline.player().level().dimension().equals(outline.dimension())) {
						drawOutline(outline.player(), outline.box());
					}
					return false;
				});
			}
		});
	}

	public static Zones get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public List<Zone> all() {
		return List.copyOf(zones.values());
	}

	@Nullable
	public Zone zone(String name) {
		return zones.get(name);
	}

	/** Two corner blocks for the teacher. */
	public static ItemStack corners() {
		ItemStack stack = new ItemStack(CORNER, 2);
		CompoundTag data = new CompoundTag();
		data.putBoolean(CORNER_TAG, true);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
		stack.set(DataComponents.ITEM_NAME, Component.translatableWithFallback("mine.zone.corner_name", "Zone corner"));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
				Component.translatableWithFallback("mine.zone.corner_lore", "Place one at each corner of the zone, then /mine zone create <name>")
						.withStyle(ChatFormatting.GRAY))));
		return stack;
	}

	public static boolean isCorner(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return stack.is(CORNER) && data != null && data.copyTag().getBooleanOr(CORNER_TAG, false);
	}

	/** A corner placed by the teacher: with two, the box between them shows. */
	public static void corner(ServerPlayer teacher, BlockPos pos) {
		List<Corner> list = new ArrayList<>(CORNERS.getOrDefault(teacher.getUUID(), List.of()));
		list.removeIf(corner -> !corner.dimension().equals(teacher.level().dimension()) || corner.pos().equals(pos));
		list.add(new Corner(teacher.level().dimension(), pos.immutable(), ticks));
		while (list.size() > 2) {
			list.removeFirst();
		}
		CORNERS.put(teacher.getUUID(), list);
		if (list.size() == 2) {
			BoundingBox box = box(teacher.level(), list.get(0).pos(), list.get(1).pos(), BELOW, ABOVE);
			int volume = box.getXSpan() * box.getYSpan() * box.getZSpan();
			outline(teacher, box);
			teacher.sendOverlayMessage(Component.translatableWithFallback("mine.zone.corners_set",
					"Zone %s × %s × %s: /mine zone create <name>", box.getXSpan(), box.getYSpan(), box.getZSpan())
					.withStyle(volume > MAX_VOLUME ? ChatFormatting.RED : ChatFormatting.GREEN));
		} else {
			teacher.sendOverlayMessage(Component.translatableWithFallback("mine.zone.corner_first",
					"First corner: now place the opposite one.").withStyle(ChatFormatting.GREEN));
		}
		drawColumn(teacher, pos);
	}

	/** A column of light over a corner: far particles, so the teacher sees it from a distance. */
	private static void drawColumn(ServerPlayer teacher, BlockPos pos) {
		for (int y = 0; y < COLUMN_HEIGHT; y++) {
			teacher.level().sendParticles(teacher, ParticleTypes.END_ROD, true, true, pos.getX() + 0.5, pos.getY() + 1 + y,
					pos.getZ() + 0.5, 1, 0, 0, 0, 0);
		}
	}

	/** Shows the box's edges to the player for a while. */
	public static void outline(ServerPlayer player, BoundingBox box) {
		OUTLINES.put(player.getUUID(), new Outline(player, player.level().dimension(), box, ticks + OUTLINE_TICKS));
		drawOutline(player, box);
	}

	private static void drawOutline(ServerPlayer player, BoundingBox box) {
		ServerLevel level = player.level();
		double x0 = box.minX(), y0 = box.minY(), z0 = box.minZ();
		double x1 = box.maxX() + 1, y1 = box.maxY() + 1, z1 = box.maxZ() + 1;
		double step = Math.max(1, Math.max(box.getXSpan(), Math.max(box.getYSpan(), box.getZSpan())) / 48.0);
		double[][] edges = {
				{x0, y0, z0, x1, y0, z0}, {x0, y1, z0, x1, y1, z0}, {x0, y0, z1, x1, y0, z1}, {x0, y1, z1, x1, y1, z1},
				{x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x0, y0, z1, x0, y1, z1}, {x1, y0, z1, x1, y1, z1},
				{x0, y0, z0, x0, y0, z1}, {x1, y0, z0, x1, y0, z1}, {x0, y1, z0, x0, y1, z1}, {x1, y1, z0, x1, y1, z1}};
		for (double[] edge : edges) {
			double length = Math.abs(edge[3] - edge[0]) + Math.abs(edge[4] - edge[1]) + Math.abs(edge[5] - edge[2]);
			for (double d = 0; d <= length; d += step) {
				double t = length == 0 ? 0 : d / length;
				level.sendParticles(player, ParticleTypes.END_ROD, true, false, edge[0] + (edge[3] - edge[0]) * t,
						edge[1] + (edge[4] - edge[1]) * t, edge[2] + (edge[5] - edge[2]) * t, 1, 0, 0, 0, 0);
			}
		}
	}

	/** The zone between two corners: their area, from {@code below} under the lower one to {@code above} over the higher one. */
	public static BoundingBox box(Level level, BlockPos a, BlockPos b, int below, int above) {
		BoundingBox corners = BoundingBox.fromCorners(a, b);
		return new BoundingBox(corners.minX(), Math.max(level.getMinY(), corners.minY() - below), corners.minZ(),
				corners.maxX(), Math.min(level.getMaxY(), corners.maxY() + above), corners.maxZ());
	}

	/** {@link #create(ServerPlayer, String, int, int)} with the usual depth and height. */
	public static Result create(ServerPlayer teacher, String name) {
		return create(teacher, name, BELOW, ABOVE);
	}

	/**
	 * Makes a zone from the teacher's two corners, from {@code below} under them to {@code above}
	 * over them, takes away the corner blocks and takes its first snapshot.
	 */
	public static Result create(ServerPlayer teacher, String name, int below, int above) {
		List<Corner> corners = CORNERS.getOrDefault(teacher.getUUID(), List.of());
		if (corners.size() < 2 || !corners.get(0).dimension().equals(teacher.level().dimension())) {
			return Result.NO_CORNERS;
		}
		ServerLevel level = teacher.level();
		for (Corner corner : corners) {
			if (level.getBlockState(corner.pos()).is(Blocks.GLAZED_TERRACOTTA.magenta())) {
				level.setBlockAndUpdate(corner.pos(), Blocks.AIR.defaultBlockState());
			}
		}
		Result result = create(level, name, box(level, corners.get(0).pos(), corners.get(1).pos(), below, above));
		if (result == Result.DONE) {
			CORNERS.remove(teacher.getUUID());
		}
		return result;
	}

	/** Makes the zone {@code name} over {@code box} in {@code level}, with its first snapshot. */
	public static Result create(ServerLevel level, String name, BoundingBox box) {
		Zones state = get(level.getServer());
		if (!NAME.matcher(name).matches()) {
			return Result.BAD_NAME;
		}
		if (state.zones.containsKey(name)) {
			return Result.EXISTS;
		}
		Zone zone = new Zone(name, level.dimension(), box, System.currentTimeMillis(), false);
		if (zone.volume() > MAX_VOLUME) {
			return Result.TOO_BIG;
		}
		state.zones.put(name, zone);
		state.setDirty();
		Result saved = save(level.getServer(), name);
		if (saved != Result.DONE) {
			state.zones.remove(name);
			return saved;
		}
		MineMod.LOGGER.info("[MINE] Zone {} made in {}: {}", name, level.dimension().identifier(), box);
		return Result.DONE;
	}

	/** Takes the zone's snapshot again: from now on, this is how it comes back. */
	public static Result save(MinecraftServer server, String name) {
		Zone zone = get(server).zones.get(name);
		if (zone == null) {
			return Result.NO_ZONE;
		}
		ServerLevel level = server.getLevel(zone.dimension());
		if (level == null) {
			return Result.NO_WORLD;
		}
		BoundingBox box = zone.box();
		Map<BlockState, Integer> palette = new HashMap<>();
		ListTag paletteTag = new ListTag();
		int[] blocks = new int[zone.volume()];
		ListTag entities = new ListTag();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int i = 0;
		for (int y = box.minY(); y <= box.maxY(); y++) {
			for (int z = box.minZ(); z <= box.maxZ(); z++) {
				for (int x = box.minX(); x <= box.maxX(); x++) {
					pos.set(x, y, z);
					BlockState block = level.getBlockState(pos);
					if (isAgent(block)) {
						block = Blocks.AIR.defaultBlockState();
					} else {
						BlockEntity entity = level.getBlockEntity(pos);
						if (entity != null) {
							CompoundTag tag = new CompoundTag();
							tag.put("Pos", new IntArrayTag(new int[] { x - box.minX(), y - box.minY(), z - box.minZ() }));
							tag.put("Nbt", entity.saveWithFullMetadata(level.registryAccess()));
							entities.add(tag);
						}
					}
					BlockState key = block;
					blocks[i++] = palette.computeIfAbsent(block, b -> {
						paletteTag.add(NbtUtils.writeBlockState(key));
						return paletteTag.size() - 1;
					});
				}
			}
		}
		CompoundTag snapshot = new CompoundTag();
		DataFormat.write(snapshot);
		snapshot.putInt("DataVersion", net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version());
		snapshot.put("Palette", paletteTag);
		snapshot.putIntArray("Blocks", blocks);
		snapshot.put("BlockEntities", entities);
		try {
			Path file = snapshot(server, name);
			Files.createDirectories(file.getParent());
			NbtIo.writeCompressed(snapshot, file);
		} catch (IOException e) {
			MineMod.LOGGER.warn("[MINE] Can't save the snapshot of zone {}: {}", name, e.toString());
			return Result.FAILED;
		}
		return Result.DONE;
	}

	/** Puts the zone back as it was in its snapshot, at once. Agents and entities stay; dropped items go. */
	public static Result restore(MinecraftServer server, String name) {
		Zone zone = get(server).zones.get(name);
		if (zone == null) {
			return Result.NO_ZONE;
		}
		ServerLevel level = server.getLevel(zone.dimension());
		if (level == null) {
			return Result.NO_WORLD;
		}
		CompoundTag snapshot;
		try {
			Path file = snapshot(server, name);
			if (!Files.isRegularFile(file)) {
				return Result.NO_SNAPSHOT;
			}
			snapshot = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
		} catch (IOException e) {
			MineMod.LOGGER.warn("[MINE] Can't read the snapshot of zone {}: {}", name, e.toString());
			return Result.FAILED;
		}
		BoundingBox box = zone.box();
		var blockLookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
		ListTag paletteTag = snapshot.getListOrEmpty("Palette");
		List<BlockState> palette = new ArrayList<>();
		for (int p = 0; p < paletteTag.size(); p++) {
			palette.add(NbtUtils.readBlockState(blockLookup, paletteTag.getCompoundOrEmpty(p)));
		}
		int[] blocks = snapshot.getIntArray("Blocks").orElse(new int[0]);
		if (blocks.length != zone.volume()) {
			return Result.FAILED;
		}
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int i = 0;
		for (int y = box.minY(); y <= box.maxY(); y++) {
			for (int z = box.minZ(); z <= box.maxZ(); z++) {
				for (int x = box.minX(); x <= box.maxX(); x++) {
					pos.set(x, y, z);
					BlockState wanted = palette.get(blocks[i++]);
					BlockState now = level.getBlockState(pos);
					if (isAgent(now) || now == wanted && level.getBlockEntity(pos) == null) {
						continue;
					}
					if (level.getBlockEntity(pos) != null) {
						// No contents spilled from what is replaced.
						level.removeBlockEntity(pos);
					}
					level.setBlock(pos, wanted, RESTORE_FLAGS);
				}
			}
		}
		ListTag entities = snapshot.getListOrEmpty("BlockEntities");
		for (int e = 0; e < entities.size(); e++) {
			CompoundTag tag = entities.getCompoundOrEmpty(e);
			int[] offset = tag.getIntArray("Pos").orElse(new int[0]);
			if (offset.length != 3) {
				continue;
			}
			BlockPos at = new BlockPos(box.minX() + offset[0], box.minY() + offset[1], box.minZ() + offset[2]);
			BlockEntity entity = level.getBlockEntity(at);
			if (entity != null) {
				entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag.getCompoundOrEmpty("Nbt")));
				entity.setChanged();
				level.sendBlockUpdated(at, entity.getBlockState(), entity.getBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		level.getEntitiesOfClass(Entity.class, AABB.of(box), entity -> entity instanceof ItemEntity || entity instanceof ExperienceOrb)
				.forEach(Entity::discard);
		MineMod.LOGGER.info("[MINE] Zone {} put back as it was", name);
		return Result.DONE;
	}

	/** Protects the zone, or not: {@code false} if there's no such zone. */
	public static boolean protect(MinecraftServer server, String name, boolean on) {
		Zones state = get(server);
		Zone zone = state.zones.get(name);
		if (zone == null) {
			return false;
		}
		state.zones.put(name, zone.withProtect(on));
		state.setDirty();
		MineMod.LOGGER.info("[MINE] Zone {} {}", name, on ? "protected" : "no longer protected");
		return true;
	}

	/** The protected zone {@code pos} is in, or {@code null}. */
	@Nullable
	public static Zone protectedAt(Level level, BlockPos pos) {
		if (level.getServer() == null) {
			return null;
		}
		for (Zone zone : get(level.getServer()).zones.values()) {
			if (zone.protect() && zone.contains(level, pos)) {
				return zone;
			}
		}
		return null;
	}

	/** Forgets a zone and its snapshot (the blocks stay as they are). */
	public static boolean remove(MinecraftServer server, String name) {
		Zones state = get(server);
		if (state.zones.remove(name) == null) {
			return false;
		}
		state.setDirty();
		try {
			Files.deleteIfExists(snapshot(server, name));
		} catch (IOException e) {
			MineMod.LOGGER.warn("[MINE] Can't delete the snapshot of zone {}: {}", name, e.toString());
		}
		return true;
	}

	/** Forgets the zones of a world that is being deleted. */
	public static void removeIn(MinecraftServer server, ResourceKey<Level> dimension) {
		for (Zone zone : get(server).all()) {
			if (zone.dimension().equals(dimension)) {
				remove(server, zone.name());
			}
		}
	}

	/** An Agent: a CC: Tweaked turtle. Snapshots leave them out. */
	static boolean isAgent(BlockState state) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		return "computercraft".equals(id.getNamespace()) && id.getPath().startsWith("turtle");
	}

	private static Path snapshot(MinecraftServer server, String name) {
		return server.getWorldPath(LevelResource.ROOT).resolve("mine").resolve("zones").resolve(name + ".nbt");
	}

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		ListTag list = new ListTag();
		for (Zone zone : zones.values()) {
			CompoundTag tag = new CompoundTag();
			tag.putString("Name", zone.name());
			tag.putString("Dimension", zone.dimension().identifier().toString());
			BoundingBox box = zone.box();
			tag.putIntArray("Box", new int[] { box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ() });
			tag.putLong("Created", zone.created());
			tag.putBoolean("Protect", zone.protect());
			list.add(tag);
		}
		nbt.put("Zones", list);
		return nbt;
	}

	public static Zones fromNbt(CompoundTag nbt) {
		Zones state = new Zones();
		ListTag list = nbt.getListOrEmpty("Zones");
		for (int i = 0; i < list.size(); i++) {
			CompoundTag tag = list.getCompoundOrEmpty(i);
			String name = tag.getStringOr("Name", "");
			int[] box = tag.getIntArray("Box").orElse(new int[0]);
			if (!NAME.matcher(name).matches() || box.length != 6) {
				continue;
			}
			ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse(tag.getStringOr("Dimension", "minecraft:overworld")));
			state.zones.put(name, new Zone(name, dimension, new BoundingBox(box[0], box[1], box[2], box[3], box[4], box[5]),
					tag.getLongOr("Created", 0), tag.getBooleanOr("Protect", false)));
		}
		return state;
	}
}
