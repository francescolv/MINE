package io.github.francescolv.mine;

import io.github.francescolv.mine.classroom.ClassCodePayloads;
import io.github.francescolv.mine.classroom.VersionCheck;
import io.github.francescolv.mine.npc.PagePayload;
import io.github.francescolv.mine.writing.WritingPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;

public class MineModClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		MineMod.LOGGER.info("[MINE] Client initialisation started.");
		ClientExports.register();
		ClientCamera.register();
		ClientPortfolio.register();
		// An NPC's dialogue opens again at the page where the student answered.
		ClientPlayNetworking.registerGlobalReceiver(PagePayload.ID, (payload, context) -> {
			if (context.client().gui.screen() instanceof BookViewScreen book) {
				book.setPage(payload.page());
			}
		});

		// Joining: the server checks that this MINE is the same version as its own.
		ClientConfigurationNetworking.registerGlobalReceiver(VersionCheck.Version.ID, (payload, context) ->
				context.responseSender().sendPacket(new VersionCheck.Version(MineMod.VERSION)));
		// Joining a class with its code: the window with the symbols, kept open between wrong tries.
		ClientConfigurationNetworking.registerGlobalReceiver(ClassCodePayloads.Ask.ID, (payload, context) ->
				context.client().execute(() -> {
					MineMod.LOGGER.info("[MINE] The server asks for the class code (wrong: {}, tries left: {})", payload.wrong(), payload.triesLeft());
					if (context.client().gui.screen() instanceof ClassCodeScreen screen) {
						if (payload.wrong()) {
							screen.wrong(payload.triesLeft());
						}
					} else {
						context.client().gui.setScreen(new ClassCodeScreen(context.client().gui.screen()));
					}
				}));
		// The teacher's /mine class code: the code large, for the projector.
		ClientPlayNetworking.registerGlobalReceiver(ClassCodePayloads.Show.ID, (payload, context) ->
				context.client().gui.setScreen(new ClassCodeShowScreen(payload.symbols(), payload.closesAt(), payload.world())));

		ClientBlocks.register();
		ClientDevices.register();
		// Writing an NPC or a board in MINE's window.
		ClientPlayNetworking.registerGlobalReceiver(WritingPayloads.Open.ID, (payload, context) ->
				context.client().gui.setScreen(new TextEditScreen(payload)));
	}
}
