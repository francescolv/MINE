package io.github.francescolv.mine;

import io.github.francescolv.mine.portfolio.PortfolioPayloads;
import io.github.francescolv.mine.portfolio.Portfolios;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * The camera, in the student's game: right-click with it and the game takes a picture of what
 * the student sees, without the hotbar and the hand, like Minecraft Education's camera. The photo
 * is made smaller (JPEG, at most 1280 × 720), kept in the game's {@code screenshots/mine} folder,
 * and sent to the server for the portfolio.
 */
final class ClientCamera {

	private static final int MAX_WIDTH = 1280;
	private static final int MAX_HEIGHT = 720;
	private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

	/** Ticks left before the picture is taken: the interface must disappear from the screen first. */
	private static int countdown = -1;
	/** Whether the camera hid the HUD, to show it again after the photo. */
	private static boolean hidHud;
	private static int uploads;

	private ClientCamera() {
	}

	static void register() {
		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!world.isClientSide() || !Portfolios.isCamera(stack)) {
				return InteractionResult.PASS;
			}
			if (countdown < 0) {
				// The photo without the hotbar and the crosshair: hidden for a moment, as with F1.
				Minecraft client = Minecraft.getInstance();
				hidHud = !client.gui.hud.isHidden();
				if (hidHud) {
					client.gui.hud.toggle();
				}
				countdown = 2;
			}
			return InteractionResult.SUCCESS;
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (countdown < 0 || --countdown > 0) {
				return;
			}
			countdown = -1;
			if (client.gui.screen() == null && client.player != null) {
				capture(client);
			} else {
				showHud(client);
			}
		});
	}

	private static void showHud(Minecraft client) {
		if (hidHud && client.gui.hud.isHidden()) {
			client.gui.hud.toggle();
		}
		hidHud = false;
	}

	private static void capture(Minecraft client) {
		Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), image -> {
			showHud(client);
			try (image) {
				// ARGB: the JPEG's RGB image leaves the alpha out by itself.
				saved(client, image.getPixels(), image.getWidth(), image.getHeight());
			}
		});
	}

	private static void saved(Minecraft client, int[] pixels, int width, int height) {
		client.player.playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1, 1.5f);
		CompletableFuture.supplyAsync(() -> jpeg(pixels, width, height), Util.backgroundExecutor())
				.thenAcceptAsync(jpeg -> {
					if (jpeg == null) {
						return;
					}
					keep(client, jpeg);
					send(jpeg);
				}, client);
	}

	/** The photo as a JPEG no larger than the server keeps; {@code null} if Java can't write JPEGs. */
	static byte[] jpeg(int[] pixels, int width, int height) {
		BufferedImage full = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		full.setRGB(0, 0, width, height, pixels, 0, width);
		double scale = Math.min(1, Math.min((double) MAX_WIDTH / width, (double) MAX_HEIGHT / height));
		int w = Math.max(1, (int) Math.round(width * scale));
		int h = Math.max(1, (int) Math.round(height * scale));
		BufferedImage small = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = small.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics.drawImage(full, 0, 0, w, h, null);
		graphics.dispose();
		for (float quality : new float[] { 0.85f, 0.7f, 0.5f, 0.3f }) {
			try {
				byte[] jpeg = write(small, quality);
				if (jpeg.length <= PortfolioPayloads.MAX_PHOTO) {
					return jpeg;
				}
			} catch (IOException | RuntimeException e) {
				MineMod.LOGGER.error("[MINE] Could not make the photo", e);
				return null;
			}
		}
		return null;
	}

	private static byte[] write(BufferedImage image, float quality) throws IOException {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(bytes)) {
			writer.setOutput(out);
			ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(quality);
			writer.write(null, new IIOImage(image, null, null), param);
		} finally {
			writer.dispose();
		}
		return bytes.toByteArray();
	}

	/** A copy for the student, in the game's screenshots folder. */
	private static void keep(Minecraft client, byte[] jpeg) {
		try {
			Path folder = client.gameDirectory.toPath().resolve(Screenshot.SCREENSHOT_DIR).resolve("mine");
			Files.createDirectories(folder);
			Files.write(folder.resolve(FILE_TIME.format(LocalDateTime.now()) + ".jpg"), jpeg);
		} catch (IOException e) {
			MineMod.LOGGER.warn("[MINE] Could not keep a copy of the photo", e);
		}
	}

	private static void send(byte[] jpeg) {
		if (!ClientPlayNetworking.canSend(PortfolioPayloads.Upload.ID)) {
			return;
		}
		int upload = ++uploads;
		int chunk = PortfolioPayloads.UPLOAD_CHUNK;
		int total = (jpeg.length + chunk - 1) / chunk;
		for (int i = 0; i < total; i++) {
			ClientPlayNetworking.send(new PortfolioPayloads.Upload(upload, i, total,
					Arrays.copyOfRange(jpeg, i * chunk, Math.min(jpeg.length, (i + 1) * chunk))));
		}
	}
}
