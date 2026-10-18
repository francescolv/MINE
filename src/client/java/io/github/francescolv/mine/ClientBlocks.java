package io.github.francescolv.mine;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.francescolv.mine.blocks.BlocksCommand;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

/**
 * The block editor from the game's side. It opens in the browser; while the student is there the
 * game doesn't pause, and a program they Run starts when they are back in the game, after a
 * countdown, so they see their Agent move. A key runs the last program again.
 */
final class ClientBlocks {

	/** How long a program waits for the student to come back to the game. */
	private static final int WAIT_TICKS = 60 * 20;
	private static final int COUNTDOWN_TICKS = 3 * 20;

	private static final KeyMapping RUN = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.mine.run_blocks",
			InputConstants.KEY_R, KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "mine"))));

	/** The program waiting to start, and for how many more ticks; then the countdown. */
	@Nullable
	private static String waiting;
	private static int waitLeft;
	private static int countdown = -1;

	private ClientBlocks() {
	}

	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(BlocksCommand.Open.ID, (payload, context) -> open(context.client(), payload));
		ClientPlayNetworking.registerGlobalReceiver(BlocksCommand.RunWhenBack.ID, (payload, context) -> {
			waiting = payload.name();
			waitLeft = WAIT_TICKS;
			countdown = -1;
		});
		ClientTickEvents.END_CLIENT_TICK.register(ClientBlocks::tick);
	}

	private static void open(Minecraft client, BlocksCommand.Open payload) {
		// In the browser the game loses focus: no pause menu, the world goes on (it is a server).
		client.options.pauseOnLostFocus = false;
		ServerData server = client.getCurrentServer();
		String host = server == null ? "localhost" : ServerAddress.parseString(server.ip).getHost();
		URI page = URI.create("http://" + (host.contains(":") ? "[" + host + "]" : host) + ":" + payload.port() + "/?code=" + payload.code());
		MineMod.LOGGER.info("[MINE] Opening the block editor at {}:{}", host, payload.port());
		Blaze3D.openUri(page);
	}

	private static void tick(Minecraft client) {
		if (client.player == null) {
			waiting = null;
			return;
		}
		while (RUN.consumeClick()) {
			waiting = null;
			ClientPlayNetworking.send(new BlocksCommand.Go(""));
		}
		if (waiting == null) {
			return;
		}
		if (countdown < 0) {
			if (client.isWindowActive() && client.gui.screen() == null) {
				countdown = COUNTDOWN_TICKS;
			} else if (--waitLeft <= 0) {
				client.gui.hud.setOverlayMessage(Component.translatableWithFallback("mine.blocks.run_cancelled",
						"%s didn't start: press %s to run it.", waiting, RUN.getTranslatedKeyMessage()).withStyle(ChatFormatting.GOLD), false);
				waiting = null;
			}
			return;
		}
		if (countdown % 20 == 0) {
			client.gui.hud.setOverlayMessage(Component.translatableWithFallback("mine.blocks.countdown", "%s starts in %s…",
					waiting, countdown / 20).withStyle(ChatFormatting.GREEN), false);
		}
		if (--countdown <= 0) {
			ClientPlayNetworking.send(new BlocksCommand.Go(waiting));
			client.gui.hud.setOverlayMessage(Component.translatableWithFallback("mine.blocks.go", "Go!").withStyle(ChatFormatting.GREEN), false);
			waiting = null;
			countdown = -1;
		}
	}
}
