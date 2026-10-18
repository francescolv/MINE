package io.github.francescolv.mine.permission;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /mine view student|teacher}: a teacher tries MINE as students see it, and comes back.
 * Always available to operators, also while in the student view, or they couldn't switch back.
 */
public final class ViewCommand {

	private ViewCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("view")
				.requires(source -> source.permissions().hasPermission(Permissions.TEACHER_PERMISSION))
				.then(Commands.literal("student").executes(ctx -> set(ctx.getSource(), true)))
				.then(Commands.literal("teacher").executes(ctx -> set(ctx.getSource(), false)));
	}

	private static int set(CommandSourceStack source, boolean student) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		Permissions.setStudentView(player, student);
		// Teacher commands disappear from (or come back to) the chat's completion.
		source.getServer().getCommands().sendCommands(player);
		if (student) {
			source.sendSuccess(() -> Component.translatableWithFallback("mine.view.student",
					"Student view: MINE now treats you like a student. Back with %s",
					Component.literal("/mine view teacher").withStyle(style -> style
							.withUnderlined(true)
							.withClickEvent(new ClickEvent.RunCommand("/mine view teacher"))))
					.withStyle(ChatFormatting.GOLD), false);
		} else {
			source.sendSuccess(() -> Component.translatableWithFallback("mine.view.teacher",
					"Teacher view: you are a teacher again.").withStyle(ChatFormatting.GREEN), false);
		}
		return 1;
	}
}
