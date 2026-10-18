package io.github.francescolv.mine.zone;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.francescolv.mine.permission.Permissions;
import io.github.francescolv.mine.world.ClassWorlds;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * {@code /mine zone} (teachers): {@code corners} (two corner blocks), {@code create <name>},
 * {@code save|restore|show|remove <name>}, {@code protect <name> on|off} and {@code list}.
 */
public final class ZoneCommand {

	private static final SuggestionProvider<CommandSourceStack> ZONES = (ctx, builder) -> SharedSuggestionProvider.suggest(
			Zones.get(ctx.getSource().getServer()).all().stream().map(Zones.Zone::name), builder);

	private ZoneCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("zone")
				.requires(Permissions::isTeacher)
				.then(Commands.literal("corners").executes(ctx -> corners(ctx.getSource())))
				.then(Commands.literal("create")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "name"), Zones.BELOW, Zones.ABOVE))
								.then(Commands.argument("below", IntegerArgumentType.integer(0, 256))
										.executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "name"),
												IntegerArgumentType.getInteger(ctx, "below"), Zones.ABOVE))
										.then(Commands.argument("above", IntegerArgumentType.integer(0, 256))
												.executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "name"),
														IntegerArgumentType.getInteger(ctx, "below"), IntegerArgumentType.getInteger(ctx, "above")))))))
				.then(Commands.literal("save").then(named(ZoneCommand::save)))
				.then(Commands.literal("restore").then(named(ZoneCommand::restore)))
				.then(Commands.literal("show").then(named(ZoneCommand::show)))
				.then(Commands.literal("remove").then(named(ZoneCommand::remove)))
				.then(Commands.literal("protect")
						.then(Commands.argument("name", StringArgumentType.word()).suggests(ZONES)
								.then(Commands.literal("on").executes(ctx -> protect(ctx.getSource(), StringArgumentType.getString(ctx, "name"), true)))
								.then(Commands.literal("off").executes(ctx -> protect(ctx.getSource(), StringArgumentType.getString(ctx, "name"), false)))))
				.then(Commands.literal("list").executes(ctx -> list(ctx.getSource())));
	}

	private interface Action {
		int run(CommandSourceStack source, String name) throws CommandSyntaxException;
	}

	private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> named(Action action) {
		return Commands.argument("name", StringArgumentType.word()).suggests(ZONES)
				.executes(ctx -> action.run(ctx.getSource(), StringArgumentType.getString(ctx, "name")));
	}

	private static int corners(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer teacher = source.getPlayerOrException();
		teacher.addItem(Zones.corners());
		source.sendSuccess(() -> Component.translatableWithFallback("mine.zone.corners",
				"Two zone corners: place one at a corner of the area and one at the opposite corner, then /mine zone create <name>. The zone goes %s blocks into the ground and %s above the corners (or /mine zone create <name> <below> <above>).",
				Zones.BELOW, Zones.ABOVE)
				.withStyle(ChatFormatting.GREEN), false);
		return 1;
	}

	private static int create(CommandSourceStack source, String name, int below, int above) throws CommandSyntaxException {
		Zones.Result result = Zones.create(source.getPlayerOrException(), name, below, above);
		if (result == Zones.Result.DONE) {
			Zones.Zone zone = Zones.get(source.getServer()).zone(name);
			Zones.outline(source.getPlayerOrException(), zone.box());
			source.sendSuccess(() -> Component.translatableWithFallback("mine.zone.created",
					"Zone %s made (%s × %s × %s), and its snapshot taken: /mine zone restore %s puts it back as it is now.",
					name, zone.box().getXSpan(), zone.box().getYSpan(), zone.box().getZSpan(), name).withStyle(ChatFormatting.GREEN), true);
			return 1;
		}
		return fail(source, result, name);
	}

	private static int save(CommandSourceStack source, String name) {
		Zones.Result result = Zones.save(source.getServer(), name);
		if (result == Zones.Result.DONE) {
			source.sendSuccess(() -> Component.translatableWithFallback("mine.zone.saved",
					"New snapshot of %s: from now on it comes back as it is now.", name).withStyle(ChatFormatting.GREEN), true);
			return 1;
		}
		return fail(source, result, name);
	}

	private static int restore(CommandSourceStack source, String name) {
		Zones.Result result = Zones.restore(source.getServer(), name);
		if (result == Zones.Result.DONE) {
			source.sendSuccess(() -> Component.translatableWithFallback("mine.zone.restored",
					"%s is back as it was. Bytes stay where they are.", name).withStyle(ChatFormatting.GREEN), true);
			return 1;
		}
		return fail(source, result, name);
	}

	private static int show(CommandSourceStack source, String name) throws CommandSyntaxException {
		ServerPlayer teacher = source.getPlayerOrException();
		Zones.Zone zone = Zones.get(source.getServer()).zone(name);
		if (zone == null) {
			return fail(source, Zones.Result.NO_ZONE, name);
		}
		if (!zone.dimension().equals(teacher.level().dimension())) {
			source.sendFailure(Component.translatableWithFallback("mine.zone.elsewhere", "%s is in another world: %s.",
					name, worldName(source.getServer(), zone)));
			return 0;
		}
		Zones.outline(teacher, zone.box());
		BlockCenter center = new BlockCenter(zone);
		source.sendSuccess(() -> Component.translatableWithFallback("mine.zone.shown", "%s: the outline shows for 15 seconds (centre %s %s %s).",
				name, center.x, center.y, center.z), false);
		return 1;
	}

	private record BlockCenter(int x, int y, int z) {
		BlockCenter(Zones.Zone zone) {
			this(zone.box().getCenter().getX(), zone.box().getCenter().getY(), zone.box().getCenter().getZ());
		}
	}

	private static int protect(CommandSourceStack source, String name, boolean on) {
		if (!Zones.protect(source.getServer(), name, on)) {
			return fail(source, Zones.Result.NO_ZONE, name);
		}
		source.sendSuccess(() -> (on
				? Component.translatableWithFallback("mine.zone.protect_on",
						"%s is protected: students and their Bytes can't change its blocks, explosions and fire don't touch it. Doors, levers and chests still work.", name)
				: Component.translatableWithFallback("mine.zone.protect_off", "%s is no longer protected.", name))
				.withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int remove(CommandSourceStack source, String name) {
		if (!Zones.remove(source.getServer(), name)) {
			return fail(source, Zones.Result.NO_ZONE, name);
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.zone.removed",
				"Zone %s and its snapshot forgotten (the blocks stay as they are).", name).withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int list(CommandSourceStack source) {
		MinecraftServer server = source.getServer();
		List<Zones.Zone> zones = Zones.get(server).all();
		if (zones.isEmpty()) {
			source.sendSuccess(() -> Component.translatableWithFallback("mine.zone.none",
					"No zones yet: /mine zone corners gives you the corner blocks."), false);
			return 0;
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.zone.list_title", "Zones:").withStyle(ChatFormatting.BOLD), false);
		for (Zones.Zone zone : zones) {
			Component line = Component.translatableWithFallback("mine.zone.list_line", "%s, in %s: %s × %s × %s",
					zone.name(), worldName(server, zone), zone.box().getXSpan(), zone.box().getYSpan(), zone.box().getZSpan());
			source.sendSuccess(() -> zone.protect()
					? line.copy().append(Component.translatableWithFallback("mine.zone.list_protected", " (protected)").withStyle(ChatFormatting.GOLD))
					: line, false);
		}
		return zones.size();
	}

	private static Component worldName(MinecraftServer server, Zones.Zone zone) {
		ServerLevel level = server.getLevel(zone.dimension());
		ClassWorlds.World world = level == null ? null : ClassWorlds.get(server).of(level);
		return world != null ? Component.literal(world.name()) : Component.translatableWithFallback("mine.world.main", "the main world");
	}

	private static int fail(CommandSourceStack source, Zones.Result result, String name) {
		source.sendFailure(switch (result) {
			case BAD_NAME -> Component.translatableWithFallback("mine.zone.bad_name",
					"A zone's name may only have small letters, digits and _ (up to 24).");
			case EXISTS -> Component.translatableWithFallback("mine.zone.exists", "There's already a zone called %s.", name);
			case NO_CORNERS -> Component.translatableWithFallback("mine.zone.no_corners",
					"Place the two corners first, in this world: /mine zone corners gives you them.");
			case TOO_BIG -> Component.translatableWithFallback("mine.zone.too_big",
					"The zone is too big: at most %s blocks (e.g. 100 × 100 × 100).", Zones.MAX_VOLUME);
			case NO_ZONE -> Component.translatableWithFallback("mine.zone.no_zone",
					"There's no zone called %s: /mine zone list shows them.", name);
			case NO_WORLD -> Component.translatableWithFallback("mine.zone.no_world", "The world of %s isn't there any more.", name);
			case NO_SNAPSHOT -> Component.translatableWithFallback("mine.zone.no_snapshot",
					"%s has no snapshot: take one with /mine zone save %s.", name, name);
			default -> Component.translatableWithFallback("mine.zone.failed", "That didn't work: the server log says why.");
		});
		return 0;
	}
}
