package io.github.francescolv.mine;

import io.github.francescolv.mine.writing.WritingPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Writing an NPC's dialogue or a board's text, in one window: what a book and quill does, without
 * fetching one, opening it in the air and giving it back. An NPC's dialogue has pages (one page,
 * one screen); a board is a single text.
 */
final class TextEditScreen extends Screen {

	private final WritingPayloads.Kind kind;
	private final UUID target;
	private final List<String> pages;
	private int page;
	private MultiLineEditBox text;

	TextEditScreen(WritingPayloads.Open open) {
		super(open.title());
		this.kind = open.kind();
		this.target = open.target();
		this.pages = new ArrayList<>(open.kind() == WritingPayloads.Kind.BOARD
				? List.of(String.join("\n", open.pages())) : open.pages());
		if (pages.isEmpty()) {
			pages.add("");
		}
	}

	private boolean paged() {
		return kind == WritingPayloads.Kind.NPC;
	}

	@Override
	protected void init() {
		int top = 44;
		int bottom = paged() ? height - 56 : height - 32;
		text = MultiLineEditBox.builder().setX(width / 2 - 150).setY(top)
				.setPlaceholder(paged()
						? Component.translatableWithFallback("mine.writing.npc_placeholder", "What does the NPC say on this page?")
						: Component.translatableWithFallback("mine.writing.board_placeholder", "# A title\nThe board's text"))
				.build(font, 300, bottom - top, Component.empty());
		text.setCharacterLimit(WritingPayloads.MAX_PAGE);
		text.setValue(pages.get(page));
		text.setValueListener(value -> pages.set(page, value));
		addRenderableWidget(text);
		setInitialFocus(text);

		if (paged()) {
			int y = height - 50;
			addRenderableWidget(Button.builder(Component.literal("◀"), b -> turn(-1))
					.bounds(width / 2 - 150, y, 20, 20).build()).active = page > 0;
			addRenderableWidget(Button.builder(Component.literal("▶"), b -> turn(1))
					.bounds(width / 2 - 70, y, 20, 20).build()).active = page < pages.size() - 1;
			addRenderableWidget(Button.builder(Component.translatableWithFallback("mine.writing.add_page", "Add page"), b -> {
				pages.add(page + 1, "");
				turn(1);
			}).bounds(width / 2 - 46, y, 96, 20).build()).active = pages.size() < WritingPayloads.MAX_PAGES;
			addRenderableWidget(Button.builder(Component.translatableWithFallback("mine.writing.remove_page", "Remove page"), b -> {
				pages.remove(page);
				turn(page >= pages.size() ? -1 : 0);
			}).bounds(width / 2 + 54, y, 96, 20).build()).active = pages.size() > 1;
		}
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> save())
				.bounds(width / 2 - 150, height - 26, 148, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
				.bounds(width / 2 + 2, height - 26, 148, 20).build());
	}

	private void turn(int by) {
		page = Math.max(0, Math.min(pages.size() - 1, page + by));
		rebuildWidgets();
	}

	private void save() {
		ClientPlayNetworking.send(new WritingPayloads.Save(kind, target, List.copyOf(pages)));
		onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		context.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
		context.centeredText(font, paged()
				? Component.translatableWithFallback("mine.writing.npc_help",
						"One page, one screen. [text](/command) is a button; lines with [ ] and [x] make a question.")
				: Component.translatableWithFallback("mine.writing.board_help", "Lines starting with # are titles."),
				width / 2, 26, 0xFFA0A0A0);
		if (paged()) {
			context.centeredText(font, Component.translatableWithFallback("mine.writing.page", "%s / %s", page + 1, pages.size()),
					width / 2 - 100, height - 44, 0xFFFFFFFF);
		}
	}
}
