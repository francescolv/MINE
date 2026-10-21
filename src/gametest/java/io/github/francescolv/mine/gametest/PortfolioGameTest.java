package io.github.francescolv.mine.gametest;

import io.github.francescolv.mine.lesson.LessonProgress;
import io.github.francescolv.mine.portfolio.PortfolioExport;
import io.github.francescolv.mine.portfolio.PortfolioPayloads;
import io.github.francescolv.mine.portfolio.PortfolioStore;
import io.github.francescolv.mine.portfolio.Portfolios;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Portfolios and class galleries: photos taken with the camera (here, made up: the game takes
 * them), notes, captions, sharing with the class, what each one may do, and the export.
 */
public class PortfolioGameTest {

	/** The smallest thing that looks like a JPEG to the server: start and end markers. */
	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x42, (byte) 0xFF, (byte) 0xD9 };

	private static void act(ServerPlayer player, PortfolioPayloads.Action action, String album, String entry, String text) {
		Portfolios.request(player, new PortfolioPayloads.Request(action, album, entry, text));
	}

	/** The camera is a spyglass marked by MINE. */
	@GameTest
	public void theCamera(GameTestHelper ctx) {
		ctx.assertTrue(Portfolios.isCamera(Portfolios.camera()), "A camera");
		ctx.assertTrue(!Portfolios.isCamera(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SPYGLASS)), "A plain spyglass isn't");
		ServerPlayer student = GameTests.player(ctx, false);
		GameTests.execute(ctx, student.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS), "mine camera", true);
		boolean got = false;
		for (int slot = 0; slot < student.getInventory().getContainerSize(); slot++) {
			got |= Portfolios.isCamera(student.getInventory().getItem(slot));
		}
		ctx.assertTrue(got, "/mine camera gives a camera");
		ctx.succeed();
	}

	/**
	 * A photo is saved in the student's portfolio with where, when and at which lesson step it was
	 * taken; the student writes its caption and notes. Not a JPEG, or too soon after the last one: refused.
	 */
	@GameTest
	public void photosAndNotes(GameTestHelper ctx) throws IOException {
		MinecraftServer server = ctx.getLevel().getServer();
		ServerPlayer student = GameTests.player(ctx, false);
		LessonProgress.get(server).put(student.getUUID(), new LessonProgress.Entry("loops", 1, 0, System.currentTimeMillis(), 0));

		ctx.assertTrue(Portfolios.addPhoto(student, "not a photo".getBytes(StandardCharsets.UTF_8)) == null, "Not a JPEG");
		PortfolioStore.Entry photo = Portfolios.addPhoto(student, JPEG);
		ctx.assertTrue(photo != null && photo.isPhoto() && photo.lesson().equals("loops") && photo.step() == 2
				&& photo.x() == student.getBlockX(), "The photo knows where and when: " + photo);
		ctx.assertTrue(Portfolios.addPhoto(student, JPEG) == null, "Too soon after the last photo");
		PortfolioStore store = PortfolioStore.get(server);
		String album = PortfolioStore.studentKey(student.getUUID());
		ctx.assertTrue(Files.exists(PortfolioStore.folder(server, album).resolve(photo.photo())), "The photo is a file in the world");

		act(student, PortfolioPayloads.Action.CAPTION, "", photo.id(), "My first tower");
		act(student, PortfolioPayloads.Action.NOTE, "", "", "Today I learnt loops.");
		PortfolioStore.Album portfolio = store.album(album);
		ctx.assertTrue(store.entry(portfolio, photo.id()).text().equals("My first tower"), "Caption");
		ctx.assertTrue(portfolio.entries().size() == 2 && portfolio.entries().get(1).text().equals("Today I learnt loops."), "Note");

		// Saved in the world.
		PortfolioStore loaded = PortfolioStore.fromNbt(store.save());
		ctx.assertTrue(loaded.album(album).entries().equals(portfolio.entries()), "Saved and loaded");

		act(student, PortfolioPayloads.Action.DELETE, "", photo.id(), "");
		ctx.assertTrue(portfolio.entries().size() == 1 && !Files.exists(PortfolioStore.folder(server, album).resolve(photo.photo())),
				"Deleted, with its file");
		LessonProgress.get(server).remove(student.getUUID());
		ctx.succeed();
	}

	/**
	 * Students share their photos with the class gallery the teacher created. They can't change
	 * other students' portfolios or entries; the teacher can.
	 */
	@GameTest
	public void theClassGallery(GameTestHelper ctx) {
		MinecraftServer server = ctx.getLevel().getServer();
		ServerPlayer teacher = GameTests.player(ctx, true);
		ServerPlayer alex = GameTests.player(ctx, false);
		ServerPlayer sam = GameTests.player(ctx, false);
		String name = "test_" + UUID.randomUUID().toString().substring(0, 6);

		GameTests.execute(ctx, alex.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS), "mine gallery create " + name, false);
		GameTests.execute(ctx, teacher.createCommandSourceStack(), "mine gallery create bad/name", false);
		GameTests.execute(ctx, teacher.createCommandSourceStack(), "mine gallery create " + name, true);
		PortfolioStore store = PortfolioStore.get(server);
		PortfolioStore.Album gallery = store.gallery(name);
		ctx.assertTrue(gallery != null && store.currentGallery() == gallery, "Created, and the current one");

		PortfolioStore.Entry photo = Portfolios.addPhoto(alex, JPEG);
		String alexAlbum = PortfolioStore.studentKey(alex.getUUID());
		// Sam can't touch Alex's things, nor share them.
		act(sam, PortfolioPayloads.Action.CAPTION, alexAlbum, photo.id(), "Hacked");
		act(sam, PortfolioPayloads.Action.SHARE, alexAlbum, photo.id(), "");
		ctx.assertTrue(store.entry(store.album(alexAlbum), photo.id()).text().isEmpty() && gallery.entries().isEmpty(),
				"Other students can't change or share a portfolio");

		act(alex, PortfolioPayloads.Action.CAPTION, "", photo.id(), "Our castle");
		act(alex, PortfolioPayloads.Action.SHARE, "", photo.id(), "");
		ctx.assertTrue(gallery.entries().size() == 1 && gallery.entries().getFirst().text().equals("Our castle")
				&& Files.exists(PortfolioStore.folder(server, gallery.key()).resolve(gallery.entries().getFirst().photo())),
				"Shared: a copy, with its photo");
		PortfolioStore.Entry shared = gallery.entries().getFirst();
		// Pressing Share again and again doesn't fill the gallery with copies.
		act(alex, PortfolioPayloads.Action.SHARE, "", photo.id(), "");
		act(alex, PortfolioPayloads.Action.SHARE, "", photo.id(), "");
		ctx.assertTrue(gallery.entries().size() == 1, "Shared once: " + gallery.entries().size());

		act(sam, PortfolioPayloads.Action.DELETE, gallery.key(), shared.id(), "");
		act(sam, PortfolioPayloads.Action.NOTE, gallery.key(), "", "Spam");
		ctx.assertTrue(gallery.entries().size() == 1, "Students don't delete others' entries or write notes in a gallery");
		act(teacher, PortfolioPayloads.Action.NOTE, gallery.key(), "", "Our lesson on loops");
		ctx.assertTrue(gallery.entries().size() == 2, "The teacher writes in the gallery");
		act(teacher, PortfolioPayloads.Action.DELETE, gallery.key(), shared.id(), "");
		ctx.assertTrue(gallery.entries().size() == 1, "The teacher deletes any entry");
		ctx.assertTrue(store.album(alexAlbum).entries().size() == 1, "Alex's own photo stays in their portfolio");

		GameTests.execute(ctx, alex.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS), "mine gallery list", true);
		GameTests.execute(ctx, teacher.createCommandSourceStack(), "mine gallery remove " + name, true);
		ctx.assertTrue(store.gallery(name) == null && !Files.exists(PortfolioStore.folder(server, gallery.key())), "Removed with its photos");
		ctx.succeed();
	}

	/** The export is a zip of web pages: an index, a page per student and per gallery, and the photos. */
	@GameTest
	public void theExport(GameTestHelper ctx) throws IOException {
		MinecraftServer server = ctx.getLevel().getServer();
		ServerPlayer teacher = GameTests.player(ctx, true);
		ServerPlayer student = GameTests.player(ctx, false);
		PortfolioStore.Entry photo = Portfolios.addPhoto(student, JPEG);
		act(student, PortfolioPayloads.Action.CAPTION, "", photo.id(), "A <tower> & more");
		String name = "export_" + UUID.randomUUID().toString().substring(0, 6);
		PortfolioStore.get(server).createGallery(name);
		act(student, PortfolioPayloads.Action.SHARE, "", photo.id(), "");

		Map<String, byte[]> files = unzip(PortfolioExport.everything(server, "en_us"));
		ctx.assertTrue(files.containsKey("index.html") && new String(files.get("index.html"), StandardCharsets.UTF_8).contains(name),
				"The index lists the gallery");
		String page = files.entrySet().stream()
				.filter(e -> e.getKey().startsWith("students/") && new String(e.getValue(), StandardCharsets.UTF_8).contains(photo.id()))
				.map(e -> new String(e.getValue(), StandardCharsets.UTF_8)).findFirst().orElse("");
		ctx.assertTrue(page.contains("A &lt;tower&gt; &amp; more"), "The student's page, with the caption escaped");
		ctx.assertTrue(files.keySet().stream().anyMatch(f -> f.startsWith("photos/") && f.endsWith(photo.photo())), "The photo is in the zip");
		ctx.assertTrue(files.containsKey("galleries/" + name + ".html"), "The gallery's page");

		GameTests.execute(ctx, teacher.createCommandSourceStack(), "mine portfolio export", true);
		// Mock players have no MINE in their game: the window can't open.
		GameTests.execute(ctx, student.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS), "mine portfolio", false);
		PortfolioStore.get(server).removeGallery(server, name);
		ctx.succeed();
	}

	private static Map<String, byte[]> unzip(byte[] zip) throws IOException {
		Map<String, byte[]> files = new HashMap<>();
		try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
			for (ZipEntry entry; (entry = in.getNextEntry()) != null; ) {
				files.put(entry.getName(), in.readAllBytes());
			}
		}
		return files;
	}
}
