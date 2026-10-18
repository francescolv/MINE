package io.github.francescolv.mine;

import com.mojang.blaze3d.Blaze3D;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

/**
 * Says that an exported file has been saved on the teacher's computer, with buttons to open it or
 * its folder: links in the chat can only be clicked with the chat open, buttons here at once.
 */
final class ExportScreen extends Screen {

	private final Path file;

	ExportScreen(Path file) {
		super(Component.translatableWithFallback("mine.export.title", "File downloaded"));
		this.file = file;
	}

	@Override
	protected void init() {
		int y = height / 2 + 10;
		addRenderableWidget(Button.builder(Component.translatableWithFallback("mine.export.open_file", "Open file"), button -> {
			Blaze3D.openPath(file);
			onClose();
		}).bounds(width / 2 - 155, y, 100, 20).build());
		addRenderableWidget(Button.builder(Component.translatableWithFallback("mine.export.open_folder", "Open folder"), button -> {
			Blaze3D.openPath(file.getParent());
			onClose();
		}).bounds(width / 2 - 50, y, 100, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(width / 2 + 55, y, 100, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		context.centeredText(font, title, width / 2, height / 2 - 40, 0xFFFFFFFF);
		context.centeredText(font, Component.literal(file.getFileName().toString()), width / 2, height / 2 - 20, 0xFF55FF55);
		context.centeredText(font, Component.literal(file.getParent().toString()), width / 2, height / 2 - 8, 0xFFA0A0A0);
	}
}
