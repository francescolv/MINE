package io.github.francescolv.mine;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * {@code /mine device}: the serial ports of the student's PC, to connect their board, and what
 * the board is saying, live. Problems are told here, in the window, not in the chat behind it.
 */
final class DeviceScreen extends Screen {

	private static final int MAX_PORTS = 4;
	private static final int ROW = 22;

	@Nullable
	private Component error;
	/** The port connected when the buttons were made. */
	@Nullable
	private String shownPort;

	DeviceScreen() {
		super(Component.translatableWithFallback("mine.device.title", "Your board"));
	}

	@Override
	protected void init() {
		int left = width / 2 - 155;
		int y = 64;
		String connected = ClientDevices.connectedPort();
		shownPort = connected;
		if (connected != null) {
			addRenderableWidget(Button.builder(Component.translatableWithFallback("mine.device.disconnect", "Disconnect"), button -> {
				ClientDevices.disconnect(true);
				error = null;
				rebuildWidgets();
			}).bounds(left, y, 150, 20).build());
		} else {
			List<ClientDevices.Choice> ports = ClientDevices.ports();
			for (ClientDevices.Choice choice : ports.subList(0, Math.min(MAX_PORTS, ports.size()))) {
				addRenderableWidget(Button.builder(Component.literal(ClientDevices.label(choice)), button -> {
					String problem = ClientDevices.connect(choice.name());
					error = problem == null ? null : Component.translatableWithFallback("mine.device.open_failed",
							"Can't open %s. On Linux the PC's user must be in the dialout group (see the guide).",
							choice.name());
					rebuildWidgets();
				}).bounds(left, y, 310, 20).build());
				y += ROW;
			}
			addRenderableWidget(Button.builder(Component.translatableWithFallback("mine.device.refresh", "Look again"),
					button -> rebuildWidgets()).bounds(left, y, 150, 20).build());
		}
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(width / 2 + 5, height - 28, 150, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		int left = width / 2 - 155;
		context.centeredText(font, title, width / 2, 14, 0xFFFFFFFF);
		String connected = ClientDevices.connectedPort();
		if (connected != null) {
			context.centeredText(font, Component.translatableWithFallback("mine.device.connected",
					"Connected: %s", connected), width / 2, 32, 0xFF55FF55);
			ClientDevices.BoardProgram program = ClientDevices.program();
			context.centeredText(font, program != null
					? Component.translatableWithFallback("mine.device.program", "On the board: %s (started at %s)",
							program.name(), program.since().toString())
					: Component.translatableWithFallback("mine.device.no_program",
							"To see its name here, the board's program writes programma:name when it starts."),
					width / 2, 46, program != null ? 0xFF55FFFF : 0xFF808080);
		} else if (ClientDevices.ports().isEmpty()) {
			context.centeredText(font, Component.translatableWithFallback("mine.device.none",
					"No board found: plug it in with its USB cable, then Look again."), width / 2, 32, 0xFFFFFF55);
		} else {
			context.centeredText(font, Component.translatableWithFallback("mine.device.choose",
					"Choose your board:"), width / 2, 32, 0xFFA0A0A0);
		}
		if (error != null) {
			context.centeredText(font, error, width / 2, height - 44, 0xFFFF5555);
		}
		if (connected == null) {
			return;
		}

		// What the board says (white) and what the Agent sends it (aqua), the newest last.
		int top = 92;
		int bottom = height - 64;
		context.fill(left - 4, top - 4, left + 314, bottom, 0xC0000000);
		List<ClientDevices.Line> lines = ClientDevices.log();
		int fit = Math.max(0, (bottom - top - 4) / 11);
		int y = top;
		if (lines.isEmpty()) {
			context.text(font, Component.translatableWithFallback("mine.device.waiting",
					"Waiting for the board to write a line key:value…"), left, y, 0xFFA0A0A0, false);
		}
		for (ClientDevices.Line line : lines.subList(Math.max(0, lines.size() - fit), lines.size())) {
			Component text = switch (line.kind()) {
				case IN -> Component.literal("→ " + line.text());
				case OUT -> Component.literal("← " + line.text());
				case IGNORED -> Component.translatableWithFallback("mine.device.ignored", "%s (not key:value: ignored)", line.text());
				case NOTE -> Component.translatableWithFallback("mine.device.unplugged", "The board was unplugged.");
			};
			int colour = switch (line.kind()) {
				case IN -> 0xFFFFFFFF;
				case OUT -> 0xFF55FFFF;
				case IGNORED -> 0xFF808080;
				case NOTE -> 0xFFFFFF55;
			};
			context.text(font, font.plainSubstrByWidth(text.getString(), 310), left, y, colour, false);
			y += 11;
		}
		String key = ClientDevices.lastKey();
		if (key != null) {
			context.centeredText(font, Component.translatableWithFallback("mine.device.hint",
					"Your Byte reads it with mine.device.read(\"%s\")", key), width / 2, bottom + 6, 0xFFA0A0A0);
		}
	}

	@Override
	public void tick() {
		// Unplugged while the window is open: show the ports again.
		if (!Objects.equals(shownPort, ClientDevices.connectedPort())) {
			rebuildWidgets();
		}
	}
}
