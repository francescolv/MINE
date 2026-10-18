package io.github.francescolv.mine.world;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.francescolv.mine.permission.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code /mine world}: which world you are in. Teachers: {@code list}, {@code maps},
 * {@code create <name> flat|map <map>}, {@code go <name> [players]}, {@code spawn},
 * {@code rules [lesson|free|<rule> on|off]} and {@code remove <name>} for the class worlds.
 */
public final class WorldCommand {

	private static final SuggestionProvider<CommandSourceStack> WORLDS = (ctx, builder) -> {
		List<String> names = new ArrayList<>();
		names.add(ClassWorlds.MAIN);
		ClassWorlds.get(ctx.getSource().getServer()).all().forEach(world -> names.add(world.name()));
		return SharedSuggestionProvider.suggest(names, builder);
	};
	private static final SuggestionProvider<CommandSourceStack> CLASS_WORLDS = (ctx, builder) -> SharedSuggestionProvider.suggest(
			ClassWorlds.get(ctx.getSource().getServer()).all().stream().map(ClassWorlds.World::name), builder);
	private static final SuggestionProvider<CommandSourceStack> MAPS = (ctx, builder) -> SharedSuggestionProvider.suggest(
			ClassWorlds.maps(ctx.getSource().getServer(), true), builder);

