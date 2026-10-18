package io.github.francescolv.mine.agent;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What MINE prepares on a student's Agent each time its shell is ready for a command
 * ({@code rom/autorun/mine.lua}): programs to copy onto its disk and items for its inventory,
 * such as the broken programs of a debugging lesson or the blocks a lesson step needs.
 *
 * <p>Programs are copied once per {@link Kit#filesToken}: a new token (e.g. the lesson was
 * restarted) copies them again, overwriting the old ones, and a new Agent, with an empty disk,
 * receives them again too. Items are given by the {@link Provider} itself, which must remember
 * not to give them twice.
 */
public final class AgentKit {

	/**
	 * Programs to copy (file name → contents) with the lines announcing them, shown only when
	 * they are copied; and other {@code lines} to show in the terminal, e.g. about items given.
	 */
	public record Kit(@Nullable String filesToken, Map<String, String> files, List<Component> filesLines, List<Component> lines) {
	}

	/** Prepares a student's Agent; called on the server thread. */
	@FunctionalInterface
	public interface Provider {

		/** May put items in the Agent's {@code inventory}; {@code selectedSlot} is the highlighted one. */
		@Nullable
		Kit prepare(MinecraftServer server, UUID owner, String language, Container inventory, int selectedSlot);
	}

	/** A line about what the student is doing (e.g. their lesson and step), shown when the Agent starts. */
	@FunctionalInterface
	public interface Status {

		@Nullable
		Component of(MinecraftServer server, UUID owner);
	}

	private static volatile Provider provider = (server, owner, language, inventory, selectedSlot) -> null;
	private static volatile Status status = (server, owner) -> null;

	private AgentKit() {
	}

	public static void setProvider(Provider newProvider) {
		provider = newProvider;
	}

	public static void setStatus(Status newStatus) {
		status = newStatus;
	}

	@Nullable
	static Component status(MinecraftServer server, UUID owner) {
		return status.of(server, owner);
	}

	@Nullable
	static Kit prepare(MinecraftServer server, UUID owner, String language, Container inventory, int selectedSlot) {
		return provider.prepare(server, owner, language, inventory, selectedSlot);
	}
}
