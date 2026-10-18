package io.github.francescolv.mine.device;

import io.github.francescolv.mine.MineMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Between a student's MINE, which reads the board on the USB port of their PC, and the server,
 * which hands what the board says to that student's Agent. One line of the board ({@code key:value})
 * is one {@link Value}.
 */
public final class DevicePayloads {

	private DevicePayloads() {
	}

	/** Open MINE's window for boards ({@code /mine device}). */
	public record Open() implements CustomPacketPayload {
		public static final Type<Open> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "device_open"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.unit(new Open());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	/** A board was connected ({@code port}: its name, for the teacher) or disconnected (empty). */
	public record Status(String port) implements CustomPacketPayload {
		public static final Type<Status> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "device_status"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Status> CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(128), Status::port, Status::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	/** What the board said: {@code key:value}, from the student's PC to the server. */
	public record Value(String key, String value) implements CustomPacketPayload {
		public static final Type<Value> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "device_value"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Value> CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(Devices.MAX_KEY), Value::key, ByteBufCodecs.stringUtf8(Devices.MAX_LINE), Value::value, Value::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	/** For the board: the student's MINE writes {@code key:value} on the port. */
	public record Send(String key, String value) implements CustomPacketPayload {
		public static final Type<Send> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "device_send"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Send> CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(Devices.MAX_KEY), Send::key, ByteBufCodecs.stringUtf8(Devices.MAX_LINE), Send::value, Send::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	/**
	 * A board block's window: which key, whose board, and whether this player can change it.
	 * {@code power}: a receiver's redstone now, or what a sender last told the board.
	 */
	public record BlockOpen(BlockPos pos, boolean receiver, String key, String owner, boolean pulse, int power,
			boolean canChange, boolean teacher, boolean connected, Component error) implements CustomPacketPayload {
		public static final Type<BlockOpen> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "device_block_open"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BlockOpen> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, BlockOpen::pos, ByteBufCodecs.BOOL, BlockOpen::receiver,
				ByteBufCodecs.stringUtf8(Devices.MAX_KEY), BlockOpen::key, ByteBufCodecs.stringUtf8(64), BlockOpen::owner,
				ByteBufCodecs.BOOL, BlockOpen::pulse, ByteBufCodecs.VAR_INT, BlockOpen::power,
				ByteBufCodecs.BOOL, BlockOpen::canChange, ByteBufCodecs.BOOL, BlockOpen::teacher,
				ByteBufCodecs.BOOL, BlockOpen::connected, ComponentSerialization.STREAM_CODEC, BlockOpen::error, BlockOpen::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	/** The block's window saved: the key, whose board (only teachers change it) and the button mode. */
	public record BlockSave(BlockPos pos, String key, String owner, boolean pulse) implements CustomPacketPayload {
		public static final Type<BlockSave> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "device_block_save"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BlockSave> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, BlockSave::pos, ByteBufCodecs.stringUtf8(64), BlockSave::key,
				ByteBufCodecs.stringUtf8(64), BlockSave::owner, ByteBufCodecs.BOOL, BlockSave::pulse, BlockSave::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}
}
