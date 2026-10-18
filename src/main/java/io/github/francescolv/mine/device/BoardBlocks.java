package io.github.francescolv.mine.device;

import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.permission.Permissions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The two board blocks, the bridge between a student's board and redstone without writing code:
 * the {@linkplain BoardReceiverBlock receiver} gives redstone when the board says its key, the
 * {@linkplain BoardSenderBlock sender} tells the board when redstone reaches it. Each block
 * belongs to one student's board: whoever placed it, or whom a teacher chose.
 */
public final class BoardBlocks {

	public static final Block RECEIVER = block("board_receiver", BoardReceiverBlock::new);
	public static final Block SENDER = block("board_sender", BoardSenderBlock::new);
	public static final BlockEntityType<BoardBlockEntity> ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "board_block"),
			FabricBlockEntityTypeBuilder.create(BoardBlockEntity::new, RECEIVER, SENDER).build());

	/** How far a player can be to change a block, like the NPCs' buttons. */
	private static final double REACH = 8;

	/** The loaded board blocks, by the student whose board they belong to. Server thread only. */
	private static final Map<UUID, Set<BoardBlockEntity>> BLOCKS = new HashMap<>();
	/** Students whose board connected: when (server tick) their senders tell it how they are. */
	private static final Map<UUID, Integer> RESEND = new HashMap<>();
	/** A micro:bit or an Arduino restarts when MINE opens its port: about 2 seconds. */
	private static final int RESEND_DELAY = 60;

	private BoardBlocks() {
	}

	private static Block block(String name, java.util.function.Function<BlockBehaviour.Properties, Block> factory) {
		Identifier id = Identifier.fromNamespaceAndPath(MineMod.MOD_ID, name);
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(BlockBehaviour.Properties.of()
				.setId(blockKey).mapColor(MapColor.STONE).strength(0.5f).sound(SoundType.STONE)
				// Lit when on, like a small lamp: the student sees it working.
				.lightLevel(BoardBlocks::light)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}

	private static int light(BlockState state) {
		if (state.hasProperty(BoardReceiverBlock.POWER)) {
			return state.getValue(BoardReceiverBlock.POWER) > 0 ? 7 : 0;
		}
		return state.hasProperty(BoardSenderBlock.POWERED) && state.getValue(BoardSenderBlock.POWERED) ? 7 : 0;
	}

	public static void register() {
		// Loading the class registered the blocks; in the creative inventory they go with redstone.
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(output ->
				output.insertAfter(Items.TARGET, RECEIVER, SENDER));
		PayloadTypeRegistry.clientboundPlay().register(DevicePayloads.BlockOpen.ID, DevicePayloads.BlockOpen.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DevicePayloads.BlockSave.ID, DevicePayloads.BlockSave.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(DevicePayloads.BlockSave.ID, (payload, context) ->
				save(context.player(), payload));
		ServerTickEvents.END_SERVER_TICK.register(BoardBlocks::resend);
	}

	static void track(BoardBlockEntity entity) {
		if (entity.owner != null) {
			BLOCKS.computeIfAbsent(entity.owner, uuid -> new HashSet<>()).add(entity);
		}
	}

	static void untrack(BoardBlockEntity entity) {
		if (entity.owner != null) {
			Set<BoardBlockEntity> set = BLOCKS.get(entity.owner);
			if (set != null && set.remove(entity) && set.isEmpty()) {
				BLOCKS.remove(entity.owner);
			}
		}
	}

	/** A student placed a board block: it's theirs, and the window asks which key. */
	static void placed(Level level, BlockPos pos, @Nullable LivingEntity placer) {
		if (level.isClientSide() || !(placer instanceof ServerPlayer player)
				|| !(level.getBlockEntity(pos) instanceof BoardBlockEntity entity)) {
			return;
		}
		// An Agent's player (CC: Tweaked's own) has its owner's profile: the block is the owner's.
		entity.configure(player.getUUID(), player.getName().getString(), entity.key, entity.pulse);
		if (!player.getClass().getName().startsWith("dan200.computercraft")) {
			open(player, pos, null);
		}
	}

	/** The block's window: everyone sees it, its student and teachers can change it. */
	static void open(Player player, BlockPos pos, @Nullable Component error) {
		if (!(player instanceof ServerPlayer server) || !(server.level().getBlockEntity(pos) instanceof BoardBlockEntity entity)) {
			return;
		}
		if (!ServerPlayNetworking.canSend(server, DevicePayloads.BlockOpen.ID)) {
			server.sendSystemMessage(Component.translatableWithFallback("mine.board_block.info", "Board of %s, key «%s»",
					entity.ownerName.isEmpty() ? "-" : entity.ownerName, entity.key));
			return;
		}
		BlockState state = entity.getBlockState();
		int power = state.is(RECEIVER) ? state.getValue(BoardReceiverBlock.POWER) : Math.max(0, entity.sent);
		ServerPlayNetworking.send(server, new DevicePayloads.BlockOpen(pos, state.is(RECEIVER), entity.key, entity.ownerName,
				entity.pulse, power, canChange(server, entity), Permissions.isTeacher(server),
				entity.owner != null && Devices.isConnected(entity.owner), error == null ? Component.empty() : error));
	}

	private static boolean canChange(ServerPlayer player, BoardBlockEntity entity) {
		return Permissions.isTeacher(player) || entity.owner == null || entity.owner.equals(player.getUUID());
	}

	private static void save(ServerPlayer player, DevicePayloads.BlockSave payload) {
		BlockPos pos = payload.pos();
		if (!(player.level().getBlockEntity(pos) instanceof BoardBlockEntity entity)
				|| player.distanceToSqr(Vec3.atCenterOf(pos)) > REACH * REACH || !canChange(player, entity)) {
			return;
		}
		String key = payload.key().strip().toLowerCase(Locale.ROOT);
		if (!key.isEmpty() && !Devices.validKey(key)) {
			open(player, pos, Component.translatableWithFallback("mine.board_block.bad_key",
					"A key has only letters a-z, digits and _ (no spaces or accents), like pulsante_a"));
			return;
		}
		UUID owner = entity.owner;
		String ownerName = entity.ownerName;
		String wanted = payload.owner().strip();
		if (Permissions.isTeacher(player) && !wanted.isEmpty() && !wanted.equalsIgnoreCase(ownerName)) {
			ServerPlayer other = player.level().getServer().getPlayerList().getPlayerByName(wanted);
			if (other == null) {
				open(player, pos, Component.translatableWithFallback("mine.board_block.no_player",
						"%s isn't on the server now: the block can only be given to someone online", wanted));
				return;
			}
			owner = other.getUUID();
			ownerName = other.getName().getString();
		}
		entity.configure(owner, ownerName, key, payload.pulse());
		ServerLevel level = (ServerLevel) player.level();
		BlockState state = level.getBlockState(pos);
		if (state.is(RECEIVER)) {
			// The value the board already gave, if any: no need to wait for the next one.
			Object value = owner == null || key.isEmpty() ? null : Devices.read(owner, key);
			setPower(level, pos, state, entity, value == null || entity.pulse ? 0 : power(value));
		} else {
			senderChanged(level, pos);
		}
	}

	/** The power a value gives: numbers 0 to 15 (beyond, limited), "on"/"off" and similar words. */
	static int power(Object value) {
		if (value instanceof Integer number) {
			return Math.max(0, Math.min(15, number));
		}
		return switch (value.toString().toLowerCase(Locale.ROOT)) {
			case "on", "true", "yes", "si", "sì", "acceso", "vero" -> 15;
			default -> 0;
		};
	}

	/** A student's board said {@code key:value}: their receivers with that key follow it. */
	static void received(MinecraftServer server, UUID student, String key, Object value) {
		Set<BoardBlockEntity> receivers = BLOCKS.get(student);
		if (receivers == null) {
			return;
		}
		for (BoardBlockEntity entity : List.copyOf(receivers)) {
			if (entity.key.equals(key) && entity.getLevel() instanceof ServerLevel level) {
				BlockState state = entity.getBlockState();
				if (state.is(RECEIVER)) {
					setPower(level, entity.getBlockPos(), state, entity, power(value));
				}
			}
		}
	}

	private static void setPower(ServerLevel level, BlockPos pos, BlockState state, BoardBlockEntity entity, int power) {
		if (entity.pulse) {
			if (power == 0) {
				return;
			}
			power = 15;
			level.scheduleTick(pos, RECEIVER, BoardReceiverBlock.PULSE_TICKS);
		}
		if (state.getValue(BoardReceiverBlock.POWER) != power) {
			level.setBlock(pos, state.setValue(BoardReceiverBlock.POWER, power), Block.UPDATE_ALL);
		}
	}

	/** Redstone around a sender changed: if its power did, the board is told. */
	static void senderChanged(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(SENDER) || !(level.getBlockEntity(pos) instanceof BoardBlockEntity entity)) {
			return;
		}
		int power = level.getBestNeighborSignal(pos);
		if (state.getValue(BoardSenderBlock.POWERED) != power > 0) {
			level.setBlock(pos, state.setValue(BoardSenderBlock.POWERED, power > 0), Block.UPDATE_CLIENTS);
		}
		if (power != entity.sent && entity.owner != null && !entity.key.isEmpty()) {
			entity.sent = power;
			Devices.send(level.getServer(), entity.owner, entity.key, Integer.toString(power));
		}
	}

	/**
	 * A student's board just connected: a few seconds later (boards restart when the port opens),
	 * their senders tell it how they are now.
	 */
	static void connected(MinecraftServer server, UUID student) {
		RESEND.put(student, server.getTickCount() + RESEND_DELAY);
	}

	private static void resend(MinecraftServer server) {
		if (RESEND.isEmpty()) {
			return;
		}
		int now = server.getTickCount();
		RESEND.entrySet().removeIf(entry -> {
			if (entry.getValue() > now) {
				return false;
			}
			for (BoardBlockEntity entity : List.copyOf(BLOCKS.getOrDefault(entry.getKey(), Set.of()))) {
				if (entity.getLevel() instanceof ServerLevel level && entity.getBlockState().is(SENDER)) {
					entity.sent = -1;
					senderChanged(level, entity.getBlockPos());
				}
			}
			return true;
		});
	}
}
