package io.github.francescolv.mine.lab;

import com.mojang.math.Transformation;
import io.github.francescolv.mine.DataFormat;
import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.board.BoardManager;
import io.github.francescolv.mine.npc.NpcManager;
import io.github.francescolv.mine.npc.Templates;
import io.github.francescolv.mine.permission.Permissions;
import io.github.francescolv.mine.world.ClassWorlds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The standard lab: the place for a class to do MINE's lessons, the same every time. One lane per
 * student, as the lesson guides ask (8 blocks apart, free space in front), in rows of ten; a
 * board at the end of each row, like a classroom blackboard; the guide NPC at the entrance.
 *
 * <p>The teacher builds it once in a flat world, standing where the lab starts and facing the way
 * students will face; then puts the students on their lanes, and clears everything they built
 * before the next class. Each world has its own lab: the main world's is saved in
 * {@code data/mine/lab.dat}, a class world's in {@code data/mine/lab_<name>.dat}.
 */
public final class Lab extends SavedData {

	public static final int MAX_LANES = 40;
	static final int PER_ROW = 10;
	static final int LANE_WIDTH = 8;
	/** From the student to the end of the lane: 12 free blocks in front of the Agent, and margin. */
	static final int LANE_LENGTH = 18;
	/** A row: the lanes, then room for the board. */
	static final int ROW_DEPTH = LANE_LENGTH + 6;
	/** Space cleared above the floor, for the tallest builds. */
	static final int HEIGHT = 16;
	static final String TAG = "mine_lab";

