package io.github.francescolv.mine.npc;

import io.github.francescolv.mine.Exports;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.permission.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * {@code /mine npc create <name>} and {@code /mine npc remove} (teachers and builders): add an NPC
 * in front of you, or remove the nearest one you may change; {@code /mine npc place <id>} adds
 * one described in a datapack. {@code /mine npc results} shows the
 * answers to the quizzes of the nearest one (teachers: of any NPC, by name), {@code /mine npc clear}
 * forgets them, {@code /mine npc export} (teachers) saves all of them in a CSV file. {@code /mine npc choose} is what the answer buttons of a quiz run.
 */
public final class NpcCommand {

	/** How close the teacher must be to the NPC to remove. */
	private static final double REMOVE_DISTANCE = 8;

	private NpcCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("npc")
				.then(Commands.literal("create")
						.requires(Permissions::isBuilder)
						.then(Commands.argument("name", StringArgumentType.greedyString())
								.executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "name").strip()))))
				.then(Commands.literal("place")
						.requires(Permissions::isBuilder)
						.then(Commands.argument("id", IdentifierArgument.id())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(Templates.npcIds(), builder))
								.executes(ctx -> place(ctx.getSource(), IdentifierArgument.getId(ctx, "id")))))
				.then(Commands.literal("remove")
						.requires(Permissions::isBuilder)
						.executes(ctx -> remove(ctx.getSource())))
				.then(Commands.literal("edit")
						.then(Commands.argument("npc", StringArgumentType.word())
								.executes(ctx -> edit(ctx.getSource(), StringArgumentType.getString(ctx, "npc")))))
				.then(Commands.literal("choose")
						.then(Commands.argument("npc", StringArgumentType.word())
								.then(Commands.argument("page", IntegerArgumentType.integer(0))
										.then(Commands.argument("question", IntegerArgumentType.integer(0))
												.then(Commands.argument("answer", IntegerArgumentType.integer(0))
														.executes(ctx -> choose(ctx.getSource(), StringArgumentType.getString(ctx, "npc"),
																IntegerArgumentType.getInteger(ctx, "page"),
																IntegerArgumentType.getInteger(ctx, "question"),
																IntegerArgumentType.getInteger(ctx, "answer"))))))))
				.then(Commands.literal("results")
						.requires(Permissions::isBuilder)
						.executes(ctx -> results(ctx.getSource()))
						.then(Commands.argument("name", StringArgumentType.greedyString())
								.requires(Permissions::isTeacher)
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
										QuizResults.get(ctx.getSource().getServer()).npcNames(), builder))
								.executes(ctx -> results(ctx.getSource(), StringArgumentType.getString(ctx, "name").strip()))))
				.then(Commands.literal("export")
						.requires(Permissions::isTeacher)
						.executes(ctx -> export(ctx.getSource())))
				.then(Commands.literal("clear")
						.requires(Permissions::isBuilder)
						.executes(ctx -> clear(ctx.getSource())));
	}

	private static int create(CommandSourceStack source, String name) throws CommandSyntaxException {
		Villager npc = NpcManager.create(source.getPlayerOrException(), name);
		if (npc == null) {
			source.sendFailure(Component.translatableWithFallback("mine.npc.blocked",
					"There's no room for the NPC two blocks in front of you: turn towards a free space."));
			return 0;
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.npc.created",
				"Here is %s! Write its dialogue in a book and quill, then right-click it with the book.",
				npc.getDisplayName()).withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	/** Places an NPC described in a datapack: it says that dialogue, in each student's language, also after /reload. */
	private static int place(CommandSourceStack source, Identifier id) throws CommandSyntaxException {
		Templates.NpcTemplate template = Templates.npc(id);
		if (template == null) {
			source.sendFailure(Component.translatableWithFallback("mine.npc.no_template",
					"There's no NPC %s in the datapacks.", id.toString()));
			return 0;
		}
		Villager npc = NpcManager.place(source.getPlayerOrException(), template);
		if (npc == null) {
			source.sendFailure(Component.translatableWithFallback("mine.npc.blocked",
					"There's no room for the NPC two blocks in front of you: turn towards a free space."));
			return 0;
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.npc.placed",
				"Here is %s, with the dialogue of %s.", npc.getDisplayName(), id.toString()).withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int remove(CommandSourceStack source) throws CommandSyntaxException {
		Villager npc = NpcManager.nearest(source.getPlayerOrException(), REMOVE_DISTANCE, Permissions.isTeacher(source));
		if (npc == null) {
			source.sendFailure(Component.translatableWithFallback("mine.npc.none_near",
					"There's no NPC of yours within %s blocks of you.", (int) REMOVE_DISTANCE));
			return 0;
		}
		Component name = npc.getDisplayName();
		npc.discard();
		source.sendSuccess(() -> Component.translatableWithFallback("mine.npc.removed", "%s has been removed.", name), true);
		return 1;
	}

	/** The NPC's dialogue in MINE's window (the button next to its name, for whoever may change it). */
	private static int edit(CommandSourceStack source, String npc) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		Entity entity;
		try {
			entity = player.level().getEntity(UUID.fromString(npc));
		} catch (IllegalArgumentException e) {
			entity = null;
		}
		if (!NpcManager.isNpc(entity) || entity.distanceToSqr(player) > REMOVE_DISTANCE * REMOVE_DISTANCE
				|| !NpcManager.canChange(player, (Villager) entity)) {
			source.sendFailure(Component.translatableWithFallback("mine.npc.none_near",
					"There's no NPC of yours within %s blocks of you.", (int) REMOVE_DISTANCE));
			return 0;
		}
		NpcManager.edit(player, (Villager) entity);
		return 1;
	}

	private static int choose(CommandSourceStack source, String npc, int page, int question, int answer)
			throws CommandSyntaxException {
		ServerPlayer student = source.getPlayerOrException();
		UUID id;
		try {
			id = UUID.fromString(npc);
		} catch (IllegalArgumentException e) {
			id = null;
		}
		NpcManager.Choice choice = id == null ? NpcManager.Choice.INVALID : NpcManager.choose(student, id, page, question, answer);
		if (choice == NpcManager.Choice.INVALID) {
			source.sendFailure(Component.translatableWithFallback("mine.npc.quiz.invalid",
					"This question isn't here any more: talk to the NPC again."));
			return 0;
		}
		return 1;
	}

	/** The answers to the quizzes of the nearest NPC the player may change. */
	private static int results(CommandSourceStack source) throws CommandSyntaxException {
		Villager npc = NpcManager.nearest(source.getPlayerOrException(), REMOVE_DISTANCE, Permissions.isTeacher(source));
		if (npc == null) {
			source.sendFailure(Component.translatableWithFallback("mine.npc.none_near",
					"There's no NPC of yours within %s blocks of you.", (int) REMOVE_DISTANCE));
			return 0;
		}
		return results(source, npc.getUUID(), npc.getDisplayName().getString());
	}

	/** The answers to the quizzes of the NPCs called {@code name}, also far away or removed. */
	private static int results(CommandSourceStack source, String name) {
		List<UUID> npcs = QuizResults.get(source.getServer()).npcsNamed(name);
		if (npcs.isEmpty()) {
			source.sendFailure(Component.translatableWithFallback("mine.npc.results_unknown",
					"Nobody has answered the questions of an NPC called %s.", name));
			return 0;
		}
		int answers = 0;
		for (UUID npc : npcs) {
			answers += results(source, npc, name);
		}
		return answers;
	}

	private static int results(CommandSourceStack source, UUID npc, String name) {
		List<QuizResults.Question> questions = QuizResults.get(source.getServer()).of(npc);
		if (questions.isEmpty()) {
			source.sendSuccess(() -> Component.translatableWithFallback("mine.npc.results_none",
					"Nobody has answered the questions of %s yet.", name).withStyle(ChatFormatting.GRAY), false);
			return 0;
		}
		source.sendSuccess(() -> Component.translatableWithFallback("mine.npc.results_header",
				"Answers to the questions of %s:", name).withStyle(ChatFormatting.LIGHT_PURPLE), false);
		int count = 0;
		for (QuizResults.Question question : questions) {
			List<QuizResults.Answer> answers = question.answers().values().stream()
					.sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name()))
					.toList();
			source.sendSuccess(() -> Component.literal(question.text().replace('\n', ' ')).withStyle(ChatFormatting.GOLD), false);
			if (!question.poll()) {
				long right = answers.stream().filter(QuizResults.Answer::right).count();
				source.sendSuccess(() -> Component.translatableWithFallback("mine.npc.results_summary",
						"   %s of %s right at the first try", right, answers.size()).withStyle(ChatFormatting.GRAY), false);
			}
			for (QuizResults.Answer answer : answers) {
				source.sendSuccess(() -> resultLine(question, answer), false);
			}
			count += answers.size();
		}
		return count;
	}

	/** {@code Alex: ✘ B) 5 — right after 2 tries}. */
	private static Component resultLine(QuizResults.Question question, QuizResults.Answer answer) {
		Component chosen = Component.literal(NpcQuiz.letter(answer.option()) + ") " + answer.text());
		Component line;
		if (question.poll()) {
			line = chosen;
		} else if (answer.right()) {
			line = Component.literal("✔ ").withStyle(ChatFormatting.GREEN).append(chosen.copy().withStyle(ChatFormatting.WHITE));
		} else {
			Component then = answer.solved()
					? Component.translatableWithFallback("mine.npc.results_solved", " — right after %s tries", answer.tries())
					: Component.translatableWithFallback("mine.npc.results_unsolved", " — not right yet");
			line = Component.literal("✘ ").withStyle(ChatFormatting.RED).append(chosen.copy().withStyle(ChatFormatting.WHITE))
					.append(then.copy().withStyle(ChatFormatting.GRAY));
		}
		return Component.translatableWithFallback("mine.npc.results_entry", "   %s: %s",
				Component.literal(answer.name()).withStyle(ChatFormatting.AQUA), line);
	}

	/** Forgets the answers to the nearest NPC's questions, e.g. before using it with another class. */
	private static int clear(CommandSourceStack source) throws CommandSyntaxException {
		Villager npc = NpcManager.nearest(source.getPlayerOrException(), REMOVE_DISTANCE, Permissions.isTeacher(source));
		if (npc == null) {
			source.sendFailure(Component.translatableWithFallback("mine.npc.none_near",
					"There's no NPC of yours within %s blocks of you.", (int) REMOVE_DISTANCE));
			return 0;
		}
		int cleared = QuizResults.get(source.getServer()).clear(npc.getUUID());
		source.sendSuccess(() -> Component.translatableWithFallback("mine.npc.results_cleared",
				"The answers to the %s questions of %s have been cleared.", cleared, npc.getDisplayName()), true);
		return 1;
	}

	/**
	 * Saves every quiz answer in a CSV file in the world and, if the teacher's game has MINE, also
	 * sends it to their computer.
	 */
	private static int export(CommandSourceStack source) {
		QuizResults results = QuizResults.get(source.getServer());
		int rows = results.all().stream().mapToInt(question -> question.answers().size()).sum();
		if (rows == 0) {
			source.sendFailure(Component.translatableWithFallback("mine.npc.export_empty", "Nobody has answered a quiz yet."));
			return 0;
		}
		String fileName = QuizExport.fileName();
		byte[] content = QuizExport.bytes(QuizExport.csv(results));
		Path file;
		try {
			file = Exports.save(source.getServer(), fileName, content);
		} catch (IOException e) {
			MineMod.LOGGER.error("[MINE] Could not save the quiz answers", e);
			source.sendFailure(Component.translatableWithFallback("mine.npc.export_failed",
					"The file couldn't be saved: %s", e.getMessage()));
			return 0;
		}
		MineMod.LOGGER.info("[MINE] Quiz answers exported to {}", file);
		source.sendSuccess(() -> Component.translatableWithFallback("mine.npc.export_saved",
				"%s answers saved on the server in %s", rows, file.toString()).withStyle(ChatFormatting.GREEN), true);
		ServerPlayer teacher = source.getPlayer();
		if (teacher != null && ExportPayload.canSend(teacher, content.length)) {
			ExportPayload.send(teacher, fileName, content);
		}
		return rows;
	}
}
