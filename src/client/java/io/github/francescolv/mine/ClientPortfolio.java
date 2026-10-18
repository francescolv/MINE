package io.github.francescolv.mine;

import io.github.francescolv.mine.portfolio.PortfolioPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Opens the portfolio window when the server sends an album, and gives it the photos it asks for. */
final class ClientPortfolio {

	private ClientPortfolio() {
	}

	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(PortfolioPayloads.View.ID, (view, context) -> {
			if (context.client().gui.screen() instanceof PortfolioScreen screen && screen.album().equals(view.album())) {
				screen.update(view);
			} else {
				if (context.client().gui.screen() instanceof PortfolioScreen old) {
					old.dispose();
				}
				context.client().gui.setScreen(new PortfolioScreen(view));
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(PortfolioPayloads.Photo.ID, (photo, context) -> {
			if (context.client().gui.screen() instanceof PortfolioScreen screen) {
				screen.photo(photo);
			}
		});
	}
}
