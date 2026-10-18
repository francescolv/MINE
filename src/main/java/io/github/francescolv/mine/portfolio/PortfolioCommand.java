package io.github.francescolv.mine.portfolio;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.npc.ExportPayload;
import io.github.francescolv.mine.permission.Permissions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /mine camera} (a camera for yourself; teachers: for students), {@code /mine portfolio}
 * (yours; teachers: a student's, by name), {@code /mine portfolio export}, and the class
 * galleries: {@code /mine gallery} to see the current one, {@code open <name>}, and for the
 * teacher {@code create|use|remove <name>} and {@code list}.
 */
public final class PortfolioCommand {

	/** Gallery names are typed in commands and used for folders: letters, digits, - and _. */
	private static final String GALLERY_NAME = "[A-Za-z0-9_-]{1,32}";

	private PortfolioCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> camera() {
		return Commands.literal("camera")
				.executes(ctx -> giveCamera(ctx.getSource(), java.util.List.of(ctx.getSource().getPlayerOrException())))
				.then(Commands.argument("students", EntityArgument.players())
						.requires(Permissions::isTeacher)
						.executes(ctx -> giveCamera(ctx.getSource(), EntityArgument.getPlayers(ctx, "students"))));
	}

	public static LiteralArgumentBuilder<CommandSourceStack> portfolio() {
		return Commands.literal("portfolio")
				.executes(ctx -> open(ctx.getSource(), null))
				.then(Commands.literal("export").executes(ctx -> export(ctx.getSource())))
				.then(Commands.argument("student", StringArgumentType.word())
						.requires(Permissions::isTeacher)
						.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(PortfolioStore.get(ctx.getSource().getServer())
								.portfolios().stream().map(PortfolioStore.Album::title), builder))
						.executes(ctx -> open(ctx.getSource(), StringArgumentType.getString(ctx, "student"))));
	}

	public static LiteralArgumentBuilder<CommandSourceStack> gallery() {
		return Commands.literal("gallery")
				.executes(ctx -> openGallery(ctx.getSource(), null))
				.then(Commands.literal("open")
						.then(galleryName().executes(ctx -> openGallery(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
				.then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
				.then(Commands.literal("create")
						.requires(Permissions::isTeacher)
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
				.then(Commands.literal("use")
						.requires(Permissions::isTeacher)
						.then(galleryName().executes(ctx -> use(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
				.then(Commands.literal("remove")
						.requires(Permissions::isTeacher)
						.then(galleryName().executes(ctx -> remove(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
	}

	private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> galleryName() {
		return Commands.argument("name", StringArgumentType.word())
				.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(PortfolioStore.get(ctx.getSource().getServer())
						.galleries().stream().map(PortfolioStore.Album::title), builder));
	}

	private static int giveCamera(CommandSourceStack source, Collection<ServerPlayer> students) {
		for (ServerPlayer student : students) {
			student.addItem(Portfolios.camera());
			if (!Portfolios.hasMine(student)) {
				source.sendFailure(Component.translatableWithFallback("mine.camera.no_mine",
						"%s doesn't have MINE in their game: the camera won't work for them.", student.getDisplayName()));
			}
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.camera.given",
				"Camera given: right-click with it to take a photo for the portfolio.").withStyle(ChatFormatting.GREEN), students.size() > 1);
		return students.size();
	}

	private static int open(CommandSourceStack source, String student) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		PortfolioStore store = PortfolioStore.get(source.getServer());
		PortfolioStore.Album album;
		if (student == null) {
			album = store.portfolio(player.getUUID(), player.getName().getString());
		} else {
			album = store.portfolios().stream().filter(a -> a.title().equalsIgnoreCase(student)).findFirst().orElse(null);
			if (album == null) {
				source.sendFailure(Component.translatableWithFallback("mine.portfolio.unknown", "%s has no portfolio yet.", student));
				return 0;
			}
		}
		return opened(source, Portfolios.open(player, album, ""));
	}

	private static int opened(CommandSourceStack source, boolean opened) {
		if (!opened) {
			source.sendFailure(Component.translatableWithFallback("mine.portfolio.needs_mine",
					"The portfolio opens in a window of MINE: it needs MINE in your game (the MINE modpack)."));
			return 0;
		}
		return 1;
	}

	/** Teachers: everybody's portfolios and the galleries; students: their own. A zip of web pages. */
	private static int export(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		String language = player.clientInformation().language();
		boolean teacher = Permissions.isTeacher(source);
		PortfolioStore store = PortfolioStore.get(source.getServer());
		try {
			String fileName;
			byte[] zip;
			if (teacher) {
				fileName = PortfolioExport.fileName("class");
				zip = PortfolioExport.everything(source.getServer(), language);
			} else {
				PortfolioStore.Album own = store.portfolio(player.getUUID(), player.getName().getString());
				fileName = PortfolioExport.fileName(player.getName().getString());
				zip = PortfolioExport.of(source.getServer(), own, language);
			}
			Path file = PortfolioExport.save(source.getServer(), fileName, zip);
			MineMod.LOGGER.info("[MINE] {} exported portfolios to {}", player.getName().getString(), file);
			if (ExportPayload.canSend(player, zip.length)) {
				ExportPayload.send(player, fileName, zip);
			} else {
				source.sendFailure(Component.translatableWithFallback("mine.portfolio.export_server_only",
						"The file can't be sent to your game: it is on the server, in %s", file.toString()));
			}
			if (teacher) {
				source.sendSuccess(() -> Component.translatableWithFallback("mine.portfolio.exported",
						"Portfolios saved on the server in %s", file.toString()).withStyle(ChatFormatting.GREEN), true);
			}
			return 1;
		} catch (IOException e) {
			MineMod.LOGGER.error("[MINE] Could not export the portfolios", e);
			source.sendFailure(Component.translatableWithFallback("mine.npc.export_failed", "The file couldn't be saved: %s", e.getMessage()));
			return 0;
		}
	}

	private static int openGallery(CommandSourceStack source, String name) throws CommandSyntaxException {
		PortfolioStore store = PortfolioStore.get(source.getServer());
		PortfolioStore.Album gallery = name == null ? store.currentGallery() : store.gallery(name);
		if (gallery == null) {
			source.sendFailure(name == null
					? Component.translatableWithFallback("mine.gallery.none", "There's no class gallery yet: the teacher creates one with /mine gallery create.")
					: Component.translatableWithFallback("mine.gallery.unknown", "There's no gallery called %s.", name));
			return 0;
		}
		return opened(source, Portfolios.open(source.getPlayerOrException(), gallery, ""));
	}

	private static int list(CommandSourceStack source) {
		PortfolioStore store = PortfolioStore.get(source.getServer());
		PortfolioStore.Album current = store.currentGallery();
		if (store.galleries().isEmpty()) {
			source.sendSuccess(() -> Component.translatableWithFallback("mine.gallery.none",
					"There's no class gallery yet: the teacher creates one with /mine gallery create.").withStyle(ChatFormatting.GRAY), false);
			return 0;
		}
		for (PortfolioStore.Album gallery : store.galleries()) {
			boolean isCurrent = current != null && current.key().equals(gallery.key());
			source.sendSuccess(() -> Component.translatableWithFallback("mine.gallery.entry", "%s: %s entries%s",
					Component.literal(gallery.title()).withStyle(ChatFormatting.AQUA), gallery.entries().size(),
					isCurrent ? Component.translatableWithFallback("mine.gallery.current_mark", " (students share here)") : Component.empty()), false);
		}
		return store.galleries().size();
	}

	private static int create(CommandSourceStack source, String name) {
		if (!name.matches(GALLERY_NAME)) {
			source.sendFailure(Component.translatableWithFallback("mine.gallery.bad_name",
					"Gallery names use letters, digits, - and _ (e.g. loops_3B)."));
			return 0;
		}
		PortfolioStore.get(source.getServer()).createGallery(name);
		source.sendSuccess(() -> Component.translatableWithFallback("mine.gallery.created",
				"Class gallery %s ready: students share their photos with it from their portfolio (/mine portfolio).", name)
				.withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int use(CommandSourceStack source, String name) {
		PortfolioStore store = PortfolioStore.get(source.getServer());
		if (store.gallery(name) == null) {
			source.sendFailure(Component.translatableWithFallback("mine.gallery.unknown", "There's no gallery called %s.", name));
			return 0;
		}
		store.setCurrentGallery(name);
		source.sendSuccess(() -> Component.translatableWithFallback("mine.gallery.used", "Students now share with %s.", name)
				.withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int remove(CommandSourceStack source, String name) {
		try {
			if (!PortfolioStore.get(source.getServer()).removeGallery(source.getServer(), name)) {
				source.sendFailure(Component.translatableWithFallback("mine.gallery.unknown", "There's no gallery called %s.", name));
				return 0;
			}
		} catch (IOException e) {
			MineMod.LOGGER.error("[MINE] Could not remove the photos of gallery {}", name, e);
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.gallery.removed", "Gallery %s removed, with its photos.", name), true);
		return 1;
	}
}
