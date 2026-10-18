package io.github.francescolv.mine.classroom;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.francescolv.mine.permission.Permissions;
import io.github.francescolv.mine.world.ClassWorlds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code /mine class} (teachers): the class code students type to join ({@code code [world]},
 * {@code code [world] new}, {@code close [world]}), who is waiting to join ({@code waiting}), and
 * letting one of them in ({@code admit <name> [world]}). Each world has its own code; without a
 * world, it's the one the teacher is in.
 */
public final class ClassCommand {

	private ClassCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		SuggestionProvider<CommandSourceStack> worlds = (ctx, builder) -> {
			List<String> names = new ArrayList<>();
			names.add(ClassWorlds.MAIN);
			ClassWorlds.get(ctx.getSource().getServer()).all().forEach(world -> names.add(world.name()));
			return SharedSuggestionProvider.suggest(names, builder);
		};
		return Commands.literal("class")
				.requires(Permissions::isTeacher)
				.then(Commands.literal("code")
						.executes(ctx -> code(ctx.getSource(), here(ctx.getSource()), false))
						.then(Commands.literal("new")
								.executes(ctx -> code(ctx.getSource(), here(ctx.getSource()), true)))
						.then(Commands.argument("world", StringArgumentType.word()).suggests(worlds)
								.executes(ctx -> code(ctx.getSource(), StringArgumentType.getString(ctx, "world"), false))
								.then(Commands.literal("new")
										.executes(ctx -> code(ctx.getSource(), StringArgumentType.getString(ctx, "world"), true)))))
				.then(Commands.literal("close")
						.executes(ctx -> closeAll(ctx.getSource()))
						.then(Commands.argument("world", StringArgumentType.word())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ClassCode.openWorlds(), builder))
								.executes(ctx -> close(ctx.getSource(), StringArgumentType.getString(ctx, "world")))))
				.then(Commands.literal("waiting")
						.executes(ctx -> waiting(ctx.getSource())))
				.then(Commands.literal("admit")
						.then(Commands.argument("name", StringArgumentType.word())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
										ClassCode.waiting().stream().map(waiting -> waiting.profile().name()), builder))
								.executes(ctx -> admit(ctx.getSource(), StringArgumentType.getString(ctx, "name"), here(ctx.getSource())))
								.then(Commands.argument("world", StringArgumentType.word()).suggests(worlds)
										.executes(ctx -> admit(ctx.getSource(), StringArgumentType.getString(ctx, "name"),
												StringArgumentType.getString(ctx, "world"))))));
	}

	/** The world the command comes from, by name: a class world, or the main one (also the console's). */
	private static String here(CommandSourceStack source) {
		ClassWorlds.World world = ClassWorlds.get(source.getServer()).of(source.getLevel());
		return world == null ? ClassWorlds.MAIN : world.name();
	}

	private static boolean exists(CommandSourceStack source, String world) {
		if (ClassWorlds.level(source.getServer(), world) != null) {
			return true;
		}
		source.sendFailure(Component.translatableWithFallback("mine.world.no_world",
				"There's no world called %s: /mine world list shows them.", world));
		return false;
	}

	/** "the class 2b", or "the main world": the world a code is for. */
	private static Component worldName(String world) {
		return ClassWorlds.MAIN.equals(world)
				? Component.translatableWithFallback("mine.world.main", "the main world")
				: Component.literal(world);
	}

	/** Shows the world's open code, or opens one; large on the teacher's screen if their game has MINE. */
	private static int code(CommandSourceStack source, String world, boolean renew) {
		if (!exists(source, world)) {
			return 0;
		}
		List<Integer> code = ClassCode.current(world);
		if (code == null || renew) {
			code = ClassCode.open(world);
		}
		long minutes = Math.max(1, Duration.between(Instant.now(), ClassCode.closesAt(world)).toMinutes());
		MutableComponent words = ClassCode.describe(code).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
		source.sendSuccess(() -> Component.translatableWithFallback("mine.class.code_world",
				"Class code for %s: %s. Students who aren't in the class yet type it to join, and arrive there; open for %s minutes (/mine class close to close it now).",
				worldName(world), words, minutes).withStyle(ChatFormatting.GREEN), true);
		ServerPlayer teacher = source.getPlayer();
		if (teacher != null && ServerPlayNetworking.canSend(teacher, ClassCodePayloads.Show.ID)) {
			ServerPlayNetworking.send(teacher, new ClassCodePayloads.Show(code, ClassCode.closesAt(world).toEpochMilli(),
					ClassWorlds.MAIN.equals(world) ? "" : world));
		}
		return 1;
	}

	private static int closeAll(CommandSourceStack source) {
		if (!ClassCode.anyOpen()) {
			source.sendFailure(Component.translatableWithFallback("mine.class.none", "There's no class code open."));
			return 0;
		}
		ClassCode.close();
		source.sendSuccess(() -> Component.translatableWithFallback("mine.class.code_closed",
				"Class code closed: only students already in the class can join.").withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int close(CommandSourceStack source, String world) {
		if (!ClassCode.close(world)) {
			source.sendFailure(Component.translatableWithFallback("mine.class.none_world", "There's no class code open for %s.",
					worldName(world)));
			return 0;
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.class.code_closed_world",
				"The class code for %s is closed.", worldName(world)).withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int waiting(CommandSourceStack source) {
		List<ClassCode.Waiting> waiting = ClassCode.waiting();
		if (waiting.isEmpty()) {
			source.sendSuccess(() -> Component.translatableWithFallback("mine.class.nobody_waiting",
					"Nobody is waiting to join the class."), false);
			return 0;
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.class.waiting",
				"Trying to join the class (click to let in):"), false);
		for (ClassCode.Waiting student : waiting) {
			long minutes = Duration.between(student.since(), Instant.now()).toMinutes();
			source.sendSuccess(() -> Component.literal(" ").append(ClassTexts.trying(student.profile().name(), false))
					.append(Component.translatableWithFallback("mine.class.minutes_ago", " (%s min ago)", minutes)
							.withStyle(ChatFormatting.GRAY)), false);
		}
		return waiting.size();
	}

	private static int admit(CommandSourceStack source, String name, String world) {
		if (!exists(source, world)) {
			return 0;
		}
		ClassCode.Waiting student = ClassCode.waiting(name);
		if (student == null) {
			source.sendFailure(Component.translatableWithFallback("mine.class.not_waiting",
					"%s hasn't tried to join. Ask them to try, or add them with /whitelist add %s", name, name));
			return 0;
		}
		ClassCode.admit(source.getServer(), student.profile(), world);
		String real = student.profile().name();
		source.sendSuccess(() -> Component.translatableWithFallback("mine.class.admitted_world",
				"%s is in the class: they can join now, and arrive in %s.", real, worldName(world)).withStyle(ChatFormatting.GREEN), true);
		return 1;
	}
}
