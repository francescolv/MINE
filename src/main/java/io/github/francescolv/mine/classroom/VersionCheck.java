package io.github.francescolv.mine.classroom;

import io.github.francescolv.mine.MineMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ConfigurationTask;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The server and the lab PCs must have the same MINE: lessons, the camera, the class code all
 * talk between the two. While joining (before the class code and the world), the server says its
 * version and the student's MINE answers with its own; a different one, or a MINE too old to
 * answer, is turned away with a message that says what to update.
 */
public final class VersionCheck {

	private VersionCheck() {
	}

	/** A MINE version: the server's (asking) or the student's (answering). */
	public record Version(String version) implements CustomPacketPayload {
		public static final Type<Version> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "version"));
		public static final StreamCodec<io.netty.buffer.ByteBuf, Version> CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(64), Version::version, Version::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	/** Before {@link JoinGate#register()}: joining checks the version first. */
	public static void register() {
		PayloadTypeRegistry.clientboundConfiguration().register(Version.ID, Version.CODEC);
		PayloadTypeRegistry.serverboundConfiguration().register(Version.ID, Version.CODEC);
		ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
			if (ServerConfigurationNetworking.canSend(handler, Version.ID)) {
				handler.addTask(new Ask());
			} else {
				handler.disconnect(problem(MineMod.VERSION, null));
			}
		});
		ServerConfigurationNetworking.registerGlobalReceiver(Version.ID, (payload, context) -> {
			Component problem = problem(MineMod.VERSION, payload.version());
			if (problem == null) {
				context.packetListener().completeTask(Ask.KEY);
			} else {
				MineMod.LOGGER.info("[MINE] A player with MINE {} tried to join (the server has {})", payload.version(), MineMod.VERSION);
				context.packetListener().disconnect(problem);
			}
		});
	}

	/**
	 * What's wrong between the server's MINE and the student's ({@code null}: a MINE too old to
	 * say its version), or {@code null} if they are the same.
	 */
	@Nullable
	public static Component problem(String server, @Nullable String student) {
		if (server.equals(student)) {
			return null;
		}
		if (student == null) {
			return Component.translatableWithFallback("mine.version.old",
					"This server has MINE %s, your Minecraft has an older one.\n\nImport the MINE %s modpack in your launcher, or ask your teacher.",
					server, server);
		}
		return Component.translatableWithFallback("mine.version.different",
				"This server has MINE %s, your Minecraft has MINE %s.\n\nServer and computers need the same version: import the MINE %s modpack in your launcher, or ask your teacher.",
				server, student, server);
	}

	/** Joining waits here until the student's MINE says its version. */
	private record Ask() implements ConfigurationTask {
		static final Type KEY = new Type(MineMod.MOD_ID + ":version");

		@Override
		public void start(Consumer<Packet<?>> sender) {
			sender.accept(ServerConfigurationNetworking.createClientboundPacket(new Version(MineMod.VERSION)));
		}

		@Override
		public Type type() {
			return KEY;
		}
	}
}