	private static final String KEY = "lab";
	private static final SavedDataType<Lab> TYPE = DataFormat.saved(KEY, Lab::new, Lab::fromNbt, Lab::save);
	private static final Identifier GUIDE = Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "lab_guide");
	private static final Identifier RULES = Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "lab");

	/** Where the lab is: the teacher's feet when it was built, and where students face. */
	@Nullable
	private ResourceKey<Level> dimension;
	private BlockPos origin = BlockPos.ZERO;
	private Direction facing = Direction.NORTH;
	private int lanes;
	/** Student → their lane (0-based), kept from one lesson to the next. */
	private final Map<UUID, Integer> assigned = new HashMap<>();

	/** A class world's lab: one type per world, made when first needed. */
	private static final Map<String, SavedDataType<Lab>> CLASS_TYPES = new java.util.concurrent.ConcurrentHashMap<>();

	/** The main world's lab. */
	public static Lab get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	/** The lab of the world {@code level} is: each class world has its own, the main world has {@link #get(MinecraftServer)}. */
	public static Lab get(ServerLevel level) {
		MinecraftServer server = level.getServer();
		ClassWorlds.World world = ClassWorlds.get(server).of(level);
		if (world == null) {
			return get(server);
		}
		return server.getDataStorage().computeIfAbsent(CLASS_TYPES.computeIfAbsent(world.name(),
				name -> DataFormat.saved(KEY + "_" + name, Lab::new, Lab::fromNbt, Lab::save)));
	}

	/** No lab any more (its world is being deleted): a new world with the same name starts without one. */
	public void forget() {
		dimension = null;
		lanes = 0;
		assigned.clear();
		setDirty();
	}

	public boolean exists() {
		return dimension != null && lanes > 0;
	}

	public int lanes() {
		return lanes;
	}

	public Direction facing() {
		return facing;
	}

	@Nullable
	public ServerLevel world(MinecraftServer server) {
		return dimension == null ? null : server.getLevel(dimension);
	}

	private Direction right() {
		return facing.getClockWise();
	}

	/** Where the student of lane {@code lane} stands (0-based): the Agent appears one block in front. */
	public BlockPos start(int lane) {
		int row = lane / PER_ROW;
		int column = lane % PER_ROW;
		return origin.relative(right(), 2 + column * LANE_WIDTH).relative(facing, 4 + row * ROW_DEPTH);
	}

	private int rows() {
		return (lanes + PER_ROW - 1) / PER_ROW;
	}

	/** The whole lab, floor included. */
	public BoundingBox area() {
		int width = Math.min(lanes, PER_ROW) * LANE_WIDTH + 2;
		int depth = 4 + rows() * ROW_DEPTH;
		BlockPos far = origin.relative(right(), width).relative(facing, depth).above(HEIGHT);
		return BoundingBox.fromCorners(origin.below(), far);
	}

	private boolean isLane(BlockPos pos) {
		for (int lane = 0; lane < lanes; lane++) {
			BlockPos start = start(lane);
			int along = alongFacing(pos) - alongFacing(start);
			int across = alongRight(pos) - alongRight(start);
			if (along >= -1 && along <= LANE_LENGTH && across >= -1 && across <= LANE_WIDTH - 2) {
				return true;
			}
		}
		return false;
	}

	private int alongFacing(BlockPos pos) {
		return pos.getX() * facing.getStepX() + pos.getZ() * facing.getStepZ();
	}

	private int alongRight(BlockPos pos) {
		return pos.getX() * right().getStepX() + pos.getZ() * right().getStepZ();
	}

	/**
	 * Builds the lab for {@code lanes} students from where the teacher stands, facing the way they
	 * face. A lab built before is replaced (its boards, NPC and labels removed, its lanes given again).
	 */
	public void build(ServerPlayer teacher, int lanes) {
		ServerLevel world = teacher.level();
		removeEntities(teacher.level().getServer());
		this.dimension = world.dimension();
		this.origin = teacher.blockPosition();
		this.facing = teacher.getDirection();
		this.lanes = lanes;
		// A new lab, a new arrangement: lanes are given again with /mine lab assign.
		assigned.clear();
		setDirty();
		clear(world);

		String language = teacher.clientInformation().language();
		for (int lane = 0; lane < lanes; lane++) {
			label(world, start(lane).relative(facing, LANE_LENGTH).above(2), String.valueOf(lane + 1));
		}
		Templates.BoardTemplate rules = Templates.board(RULES);
		for (int row = 0; row < rows(); row++) {
			int inRow = Math.min(PER_ROW, lanes - row * PER_ROW);
			BlockPos middle = origin.relative(right(), 1 + inRow * LANE_WIDTH / 2).relative(facing, 4 + row * ROW_DEPTH + LANE_LENGTH + 3).above(4);
			if (rules != null) {
				tag(BoardManager.create(world, middle, facing.getOpposite(), teacher.getUUID(), rules.size(),
						List.of(String.join("\n", rules.text(language)))));
			}
		}
		Templates.NpcTemplate guide = Templates.npc(GUIDE);
		if (guide != null) {
			tag(NpcManager.place(world, origin.relative(facing, 2), facing.getOpposite(), teacher.getUUID(), guide));
		}
		MineMod.LOGGER.info("[MINE] {} built a lab with {} lanes at {}", teacher.getName().getString(), lanes, origin.toShortString());
	}

	/** Floor and empty space: the lab as it is before the class arrives. Builds, Agents and dropped items go. */
	public void clear(ServerLevel world) {
		BoundingBox box = area();
		BlockState floor = Blocks.CONCRETE.white().defaultBlockState();
		BlockState lane = Blocks.CONCRETE.lightGray().defaultBlockState();
		BlockState start = Blocks.CONCRETE.lime().defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = box.minX(); x <= box.maxX(); x++) {
			for (int z = box.minZ(); z <= box.maxZ(); z++) {
				pos.set(x, origin.getY() - 1, z);
				world.setBlock(pos, isLane(pos) ? lane : floor, Block.UPDATE_CLIENTS);
				for (int y = origin.getY(); y <= box.maxY(); y++) {
					pos.setY(y);
					if (!world.getBlockState(pos).isAir()) {
						world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
		for (int i = 0; i < lanes; i++) {
			world.setBlock(start(i).below(), start, Block.UPDATE_CLIENTS);
		}
		world.getEntitiesOfClass(ItemEntity.class, AABB.of(box), item -> true).forEach(Entity::discard);
	}

	/** A large number above the end of a lane, facing the student. */
	private void label(ServerLevel world, BlockPos pos, String text) {
		Display.TextDisplay label = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, world);
		label.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, facing.getOpposite().toYRot(), 0);
		label.setBillboardConstraints(Display.BillboardConstraints.FIXED);
		label.setText(Component.literal(text).withStyle(ChatFormatting.BOLD));
		label.setBrightnessOverride(new Brightness(15, 15));
		label.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(4, 4, 4), new Quaternionf()));
		tag(label);
		world.addFreshEntity(label);
	}

	private static void tag(@Nullable Entity entity) {
		if (entity != null) {
			entity.addTag(TAG);
			if (entity instanceof Interaction board) {
				Display.TextDisplay display = BoardManager.display(board);
				if (display != null) {
					display.addTag(TAG);
				}
			}
		}
	}

	/** Removes what the lab put in the world besides blocks: labels, boards, the guide. */
	private void removeEntities(MinecraftServer server) {
		ServerLevel world = world(server);
		if (world == null || !exists()) {
			return;
		}
		world.getEntitiesOfClass(Entity.class, AABB.of(area()).inflate(4), entity -> entity.entityTags().contains(TAG))
				.forEach(Entity::discard);
	}

	/**
	 * Puts the students on their lanes, facing the right way, and makes it their spawn point.
	 * Students keep the lane they had; new ones get the first free lanes, by name. Returns the
	 * students left without a lane (more students than lanes).
	 */
	public List<ServerPlayer> assign(List<ServerPlayer> students) {
		ServerLevel world = world(students.isEmpty() ? null : students.getFirst().level().getServer());
		List<ServerPlayer> left = new ArrayList<>();
		if (world == null) {
			return students;
		}
		List<ServerPlayer> sorted = students.stream()
				.sorted(Comparator.comparing(student -> student.getName().getString(), String.CASE_INSENSITIVE_ORDER))
				.toList();
		// Lanes of students who aren't here today stay theirs.
		List<Integer> taken = new ArrayList<>(assigned.values());
		for (ServerPlayer student : sorted) {
			Integer lane = assigned.get(student.getUUID());
			if (lane == null || lane >= lanes) {
				lane = null;
				for (int i = 0; i < lanes && lane == null; i++) {
					if (!taken.contains(i)) {
						lane = i;
					}
				}
				if (lane == null) {
					left.add(student);
					continue;
				}
				assigned.put(student.getUUID(), lane);
				taken.add(lane);
				setDirty();
			}
			BlockPos start = start(lane);
			float yaw = facing.toYRot();
			student.teleportTo(world, start.getX() + 0.5, start.getY(), start.getZ() + 0.5, Set.of(), yaw, 0, true);
			student.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(world.dimension(), start, yaw, 0), true), false);
			student.sendOverlayMessage(Component.translatableWithFallback("mine.lab.your_lane", "This is your lane: number %s.", lane + 1)
					.withStyle(ChatFormatting.GREEN));
		}
		return left;
	}

	/**
	 * A student new to the class: their lane, as {@link #assign}, which is also where they come back
	 * to; but this first time they arrive at the entrance, in front of the guide. Returns whether
	 * they got a lane.
	 */
	public boolean welcome(ServerPlayer student) {
		boolean placed = assign(List.of(student)).isEmpty();
		ServerLevel world = world(student.level().getServer());
		if (world != null) {
			student.teleportTo(world, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, Set.of(), facing.toYRot(), 0, true);
		}
		return placed;
	}

	@Nullable
	public Integer laneOf(UUID student) {
		return assigned.get(student);
	}

	/** Teachers aren't given a lane. */
	public static List<ServerPlayer> students(MinecraftServer server) {
		return server.getPlayerList().getPlayers().stream().filter(player -> !Permissions.isTeacher(player)).toList();
	}

	/** The students in {@code level}: a class's lab is for the students in its world. */
	public static List<ServerPlayer> students(ServerLevel level) {
		return level.players().stream().filter(player -> !Permissions.isTeacher(player)).toList();
	}

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		if (dimension != null) {
			nbt.putString("Dimension", dimension.identifier().toString());
		}
		nbt.putIntArray("Origin", new int[] { origin.getX(), origin.getY(), origin.getZ() });
		nbt.putString("Facing", facing.getSerializedName());
		nbt.putInt("Lanes", lanes);
		ListTag list = new ListTag();
		assigned.forEach((student, lane) -> {
			CompoundTag tag = new CompoundTag();
			tag.store("Player", UUIDUtil.CODEC, student);
			tag.putInt("Lane", lane);
			list.add(tag);
		});
		nbt.put("Assigned", list);
		return nbt;
	}

	public static Lab fromNbt(CompoundTag nbt) {
		Lab lab = new Lab();
		if (nbt.contains("Dimension")) {
			lab.dimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse(nbt.getStringOr("Dimension", "")));
		}
		int[] origin = nbt.getIntArray("Origin").orElse(new int[0]);
		if (origin.length == 3) {
			lab.origin = new BlockPos(origin[0], origin[1], origin[2]);
		}
		Direction facing = Direction.byName(nbt.getStringOr("Facing", ""));
		lab.facing = facing != null && facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
		lab.lanes = nbt.getIntOr("Lanes", 0);
		ListTag list = nbt.getListOrEmpty("Assigned");
		for (int i = 0; i < list.size(); i++) {
			lab.assigned.put(list.getCompoundOrEmpty(i).read("Player", UUIDUtil.CODEC).orElseThrow(), list.getCompoundOrEmpty(i).getIntOr("Lane", 0));
		}
		return lab;
	}
}
