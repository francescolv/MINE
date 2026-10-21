package io.github.francescolv.mine.npc;

import io.github.francescolv.mine.MinePayloads;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A file the server sends to a teacher's game, which saves it on their computer (in their
 * Downloads folder): e.g. the quiz answers as CSV, or the portfolios as a zip. Only games with
 * MINE installed can receive it. A packet carries at most about 1 MB, so files go in pieces:
 * piece {@code index} of {@code total} of download number {@code download}.
 */
public record ExportPayload(int download, String fileName, int index, int total, byte[] content) implements CustomPacketPayload {

	public static final int CHUNK = 900_000;
	/** Larger files stay on the server only. */
	public static final int MAX_SIZE = 256 * 1024 * 1024;

	private static final AtomicInteger DOWNLOADS = new AtomicInteger();

	public static final Type<ExportPayload> ID = MinePayloads.type("export");
	public static final StreamCodec<RegistryFriendlyByteBuf, ExportPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ExportPayload::download,
			ByteBufCodecs.stringUtf8(255), ExportPayload::fileName,
			ByteBufCodecs.VAR_INT, ExportPayload::index,
			ByteBufCodecs.VAR_INT, ExportPayload::total,
			ByteBufCodecs.byteArray(CHUNK), ExportPayload::content,
			ExportPayload::new);

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(ID, CODEC);
	}

	/** Whether the player's game can receive the file (it has MINE, and the file isn't too large). */
	public static boolean canSend(ServerPlayer player, int size) {
		return size <= MAX_SIZE && ServerPlayNetworking.canSend(player, ID);
	}

	/** Sends the file to the player's game, in pieces. */
	public static void send(ServerPlayer player, String fileName, byte[] content) {
		int download = DOWNLOADS.incrementAndGet();
		int total = Math.max(1, (content.length + CHUNK - 1) / CHUNK);
		for (int i = 0; i < total; i++) {
			byte[] piece = Arrays.copyOfRange(content, i * CHUNK, Math.min(content.length, (i + 1) * CHUNK));
			ServerPlayNetworking.send(player, new ExportPayload(download, fileName, i, total, piece));
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
