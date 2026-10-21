package io.github.francescolv.mine;

import io.github.francescolv.mine.classroom.ClassCode;
import io.github.francescolv.mine.classroom.ClassCodePayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Typing the class code, while joining a server whose class the student isn't in yet: they click
 * the symbols their teacher shows, in order. The connection waits; the right code lets them in.
 *
 * <p>It opens over Minecraft's "Connecting" screen, which keeps the connection going (it ticks
 * it, and shows the server's goodbye): this window keeps ticking it underneath.
 */
final class ClassCodeScreen extends Screen {

	private static final int COLUMNS = 6;
	private static final int CELL = 36;

	private final List<Integer> typed = new ArrayList<>();
	private final List<Button> symbols = new ArrayList<>();
	private Button delete;
	private Button join;
	private Component message;
	private int messageColor = MineScreens.GRAY;
	private boolean waiting;
	private boolean gaveUp;
	@Nullable
	private final Screen connecting;

	ClassCodeScreen(@Nullable Screen connecting) {
		super(Component.translatableWithFallback("mine.class.screen.title", "Class code"));
		this.connecting = connecting;
		this.message = Component.translatableWithFallback("mine.class.screen.help",
				"You're not in this class yet: click the symbols your teacher shows you, in order.");
	}

	/** The server's answer: the code was wrong. */
	void wrong(int triesLeft) {
		typed.clear();
		waiting = false;
		message = Component.translatableWithFallback("mine.class.screen.wrong", "That's not the code: try again (tries left: %s).", triesLeft);
		messageColor = MineScreens.RED;
		update();
		minecraft.getNarrator().saySystemNow(message);
	}

	@Override
	protected void init() {
		symbols.clear();
		int left = width / 2 - COLUMNS * CELL / 2;
		int top = gridTop();
		for (int i = 0; i < ClassCode.SYMBOLS.size(); i++) {
			int symbol = i;
			Button button = Button.builder(Component.empty(), b -> type(symbol))
					.bounds(left + (i % COLUMNS) * CELL, top + (i / COLUMNS) * CELL, CELL - 2, CELL - 2)
					.tooltip(Tooltip.create(ClassCode.name(i)))
					// The button shows only the symbol's picture: the narrator says its name.
					.createNarration(message -> Component.translatable("gui.narrate.button", ClassCode.name(symbol)))
					.build();
			symbols.add(addRenderableWidget(button));
		}
		int y = top + 2 * CELL + 8;
		delete = addRenderableWidget(Button.builder(Component.translatableWithFallback("mine.class.screen.delete", "Delete"), b -> {
			if (!typed.isEmpty()) {
				typed.removeLast();
			}
			update();
		}).bounds(width / 2 - 154, y, 100, 20).build());
		join = addRenderableWidget(Button.builder(Component.translatableWithFallback("mine.class.screen.join", "Join"), b -> send())
				.bounds(width / 2 - 50, y, 100, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
				.bounds(width / 2 + 54, y, 100, 20).build());
		update();
	}

	private int slotsTop() {
		return Math.max(48, height / 2 - 70);
	}

	private int gridTop() {
		return slotsTop() + CELL + 14;
	}

	private void type(int symbol) {
		if (typed.size() < ClassCode.LENGTH) {
			typed.add(symbol);
			message = Component.translatableWithFallback("mine.class.screen.help",
					"You're not in this class yet: click the symbols your teacher shows you, in order.");
			messageColor = MineScreens.GRAY;
		}
		update();
	}

	private void send() {
		waiting = true;
		message = Component.translatableWithFallback("mine.class.screen.checking", "Checking the code...");
		messageColor = MineScreens.GRAY;
		ClientConfigurationNetworking.send(new ClassCodePayloads.Answer(List.copyOf(typed)));
		update();
	}

	private void update() {
		symbols.forEach(button -> button.active = !waiting && typed.size() < ClassCode.LENGTH);
		if (delete != null) {
			delete.active = !waiting && !typed.isEmpty();
			join.active = !waiting && typed.size() == ClassCode.LENGTH;
		}
	}

	/** For the narrator: the title, what to do, and the symbols typed so far. */
	@Override
	public Component getNarrationMessage() {
		Component typedNames = ComponentUtils.formatList(typed.stream().map(ClassCode::name).toList(), Component.literal(", "));
		return CommonComponents.joinForNarration(title, message, Component.translatableWithFallback("mine.class.screen.typed",
				"Typed: %s of %s", typed.size(), ClassCode.LENGTH), typedNames);
	}

	@Override
	public void tick() {
		if (connecting != null) {
			connecting.tick();
		}
	}

	/** Esc or Cancel: the student gives up, and the server says goodbye (the window stays until then). */
	@Override
	public void onClose() {
		if (!gaveUp) {
			gaveUp = true;
			waiting = true;
			update();
			ClientConfigurationNetworking.send(new ClassCodePayloads.Answer(List.of()));
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		context.centeredText(font, title, width / 2, slotsTop() - 34, MineScreens.WHITE);
		context.centeredText(font, message, width / 2, slotsTop() - 18, messageColor);
		// The code typed so far: four boxes.
		int left = width / 2 - ClassCode.LENGTH * CELL / 2;
		for (int i = 0; i < ClassCode.LENGTH; i++) {
			int x = left + i * CELL;
			context.fill(x, slotsTop(), x + CELL - 2, slotsTop() + CELL - 2, 0xFF000000);
			context.outline(x, slotsTop(), CELL - 2, CELL - 2, i == typed.size() ? MineScreens.WHITE : MineScreens.DARK_GRAY);
			if (i < typed.size()) {
				drawSymbol(context, typed.get(i), x + 1, slotsTop() + 1, 2);
			}
		}
		for (int i = 0; i < symbols.size(); i++) {
			Button button = symbols.get(i);
			drawSymbol(context, i, button.getX() + 1, button.getY() + 1, 2);
		}
	}

	/**
	 * A symbol drawn {@code scale} times the size of an item in the inventory, at its top left
	 * corner. MINE's own picture of the item, 256 pixels and smoothed: Minecraft's are 16 pixels,
	 * blocky when this large (and items can't be drawn yet while a student joins).
	 */
	static void drawSymbol(GuiGraphicsExtractor context, int symbol, int x, int y, int scale) {
		int size = 16 * scale;
		context.blit(RenderPipelines.GUI_TEXTURED, texture(symbol), x, y, 0, 0, size, size, size, size);
	}

	private static Identifier texture(int symbol) {
		String item = BuiltInRegistries.ITEM.getKey(ClassCode.SYMBOLS.get(symbol)).getPath();
		return Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "textures/gui/class_code/" + item + ".png");
	}
}
