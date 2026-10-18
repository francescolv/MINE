package io.github.francescolv.mine.permission;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /mine builder add|remove <students>} and {@code /mine builder list}, for teachers: which
 * students may build with MINE's tools (see {@link Permissions}).
 */
public final class BuilderCommand {

	private BuilderCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("builder")
				.requires(Permissions::isTeacher)
				.then(Commands.literal("add")
						.then(Commands.argument("students", EntityArgument.players())
								.executes(ctx -> change(ctx.getSource(), EntityArgument.getPlayers(ctx, "students"),
										Permissions::addBuilder, "mine.builder.added", "%s can now build with MINE's tools, such as NPCs."))))
				.then(Commands.literal("remove")
						.then(Commands.argument("students", EntityArgument.players())
								.executes(ctx -> change(ctx.getSource(), EntityArgument.getPlayers(ctx, "students"),
										Permissions::removeBuilder, "mine.builder.removed", "%s is no longer a builder."))))
				.then(Commands.literal("list").executes(ctx -> list(ctx.getSource())));
	}

	private static int change(CommandSourceStack source, Collection<ServerPlayer> students,
			BiPredicate<Permissions, ServerPlayer> action, String key, String fallback) {
		Permissions permissions = Permissions.get(source.getServer());
		int changed = 0;
		for (ServerPlayer student : students) {
			if (action.test(permissions, student)) {
				changed++;
				source.sendSuccess(() -> Component.translatableWithFallback(key, fallback, student.getDisplayName()), true);
				// The commands they may use have changed: tell their game, for completion.
				source.getServer().getCommands().sendCommands(student);
			}
		}
		return changed;
	}

	private static int list(CommandSourceStack source) {
		List<String> names = Permissions.get(source.getServer()).builderNames();
		source.sendSuccess(() -> names.isEmpty()
				? Component.translatableWithFallback("mine.builder.none", "No students are builders.").withStyle(ChatFormatting.GRAY)
				: Component.translatableWithFallback("mine.builder.list", "Builders: %s", String.join(", ", names)), false);
		return names.size();
	}
}
