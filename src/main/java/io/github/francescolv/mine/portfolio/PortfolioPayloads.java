package io.github.francescolv.mine.portfolio;

import io.github.francescolv.mine.MineMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * What the portfolio window and the camera send between the student's game and the server. They
 * need MINE in the student's game: photos can only be taken there.
 */
public final class PortfolioPayloads {

	/** A packet from a game to the server can carry about 32 KB: photos go in pieces this big. */
	public static final int UPLOAD_CHUNK = 30_000;
	/** Largest photo the server keeps. */
	public static final int MAX_PHOTO = 1_000_000;
	public static final int MAX_CAPTION = 200;
	public static final int MAX_NOTE = 1_000;

	private PortfolioPayloads() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(Upload.ID, Upload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Request.ID, Request.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(View.ID, View.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Photo.ID, Photo.CODEC);
	}

	/** A piece ({@code index} of {@code total}) of photo number {@code upload}, a JPEG. */
	public record Upload(int upload, int index, int total, byte[] data) implements CustomPacketPayload {

		public static final Type<Upload> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "photo_upload"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Upload> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Upload::upload,
				ByteBufCodecs.VAR_INT, Upload::index,
				ByteBufCodecs.VAR_INT, Upload::total,
				ByteBufCodecs.byteArray(UPLOAD_CHUNK), Upload::data,
				Upload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	/** What the student does in the portfolio window. */
	public enum Action {
		/** Open the album (empty = one's own portfolio). */
		OPEN,
		/** Send the photo of an entry. */
		PHOTO,
		CAPTION,
		DELETE,
		/** Share an entry with the current class gallery. */
		SHARE,
		/** Add a note to the album. */
		NOTE
	}

	public record Request(Action action, String album, String entry, String text) implements CustomPacketPayload {

		public static final Type<Request> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "portfolio_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT.map(i -> Action.values()[Math.floorMod(i, Action.values().length)], Action::ordinal), Request::action,
				ByteBufCodecs.stringUtf8(64), Request::album,
				ByteBufCodecs.stringUtf8(64), Request::entry,
				ByteBufCodecs.stringUtf8(MAX_NOTE), Request::text,
				Request::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	/**
	 * An entry as the window shows it: {@code details} is already written in the reader's
	 * language (date, place, lesson); {@code editable} if the reader may change or delete it;
	 * {@code shared} if it is already in the class gallery.
	 */
	public record EntryView(String id, String author, boolean photo, String text, String details, boolean editable,
			boolean shared) {

		static final StreamCodec<RegistryFriendlyByteBuf, EntryView> CODEC = StreamCodec.ofMember(EntryView::write, EntryView::read);

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUtf(id, 64);
			buf.writeUtf(author, 64);
			buf.writeBoolean(photo);
			buf.writeUtf(text, MAX_NOTE);
			buf.writeUtf(details, 512);
			buf.writeBoolean(editable);
			buf.writeBoolean(shared);
		}

		private static EntryView read(RegistryFriendlyByteBuf buf) {
			return new EntryView(buf.readUtf(64), buf.readUtf(64), buf.readBoolean(), buf.readUtf(MAX_NOTE),
					buf.readUtf(512), buf.readBoolean(), buf.readBoolean());
		}
	}

	/**
	 * Opens (or refreshes) the portfolio window on an album, at entry {@code select} if given.
	 * {@code shareTo} is the gallery the reader can share their entries with (empty if none);
	 * {@code canNote} if they may add notes.
	 */
	public record View(String album, String title, boolean gallery, boolean canNote, String shareTo, String select,
			List<EntryView> entries) implements CustomPacketPayload {

		public static final Type<View> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "portfolio_view"));
		public static final StreamCodec<RegistryFriendlyByteBuf, View> CODEC = StreamCodec.ofMember(View::write, View::read);

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUtf(album);
			buf.writeUtf(title);
			buf.writeBoolean(gallery);
			buf.writeBoolean(canNote);
			buf.writeUtf(shareTo);
			buf.writeUtf(select);
			buf.writeVarInt(entries.size());
			entries.forEach(entry -> EntryView.CODEC.encode(buf, entry));
		}

		private static View read(RegistryFriendlyByteBuf buf) {
			String album = buf.readUtf();
			String title = buf.readUtf();
			boolean gallery = buf.readBoolean();
			boolean canNote = buf.readBoolean();
			String shareTo = buf.readUtf();
			String select = buf.readUtf();
			int size = buf.readVarInt();
			List<EntryView> entries = new ArrayList<>();
			for (int i = 0; i < size; i++) {
				entries.add(EntryView.CODEC.decode(buf));
			}
			return new View(album, title, gallery, canNote, shareTo, select, entries);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	/** The photo (a JPEG) of an entry, for the window to show. */
	public record Photo(String album, String entry, byte[] data) implements CustomPacketPayload {

		public static final Type<Photo> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "photo"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Photo> CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(64), Photo::album,
				ByteBufCodecs.stringUtf8(64), Photo::entry,
				ByteBufCodecs.byteArray(MAX_PHOTO), Photo::data,
				Photo::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}
}
