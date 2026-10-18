package io.github.francescolv.mine.text;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.francescolv.mine.MineMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Turns MINE's texts into plain strings on the server, in the student's language, for places
 * the client can't translate, such as the Agent's terminal. Reads MINE's own language files;
 * other keys use their fallback.
 */
public final class ServerLanguage {

	private static final String DEFAULT = "en_us";
	private static final Map<String, Map<String, String>> CACHE = new ConcurrentHashMap<>();

	private ServerLanguage() {
	}

	/** {@code text} in the language the player's game is set to. */
	public static String render(Component text, ServerPlayer player) {
		return render(text, player.clientInformation().language());
	}

	/** {@code text} in {@code language} (e.g. {@code it_it}), falling back to English. */
	public static String render(Component text, String language) {
		Map<String, String> lang = load(language.toLowerCase());
		Map<String, String> english = load(DEFAULT);
		StringBuilder out = new StringBuilder();
		append(out, text, lang, english);
		return out.toString();
	}

	private static void append(StringBuilder out, Component text, Map<String, String> lang, Map<String, String> english) {
		if (text.getContents() instanceof TranslatableContents translatable) {
			String key = translatable.getKey();
			String pattern = lang.getOrDefault(key, english.getOrDefault(key,
					translatable.getFallback() != null ? translatable.getFallback() : key));
			Object[] args = translatable.getArgs().clone();
			for (int i = 0; i < args.length; i++) {
				if (args[i] instanceof Component arg) {
					StringBuilder rendered = new StringBuilder();
					append(rendered, arg, lang, english);
					args[i] = rendered.toString();
				}
			}
			try {
				out.append(String.format(pattern, args));
			} catch (IllegalFormatException e) {
				out.append(pattern);
			}
		} else {
			text.getContents().visit(part -> {
				out.append(part);
				return Optional.empty();
			});
		}
		for (Component sibling : text.getSiblings()) {
			append(out, sibling, lang, english);
		}
	}

	private static Map<String, String> load(String language) {
		return CACHE.computeIfAbsent(language, code -> {
			Map<String, String> entries = new HashMap<>();
			try (InputStream in = ServerLanguage.class.getResourceAsStream("/assets/" + MineMod.MOD_ID + "/lang/" + code + ".json")) {
				if (in != null) {
					JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
					json.entrySet().forEach(entry -> entries.put(entry.getKey(), entry.getValue().getAsString()));
				}
			} catch (Exception e) {
				MineMod.LOGGER.warn("[MINE] Could not read the {} language file", code, e);
			}
			return entries;
		});
	}
}
