package io.github.francescolv.mine.portfolio;

import io.github.francescolv.mine.ChunkAssembler;
import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.PlayerSounds;
import io.github.francescolv.mine.lesson.Lesson;
import io.github.francescolv.mine.lesson.LessonProgress;
import io.github.francescolv.mine.lesson.Lessons;
import io.github.francescolv.mine.permission.Permissions;
import io.github.francescolv.mine.text.ServerLanguage;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Portfolios and class galleries, like the Camera and Portfolio of Minecraft Education. Students
 * take photos with the camera (the game, with MINE, takes the picture and sends it to the server)
 * and write notes; each student's portfolio is seen by them and the teacher. The teacher creates
 * class galleries, where students share their photos with everybody.
 */
public final class Portfolios {

	/** Most entries kept in one album. */
	static final int MAX_ENTRIES = 300;
	/** Shortest time between two photos of a student, in milliseconds. */
	static final long PHOTO_COOLDOWN = 2_000;

	private static final String CAMERA_TAG = "mine_camera";
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	/** Photos still arriving, by student. */
	private static final Map<UUID, ChunkAssembler> UPLOADS = new HashMap<>();
	private static final Map<UUID, Long> LAST_PHOTO = new HashMap<>();

	private Portfolios() {
	}

	public static void register() {
		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			// In the game the camera is handled by MINE's client, which takes the photo.
			if (world.isClientSide() || !isCamera(stack)) {
				return InteractionResult.PASS;
			}
			// The student's MINE takes the photo and sends it.
			return InteractionResult.SUCCESS;
		});
		ServerPlayNetworking.registerGlobalReceiver(PortfolioPayloads.Upload.ID,
				(payload, context) -> upload(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(PortfolioPayloads.Request.ID,
				(payload, context) -> request(context.player(), payload));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> UPLOADS.remove(handler.getPlayer().getUUID()));
	}

	/** Whether the player's game has MINE: the camera and the portfolio window need it. */
	public static boolean hasMine(ServerPlayer player) {
		return ServerPlayNetworking.canSend(player, PortfolioPayloads.View.ID);
	}

	/** A camera: a spyglass that takes photos for the portfolio. */
	public static ItemStack camera() {
		ItemStack camera = new ItemStack(Items.SPYGLASS);
		CompoundTag data = new CompoundTag();
		data.putBoolean(CAMERA_TAG, true);
		camera.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
		camera.set(DataComponents.ITEM_NAME, Component.translatableWithFallback("mine.camera.name", "Camera"));
		camera.set(DataComponents.LORE, new ItemLore(List.of(
				Component.translatableWithFallback("mine.camera.lore", "Right-click: a photo for your portfolio")
						.withStyle(ChatFormatting.GRAY))));
		return camera;
	}

	public static boolean isCamera(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return stack.is(Items.SPYGLASS) && data != null && data.copyTag().getBooleanOr(CAMERA_TAG, false);
	}

	// ---- Photos ----

	private static void upload(ServerPlayer student, PortfolioPayloads.Upload piece) {
		int maxPieces = PortfolioPayloads.MAX_PHOTO / PortfolioPayloads.UPLOAD_CHUNK + 1;
		if (piece.total() < 1 || piece.total() > maxPieces || piece.index() >= piece.total()) {
			return;
		}
		byte[] photo = ChunkAssembler.receive(UPLOADS, student.getUUID(), piece.index(), piece.total(), piece.data(),
				PortfolioPayloads.MAX_PHOTO);
		if (photo != null) {
			addPhoto(student, photo);
		}
	}

	/** Saves a photo in the student's portfolio; {@code null} if it wasn't saved (and they are told why). */
	@Nullable
	public static PortfolioStore.Entry addPhoto(ServerPlayer student, byte[] jpeg) {
		long now = System.currentTimeMillis();
		if (!isJpeg(jpeg)) {
			MineMod.LOGGER.warn("[MINE] {} sent a photo that isn't a JPEG", student.getName().getString());
			return null;
		}
		if (now - LAST_PHOTO.getOrDefault(student.getUUID(), 0L) < PHOTO_COOLDOWN) {
			return null;
		}
		MinecraftServer server = student.level().getServer();
		PortfolioStore store = PortfolioStore.get(server);
		PortfolioStore.Album album = store.portfolio(student.getUUID(), student.getName().getString());
		if (album.entries().size() >= MAX_ENTRIES) {
			student.sendSystemMessage(Component.translatableWithFallback("mine.portfolio.full",
					"Your portfolio is full (%s entries): delete some to make room.", MAX_ENTRIES).withStyle(ChatFormatting.RED));
			return null;
		}
		PortfolioStore.Entry entry = newEntry(student, now, "");
		entry = entry.withPhoto(entry.id() + ".jpg");
		try {
			Path folder = PortfolioStore.folder(server, album.key());
			Files.createDirectories(folder);
			Files.write(folder.resolve(entry.photo()), jpeg);
		} catch (IOException e) {
			MineMod.LOGGER.error("[MINE] Could not save the photo of {}", student.getName().getString(), e);
			student.sendSystemMessage(Component.translatableWithFallback("mine.portfolio.photo_failed",
					"The photo couldn't be saved on the server.").withStyle(ChatFormatting.RED));
			return null;
		}
		LAST_PHOTO.put(student.getUUID(), now);
		store.add(album, entry);
		MineMod.LOGGER.info("[MINE] {} took a photo at {}", student.getName().getString(), student.blockPosition().toShortString());
		PlayerSounds.play(student, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1, 1.2f);
		student.sendOverlayMessage(Component.translatableWithFallback("mine.portfolio.photo_saved",
				"Photo saved in your portfolio (%s entries): /mine portfolio to see it and write a caption.",
				album.entries().size()).withStyle(ChatFormatting.GREEN));
		return entry;
	}

	static boolean isJpeg(byte[] data) {
		return data.length > 4 && data.length <= PortfolioPayloads.MAX_PHOTO
				&& (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8
				&& (data[data.length - 2] & 0xFF) == 0xFF && (data[data.length - 1] & 0xFF) == 0xD9;
	}

	/** A new entry by the student, where they are now and at the lesson step they are at. */
	static PortfolioStore.Entry newEntry(ServerPlayer author, long time, String text) {
		String id = Long.toString(time, 36) + Integer.toString(ThreadLocalRandom.current().nextInt(36 * 36 * 36), 36);
		LessonProgress.Entry progress = LessonProgress.get(author.level().getServer()).get(author.getUUID());
		boolean inLesson = progress != null && progress.status() == LessonProgress.Status.IN_PROGRESS;
		BlockPos pos = author.blockPosition();
		return new PortfolioStore.Entry(id, author.getUUID(), author.getName().getString(), time, "", text,
				author.level().dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(),
				inLesson ? progress.lesson() : "", inLesson ? progress.step() + 1 : 0);
	}

	// ---- What students and teachers may do ----

	/** Students read their own portfolio and the galleries; the teacher reads everything. */
	static boolean canRead(ServerPlayer player, PortfolioStore.Album album) {
		return album.isGallery() || Permissions.isTeacher(player)
				|| album.key().equals(PortfolioStore.studentKey(player.getUUID()));
	}

	/** Entries are changed and deleted by their author and by the teacher. */
	static boolean canEdit(ServerPlayer player, PortfolioStore.Entry entry) {
		return Permissions.isTeacher(player) || entry.author().equals(player.getUUID());
	}

	/** Notes are added by students to their portfolio, and by the teacher to galleries. */
	static boolean canNote(ServerPlayer player, PortfolioStore.Album album) {
		return album.isGallery() ? Permissions.isTeacher(player) : album.key().equals(PortfolioStore.studentKey(player.getUUID()));
	}

	// ---- The portfolio window ----

	/** Opens the album in the player's portfolio window, at entry {@code select} (empty: the last one). */
	public static boolean open(ServerPlayer player, PortfolioStore.Album album, String select) {
		if (!hasMine(player) || !canRead(player, album)) {
			return false;
		}
		PortfolioStore store = PortfolioStore.get(player.level().getServer());
		PortfolioStore.Album gallery = store.currentGallery();
		boolean own = album.key().equals(PortfolioStore.studentKey(player.getUUID()));
		Set<String> shared = new HashSet<>();
		if (own && gallery != null) {
			gallery.entries().forEach(entry -> shared.add(entry.id()));
		}
		List<PortfolioPayloads.EntryView> entries = new ArrayList<>();
		for (PortfolioStore.Entry entry : album.entries()) {
			entries.add(new PortfolioPayloads.EntryView(entry.id(), entry.authorName(), entry.isPhoto(), entry.text(),
					details(entry, album.isGallery(), player.clientInformation().language()), canEdit(player, entry),
					shared.contains(sharedId(entry))));
		}
		ServerPlayNetworking.send(player, new PortfolioPayloads.View(album.key(), album.title(), album.isGallery(),
				canNote(player, album), own && gallery != null ? gallery.title() : "", select, entries));
		return true;
	}

	/** {@code 2026-10-07 10:31 · Alex · Loops, step 2 · 12 64 -30}, in the reader's language. */
	public static String details(PortfolioStore.Entry entry, boolean withAuthor, String language) {
		List<String> parts = new ArrayList<>();
		parts.add(DATE.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(entry.time()), ZoneId.systemDefault())));
		if (withAuthor) {
			parts.add(entry.authorName());
		}
		Lesson lesson = entry.lesson().isEmpty() ? null : Lessons.get(entry.lesson());
		if (lesson != null) {
			parts.add(ServerLanguage.render(Component.translatableWithFallback("mine.portfolio.lesson_step", "%s, step %s",
					lesson.titleText(), entry.step()), language));
		}
		String place = entry.x() + " " + entry.y() + " " + entry.z();
		parts.add(entry.dimension().equals("minecraft:overworld") ? place : place + " (" + entry.dimension() + ")");
		return String.join(" · ", parts);
	}

	/** What a student does in the portfolio window. */
	public static void request(ServerPlayer player, PortfolioPayloads.Request request) {
		MinecraftServer server = player.level().getServer();
		PortfolioStore store = PortfolioStore.get(server);
		PortfolioStore.Album album = request.album().isEmpty()
				? store.portfolio(player.getUUID(), player.getName().getString())
				: store.album(request.album());
		if (album == null || !canRead(player, album)) {
			return;
		}
		PortfolioStore.Entry entry = store.entry(album, request.entry());
		try {
			switch (request.action()) {
				case OPEN -> open(player, album, request.entry());
				case PHOTO -> {
					if (entry != null && entry.isPhoto()) {
						sendPhoto(server, player, album.key(), entry);
					}
				}
				case CAPTION -> {
					if (entry != null && canEdit(player, entry)) {
						int max = entry.isPhoto() ? PortfolioPayloads.MAX_CAPTION : PortfolioPayloads.MAX_NOTE;
						String text = request.text().strip();
						store.replace(album, entry.withText(text.length() > max ? text.substring(0, max) : text));
						open(player, album, entry.id());
					}
				}
				case DELETE -> {
					if (entry != null && canEdit(player, entry)) {
						int index = album.entries().indexOf(entry);
						store.remove(server, album, entry);
						MineMod.LOGGER.info("[MINE] {} deleted an entry of {} by {}", player.getName().getString(),
								album.title(), entry.authorName());
						List<PortfolioStore.Entry> left = album.entries();
						open(player, album, left.isEmpty() ? "" : left.get(Math.min(index, left.size() - 1)).id());
					}
				}
				case SHARE -> {
					share(player, album, entry);
					// The window shows that it is shared: the message in the action bar is hidden behind it.
					open(player, album, request.entry());
				}
				case NOTE -> {
					String text = request.text().strip();
					if (canNote(player, album) && !text.isEmpty() && album.entries().size() < MAX_ENTRIES) {
						PortfolioStore.Entry note = newEntry(player, System.currentTimeMillis(),
								text.length() > PortfolioPayloads.MAX_NOTE ? text.substring(0, PortfolioPayloads.MAX_NOTE) : text);
						store.add(album, note);
						open(player, album, note.id());
					}
				}
			}
		} catch (IOException e) {
			MineMod.LOGGER.error("[MINE] Portfolio: {} failed", request.action(), e);
		}
	}

	/** The id of an entry's copy in a gallery: the same entry is shared only once. */
	static String sharedId(PortfolioStore.Entry entry) {
		return "s" + entry.id();
	}

	/** Sends a photo to the player's portfolio window: read from the disk away from the server thread. */
	private static void sendPhoto(MinecraftServer server, ServerPlayer player, String album, PortfolioStore.Entry entry) {
		Path file = PortfolioStore.folder(server, album).resolve(entry.photo());
		CompletableFuture.supplyAsync(() -> {
			try {
				return Files.isRegularFile(file) && Files.size(file) <= PortfolioPayloads.MAX_PHOTO ? Files.readAllBytes(file) : null;
			} catch (IOException e) {
				MineMod.LOGGER.warn("[MINE] Could not read the photo {}: {}", file, e.toString());
				return null;
			}
		}, Util.ioPool()).thenAcceptAsync(photo -> {
			if (photo != null && !player.hasDisconnected()) {
				ServerPlayNetworking.send(player, new PortfolioPayloads.Photo(album, entry.id(), photo));
			}
		}, server);
	}

	/** Copies an entry of the student's portfolio into the current class gallery, if it isn't there yet. */
	static boolean share(ServerPlayer player, PortfolioStore.Album album, @Nullable PortfolioStore.Entry entry) throws IOException {
		MinecraftServer server = player.level().getServer();
		PortfolioStore store = PortfolioStore.get(server);
		PortfolioStore.Album gallery = store.currentGallery();
		if (entry == null || gallery == null || album.isGallery() || !entry.author().equals(player.getUUID())
				|| gallery.entries().size() >= MAX_ENTRIES || store.entry(gallery, sharedId(entry)) != null) {
			return false;
		}
		String id = sharedId(entry);
		PortfolioStore.Entry copy = new PortfolioStore.Entry(id, entry.author(), entry.authorName(), entry.time(),
				entry.isPhoto() ? id + ".jpg" : "", entry.text(), entry.dimension(), entry.x(), entry.y(), entry.z(),
				entry.lesson(), entry.step());
		if (entry.isPhoto()) {
			Path target = PortfolioStore.folder(server, gallery.key());
			Files.createDirectories(target);
			Files.copy(PortfolioStore.folder(server, album.key()).resolve(entry.photo()), target.resolve(copy.photo()));
		}
		store.add(gallery, copy);
		player.sendOverlayMessage(Component.translatableWithFallback("mine.portfolio.shared", "Shared with the class gallery %s.",
				gallery.title()).withStyle(ChatFormatting.GREEN));
		return true;
	}
}
