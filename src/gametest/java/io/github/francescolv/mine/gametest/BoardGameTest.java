package io.github.francescolv.mine.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.francescolv.mine.board.BoardHit;
import io.github.francescolv.mine.board.BoardManager;
import io.github.francescolv.mine.npc.NpcCondition;
import io.github.francescolv.mine.npc.NpcManager;
import io.github.francescolv.mine.npc.NpcQuiz;
import io.github.francescolv.mine.npc.Templates;
import io.github.francescolv.mine.writing.Writing;
import io.github.francescolv.mine.writing.WritingPayloads;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Boards with large text, and NPCs and boards described in datapacks, for lessons that come with
 * their own characters.
 */
public class BoardGameTest {

	private static final Identifier GUIDE = Identifier.fromNamespaceAndPath("mine", "loops_guide");
	private static final Identifier BOARD = Identifier.fromNamespaceAndPath("mine", "loops");

	private static ServerPlayer player(GameTestHelper ctx, boolean teacher) {
		ServerPlayer player = ctx.makeMockServerPlayerInLevel();
		BlockPos feet = ctx.absolutePos(new BlockPos(3, 1, 1));
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0f, 0.0f);
		if (teacher) {
			ctx.getLevel().getServer().getPlayerList().getOps()
					.add(new net.minecraft.server.players.ServerOpListEntry(player.nameAndId(), LevelBasedPermissionSet.OWNER, false));
		}
		return player;
	}

	private static ItemStack bookWith(String... pages) {
		ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
		book.set(DataComponents.WRITABLE_BOOK_CONTENT,
				new WritableBookContent(List.of(pages).stream().map(Filterable::passThrough).toList()));
		return book;
	}

	/** MINE's example guide and board are read from its datapack, in every language they have. */
	@GameTest
	public void templatesComeFromDatapacks(GameTestHelper ctx) {
		Templates.NpcTemplate guide = Templates.npc(GUIDE);
		ctx.assertTrue(guide != null && guide.name().equals("Ada"), "The loops guide should be loaded");
		ctx.assertTrue(guide.pages("it_it").getFirst().startsWith("Ciao"), "Italian pages");
		ctx.assertTrue(guide.pages("fr_fr").getFirst().startsWith("Hi"), "No French: English");
		Templates.BoardTemplate board = Templates.board(BOARD);
		ctx.assertTrue(board != null && board.size() == 2 && board.text("it_it").getFirst().equals("# I cicli"), "The loops board");

		// Every NPC of MINE's datapack (the lab guide too): conditions MINE understands, in every language.
		for (Identifier id : Templates.npcIds()) {
			Templates.NpcTemplate npc = Templates.npc(id);
			for (String language : List.of("en_us", "it_it")) {
				ctx.assertTrue(npc.pages(language).size() == npc.pages("en_us").size(), id + " " + language + ": same pages as in English");
				for (String page : npc.pages(language)) {
					NpcCondition.Page parsed = NpcCondition.parse(page);
					ctx.assertTrue(parsed.errors().isEmpty(), id + " " + language + ": not understood " + parsed.errors());
					parsed.conditions().forEach(condition -> ctx.assertTrue(condition.problem() == null, id + ": " + condition));
				}
			}
		}
		ctx.assertTrue(Templates.npc(Identifier.fromNamespaceAndPath("mine", "lab_guide")) != null && Templates.board(Identifier.fromNamespaceAndPath("mine", "lab")) != null,
				"The lab's guide and board");

		// The example must be right: conditions MINE understands, for lessons and steps that exist, and real questions.
		for (String language : List.of("en_us", "it_it")) {
			List<String> pages = guide.pages(language);
			ctx.assertTrue(pages.size() == guide.pages("en_us").size(), language + ": same pages as in English");
			for (String page : pages) {
				NpcCondition.Page parsed = NpcCondition.parse(page);
				ctx.assertTrue(parsed.errors().isEmpty(), language + ": not understood " + parsed.errors());
				parsed.conditions().forEach(condition -> ctx.assertTrue(condition.problem() == null,
						language + ": " + condition));
			}
			ctx.assertTrue(NpcQuiz.parse(NpcCondition.parse(pages.get(5)).body()) != null, language + ": the quiz");
		}
		ctx.succeed();
	}

	/**
	 * An NPC placed from a datapack says its dialogue in each reader's language; a book of one's
	 * own replaces it. The link is saved with the NPC.
	 */
	@GameTest
	public void npcsFromDatapacks(GameTestHelper ctx) {
		ServerPlayer teacher = player(ctx, true);
		Villager npc = NpcManager.place(teacher, Templates.npc(GUIDE));
		ctx.assertTrue(npc != null && npc.getCustomName().getString().equals("Ada"), "Placed");
		ctx.assertTrue(NpcManager.dialogue(npc, teacher).equals(Templates.npc(GUIDE).pages("en_us")), "The datapack's dialogue");

		TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess());
		npc.saveWithoutId(saved);
		Villager loaded = new Villager(EntityTypes.VILLAGER, ctx.getLevel());
		loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess(), saved.buildResult()));
		ctx.assertTrue(NpcManager.template(loaded) != null, "The link is saved with the NPC");

		teacher.setItemInHand(InteractionHand.MAIN_HAND, bookWith("My own"));
		NpcManager.interact(teacher, npc);
		ctx.assertTrue(NpcManager.template(npc) == null && NpcManager.dialogue(npc, teacher).equals(List.of("My own")),
				"A book replaces the datapack's dialogue");

		CommandSourceStack asTeacher = teacher.createCommandSourceStack();
		execute(ctx, player(ctx, false).createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS), "mine npc place mine:loops_guide", false);
		execute(ctx, asTeacher, "mine npc place mine:nobody", false);
		ctx.succeed();
	}

	/**
	 * The teacher creates a board and writes it with a book; students can't change it. Sneaking
	 * with an empty hand gives the text back; boards can be resized and removed.
	 */
	@GameTest
	public void boardsAreWrittenWithABook(GameTestHelper ctx) {
		ServerPlayer teacher = player(ctx, true);
		Interaction board = BoardManager.create(teacher, 2, List.of());
		ctx.assertTrue(board != null && BoardManager.isBoard(board), "Created");
		Display.TextDisplay display = BoardManager.display(board);
		ctx.assertTrue(display != null && !display.getText().getString().isEmpty(), "An empty board says how to write it");

		teacher.setItemInHand(InteractionHand.MAIN_HAND, bookWith("# Rules\nBuild only in your area.\nHave fun!"));
		BoardManager.interact(teacher, board);
		ctx.assertTrue(display.getText().getString().equals("Rules\nBuild only in your area.\nHave fun!"),
				"The text, with the title without #: " + display.getText().getString());
		float small = board.getHeight();

		ServerPlayer student = player(ctx, false);
		student.setItemInHand(InteractionHand.MAIN_HAND, bookWith("Hacked"));
		BoardManager.interact(student, board);
		ctx.assertTrue(BoardManager.source(board).getFirst().startsWith("# Rules"), "Students can't change boards");

		teacher.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		teacher.setShiftKeyDown(true);
		BoardManager.interact(teacher, board);
		boolean gotBook = false;
		for (int slot = 0; slot < teacher.getInventory().getContainerSize(); slot++) {
			List<String> pages = NpcManager.pagesOf(teacher.getInventory().getItem(slot));
			gotBook |= pages != null && pages.equals(BoardManager.source(board));
		}
		ctx.assertTrue(gotBook, "The teacher gets the text back in a book");

		BoardManager.setSize(board, 4);
		ctx.assertTrue(board.getHeight() > small, "A bigger board is taller to click");
		BoardManager.remove(board);
		ctx.assertTrue(board.isRemoved() && display.isRemoved(), "Both parts removed");
		ctx.succeed();
	}

	/** Board commands: only teachers and builders; boards from datapacks; builders change only their own. */
	@GameTest
	public void boardCommands(GameTestHelper ctx) {
		ServerPlayer player = player(ctx, false);
		CommandSourceStack asStudent = player.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS);
		CommandSourceStack asTeacher = player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER);

		execute(ctx, asStudent, "mine board create", false);
		execute(ctx, asTeacher, "mine board place mine:loops", true);
		Interaction board = ctx.getLevel().getEntitiesOfClass(Interaction.class,
				player.getBoundingBox().inflate(5), BoardManager::isBoard).getFirst();
		ctx.assertTrue(BoardManager.source(board).getFirst().startsWith("# Loops"), "The datapack's text: " + BoardManager.source(board));
		execute(ctx, asTeacher, "mine board size 3", true);
		ctx.assertTrue(BoardManager.size(board) == 3, "Resized");
		execute(ctx, asTeacher, "mine board size 20", false);
		execute(ctx, asStudent, "mine board remove", false);
		execute(ctx, asTeacher, "mine board remove", true);
		ctx.assertTrue(board.isRemoved(), "Removed");
		execute(ctx, asTeacher, "mine board place mine:nothing", false);

		// A builder's board: theirs to change, not another builder's.
		ServerPlayer teacher = player(ctx, true);
		Interaction teachers = BoardManager.create(teacher, 2, List.of("Teacher's"));
		ServerPlayer builder = player(ctx, false);
		execute(ctx, builder.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER), "mine builder add @s", true);
		builder.setItemInHand(InteractionHand.MAIN_HAND, bookWith("Mine now"));
		BoardManager.interact(builder, teachers);
		ctx.assertTrue(BoardManager.source(teachers).equals(List.of("Teacher's")), "A builder can't change the teacher's board");
		execute(ctx, builder.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER), "mine builder remove @s", true);
		ctx.succeed();
	}

	/** A board's text can be clicked anywhere on it: where a look meets it, as Minecraft draws it. */
	@GameTest
	public void clickingABoardsText(GameTestHelper ctx) {
		Vec3 board = Vec3.ZERO;
		Vec3 ahead = new Vec3(0, 0, -1);
		// Size 2: 0.05 blocks a pixel. Two lines (up to 19 pixels high), 150 pixels wide.
		ctx.assertTrue(BoardHit.distance(new Vec3(0, 0.5, 3), ahead, board, 0, 2, 150, 2, 10) == 3, "In the middle, 3 blocks away");
		ctx.assertTrue(BoardHit.distance(new Vec3(3.5, 0.5, 3), ahead, board, 0, 2, 150, 2, 10) > 0, "Near the edge");
		ctx.assertTrue(BoardHit.distance(new Vec3(5, 0.5, 3), ahead, board, 0, 2, 150, 2, 10) < 0, "Beside the board");
		ctx.assertTrue(BoardHit.distance(new Vec3(0, 1.5, 3), ahead, board, 0, 2, 150, 2, 10) < 0, "Above the text");
		ctx.assertTrue(BoardHit.distance(new Vec3(0, 0.5, 12), ahead, board, 0, 2, 150, 2, 10) < 0, "Out of reach");
		ctx.assertTrue(BoardHit.distance(new Vec3(0, 0.5, 3), new Vec3(1, 0, 0), board, 0, 2, 150, 2, 10) < 0, "Looking away");
		ctx.assertTrue(BoardHit.distance(new Vec3(3, 0.5, 0), new Vec3(-1, 0, 0), board, 90, 2, 150, 2, 10) == 3, "A board facing east");
		ctx.succeed();
	}

	/** MINE's window saves a board's text for whoever may change it; nobody else, and not from afar. */
	@GameTest
	public void writingABoardInMinesWindow(GameTestHelper ctx) {
		ServerPlayer teacher = player(ctx, true);
		Interaction board = BoardManager.create(teacher, 2, List.of("Old"));
		ctx.assertTrue(Writing.save(teacher, WritingPayloads.Kind.BOARD, board.getUUID(), List.of("# New", "text")), "Saved");
		ctx.assertTrue(BoardManager.source(board).equals(List.of("# New", "text")), "The new text: " + BoardManager.source(board));

		ServerPlayer student = player(ctx, false);
		ctx.assertFalse(Writing.save(student, WritingPayloads.Kind.BOARD, board.getUUID(), List.of("Hacked")), "Not a student's");
		ctx.assertFalse(Writing.save(teacher, WritingPayloads.Kind.NPC, board.getUUID(), List.of("Hacked")), "Not an NPC");
		teacher.snapTo(teacher.getX() + 40, teacher.getY(), teacher.getZ());
		ctx.assertFalse(Writing.save(teacher, WritingPayloads.Kind.BOARD, board.getUUID(), List.of("Far")), "Not from afar");
		ctx.assertTrue(BoardManager.source(board).equals(List.of("# New", "text")), "Unchanged: " + BoardManager.source(board));
		BoardManager.remove(board);
		ctx.succeed();
	}

	private static void execute(GameTestHelper ctx, CommandSourceStack source, String command, boolean succeeds) {
		int result;
		try {
			result = ctx.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
		} catch (CommandSyntaxException e) {
			io.github.francescolv.mine.MineMod.LOGGER.info("[MINE test] /{} failed: {}", command, e.getMessage());
			result = 0;
		}
		ctx.assertTrue((result > 0) == succeeds, "/" + command + (succeeds ? " should succeed" : " should fail"));
	}
}
