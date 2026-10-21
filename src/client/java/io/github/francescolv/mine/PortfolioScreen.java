package io.github.francescolv.mine;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.francescolv.mine.portfolio.PortfolioPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The portfolio window: a student's portfolio or a class gallery, one entry at a time, with
 * arrows to go through them. Photos are asked to the server when they are shown. Whoever may,
 * writes captions, adds notes, shares with the class gallery and deletes.
 */
final class PortfolioScreen extends Screen {

	private enum Mode { VIEW, CAPTION, NOTE }

	private record Photo(Identifier texture, int width, int height) {
	}

	private PortfolioPayloads.View view;
	private int index;
	private Mode mode = Mode.VIEW;
	private final Map<String, Photo> photos = new HashMap<>();
	private final Set<String> asked = new HashSet<>();
	@Nullable
	private EditBox caption;
	@Nullable
	private MultiLineEditBox note;

	PortfolioScreen(PortfolioPayloads.View view) {
		super(Component.literal(view.title()));
		this.view = view;
		this.index = select(view, view.select(), view.entries().size() - 1);
	}

	String album() {
		return view.album();
	}

	/** New contents from the server, e.g. after a change: stay on the same entry, or on the one it says. */
	void update(PortfolioPayloads.View newView) {
		String current = current() == null ? "" : current().id();
		this.view = newView;
		this.index = select(newView, newView.select().isEmpty() ? current : newView.select(), Math.min(index, newView.entries().size() - 1));
		this.mode = Mode.VIEW;
		rebuildWidgets();
	}

	private static int select(PortfolioPayloads.View view, String id, int otherwise) {
		for (int i = 0; i < view.entries().size(); i++) {
			if (view.entries().get(i).id().equals(id)) {
				return i;
			}
		}
		return Math.max(0, otherwise);
	}

	@Nullable
	private PortfolioPayloads.EntryView current() {
		return index >= 0 && index < view.entries().size() ? view.entries().get(index) : null;
	}

