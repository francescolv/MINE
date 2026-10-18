package io.github.francescolv.mine.blocks;

import io.github.francescolv.mine.agent.PythonBridge;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who is using the block editor in their browser: each student gets a personal link, with a
 * secret code that stands for them while they are in the game. Programs sent with it go to their
 * Agent and nobody else's; the code is forgotten when they leave.
 */
public final class BlockSessions {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Map<String, UUID> BY_CODE = new ConcurrentHashMap<>();
	private static final Map<UUID, String> BY_PLAYER = new ConcurrentHashMap<>();

	private BlockSessions() {
	}

	static void register() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			forget(handler.getPlayer().getUUID());
			PythonBridge.forget(handler.getPlayer().getUUID());
		});
	}

	/** The student's code: the same until they leave the game. */
	public static String codeFor(UUID player) {
		return BY_PLAYER.computeIfAbsent(player, uuid -> {
			byte[] bytes = new byte[18];
			RANDOM.nextBytes(bytes);
			String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
			BY_CODE.put(code, uuid);
			return code;
		});
	}

	/** Who the code stands for, or {@code null}. */
	@Nullable
	public static UUID player(@Nullable String code) {
		return code == null ? null : BY_CODE.get(code);
	}

	public static void forget(UUID player) {
		String code = BY_PLAYER.remove(player);
		if (code != null) {
			BY_CODE.remove(code);
		}
	}
}
