package io.github.francescolv.mine;

import io.github.francescolv.mine.classroom.ClassCode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The class code, large, on the teacher's screen: to show it on the classroom projector, like
 * Minecraft Education's join code. Each symbol has its name under it, to say it aloud.
 */
final class ClassCodeShowScreen extends Screen {

	private static final int CELL = 80;

	private final List<Integer> code;
	private final String closesAt;

	/** {@code world}: the class world the code takes students to, "" for the main world. */
	ClassCodeShowScreen(List<Integer> code, long closesAt, String world) {
		super(world.isEmpty() ? Component.translatableWithFallback("mine.class.screen.title", "Class code")
				: Component.translatableWithFallback("mine.class.screen.title_world", "Class code for %s", world));
		this.code = code;
		this.closesAt = DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(closesAt).atZone(ZoneId.systemDefault()));
	}

	@Override
	protected void init() {
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(width / 2 - 50, top() + CELL + 50, 100, 20).build());
	}

	private int top() {
		return Math.max(50, height / 2 - 60);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		context.centeredText(font, title, width / 2, top() - 36, 0xFFFFFFFF);
		context.centeredText(font, Component.translatableWithFallback("mine.class.screen.show_help",
				"Students who aren't in the class yet click these symbols, in this order, when they join."), width / 2, top() - 22, 0xFFA0A0A0);
		int left = width / 2 - code.size() * CELL / 2;
		for (int i = 0; i < code.size(); i++) {
			int x = left + i * CELL;
			context.fill(x + 4, top(), x + CELL - 4, top() + CELL - 8, 0xC0000000);
			ClassCodeScreen.drawSymbol(context, code.get(i), x + 8, top() + 2, 4);
			context.centeredText(font, ClassCode.name(code.get(i)), x + CELL / 2, top() + CELL - 2, 0xFFFFFFFF);
		}
		context.centeredText(font, Component.translatableWithFallback("mine.class.screen.open_until",
				"Open until %s. Close it before then with /mine class close", closesAt), width / 2, top() + CELL + 20, 0xFFA0A0A0);
	}

	/** For the narrator: the title, then the symbols in order. */
	@Override
	public Component getNarrationMessage() {
		return CommonComponents.joinForNarration(title, ComponentUtils.formatList(code.stream().map(ClassCode::name).toList(),
				Component.literal(", ")));
	}
}
