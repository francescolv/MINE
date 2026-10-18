package io.github.francescolv.mine;

import io.github.francescolv.mine.board.BoardHit;
import io.github.francescolv.mine.board.BoardManager;
import io.github.francescolv.mine.writing.Writing;
import io.github.francescolv.mine.writing.WritingPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * A right click on a board's text, anywhere on it and from a little farther than an entity can
 * be reached: boards hang high and wide, their clickable box is a thin column in the middle.
 * The server decides what the click does (MINE's window for whoever may change the board).
 */
public final class ClientBoards {

	private ClientBoards() {
	}

	/** Whether the click was on a board: then the server is told, and the game does nothing else with it. */
	public static boolean click(Minecraft client) {
		if (client.player == null || client.level == null || client.gui.screen() != null
				|| client.hitResult instanceof EntityHitResult
				|| !ClientPlayNetworking.canSend(WritingPayloads.BoardClick.ID)) {
			// On an entity (an NPC, the board's own box) the game's click goes to the server as it is.
			return false;
		}
		Vec3 eye = client.player.getEyePosition();
		Vec3 look = client.player.getViewVector(1);
		HitResult wall = client.level.clip(new ClipContext(eye, eye.add(look.scale(Writing.BOARD_REACH)),
				ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, client.player));
		double nearest = wall.getType() == HitResult.Type.MISS ? Writing.BOARD_REACH : wall.getLocation().distanceTo(eye);
		Interaction clicked = null;
		for (Entity entity : client.level.entitiesForRendering()) {
			UUID text = entity instanceof Interaction ? entity.getAttached(BoardManager.DISPLAY) : null;
			if (text == null || entity.distanceToSqr(eye) > 4 * Writing.BOARD_REACH * Writing.BOARD_REACH
					|| !(client.level.getEntity(text) instanceof Display.TextDisplay display)) {
				continue;
			}
			List<FormattedCharSequence> lines = client.font.split(display.getText(), display.getLineWidth());
			int width = lines.stream().mapToInt(client.font::width).max().orElse(0);
			double at = BoardHit.distance(eye, look, display.position(), display.getYRot(),
					entity.getAttachedOrElse(BoardManager.SIZE, 1f), width, lines.size(), nearest);
			if (at > 0) {
				nearest = at;
				clicked = (Interaction) entity;
			}
		}
		if (clicked == null) {
			return false;
		}
		ClientPlayNetworking.send(new WritingPayloads.BoardClick(clicked.getUUID()));
		return true;
	}
}