	/** The photo arrived: it becomes a texture to draw. */
	void photo(PortfolioPayloads.Photo photo) {
		if (!photo.album().equals(view.album()) || photos.containsKey(photo.entry())) {
			return;
		}
		try {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(photo.data()));
			if (image == null) {
				return;
			}
			NativeImage pixels = new NativeImage(image.getWidth(), image.getHeight(), false);
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					int rgb = image.getRGB(x, y);
					pixels.setPixel(x, y, 0xFF000000 | rgb);
				}
			}
			Identifier texture = Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "portfolio/" + photo.entry());
			Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(texture::toString, pixels));
			photos.put(photo.entry(), new Photo(texture, image.getWidth(), image.getHeight()));
		} catch (IOException | RuntimeException e) {
			MineMod.LOGGER.warn("[MINE] Could not show a photo", e);
		}
	}

	private void send(PortfolioPayloads.Action action, String entry, String text) {
		ClientPlayNetworking.send(new PortfolioPayloads.Request(action, view.album(), entry, text));
	}

	@Override
	protected void init() {
		caption = null;
		note = null;
		PortfolioPayloads.EntryView entry = current();
		int y = height - 26;
		if (mode == Mode.CAPTION && entry != null) {
			caption = new EditBox(font, width / 2 - 150, y - 26, 300, 20, Component.empty());
			caption.setMaxLength(PortfolioPayloads.MAX_CAPTION);
			caption.setValue(entry.text());
			addRenderableWidget(caption);
			setInitialFocus(caption);
			buttons(y, List.of(
					button("mine.portfolio.ui.save", "Save", b -> send(PortfolioPayloads.Action.CAPTION, entry.id(), caption.getValue())),
					Button.builder(CommonComponents.GUI_CANCEL, b -> setMode(Mode.VIEW))));
			return;
		}
		if (mode == Mode.NOTE) {
			note = MultiLineEditBox.builder().setX(width / 2 - 150).setY(30)
					.setPlaceholder(Component.translatableWithFallback("mine.portfolio.ui.note_placeholder", "What did you do? What did you learn?"))
					.build(font, 300, height - 70, Component.empty());
			note.setCharacterLimit(PortfolioPayloads.MAX_NOTE);
			if (entry != null && !entry.photo() && entry.editable() && editingNote) {
				note.setValue(entry.text());
			}
			addRenderableWidget(note);
			setInitialFocus(note);
			String id = editingNote && entry != null ? entry.id() : "";
			buttons(y, List.of(
					button("mine.portfolio.ui.save", "Save", b -> {
						if (id.isEmpty()) {
							send(PortfolioPayloads.Action.NOTE, "", note.getValue());
						} else {
							send(PortfolioPayloads.Action.CAPTION, id, note.getValue());
						}
					}),
					Button.builder(CommonComponents.GUI_CANCEL, b -> setMode(Mode.VIEW))));
			return;
		}

		// Arrows on the sides.
		Button previous = addRenderableWidget(arrow("<", "mine.portfolio.ui.previous", "Previous", -1)
				.bounds(4, height / 2 - 10, 20, 20).build());
		Button next = addRenderableWidget(arrow(">", "mine.portfolio.ui.next", "Next", 1)
				.bounds(width - 24, height / 2 - 10, 20, 20).build());
		previous.active = index > 0;
		next.active = index < view.entries().size() - 1;

		List<Button.Builder> row = new java.util.ArrayList<>();
		Button.Builder sharedSign = null;
		if (entry != null && entry.editable()) {
			row.add(button(entry.photo() ? "mine.portfolio.ui.caption" : "mine.portfolio.ui.edit",
					entry.photo() ? "Caption" : "Edit", b -> {
						editingNote = !entry.photo();
						setMode(entry.photo() ? Mode.CAPTION : Mode.NOTE);
					}));
			if (!view.shareTo().isEmpty()) {
				if (entry.shared()) {
					sharedSign = button("mine.portfolio.ui.shared", "Shared ✔", b -> { }).tooltip(Tooltip.create(
							Component.translatableWithFallback("mine.portfolio.ui.shared_tip", "Already in the class gallery %s", view.shareTo())));
				}
				row.add(entry.shared() ? sharedSign : button("mine.portfolio.ui.share", "Share", b -> {
							b.active = false;
							send(PortfolioPayloads.Action.SHARE, entry.id(), "");
						}).tooltip(Tooltip.create(Component.translatableWithFallback("mine.portfolio.ui.share_tip",
								"Share with the class gallery %s", view.shareTo()))));
			}
			row.add(button("mine.portfolio.ui.delete", "Delete", b -> minecraft.gui.setScreen(new ConfirmScreen(yes -> {
				if (yes) {
					send(PortfolioPayloads.Action.DELETE, entry.id(), "");
				}
				minecraft.gui.setScreen(this);
			}, Component.translatableWithFallback("mine.portfolio.ui.delete_title", "Delete this entry?"),
					Component.translatableWithFallback("mine.portfolio.ui.delete_message", "It can't be brought back.")))));
		}
		if (view.canNote()) {
			row.add(button("mine.portfolio.ui.new_note", "New note", b -> {
				editingNote = false;
				setMode(Mode.NOTE);
			}));
		}
		row.add(Button.builder(CommonComponents.GUI_DONE, b -> onClose()));
		List<Button> built = buttons(y, row);
		if (sharedSign != null) {
			// "Shared ✔" is only a sign: it can't be pressed again.
			built.get(row.indexOf(sharedSign)).active = false;
		}
	}

	private boolean editingNote;

	/** An arrow: the narrator and the tooltip say where it goes. */
	private Button.Builder arrow(String sign, String key, String fallback, int step) {
		Component name = Component.translatableWithFallback(key, fallback);
		return Button.builder(Component.literal(sign), b -> move(step))
				.tooltip(Tooltip.create(name))
				.createNarration(message -> Component.translatable("gui.narrate.button", name));
	}

	private static Button.Builder button(String key, String fallback, Button.OnPress action) {
		return Button.builder(Component.translatableWithFallback(key, fallback), action);
	}

	/** A row of buttons, centred, as wide as there is room for. */
	private List<Button> buttons(int y, List<Button.Builder> row) {
		int gap = 4;
		int width = Math.min(90, (this.width - 20) / row.size() - gap);
		int x = (this.width - (width + gap) * row.size() + gap) / 2;
		List<Button> built = new java.util.ArrayList<>();
		for (Button.Builder button : row) {
			built.add(addRenderableWidget(button.bounds(x, y, width, 20).build()));
			x += width + gap;
		}
		return built;
	}

	private void setMode(Mode mode) {
		this.mode = mode;
		rebuildWidgets();
	}

	private void move(int by) {
		index = Math.max(0, Math.min(view.entries().size() - 1, index + by));
		rebuildWidgets();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (mode == Mode.VIEW && (event.key() == 263 || event.key() == 262)) {
			move(event.key() == 263 ? -1 : 1);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		PortfolioPayloads.EntryView entry = current();
		Component heading = view.gallery()
				? Component.translatableWithFallback("mine.portfolio.ui.gallery_title", "Class gallery: %s", view.title())
				: Component.translatableWithFallback("mine.portfolio.ui.title", "Portfolio of %s", view.title());
		String counter = view.entries().isEmpty() ? "" : "  " + (index + 1) + "/" + view.entries().size();
		context.centeredText(font, heading.copy().append(counter), width / 2, 8, MineScreens.WHITE);
		if (mode == Mode.NOTE) {
			return;
		}
		if (entry == null) {
			context.centeredText(font, view.gallery()
					? Component.translatableWithFallback("mine.portfolio.ui.empty_gallery", "Nothing shared yet: students share photos from their portfolio.")
					: Component.translatableWithFallback("mine.portfolio.ui.empty", "Empty: take photos with the camera (/mine camera) or write a note."),
					width / 2, height / 2 - 4, 0xFFAAAAAA);
			return;
		}
		int top = 24;
		int bottom = height - (mode == Mode.CAPTION ? 84 : 58);
		int textLines = entry.photo() && !entry.text().isEmpty() ? 2 : 0;
		int areaBottom = bottom - textLines * 10 - 4;
		int areaWidth = width - 60;
		if (entry.photo()) {
			Photo photo = photos.get(entry.id());
			if (photo == null) {
				if (asked.add(entry.id())) {
					send(PortfolioPayloads.Action.PHOTO, entry.id(), "");
				}
				context.centeredText(font, Component.translatableWithFallback("mine.portfolio.ui.loading", "Loading…"),
						width / 2, (top + areaBottom) / 2, 0xFFAAAAAA);
			} else {
				double scale = Math.min((double) areaWidth / photo.width(), (double) (areaBottom - top) / photo.height());
				int w = (int) (photo.width() * scale);
				int h = (int) (photo.height() * scale);
				context.blit(RenderPipelines.GUI_TEXTURED, photo.texture(), (width - w) / 2, top, 0, 0, w, h, w, h);
				areaBottom = top + h + 4;
			}
			int y = areaBottom;
			for (FormattedCharSequence line : font.split(Component.literal(entry.text()), areaWidth).stream().limit(2).toList()) {
				context.centeredText(font, line, width / 2, y, MineScreens.WHITE);
				y += 10;
			}
		} else {
			// A note: written on a sheet of paper.
			int left = width / 2 - Math.min(160, areaWidth / 2);
			int right = width / 2 + Math.min(160, areaWidth / 2);
			context.fill(left - 6, top - 4, right + 6, areaBottom + 4, 0xFFF4EEDC);
			int y = top;
			for (FormattedCharSequence line : font.split(Component.literal(entry.text()), right - left)) {
				if (y > areaBottom - 9) {
					break;
				}
				context.text(font, line, left, y, 0xFF303030, false);
				y += 10;
			}
		}
		context.centeredText(font, Component.literal(entry.details()), width / 2, bottom + 2, 0xFF999999);
	}

	@Override
	public void onClose() {
		dispose();
		super.onClose();
	}

	/** Frees the photos' textures. */
	void dispose() {
		photos.values().forEach(photo -> Minecraft.getInstance().getTextureManager().release(photo.texture()));
		photos.clear();
		asked.clear();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
