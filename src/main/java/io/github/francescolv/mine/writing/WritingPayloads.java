package io.github.francescolv.mine.writing;

import io.github.francescolv.mine.MinePayloads;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.UUID;

/**
 * Writing an NPC's dialogue or a board's text in a window of MINE's, instead of a book and quill:
 * the server opens it with the text as it is ({@link Open}), the teacher saves it ({@link Save}).
 * And a click on a board's text, wherever it is, seen by MINE's client ({@link BoardClick}).
 */
public final class WritingPayloads {

	/** Pages the window takes, as many as a book and quill. */
	public static final int MAX_PAGES = 100;
	/** Characters on a page, as in a book and quill. */
	public static final int MAX_PAGE = 1024;

	private WritingPayloads() {
	}

	/** What is being written. */
	public enum Kind {
		NPC, BOARD
	}

	private static final StreamCodec<io.netty.buffer.ByteBuf, Kind> KIND = MinePayloads.enumCodec(Kind.class);
	private static final StreamCodec<io.netty.buffer.ByteBuf, List<String>> PAGES =
			ByteBufCodecs.stringUtf8(MAX_PAGE * 4).apply(ByteBufCodecs.list(MAX_PAGES));

	/** Opens the window: {@code target} is the NPC or the board. */
	public record Open(Kind kind, UUID target, Component title, List<String> pages) implements CustomPacketPayload {
		public static final Type<Open> ID = MinePayloads.type("writing_open");
		public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(
				KIND, Open::kind, UUIDUtil.STREAM_CODEC, Open::target, ComponentSerialization.STREAM_CODEC, Open::title,
				PAGES, Open::pages, Open::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	/** The text written in the window. */
	public record Save(Kind kind, UUID target, List<String> pages) implements CustomPacketPayload {
		public static final Type<Save> ID = MinePayloads.type("writing_save");
		public static final StreamCodec<RegistryFriendlyByteBuf, Save> CODEC = StreamCodec.composite(
				KIND, Save::kind, UUIDUtil.STREAM_CODEC, Save::target, PAGES, Save::pages, Save::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	/** The player right-clicked the text of {@code board}. */
	public record BoardClick(UUID board) implements CustomPacketPayload {
		public static final Type<BoardClick> ID = MinePayloads.type("board_click");
		public static final StreamCodec<RegistryFriendlyByteBuf, BoardClick> CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, BoardClick::board, BoardClick::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	static void register() {
		PayloadTypeRegistry.clientboundPlay().register(Open.ID, Open.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Save.ID, Save.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(BoardClick.ID, BoardClick.CODEC);
	}
}
