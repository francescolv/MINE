package io.github.francescolv.mine.writing;

import io.github.francescolv.mine.board.BoardManager;
import io.github.francescolv.mine.npc.NpcManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.List;
import java.util.UUID;

/**
 * Teachers write NPCs and boards in MINE's window: one click, no book and quill to fetch, open in
 * the air and give back. A game without MINE keeps the book.
 */
public final class Writing {

	/** How far the NPC or the board may be when the window saves: it was in reach when it opened. */
	static final double SAVE_DISTANCE = 16;
	/** How far a board's text can be clicked: farther than an entity, boards hang high. */
	public static final double BOARD_REACH = 10;

	private Writing() {
	}

	public static void register() {
		WritingPayloads.register();
		ServerPlayNetworking.registerGlobalReceiver(WritingPayloads.Save.ID, (payload, context) ->
				save(context.player(), payload.kind(), payload.target(), payload.pages()));
		ServerPlayNetworking.registerGlobalReceiver(WritingPayloads.BoardClick.ID, (payload, context) -> {
			Entity entity = context.player().level().getEntity(payload.board());
			if (BoardManager.isBoard(entity) && entity.distanceToSqr(context.player()) <= (BOARD_REACH + 2) * (BOARD_REACH + 2)) {
				BoardManager.interact(context.player(), (Interaction) entity);
			}
		});
	}

	/** Opens the window, if the player's game has MINE: otherwise {@code false}, and the book it is. */
	public static boolean open(ServerPlayer player, WritingPayloads.Kind kind, UUID target, Component title, List<String> pages) {
		if (!ServerPlayNetworking.canSend(player, WritingPayloads.Open.ID)) {
			return false;
		}
		ServerPlayNetworking.send(player, new WritingPayloads.Open(kind, target, title, pages));
		return true;
	}

	/** What the window saved: written as a book given to the NPC or the board would be. */
	public static boolean save(ServerPlayer player, WritingPayloads.Kind kind, UUID target, List<String> written) {
		Entity entity = player.level().getEntity(target);
		if (entity == null || entity.distanceToSqr(player) > SAVE_DISTANCE * SAVE_DISTANCE) {
			return false;
		}
		List<String> pages = written.stream().limit(WritingPayloads.MAX_PAGES)
				.map(page -> page.length() > WritingPayloads.MAX_PAGE ? page.substring(0, WritingPayloads.MAX_PAGE) : page)
				.toList();
		return switch (kind) {
			case NPC -> NpcManager.isNpc(entity) && NpcManager.canChange(player, (Villager) entity)
					&& NpcManager.write(player, (Villager) entity, pages);
			case BOARD -> BoardManager.isBoard(entity) && BoardManager.canChange(player, (Interaction) entity)
					&& BoardManager.write(player, (Interaction) entity, pages);
		};
	}
}
