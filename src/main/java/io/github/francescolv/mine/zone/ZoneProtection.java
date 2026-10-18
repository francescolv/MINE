package io.github.francescolv.mine.zone;

import io.github.francescolv.mine.permission.Permissions;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protected zones: only teachers change their blocks. Students can't break or place blocks in
 * them (nor pour water or lava, light fire, till or strip), and neither can their Agents;
 * explosions and fire don't touch them. Doors, levers, buttons and chests keep working: they
 * don't change the zone's blocks.
 *
 * <p>Breaking (players and Agents) and the Agents' placing are stopped here; a player's placing
 * and tools in {@code ServerPlayerGameModeMixin}, explosions in {@code ServerExplosionMixin},
 * fire in {@code FireBlockMixin}.
 */
public final class ZoneProtection {

	/** One notice every few seconds, also when a student keeps trying. */
	private static final long NOTICE_EVERY = 3000;
	private static final Map<UUID, Long> NOTIFIED = new ConcurrentHashMap<>();

	private ZoneProtection() {
	}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> mayChange(player, level, pos));
		// Agents place through this event (their player is CC: Tweaked's own): players are checked later,
		// once doors and levers had their turn.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level.isClientSide() || !isAgent(player) || player.getItemInHand(hand).isEmpty()) {
				return InteractionResult.PASS;
			}
			return mayChange(player, level, hit.getBlockPos()) && mayChange(player, level, hit.getBlockPos().relative(hit.getDirection()))
					? InteractionResult.PASS : InteractionResult.FAIL;
		});
		// Buckets: what they'd pour or pick up, where the player looks.
		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (level.isClientSide() || !(stack.getItem() instanceof BucketItem)) {
				return InteractionResult.PASS;
			}
			HitResult hit = player.pick(player.blockInteractionRange(), 1.0f, true);
			if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
					&& !(mayChange(player, level, block.getBlockPos()) && mayChange(player, level, block.getBlockPos().relative(block.getDirection())))) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
	}

	/**
	 * Whether {@code player} may change the block at {@code pos}: anywhere outside protected
	 * zones; inside, only teachers (and their Agents). A student who may not is told.
	 */
	public static boolean mayChange(@Nullable Player player, Level level, BlockPos pos) {
		Zones.Zone zone = Zones.protectedAt(level, pos);
		if (zone == null) {
			return true;
		}
		if (player instanceof ServerPlayer server && Permissions.isTeacher(server)) {
			return true;
		}
		if (player instanceof ServerPlayer server && !isAgent(player)) {
			// The block the client already changed comes back.
			server.connection.send(new ClientboundBlockUpdatePacket(level, pos));
			long now = System.currentTimeMillis();
			Long last = NOTIFIED.get(player.getUUID());
			if (last == null || now - last > NOTICE_EVERY) {
				NOTIFIED.put(player.getUUID(), now);
				server.sendOverlayMessage(Component.translatableWithFallback("mine.zone.protected",
						"%s is a protected zone: its blocks can't be changed.", zone.name()).withStyle(ChatFormatting.GOLD));
			}
		}
		return false;
	}

	/** Explosions and fire: nobody may change a protected zone. */
	public static boolean isProtected(Level level, BlockPos pos) {
		return Zones.protectedAt(level, pos) != null;
	}

	/** An Agent's player: CC: Tweaked's own, which acts for the Agent's owner. */
	static boolean isAgent(Player player) {
		return player.getClass().getName().startsWith("dan200.computercraft");
	}
}
