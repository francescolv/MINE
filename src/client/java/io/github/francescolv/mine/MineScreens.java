package io.github.francescolv.mine;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;

import java.util.function.Function;

/**
 * What MINE's windows have in common: their text colours and their buttons. Colours need their
 * alpha (0xFF...): text without it isn't drawn at all.
 */
final class MineScreens {

	/** Titles and labels. */
	static final int WHITE = 0xFFFFFFFF;
	/** Help and explanations. */
	static final int GRAY = 0xFFA0A0A0;
	static final int DARK_GRAY = 0xFF808080;
	static final int RED = 0xFFFF5555;
	static final int GREEN = 0xFF55FF55;
	static final int YELLOW = 0xFFFFFF55;
	static final int AQUA = 0xFF55FFFF;

	private MineScreens() {
	}

	/**
	 * Done (on the left) and Cancel, side by side and centred at {@code y}, each {@code width}
	 * wide; {@code add} is the window's addRenderableWidget.
	 */
	static void doneAndCancel(Function<Button, Button> add, int screenWidth, int y, int width, Button.OnPress done,
			Button.OnPress cancel) {
		add.apply(Button.builder(CommonComponents.GUI_DONE, done).bounds(screenWidth / 2 - width - 4, y, width, 20).build());
		add.apply(Button.builder(CommonComponents.GUI_CANCEL, cancel).bounds(screenWidth / 2 + 4, y, width, 20).build());
	}
}
