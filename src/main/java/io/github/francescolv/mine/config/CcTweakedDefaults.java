package io.github.francescolv.mine.config;

import io.github.francescolv.mine.MineMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * On first start, applies classroom-friendly CC: Tweaked settings: turtles need no fuel,
 * so students start programming right away instead of hunting for coal.
 *
 * <p>This happens only once (remembered in {@code config/mine/cc-defaults-applied}): if the
 * administrator later changes the CC: Tweaked configuration, MINE leaves it alone.
 */
public final class CcTweakedDefaults {

	private static final Pattern NEED_FUEL = Pattern.compile("^(\\s*need_fuel\\s*=\\s*)true\\s*$", Pattern.MULTILINE);

	private CcTweakedDefaults() {
	}

	public static void apply() {
		Path configDir = FabricLoader.getInstance().getConfigDir();
		Path marker = configDir.resolve("mine").resolve("cc-defaults-applied");
		if (Files.exists(marker)) {
			return;
		}

		Path ccConfig = configDir.resolve("computercraft-server.toml");
		try {
			if (Files.exists(ccConfig)) {
				String toml = Files.readString(ccConfig);
				Matcher matcher = NEED_FUEL.matcher(toml);
				if (matcher.find()) {
					Files.writeString(ccConfig, matcher.replaceFirst("$1false"));
				}
			} else {
				// CC: Tweaked fills in all the other options when the server first starts.
				Files.writeString(ccConfig, "[turtle]\n\t#Set by MINE: turtles need no fuel.\n\tneed_fuel = false\n");
			}
			Files.createDirectories(marker.getParent());
			Files.writeString(marker, "Delete this file to make MINE apply its CC: Tweaked defaults again.\n");
			MineMod.LOGGER.info("[MINE] Classroom defaults applied to CC: Tweaked (turtles need no fuel).");
		} catch (IOException e) {
			MineMod.LOGGER.error("[MINE] Could not apply the CC: Tweaked defaults", e);
		}
	}
}
