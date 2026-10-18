package io.github.francescolv.mine;

import io.github.francescolv.mine.device.DevicePayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * A board block's window, opened when it's placed or right-clicked: which key it listens to (or
 * writes), whose board, and for a receiver whether it works like a lever or like a button.
 * Students who don't own it see it without changing it. It fits small game windows too: texts
 * wrap, fields get narrower.
 */
final class BoardBlockScreen extends Screen {

	private static final int ROW = 24;
	private static final int LINE = 11;

	private final DevicePayloads.BlockOpen block;
	private EditBox key;
	private EditBox owner;
	private boolean pulse;
	/** The window's layout, made in init() from its size. */
	private int left;
	private int wide;
	private int labels;
	private int top;
	private int rowsTop;
	private int rowsEnd;

	BoardBlockScreen(DevicePayloads.BlockOpen block) {
		super(block.receiver()
				? Component.translatableWithFallback("block.mine.board_receiver", "Board Receiver")
				: Component.translatableWithFallback("block.mine.board_sender", "Board Sender"));
		this.block = block;
		this.pulse = block.pulse();
	}

	private Component help() {
		return block.receiver()
				? Component.translatableWithFallback("mine.board_block.receiver_help",
						"Gives redstone when the board writes key:value (1 to 15 on, 0 off).")
				: Component.translatableWithFallback("mine.board_block.sender_help",
						"When redstone reaches it, it writes key:power (0 to 15) on the board.");
	}

	private Component whose() {
		Component name = block.owner().isEmpty() ? Component.literal("-") : Component.literal(block.owner());
		return Component.translatableWithFallback("mine.board_block.whose", "Board of %s: %s", name,
				block.connected()
						? Component.translatableWithFallback("mine.board_block.connected", "connected")
						: Component.translatableWithFallback("mine.board_block.not_connected", "not connected (/mine device)"));
	}

	@Override
	protected void init() {
		wide = Math.min(320, width - 24);
		left = width / 2 - wide / 2;
		List<Component> names = new ArrayList<>(List.of(Component.translatableWithFallback("mine.board_block.key", "Key")));
		if (block.receiver()) {
			names.add(Component.translatableWithFallback("mine.board_block.works", "Works"));
		}
		if (block.teacher()) {
			names.add(Component.translatableWithFallback("mine.board_block.owner", "Board of"));
		}
		labels = names.stream().mapToInt(font::width).max().orElse(40) + 10;
		int fieldWidth = wide - labels;

		int rows = 1 + (block.receiver() ? 1 : 0) + (block.teacher() ? 1 : 0);
		int headLines = 1 + font.split(help(), wide).size() + font.split(whose(), wide).size();
		int needed = headLines * LINE + 10 + rows * ROW + 5 * LINE + 30;
		top = Math.max(8, (height - needed) / 2);
		rowsTop = top + headLines * LINE + 10;

		int y = rowsTop;
		key = new EditBox(font, left + labels, y, fieldWidth, 20, Component.translatableWithFallback("mine.board_block.key", "Key"));
		key.setMaxLength(32);
		key.setValue(key.getValue().isEmpty() ? block.key() : key.getValue());
		key.setEditable(block.canChange());
		key.setHint(Component.literal(block.receiver() ? "pulsante_a" : "led"));
		addRenderableWidget(key);
		y += ROW;
		if (block.receiver()) {
			Button mode = Button.builder(modeLabel(), button -> {
				pulse = !pulse;
				button.setMessage(modeLabel());
			}).bounds(left + labels, y, fieldWidth, 20).build();
			mode.active = block.canChange();
			addRenderableWidget(mode);
			y += ROW;
		}
		if (block.teacher()) {
			owner = new EditBox(font, left + labels, y, fieldWidth, 20, Component.translatableWithFallback("mine.board_block.owner", "Board of"));
			owner.setMaxLength(16);
			owner.setValue(block.owner());
			addRenderableWidget(owner);
			y += ROW;
		}
		rowsEnd = y;
		int buttons = height - 26;
		int half = Math.min(150, wide / 2 - 4);
		if (block.canChange()) {
			addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> save())
					.bounds(width / 2 - half - 4, buttons, half, 20).build());
			addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
					.bounds(width / 2 + 4, buttons, half, 20).build());
		} else {
			addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
					.bounds(width / 2 - half / 2, buttons, half, 20).build());
		}
		setInitialFocus(key);
	}

	private Component modeLabel() {
		return pulse
				? Component.translatableWithFallback("mine.board_block.pulse", "Like a button (1 s)")
				: Component.translatableWithFallback("mine.board_block.switch", "Like a lever");
	}

	private void save() {
		ClientPlayNetworking.send(new DevicePayloads.BlockSave(block.pos(), key.getValue(),
				owner == null ? "" : owner.getValue(), pulse));
		onClose();
	}

	/** {@code text} wrapped to the window, centred, from {@code y}; returns the y after it. */
	private int lines(GuiGraphicsExtractor context, Component text, int y, int color) {
		for (FormattedCharSequence line : font.split(text, wide)) {
			context.centeredText(font, line, width / 2, y, color);
			y += LINE;
		}
		return y;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		int y = lines(context, title, top, 0xFFFFFFFF);
		y = lines(context, help(), y, 0xFFA0A0A0);
		lines(context, whose(), y, block.connected() ? 0xFF55FF55 : 0xFFFFAA00);

		int row = rowsTop + 6;
		context.text(font, Component.translatableWithFallback("mine.board_block.key", "Key"), left, row, 0xFFFFFFFF);
		row += ROW;
		if (block.receiver()) {
			context.text(font, Component.translatableWithFallback("mine.board_block.works", "Works"), left, row, 0xFFFFFFFF);
			row += ROW;
		}
		if (owner != null) {
			context.text(font, Component.translatableWithFallback("mine.board_block.owner", "Board of"), left, row, 0xFFFFFFFF);
		}

		String name = key.getValue().isBlank() ? "…" : key.getValue().strip();
		Component example = block.receiver()
				? Component.translatableWithFallback("mine.board_block.receiver_example", "On the board: %s:1 switches it on, %s:0 off",
						name, name)
				: Component.translatableWithFallback("mine.board_block.sender_example", "The board reads %s:15 (powered) or %s:0", name, name);
		y = lines(context, example, rowsEnd + 6, 0xFFA0A0A0);
		Component now = block.receiver()
				? Component.translatableWithFallback("mine.board_block.power_now", "Redstone now: %s", block.power())
				: Component.translatableWithFallback("mine.board_block.sent_now", "Last told the board: %s", block.power());
		y = lines(context, now, y, 0xFFA0A0A0);
		if (!block.error().getString().isEmpty()) {
			lines(context, block.error(), y + 4, 0xFFFF5555);
		} else if (!block.canChange()) {
			lines(context, Component.translatableWithFallback("mine.board_block.not_yours",
					"Only %s and the teachers can change it.", block.owner()), y + 4, 0xFFFFAA00);
		}
	}

	/** For the narrator: the title, what the block does and whose board it is. */
	@Override
	public Component getNarrationMessage() {
		return CommonComponents.joinForNarration(title, help(), whose());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
