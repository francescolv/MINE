package io.github.francescolv.mine.board;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.francescolv.mine.npc.Templates;
import io.github.francescolv.mine.permission.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Interaction;

import java.util.List;

/**
 * {@code /mine board create [size]}, {@code place <id>}, {@code size <size>} and {@code remove}
 * (teachers and builders): add a board in front of you (empty, or described in a datapack),
 * change the size of the nearest one, or remove it.
 */
public final class BoardCommand {

	private static final double DISTANCE = 8;

	private BoardCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("board")
				.requires(Permissions::isBuilder)
				.then(Commands.literal("create")
						.executes(ctx -> create(ctx.getSource(), BoardManager.DEFAULT_SIZE))
						.then(Commands.argument("size", FloatArgumentType.floatArg(BoardManager.MIN_SIZE, BoardManager.MAX_SIZE))
								.executes(ctx -> create(ctx.getSource(), FloatArgumentType.getFloat(ctx, "size")))))
				.then(Commands.literal("place")
						.then(Commands.argument("id", IdentifierArgument.id())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(Templates.boardIds(), builder))
								.executes(ctx -> place(ctx.getSource(), IdentifierArgument.getId(ctx, "id")))))
				.then(Commands.literal("size")
						.then(Commands.argument("size", FloatArgumentType.floatArg(BoardManager.MIN_SIZE, BoardManager.MAX_SIZE))
								.executes(ctx -> size(ctx.getSource(), FloatArgumentType.getFloat(ctx, "size")))))
				.then(Commands.literal("remove")
						.executes(ctx -> remove(ctx.getSource())));
	}

	private static int create(CommandSourceStack source, float size) throws CommandSyntaxException {
		Interaction board = BoardManager.create(source.getPlayerOrException(), size, List.of());
		if (board == null) {
			return blocked(source);
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.board.created",
				"Here is the board! Write its text in a book and quill, then right-click the middle of the board with it.")
				.withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	/** A board described in a datapack, with the text in the language of whoever places it. */
	private static int place(CommandSourceStack source, Identifier id) throws CommandSyntaxException {
		Templates.BoardTemplate template = Templates.board(id);
		if (template == null) {
			source.sendFailure(Component.translatableWithFallback("mine.board.no_template", "There's no board %s in the datapacks.",
					id.toString()));
			return 0;
		}
		ServerPlayer player = source.getPlayerOrException();
		List<String> lines = template.text(player.clientInformation().language());
		Interaction board = BoardManager.create(player, template.size(), List.of(String.join("\n", lines)));
		if (board == null) {
			return blocked(source);
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.board.placed", "Here is the board %s.", id.toString())
				.withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int size(CommandSourceStack source, float size) throws CommandSyntaxException {
		Interaction board = BoardManager.nearest(source.getPlayerOrException(), DISTANCE, Permissions.isTeacher(source));
		if (board == null) {
			return noneNear(source);
		}
		BoardManager.setSize(board, size);
		source.sendSuccess(() -> Component.translatableWithFallback("mine.board.resized", "The board's size is now %s.", size), false);
		return 1;
	}

	private static int remove(CommandSourceStack source) throws CommandSyntaxException {
		Interaction board = BoardManager.nearest(source.getPlayerOrException(), DISTANCE, Permissions.isTeacher(source));
		if (board == null) {
			return noneNear(source);
		}
		BoardManager.remove(board);
		source.sendSuccess(() -> Component.translatableWithFallback("mine.board.removed", "The board has been removed."), true);
		return 1;
	}

	private static int blocked(CommandSourceStack source) {
		source.sendFailure(Component.translatableWithFallback("mine.board.blocked",
				"There's no room for the board two blocks in front of you: turn towards a free space."));
		return 0;
	}

	private static int noneNear(CommandSourceStack source) {
		source.sendFailure(Component.translatableWithFallback("mine.board.none_near",
				"There's no board of yours within %s blocks of you.", (int) DISTANCE));
		return 0;
	}
}
