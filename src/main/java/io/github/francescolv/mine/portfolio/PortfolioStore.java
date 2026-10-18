package io.github.francescolv.mine.portfolio;

import io.github.francescolv.mine.DataFormat;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Students' portfolios and the class galleries, saved in the world ({@code data/mine/portfolio.dat});
 * the photos are files in the world folder, in {@code mine/portfolio/<album>/<entry>.jpg}.
 *
 * <p>An album is a student's portfolio (its key is their UUID) or a class gallery created by the
 * teacher (its key is {@code gallery-<name>}).
 */
public final class PortfolioStore extends SavedData {

	private static final String KEY = "portfolio";
	private static final SavedDataType<PortfolioStore> TYPE = DataFormat.saved(KEY, PortfolioStore::new, PortfolioStore::fromNbt, PortfolioStore::save);
	private static final String GALLERY_PREFIX = "gallery-";

	/**
	 * A photo (with its caption) or a note. {@code photo} is the photo's file name in the album's
	 * folder, empty for notes. Where and when it was made, and the lesson step the author was at
	 * ({@code lesson} empty if none).
	 */
	public record Entry(String id, UUID author, String authorName, long time, String photo, String text,
			String dimension, int x, int y, int z, String lesson, int step) {

		public boolean isPhoto() {
			return !photo.isEmpty();
		}

		Entry withText(String newText) {
			return new Entry(id, author, authorName, time, photo, newText, dimension, x, y, z, lesson, step);
		}

		Entry withPhoto(String newPhoto) {
			return new Entry(id, author, authorName, time, newPhoto, text, dimension, x, y, z, lesson, step);
		}
	}

	/** A portfolio or a gallery: {@code title} is the student's name or the gallery's. */
	public record Album(String key, String title, List<Entry> entries) {

		public boolean isGallery() {
			return key.startsWith(GALLERY_PREFIX);
		}
	}

	private final Map<String, Album> albums = new LinkedHashMap<>();
	/** The gallery students share their photos with; empty if there is none. */
	private String currentGallery = "";

	public static PortfolioStore get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public static String studentKey(UUID student) {
		return student.toString();
	}

	public static String galleryKey(String name) {
		return GALLERY_PREFIX + name;
	}

	/** Where the photos of an album are. */
	public static Path folder(MinecraftServer server, String album) {
		return server.getWorldPath(LevelResource.ROOT).resolve("mine").resolve("portfolio").resolve(album);
	}

	@Nullable
	public Album album(String key) {
		return albums.get(key);
	}

	/** The student's portfolio, created if they have none yet; its title follows their name. */
	public Album portfolio(UUID student, String name) {
		Album album = albums.get(studentKey(student));
		if (album == null || !album.title().equals(name)) {
			album = new Album(studentKey(student), name, album == null ? new ArrayList<>() : album.entries());
			albums.put(album.key(), album);
			setDirty();
		}
		return album;
	}

	public List<Album> portfolios() {
		return albums.values().stream().filter(album -> !album.isGallery()).toList();
	}

	public List<Album> galleries() {
		return albums.values().stream().filter(Album::isGallery).toList();
	}

	@Nullable
	public Album gallery(String name) {
		return albums.get(galleryKey(name));
	}

	/** Creates a gallery, or finds it if it exists; it becomes the one students share with. */
	public Album createGallery(String name) {
		Album album = albums.computeIfAbsent(galleryKey(name), key -> new Album(key, name, new ArrayList<>()));
		currentGallery = name;
		setDirty();
		return album;
	}

	@Nullable
	public Album currentGallery() {
		return currentGallery.isEmpty() ? null : gallery(currentGallery);
	}

	public void setCurrentGallery(String name) {
		currentGallery = name;
		setDirty();
	}

	/** Removes a gallery and its photos. */
	public boolean removeGallery(MinecraftServer server, String name) throws IOException {
		Album album = albums.remove(galleryKey(name));
		if (album == null) {
			return false;
		}
		if (currentGallery.equals(name)) {
			currentGallery = "";
		}
		setDirty();
		deleteFolder(folder(server, album.key()));
		return true;
	}

	public void add(Album album, Entry entry) {
		album.entries().add(entry);
		setDirty();
	}

	@Nullable
	public Entry entry(Album album, String id) {
		return album.entries().stream().filter(entry -> entry.id().equals(id)).findFirst().orElse(null);
	}

	public void replace(Album album, Entry entry) {
		album.entries().replaceAll(old -> old.id().equals(entry.id()) ? entry : old);
		setDirty();
	}

	/** Removes an entry and its photo. */
	public void remove(MinecraftServer server, Album album, Entry entry) throws IOException {
		album.entries().removeIf(old -> old.id().equals(entry.id()));
		setDirty();
		if (entry.isPhoto()) {
			Files.deleteIfExists(folder(server, album.key()).resolve(entry.photo()));
		}
	}

	private static void deleteFolder(Path folder) throws IOException {
		if (!Files.isDirectory(folder)) {
			return;
		}
		try (var files = Files.list(folder)) {
			for (Path file : files.toList()) {
				Files.deleteIfExists(file);
			}
		}
		Files.deleteIfExists(folder);
	}

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		ListTag list = new ListTag();
		for (Album album : albums.values()) {
			CompoundTag tag = new CompoundTag();
			tag.putString("Key", album.key());
			tag.putString("Title", album.title());
			ListTag entries = new ListTag();
			for (Entry entry : album.entries()) {
				CompoundTag e = new CompoundTag();
				e.putString("Id", entry.id());
				e.store("Author", UUIDUtil.CODEC, entry.author());
				e.putString("AuthorName", entry.authorName());
				e.putLong("Time", entry.time());
				e.putString("Photo", entry.photo());
				e.putString("Text", entry.text());
				e.putString("Dimension", entry.dimension());
				e.putIntArray("Pos", new int[] { entry.x(), entry.y(), entry.z() });
				e.putString("Lesson", entry.lesson());
				e.putInt("Step", entry.step());
				entries.add(e);
			}
			tag.put("Entries", entries);
			list.add(tag);
		}
		nbt.put("Albums", list);
		nbt.putString("CurrentGallery", currentGallery);
		return nbt;
	}

	public static PortfolioStore fromNbt(CompoundTag nbt) {
		PortfolioStore store = new PortfolioStore();
		ListTag list = nbt.getListOrEmpty("Albums");
		for (int i = 0; i < list.size(); i++) {
			CompoundTag tag = list.getCompoundOrEmpty(i);
			List<Entry> entries = new ArrayList<>();
			ListTag entryList = tag.getListOrEmpty("Entries");
			for (int j = 0; j < entryList.size(); j++) {
				CompoundTag e = entryList.getCompoundOrEmpty(j);
				int[] pos = e.getIntArray("Pos").orElse(new int[0]);
				entries.add(new Entry(e.getStringOr("Id", ""), e.read("Author", UUIDUtil.CODEC).orElseThrow(), e.getStringOr("AuthorName", ""), e.getLongOr("Time", 0L),
						e.getStringOr("Photo", ""), e.getStringOr("Text", ""), e.getStringOr("Dimension", ""),
						pos.length == 3 ? pos[0] : 0, pos.length == 3 ? pos[1] : 0, pos.length == 3 ? pos[2] : 0,
						e.getStringOr("Lesson", ""), e.getIntOr("Step", 0)));
			}
			Album album = new Album(tag.getStringOr("Key", ""), tag.getStringOr("Title", ""), entries);
			store.albums.put(album.key(), album);
		}
		store.currentGallery = nbt.getStringOr("CurrentGallery", "");
		return store;
	}
}