	private WorldCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		LiteralArgumentBuilder<CommandSourceStack> rules = Commands.literal("rules").requires(Permissions::isTeacher)
				.executes(ctx -> showRules(ctx.getSource()))
				.then(Commands.literal("lesson").executes(ctx -> setRules(ctx.getSource(), ClassWorlds.LESSON, Preset.LESSON)))
				.then(Commands.literal("free").executes(ctx -> setRules(ctx.getSource(), ClassWorlds.FREE, Preset.FREE)));
		for (ClassWorlds.Rule rule : ClassWorlds.Rule.values()) {
			rules.then(Commands.literal(rule.id())
					.then(Commands.literal("on").executes(ctx -> setRule(ctx.getSource(), rule, true)))
					.then(Commands.literal("off").executes(ctx -> setRule(ctx.getSource(), rule, false))));
		}
		return Commands.literal("world")
				.then(rules)
				.executes(ctx -> where(ctx.getSource()))
				.then(Commands.literal("list").requires(Permissions::isTeacher)
						.executes(ctx -> list(ctx.getSource())))
				.then(Commands.literal("maps").requires(Permissions::isTeacher)
						.executes(ctx -> maps(ctx.getSource())))
				.then(Commands.literal("create").requires(Permissions::isTeacher)
						.then(Commands.argument("name", StringArgumentType.word())
								.then(Commands.literal("flat")
										.executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "name"),
												ClassWorlds.Kind.FLAT, null)))
								.then(Commands.literal("map")
										.then(Commands.argument("map", StringArgumentType.word()).suggests(MAPS)
												.executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "name"),
														ClassWorlds.Kind.MAP, StringArgumentType.getString(ctx, "map")))))))
				.then(Commands.literal("go").requires(Permissions::isTeacher)
						.then(Commands.argument("name", StringArgumentType.word()).suggests(WORLDS)
								.executes(ctx -> go(ctx.getSource(), StringArgumentType.getString(ctx, "name"),
										List.of(ctx.getSource().getPlayerOrException())))
								.then(Commands.argument("players", EntityArgument.players())
										.executes(ctx -> go(ctx.getSource(), StringArgumentType.getString(ctx, "name"),
												EntityArgument.getPlayers(ctx, "players"))))))
				.then(Commands.literal("spawn").requires(Permissions::isTeacher)
						.executes(ctx -> spawn(ctx.getSource())))
				.then(Commands.literal("remove").requires(Permissions::isTeacher)
						.then(Commands.argument("name", StringArgumentType.word()).suggests(CLASS_WORLDS)
								.executes(ctx -> askRemove(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
								.then(Commands.literal("confirm")
										.executes(ctx -> remove(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))));
	}

	private static int where(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ClassWorlds.World world = ClassWorlds.get(source.getServer()).of(player.level());
		source.sendSuccess(() -> world == null
				? Component.translatableWithFallback("mine.world.here_main", "You are in the main world.")
				: Component.translatableWithFallback("mine.world.here", "You are in the class world %s.", world.name()), false);
		return 1;
	}

	private static int list(CommandSourceStack source) {
		MinecraftServer server = source.getServer();
		List<ClassWorlds.World> worlds = ClassWorlds.get(server).all();
		if (worlds.isEmpty()) {
			source.sendSuccess(() -> Component.translatableWithFallback("mine.world.none",
					"There are no class worlds yet: /mine world create <name> flat, or map <map>."), false);
			return 0;
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.world.list_title", "Class worlds:").withStyle(ChatFormatting.BOLD), false);
		for (ClassWorlds.World world : worlds) {
			ServerLevel level = server.getLevel(ClassWorlds.key(world.name()));
			int players = level == null ? 0 : level.players().size();
			Component kind = world.kind() == ClassWorlds.Kind.FLAT
					? Component.translatableWithFallback("mine.world.kind_flat", "flat")
					: Component.translatableWithFallback("mine.world.kind_map", "map %s", world.map());
			source.sendSuccess(() -> Component.translatableWithFallback("mine.world.list_line", "%s (%s): %s players in it",
					world.name(), kind, players), false);
		}
		return worlds.size();
	}

	private static int maps(CommandSourceStack source) {
		MinecraftServer server = source.getServer();
		List<String> usable = ClassWorlds.maps(server, true);
		List<String> all = ClassWorlds.maps(server, false);
		String folder = ClassWorlds.maps(server).toAbsolutePath().normalize().toString();
		if (all.isEmpty()) {
			source.sendSuccess(() -> Component.translatableWithFallback("mine.world.maps_none",
					"No maps yet. Put the folder of a Java world (with its level.dat and region folder) in %s, with a name of small letters, digits and _.",
					folder), false);
			return 0;
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.world.maps", "Maps in %s: %s", folder,
				usable.isEmpty() ? "-" : String.join(", ", usable)), false);
		List<String> bad = all.stream().filter(map -> !usable.contains(map)).toList();
		if (!bad.isEmpty()) {
			source.sendFailure(Component.translatableWithFallback("mine.world.maps_bad",
					"These folders don't look like Java worlds (no region folder with .mca files): %s", String.join(", ", bad)));
		}
		return usable.size();
	}

	private static int create(CommandSourceStack source, String name, ClassWorlds.Kind kind, String map) {
		ClassWorlds.Result result = ClassWorlds.create(source.getServer(), name, kind, map);
		switch (result) {
			case CREATED -> source.sendSuccess(() -> Component.translatableWithFallback("mine.world.created",
					"Class world %s ready: always midday, no rain, no monsters. Go there with /mine world go %s, take the class with /mine world go %s @a.",
					name, name, name).withStyle(ChatFormatting.GREEN), true);
			case BAD_NAME -> source.sendFailure(Component.translatableWithFallback("mine.world.bad_name",
					"A world's name may only have small letters, digits and _ (up to 16), and can't be \"main\"."));
			case EXISTS -> source.sendFailure(Component.translatableWithFallback("mine.world.exists",
					"There's already a world called %s.", name));
			case NO_MAP -> source.sendFailure(Component.translatableWithFallback("mine.world.no_map",
					"There's no map called %s: /mine world maps shows them.", map));
			case BAD_MAP -> source.sendFailure(Component.translatableWithFallback("mine.world.bad_map",
					"The map %s doesn't look like a Java world: it needs a region folder with .mca files.", map));
			case FAILED -> source.sendFailure(Component.translatableWithFallback("mine.world.failed",
					"The world couldn't be made: the server log says why."));
		}
		return result == ClassWorlds.Result.CREATED ? 1 : 0;
	}

	private static int go(CommandSourceStack source, String name, Collection<ServerPlayer> players) {
		MinecraftServer server = source.getServer();
		ServerLevel level = ClassWorlds.level(server, name);
		if (level == null) {
			return noWorld(source, name);
		}
		ClassWorlds.World world = ClassWorlds.get(server).world(name);
		for (ServerPlayer player : players) {
			ClassWorlds.goTo(player, level, world);
		}
		Component where = world == null ? Component.translatableWithFallback("mine.world.main", "the main world") : Component.literal(name);
		source.sendSuccess(() -> Component.translatableWithFallback("mine.world.went", "In %s: %s",
				where, players.stream().map(player -> player.getName().getString()).collect(Collectors.joining(", "))), true);
		return players.size();
	}

	private static int spawn(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer teacher = source.getPlayerOrException();
		if (!ClassWorlds.setSpawn(teacher)) {
			source.sendFailure(Component.translatableWithFallback("mine.world.spawn_main",
					"You are in the main world: go to a class world first (its spawn is the one that changes)."));
			return 0;
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.world.spawn",
				"Students who come to this world arrive here, facing the way you face.").withStyle(ChatFormatting.GREEN), false);
		return 1;
	}

	private enum Preset { LESSON, FREE }

	/**
	 * The class world the command comes from: the teacher's, or a command block's. {@code null}
	 * (and they are told) if it's the main world.
	 */
	@org.jetbrains.annotations.Nullable
	private static ClassWorlds.World here(CommandSourceStack source) {
		ClassWorlds.World world = ClassWorlds.get(source.getServer()).of(source.getLevel());
		if (world == null) {
			source.sendFailure(Component.translatableWithFallback("mine.world.rules_main",
					"Class rules are for class worlds: go to one first (/mine world go <name>)."));
		}
		return world;
	}

	private static int showRules(CommandSourceStack source) {
		ClassWorlds.World world = here(source);
		if (world == null) {
			return 0;
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.world.rules", "Rules in %s: %s", world.name(), rules(world)), false);
		source.sendSuccess(() -> Component.translatableWithFallback("mine.world.rules_help",
				"/mine world rules lesson: nothing that gets in the way. /mine world rules free: free time. Or one at a time: /mine world rules night on")
				.withStyle(ChatFormatting.GRAY), false);
		return world.rules().size();
	}

	private static int setRules(CommandSourceStack source, java.util.Set<ClassWorlds.Rule> rules, Preset preset) {
		ClassWorlds.World world = here(source);
		if (world == null) {
			return 0;
		}
		ClassWorlds.setRules(source.getServer(), world.name(), rules);
		ClassWorlds.World now = ClassWorlds.get(source.getServer()).world(world.name());
		Component message = preset == Preset.FREE
				? Component.translatableWithFallback("mine.world.free", "Free time! Allowed: %s", rules(now))
				: Component.translatableWithFallback("mine.world.lesson", "Lesson time: no monsters, always day, no fire, explosions or fighting.");
		tell(source, now, message.copy().withStyle(preset == Preset.FREE ? ChatFormatting.GOLD : ChatFormatting.AQUA));
		return 1;
	}

	private static int setRule(CommandSourceStack source, ClassWorlds.Rule rule, boolean on) {
		ClassWorlds.World world = here(source);
		if (world == null) {
			return 0;
		}
		java.util.Set<ClassWorlds.Rule> rules = java.util.EnumSet.noneOf(ClassWorlds.Rule.class);
		rules.addAll(world.rules());
		if (on) {
			rules.add(rule);
		} else {
			rules.remove(rule);
		}
		ClassWorlds.setRules(source.getServer(), world.name(), rules);
		Component message = on
				? Component.translatableWithFallback("mine.world.rule_on", "The teacher allows: %s", ruleName(rule))
				: Component.translatableWithFallback("mine.world.rule_off", "The teacher stops: %s", ruleName(rule));
		tell(source, ClassWorlds.get(source.getServer()).world(world.name()), message.copy().withStyle(on ? ChatFormatting.GOLD : ChatFormatting.AQUA));
		return 1;
	}

	/** Everyone in the world hears about new rules: the teacher too, who also sees them all. */
	private static void tell(CommandSourceStack source, ClassWorlds.World world, Component message) {
		ServerLevel level = source.getServer().getLevel(ClassWorlds.key(world.name()));
		if (level != null) {
			for (ServerPlayer player : level.players()) {
				if (player != source.getEntity()) {
					player.sendSystemMessage(message);
				}
			}
		}
		source.sendSuccess(() -> message, true);
		source.sendSuccess(() -> Component.translatableWithFallback("mine.world.rules", "Rules in %s: %s", world.name(), rules(world))
				.withStyle(ChatFormatting.GRAY), false);
	}

	/** Each rule, green and ticked if allowed, red and crossed if not. */
	private static Component rules(ClassWorlds.World world) {
		net.minecraft.network.chat.MutableComponent all = Component.empty();
		boolean first = true;
		for (ClassWorlds.Rule rule : ClassWorlds.Rule.values()) {
			if (!first) {
				all.append(", ");
			}
			first = false;
			boolean on = world.allows(rule);
			all.append(Component.empty().append(ruleName(rule)).append(on ? " ✔" : " ✘")
					.withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
		}
		return all;
	}

	private static Component ruleName(ClassWorlds.Rule rule) {
		return switch (rule) {
			case MONSTERS -> Component.translatableWithFallback("mine.world.rule.monsters", "monsters");
			case NIGHT -> Component.translatableWithFallback("mine.world.rule.night", "night");
			case FIRE -> Component.translatableWithFallback("mine.world.rule.fire", "fire");
			case EXPLOSIONS -> Component.translatableWithFallback("mine.world.rule.explosions", "explosions");
			case PVP -> Component.translatableWithFallback("mine.world.rule.pvp", "fighting (PvP)");
		};
	}

	private static int askRemove(CommandSourceStack source, String name) {
		if (ClassWorlds.get(source.getServer()).world(name) == null) {
			return noWorld(source, name);
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.world.remove_ask",
				"This deletes the world %s and everything in it, for good. Whoever is in it goes back to the main world. To do it: /mine world remove %s confirm",
				name, name).withStyle(ChatFormatting.GOLD), false);
		return 0;
	}

	private static int remove(CommandSourceStack source, String name) {
		if (!ClassWorlds.remove(source.getServer(), name)) {
			return noWorld(source, name);
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.world.removed", "The world %s has been deleted.", name)
				.withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int noWorld(CommandSourceStack source, String name) {
		source.sendFailure(Component.translatableWithFallback("mine.world.no_world",
				"There's no world called %s: /mine world list shows them.", name));
		return 0;
	}
}
