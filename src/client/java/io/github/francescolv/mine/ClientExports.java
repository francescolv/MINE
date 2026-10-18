package io.github.francescolv.mine;

import io.github.francescolv.mine.npc.ExportPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Saves the files the server exports for the teacher (see {@link ExportPayload}) in their
 * Downloads folder, and says where in a window with buttons to open the file or its folder (and
 * in the chat, to find it again later).
 */
final class ClientExports {

	/** Downloads still arriving, by number. */
	private static final Map<Integer, ByteArrayOutputStream> DOWNLOADS = new HashMap<>();

	private static final Pattern XDG_DOWNLOAD = Pattern.compile("^XDG_DOWNLOAD_DIR=\"(.*)\"\\s*$");

	private ClientExports() {
	}

	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ExportPayload.ID, (payload, context) -> {
			Minecraft client = context.client();
			// Files arrive in pieces, in order: keep them until the last one.
			ByteArrayOutputStream content = payload.index() == 0 ? new ByteArrayOutputStream()
					: DOWNLOADS.get(payload.download());
			if (content == null) {
				return;
			}
			content.writeBytes(payload.content());
			if (payload.index() + 1 < payload.total()) {
				DOWNLOADS.put(payload.download(), content);
				client.gui.hud.setOverlayMessage(Component.translatableWithFallback("mine.export.progress", "Downloading %s: %s%%",
						payload.fileName(), 100 * (payload.index() + 1) / payload.total()), false);
				return;
			}
			DOWNLOADS.remove(payload.download());
			try {
				Path file = save(client, payload.fileName(), content.toByteArray());
				MineMod.LOGGER.info("[MINE] Saved {}", file);
				client.gui.hud.getChat().addClientSystemMessage(Component.translatableWithFallback("mine.export.downloaded",
						"File saved on your computer: %s %s", link(Component.literal(file.getFileName().toString()), file),
						link(Component.translatableWithFallback("mine.export.folder", "[open folder]"), file.getParent())).withStyle(ChatFormatting.GREEN));
				// The chat's links need the chat open: a window with buttons is clicked at once.
				client.gui.setScreen(new ExportScreen(file));
			} catch (IOException e) {
				MineMod.LOGGER.error("[MINE] Could not save {}", payload.fileName(), e);
				client.gui.hud.getChat().addClientSystemMessage(Component.translatableWithFallback("mine.export.download_failed",
						"The file couldn't be saved on your computer: %s", e.getMessage()).withStyle(ChatFormatting.RED));
			}
		});
	}

	private static Path save(Minecraft client, String fileName, byte[] content) throws IOException {
		// Only a plain name: the server doesn't choose where files go on the teacher's computer.
		String name = Path.of(fileName).getFileName().toString();
		if (name.isBlank() || name.startsWith(".")) {
			name = "mine-export.csv";
		}
		Path folder = downloads(client);
		Files.createDirectories(folder);
		Path file = folder.resolve(name);
		Files.write(file, content);
		return file;
	}

	/**
	 * The Downloads folder: the one set for the desktop on Linux (e.g. {@code ~/Scaricati} in
	 * Italian), else {@code ~/Downloads}, else {@code mine-exports} in the game folder.
	 */
	static Path downloads(Minecraft client) {
		Path home = Path.of(System.getProperty("user.home"));
		Path userDirs = home.resolve(".config").resolve("user-dirs.dirs");
		try {
			if (Files.isReadable(userDirs)) {
				for (String line : Files.readAllLines(userDirs)) {
					Matcher matcher = XDG_DOWNLOAD.matcher(line);
					if (matcher.matches()) {
						Path dir = Path.of(matcher.group(1).replace("$HOME", home.toString()));
						if (Files.isDirectory(dir)) {
							return dir;
						}
					}
				}
			}
		} catch (IOException | RuntimeException e) {
			MineMod.LOGGER.warn("[MINE] Could not read {}", userDirs, e);
		}
		for (Path dir : List.of(home.resolve("Downloads"))) {
			if (Files.isDirectory(dir)) {
				return dir;
			}
		}
		return client.gameDirectory.toPath().resolve("mine-exports");
	}

	/** A link that opens a file or folder, as Minecraft does for screenshots. */
	private static Component link(Component label, Path target) {
		return label.copy().withStyle(style -> style
				.withUnderlined(true)
				.withClickEvent(new ClickEvent.OpenFile(target.toAbsolutePath().toString()))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(target.toAbsolutePath().toString()))));
	}
}
