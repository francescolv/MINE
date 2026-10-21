package io.github.francescolv.mine.writing;

import io.github.francescolv.mine.permission.Permissions;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * What NPCs and boards have in common: teachers and builders place them in the world, right-click
 * them, change them, and get their text in a book and quill.
 */
public final class Placed {

	private Placed() {
	}

	/**
	 * Right-clicking an entity that {@code is} one of them calls {@code interact}, once per click:
	 * one click can reach the server more than once (for each hand, and on some versions at a point
	 * and then on the entity), so it's answered once per tick. The entity never reacts by itself
	 * (a villager never opens its trades).
	 */
	public static <T extends Entity> void onClick(Class<T> type, Predicate<Entity> is, BiConsumer<ServerPlayer, T> interact) {
		// When each player last clicked one (game time).
		Map<UUID, Long> lastClick = new ConcurrentHashMap<>();
		UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (world.isClientSide() || !is.test(entity) || !(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}
			long now = world.getGameTime();
			if (hand == InteractionHand.MAIN_HAND && !Long.valueOf(now).equals(lastClick.put(serverPlayer.getUUID(), now))) {
				interact.accept(serverPlayer, type.cast(entity));
			}
			return InteractionResult.SUCCESS;
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> lastClick.remove(handler.getPlayer().getUUID()));
	}

	/** Two blocks in front of the player: where what they create goes, facing them. */
	public static BlockPos inFront(ServerPlayer player) {
		return player.blockPosition().relative(player.getDirection(), 2);
	}

	/** Teachers can change all of them; builders only the ones they made. */
	public static boolean canChange(ServerPlayer player, @Nullable UUID maker) {
		return Permissions.isTeacher(player) || (Permissions.isBuilder(player) && player.getUUID().equals(maker));
	}

	/** The one of {@code type} closest to the player within {@code distance} blocks, among those {@code wanted}. */
	@Nullable
	public static <T extends Entity> T nearest(ServerPlayer player, Class<T> type, double distance, Predicate<T> wanted) {
		return player.level().getEntitiesOfClass(type, player.getBoundingBox().inflate(distance), wanted).stream()
				.min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
				.orElse(null);
	}

	/** A book and quill with {@code pages}, named {@code name}, ready to be changed. */
	public static ItemStack book(List<String> pages, Component name) {
		ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
		book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(pages.stream().map(Filterable::passThrough).toList()));
		book.set(DataComponents.CUSTOM_NAME, name);
		return book;
	}
}
