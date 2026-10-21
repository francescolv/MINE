package io.github.francescolv.mine;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/** Files MINE writes on the server: exports for the teacher, and folders it removes. */
public final class Exports {

	private Exports() {
	}

	/** Saves an export (quiz answers, portfolios) in the world's {@code mine/exports} folder; returns where. */
	public static Path save(MinecraftServer server, String fileName, byte[] content) throws IOException {
		Path folder = server.getWorldPath(LevelResource.ROOT).resolve("mine").resolve("exports");
		Files.createDirectories(folder);
		Path file = folder.resolve(fileName);
		Files.write(file, content);
		return file.normalize();
	}

	/** Deletes a folder with everything in it, if it exists. */
	public static void deleteFolder(Path folder) throws IOException {
		if (!Files.exists(folder)) {
			return;
		}
		try (Stream<Path> files = Files.walk(folder)) {
			List<Path> all = new ArrayList<>(files.toList());
			// What is inside first, the folder itself last.
			Collections.reverse(all);
			for (Path file : all) {
				Files.delete(file);
			}
		}
	}
}
