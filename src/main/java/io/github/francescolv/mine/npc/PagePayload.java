package io.github.francescolv.mine.npc;

import io.github.francescolv.mine.MinePayloads;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Turns the book window just opened to {@code page}: Minecraft opens books at their first page,
 * and an NPC's dialogue must open again where the student answered. Only games with MINE
 * installed understand it.
 */
public record PagePayload(int page) implements CustomPacketPayload {

	public static final Type<PagePayload> ID = MinePayloads.type("book_page");
	public static final StreamCodec<RegistryFriendlyByteBuf, PagePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PagePayload::page, PagePayload::new);

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(ID, CODEC);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
