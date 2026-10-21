package io.github.francescolv.mine;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ByIdMap;

/** What MINE's payloads between server and client have in common. */
public final class MinePayloads {

	private MinePayloads() {
	}

	/** The type of one of MINE's payloads: {@code mine:<name>}. */
	public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name) {
		return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, name));
	}

	/** An enum by its position; a number that isn't one of them (a client sending anything) reads as the first. */
	public static <E extends Enum<E>> StreamCodec<ByteBuf, E> enumCodec(Class<E> type) {
		E[] values = type.getEnumConstants();
		return ByteBufCodecs.idMapper(ByIdMap.continuous(Enum::ordinal, values, ByIdMap.OutOfBoundsStrategy.ZERO), Enum::ordinal);
	}
}
