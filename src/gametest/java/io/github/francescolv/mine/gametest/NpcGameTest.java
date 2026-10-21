package io.github.francescolv.mine.gametest;

import io.github.francescolv.mine.lesson.LessonProgress;
import io.github.francescolv.mine.npc.NpcCondition;
import io.github.francescolv.mine.npc.NpcManager;
import io.github.francescolv.mine.npc.NpcQuiz;
import io.github.francescolv.mine.npc.NpcText;
import io.github.francescolv.mine.npc.QuizExport;
import io.github.francescolv.mine.npc.QuizResults;
import io.github.francescolv.mine.permission.Permissions;
import io.github.francescolv.mine.writing.Writing;
import io.github.francescolv.mine.writing.WritingPayloads;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * Teaching NPCs: the teacher creates them and writes their dialogue in a book, in game;
 * students read it, a page at a time, and click the buttons written in it.
 */
public class NpcGameTest {

	/** The teacher writes the dialogue in a book and gives it to the NPC; they can take it back to change it. */
	@GameTest
	public void theTeacherWritesTheDialogueInABook(GameTestHelper ctx) {
		ServerPlayer teacher = GameTests.player(ctx, true);
		Villager npc = NpcManager.create(teacher, "Ada");
		ctx.assertTrue(npc != null && NpcManager.isNpc(npc), "The NPC should have been created");
		ctx.assertTrue(npc.isNoAi() && npc.isInvulnerable(), "The NPC should stand still and not get hurt");

		teacher.setItemInHand(InteractionHand.MAIN_HAND, GameTests.bookWith("Hi! I'm Ada.", "Ready? [Start](/mine lesson start loops)", "", ""));
		NpcManager.interact(teacher, npc);
		ctx.assertTrue(NpcManager.dialogue(npc).equals(List.of("Hi! I'm Ada.", "Ready? [Start](/mine lesson start loops)")),
				"The book's pages, without the empty ones at the end: " + NpcManager.dialogue(npc));

		teacher.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		teacher.setShiftKeyDown(true);
		NpcManager.interact(teacher, npc);
		boolean gotBook = false;
		for (int slot = 0; slot < teacher.getInventory().getContainerSize(); slot++) {
			List<String> pages = NpcManager.pagesOf(teacher.getInventory().getItem(slot));
			gotBook |= pages != null && pages.equals(NpcManager.dialogue(npc));
		}
		ctx.assertTrue(gotBook, "Sneaking with an empty hand should give the teacher the dialogue back in a book");

		// A student with a book only reads: they can't change what the NPC says.
		ServerPlayer student = GameTests.player(ctx, false);
		student.setItemInHand(InteractionHand.MAIN_HAND, GameTests.bookWith("Hacked!"));
		NpcManager.interact(student, npc);
		ctx.assertTrue(NpcManager.dialogue(npc).size() == 2, "A student shouldn't be able to change the dialogue");
		ctx.succeed();
	}

	/** Buttons written as [label](/command) or [label](https://...) become clickable. */
	@GameTest
	public void linksBecomeButtons(GameTestHelper ctx) {
		Component page = NpcText.page("Ready? [Start](/mine lesson start loops) or [read](https://example.org) [this](nowhere)");
		List<ClickEvent> clicks = new ArrayList<>();
		page.visit((style, part) -> {
			if (style.getClickEvent() != null) {
				clicks.add(style.getClickEvent());
			}
			return java.util.Optional.empty();
		}, net.minecraft.network.chat.Style.EMPTY);
		ctx.assertTrue(clicks.size() == 2, "Two buttons expected, found " + clicks);
		ctx.assertTrue(clicks.get(0) instanceof ClickEvent.RunCommand run
				&& run.command().equals("/mine lesson start loops"), "Command button: " + clicks.get(0));
		ctx.assertTrue(clicks.get(1) instanceof ClickEvent.OpenUrl, "Link button: " + clicks.get(1));
		ctx.assertTrue(page.getString().contains("[this](nowhere)"), "Not a link: left as written. " + page.getString());
		ctx.succeed();
	}

	/**
	 * Students read the dialogue in a book window, where buttons can be clicked with the mouse:
	 * the NPC's name opens the first page, every page of the dialogue is there. Their inventory
	 * on the server doesn't change.
	 */
	@GameTest
	public void theDialogueOpensAsABook(GameTestHelper ctx) {
		Villager npc = NpcManager.create(GameTests.player(ctx, true), "Ada");
		NpcManager.setDialogue(npc, List.of("Hi! I'm Ada.", "Ready? [Start](/mine lesson start loops)"));
		ItemStack book = NpcManager.dialogueBook(npc);
		var content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
		ctx.assertTrue(content != null && content.pages().size() == 2, "Two pages expected");
		ctx.assertTrue(content.pages().getFirst().raw().getString().startsWith("Ada"), "The name opens the first page");
		boolean[] button = { false };
		content.pages().get(1).raw().visit((style, part) -> {
			button[0] |= style.getClickEvent() instanceof ClickEvent.RunCommand;
			return java.util.Optional.empty();
		}, net.minecraft.network.chat.Style.EMPTY);
		ctx.assertTrue(button[0], "The second page should have a clickable button");

		ServerPlayer student = GameTests.player(ctx, false);
		student.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT));
		NpcManager.talk(student, npc);
		ctx.assertTrue(student.getMainHandItem().is(Items.DIRT), "Reading shouldn't change what the student holds");
		ctx.succeed();
	}

	/** Whoever may change an NPC finds an Edit button next to its name; MINE's window saves the dialogue. */
	@GameTest
	public void writingAnNpcInMinesWindow(GameTestHelper ctx) {
		ServerPlayer teacher = GameTests.player(ctx, true);
		Villager npc = NpcManager.create(teacher, "Ada");
		NpcManager.setDialogue(npc, List.of("Hello"));
		String writerView = NpcManager.dialogueBook(npc, NpcManager.allPages(npc), NpcText.Notes.NONE, true)
				.get(DataComponents.WRITTEN_BOOK_CONTENT).pages().getFirst().raw().toString();
		ctx.assertTrue(writerView.contains("/mine npc edit " + npc.getUUID()), "The Edit button: " + writerView);
		String studentView = NpcManager.dialogueBook(npc, NpcManager.allPages(npc), NpcText.Notes.NONE, false)
				.get(DataComponents.WRITTEN_BOOK_CONTENT).pages().getFirst().raw().toString();
		ctx.assertFalse(studentView.contains("/mine npc edit"), "No button for students");

		ctx.assertTrue(Writing.save(teacher, WritingPayloads.Kind.NPC, npc.getUUID(), List.of("One", "Two", "", "")), "Saved");
		ctx.assertTrue(NpcManager.dialogue(npc).equals(List.of("One", "Two")), "Empty pages at the end dropped: " + NpcManager.dialogue(npc));
		ctx.assertFalse(Writing.save(GameTests.player(ctx, false), WritingPayloads.Kind.NPC, npc.getUUID(), List.of("Hacked")), "Not a student's");
		ctx.assertTrue(NpcManager.dialogue(npc).equals(List.of("One", "Two")), "Unchanged");
		ctx.succeed();
	}

	/** Clicking with a book nobody wrote in yet keeps the dialogue: the teacher is told how to write it. */
	@GameTest
	public void anEmptyBookKeepsTheDialogue(GameTestHelper ctx) {
		ServerPlayer teacher = GameTests.player(ctx, true);
		Villager npc = NpcManager.create(teacher, "Ada");
		NpcManager.setDialogue(npc, List.of("Hello"));
		teacher.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WRITABLE_BOOK));
		NpcManager.interact(teacher, npc);
		ctx.assertTrue(NpcManager.dialogue(npc).equals(List.of("Hello")), "Still: " + NpcManager.dialogue(npc));
		ctx.succeed();
	}

	/** The dialogue is saved with the NPC, so it survives a server restart. */
	@GameTest
	public void theDialogueIsSavedWithTheNpc(GameTestHelper ctx) {
		Villager npc = NpcManager.create(GameTests.player(ctx, true), "Ada");
		NpcManager.setDialogue(npc, List.of("One", "Two"));
		TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess());
		npc.saveWithoutId(saved);
		Villager loaded = new Villager(EntityTypes.VILLAGER, ctx.getLevel());
		loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess(), saved.buildResult()));
		ctx.assertTrue(NpcManager.isNpc(loaded) && NpcManager.dialogue(loaded).equals(List.of("One", "Two")),
				"Loaded: " + NpcManager.dialogue(loaded));
		ctx.succeed();
	}

	/** Only teachers create and remove NPCs; everyone can turn the pages, when close enough. */
	@GameTest
	public void commandsAndPermissions(GameTestHelper ctx) {
		ServerPlayer player = GameTests.player(ctx, false);
		CommandSourceStack asStudent = player.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS);
		CommandSourceStack asTeacher = player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER);

		GameTests.execute(ctx, asStudent, "mine npc create Ada", false);
		GameTests.execute(ctx, asTeacher, "mine npc create Ada Lovelace", true);
		Villager npc = ctx.getLevel().getEntitiesOfClass(Villager.class,
				player.getBoundingBox().inflate(5), NpcManager::isNpc).getFirst();
		ctx.assertTrue(npc.getCustomName().getString().equals("Ada Lovelace"), "Name: " + npc.getCustomName());


		GameTests.execute(ctx, asStudent, "mine npc remove", false);
		GameTests.execute(ctx, asTeacher, "mine npc remove", true);
		ctx.assertTrue(npc.isRemoved(), "The NPC should have been removed");
		GameTests.execute(ctx, asTeacher, "mine npc remove", false);
		ctx.succeed();
	}

	/**
	 * Builders (students the teacher allows) create NPCs and change only their own; the teacher
	 * changes everybody's. Removing the role takes the power away.
	 */
	@GameTest
	public void buildersChangeOnlyTheirOwnNpcs(GameTestHelper ctx) {
		ServerPlayer teacher = GameTests.player(ctx, true);
		ServerPlayer builder = GameTests.player(ctx, false);
		ServerPlayer other = GameTests.player(ctx, false);
		CommandSourceStack asTeacher = teacher.createCommandSourceStack();
		CommandSourceStack asBuilder = builder.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS);

		GameTests.execute(ctx, asBuilder, "mine npc create Mine", false);
		GameTests.execute(ctx, builder.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS), "mine builder add @s", false);
		// All test players have the same name: the teacher's command is run "as" the builder (@s).
		CommandSourceStack teacherAtBuilder = builder.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER);
		GameTests.execute(ctx, teacherAtBuilder, "mine builder add @s", true);
		ctx.assertTrue(Permissions.isBuilder(builder) && !Permissions.isBuilder(other), "Only the chosen student is a builder");

		Villager mine = NpcManager.create(builder, "Mine");
		Villager teachers = NpcManager.create(teacher, "Teacher's");
		ctx.assertTrue(NpcManager.canChange(builder, mine) && !NpcManager.canChange(builder, teachers),
				"A builder changes only their own NPCs");
		ctx.assertTrue(NpcManager.canChange(teacher, mine), "The teacher changes every NPC");
		ctx.assertTrue(!NpcManager.canChange(other, mine), "Other students only read");

		builder.setItemInHand(InteractionHand.MAIN_HAND, GameTests.bookWith("My museum"));
		NpcManager.interact(builder, mine);
		NpcManager.interact(builder, teachers);
		ctx.assertTrue(NpcManager.dialogue(mine).equals(List.of("My museum")) && NpcManager.dialogue(teachers).isEmpty(),
				"The builder's book should only change their own NPC");

		GameTests.execute(ctx, asTeacher, "mine builder list", true);
		GameTests.execute(ctx, teacherAtBuilder, "mine builder remove @s", true);
		ctx.assertTrue(!Permissions.isBuilder(builder) && !NpcManager.canChange(builder, mine),
				"Without the role, the student only reads, even their own NPCs");
		GameTests.execute(ctx, asBuilder, "mine npc create Again", false);
		ctx.succeed();
	}

	/** In the student view a teacher only reads, even their own NPCs, and loses MINE's teacher commands. */
	@GameTest
	public void theStudentView(GameTestHelper ctx) {
		ServerPlayer teacher = GameTests.player(ctx, true);
		CommandSourceStack asTeacher = teacher.createCommandSourceStack();
		Villager npc = NpcManager.create(teacher, "Ada");

		GameTests.execute(ctx, asTeacher, "mine view student", true);
		ctx.assertTrue(!Permissions.isTeacher(teacher) && !NpcManager.canChange(teacher, npc),
				"In the student view the teacher only reads");
		teacher.setItemInHand(InteractionHand.MAIN_HAND, GameTests.bookWith("Changed"));
		NpcManager.interact(teacher, npc);
		ctx.assertTrue(NpcManager.dialogue(npc).isEmpty(), "The book shouldn't change the NPC in the student view");
		GameTests.execute(ctx, asTeacher, "mine npc create Bob", false);
		GameTests.execute(ctx, asTeacher, "mine lesson status @s", false);

		GameTests.execute(ctx, asTeacher, "mine view teacher", true);
		ctx.assertTrue(Permissions.isTeacher(teacher) && NpcManager.canChange(teacher, npc), "Back to teacher");
		GameTests.execute(ctx, asTeacher, "mine lesson status @s", true);
		ctx.succeed();
	}

	/** A page with [ ] and [x] lines is a question; without [x] it is a poll. */
	@GameTest
	public void quizzesAreWrittenAsChecklists(GameTestHelper ctx) {
		NpcQuiz.Page parsed = NpcQuiz.parse("How many blocks?\nfor i = 1, 4\n[ ] 3\n[x] 4\n [X]   four\n\nWell done!");
		ctx.assertTrue(parsed != null && parsed.questions().size() == 1 && parsed.after().equals("Well done!"), "Parsed: " + parsed);
		NpcQuiz quiz = parsed.questions().getFirst();
		ctx.assertTrue(quiz.question().equals("How many blocks?\nfor i = 1, 4") && quiz.options().size() == 3, "Quiz: " + quiz);
		ctx.assertTrue(!quiz.options().get(0).right() && quiz.options().get(1).right() && quiz.options().get(2).right()
				&& quiz.options().get(2).text().equals("four"), "Answers: " + quiz.options());
		ctx.assertTrue(!quiz.isPoll(), "A question with right answers isn't a poll");
		ctx.assertTrue(NpcQuiz.parse("Which did you like?\n[ ] loops\n[ ] functions").questions().getFirst().isPoll(), "No [x]: a poll");
		ctx.assertTrue(NpcQuiz.parse("Just talking. [Start](/mine lesson start loops)\n[] not an answer") == null, "Not a question");

		// More questions on a page, each with its own answers.
		NpcQuiz.Page two = NpcQuiz.parse("2+2?\n[ ] 3\n[x] 4\n\n3+3?\n[x] 6\n[ ] 7");
		ctx.assertTrue(two.questions().size() == 2 && two.questions().get(0).question().equals("2+2?")
				&& two.questions().get(1).question().equals("3+3?") && two.questions().get(1).options().get(0).right()
				&& two.questions().get(0).options().size() == 2, "Two questions: " + two);

		// In the book, answers are buttons that choose them.
		Component page = NpcText.quiz(two, java.util.UUID.randomUUID(), 2, NpcText.Notes.NONE);
		List<String> commands = new ArrayList<>();
		page.visit((style, part) -> {
			if (style.getClickEvent() != null) {
				commands.add(style.getClickEvent() instanceof ClickEvent.RunCommand run ? run.command() : "");
			}
			return java.util.Optional.empty();
		}, net.minecraft.network.chat.Style.EMPTY);
		ctx.assertTrue(commands.size() == 4 && commands.get(1).startsWith("/mine npc choose ") && commands.get(1).endsWith(" 2 0 1")
				&& commands.get(2).endsWith(" 2 1 0"), "Answer buttons: " + commands);
		ctx.succeed();
	}

	/**
	 * Students choose answers in the book: the first choice is saved for the teacher, with how many
	 * tries it took to get it right. Polls keep the latest choice.
	 */
	@GameTest
	public void studentsAnswerQuizzes(GameTestHelper ctx) {
		ServerPlayer teacher = GameTests.player(ctx, true);
		Villager npc = NpcManager.create(teacher, "Quiz master");
		NpcManager.setDialogue(npc, List.of("Hi!", "How many?\n[ ] 3\n[x] 4", "Favourite?\n[ ] loops\n[ ] functions"));
		ServerPlayer alex = GameTests.player(ctx, false);
		ServerPlayer sam = GameTests.player(ctx, false);
		QuizResults results = QuizResults.get(ctx.getLevel().getServer());

		ctx.assertTrue(NpcManager.choose(alex, npc.getUUID(), 1, 0, 1) == NpcManager.Choice.RIGHT, "4 is right");
		ctx.assertTrue(NpcManager.choose(sam, npc.getUUID(), 1, 0, 0) == NpcManager.Choice.WRONG, "3 is wrong");
		ctx.assertTrue(NpcManager.choose(sam, npc.getUUID(), 1, 0, 0) == NpcManager.Choice.WRONG, "Still wrong");
		ctx.assertTrue(NpcManager.choose(sam, npc.getUUID(), 1, 0, 1) == NpcManager.Choice.RIGHT, "Finally right");
		// After getting it right, more clicks change nothing.
		NpcManager.choose(sam, npc.getUUID(), 1, 0, 0);

		QuizResults.Answer first = results.answer(npc.getUUID(), "How many?", alex.getUUID());
		ctx.assertTrue(first != null && first.right() && first.solved() && first.tries() == 1, "Alex: " + first);
		QuizResults.Answer tried = results.answer(npc.getUUID(), "How many?", sam.getUUID());
		ctx.assertTrue(tried != null && !tried.right() && tried.option() == 0 && tried.text().equals("3")
				&& tried.solved() && tried.tries() == 3, "Sam: " + tried);

		ctx.assertTrue(NpcManager.choose(alex, npc.getUUID(), 2, 0, 0) == NpcManager.Choice.SAVED, "Polls are just saved");
		NpcManager.choose(alex, npc.getUUID(), 2, 0, 1);
		QuizResults.Answer poll = results.answer(npc.getUUID(), "Favourite?", alex.getUUID());
		ctx.assertTrue(poll != null && poll.text().equals("functions"), "A poll keeps the latest choice: " + poll);
		ctx.assertTrue(results.of(npc.getUUID()).size() == 2
				&& results.of(npc.getUUID()).getFirst().text().equals("How many?"), "Questions in dialogue order");

		// Not a question, an answer that doesn't exist, or a question rewritten since the book was opened.
		ctx.assertTrue(NpcManager.choose(alex, npc.getUUID(), 0, 0, 0) == NpcManager.Choice.INVALID, "Page 0 isn't a question");
		ctx.assertTrue(NpcManager.choose(alex, npc.getUUID(), 1, 0, 5) == NpcManager.Choice.INVALID, "No answer F");
		ctx.assertTrue(NpcManager.choose(alex, java.util.UUID.randomUUID(), 1, 0, 1) == NpcManager.Choice.INVALID, "No such NPC");

		// The results survive a server restart.
		QuizResults loaded = QuizResults.fromNbt(results.save());
		ctx.assertTrue(tried.equals(loaded.answer(npc.getUUID(), "How many?", sam.getUUID())), "Saved and loaded");
		ctx.succeed();
	}

	/** Students must be next to the NPC to answer: they can't answer from anywhere by typing the command. */
	@GameTest
	public void quizzesAreAnsweredNextToTheNpc(GameTestHelper ctx) {
		Villager npc = NpcManager.create(GameTests.player(ctx, true), "Far");
		NpcManager.setDialogue(npc, List.of("Ok?\n[x] yes\n[ ] no"));
		ServerPlayer student = GameTests.player(ctx, false);
		student.snapTo(student.getX() + 20, student.getY(), student.getZ(), 0, 0);
		ctx.assertTrue(NpcManager.choose(student, npc.getUUID(), 0, 0, 0) == NpcManager.Choice.INVALID, "Too far to answer");
		ctx.succeed();
	}

	/** Teachers and the NPC's builder read the answers next to it; teachers also from afar, by name; and clear them. */
	@GameTest
	public void teachersReadTheResults(GameTestHelper ctx) {
		ServerPlayer player = GameTests.player(ctx, false);
		CommandSourceStack asStudent = player.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS);
		CommandSourceStack asTeacher = player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER);
		String name = "Results " + java.util.UUID.randomUUID().toString().substring(0, 8);
		Villager npc = NpcManager.create(player, name);
		NpcManager.setDialogue(npc, List.of("2+2?\n[ ] 3\n[x] 4"));

		GameTests.execute(ctx, asTeacher, "mine npc results", false);
		GameTests.execute(ctx, asStudent, "mine npc choose " + npc.getUUID() + " 0 0 0", true);
		GameTests.execute(ctx, asStudent, "mine npc choose not-a-uuid 0 0 0", false);
		GameTests.execute(ctx, asStudent, "mine npc results", false);
		GameTests.execute(ctx, asTeacher, "mine npc results", true);
		GameTests.execute(ctx, asTeacher, "mine npc results " + name, true);
		GameTests.execute(ctx, asTeacher, "mine npc results Nobody at all", false);

		GameTests.execute(ctx, asStudent, "mine npc clear", false);
		GameTests.execute(ctx, asTeacher, "mine npc clear", true);
		ctx.assertTrue(QuizResults.get(ctx.getLevel().getServer()).of(npc.getUUID()).isEmpty(), "Answers cleared");
		ctx.succeed();
	}

	/**
	 * The book always has every page, so students can turn back; the result of an answer is
	 * written under its question, and questions already got right keep their mark.
	 */
	@GameTest
	public void theResultIsUnderTheQuestion(GameTestHelper ctx) {
		Villager npc = NpcManager.create(GameTests.player(ctx, true), "Ada");
		NpcManager.setDialogue(npc, List.of("Hi!", "Ok?\n[x] yes\n[ ] no\n\nBye", "More?\n[x] sure\n[ ] no"));
		ServerPlayer student = GameTests.player(ctx, false);
		NpcManager.choose(student, npc.getUUID(), 1, 0, 0);

		var pages = NpcManager.dialogueBook(npc, NpcManager.allPages(npc), NpcManager.notes(student, npc,
				new NpcManager.Note(2, 0, Component.literal("Try again"))), false).get(DataComponents.WRITTEN_BOOK_CONTENT).pages();
		ctx.assertTrue(pages.size() == 3 && pages.get(0).raw().getString().startsWith("Ada"), "Every page, name first");
		String answered = pages.get(1).raw().getString();
		ctx.assertTrue(answered.indexOf("B) no") < answered.indexOf("✔") && answered.indexOf("✔") < answered.indexOf("Bye"),
				"The mark under the question got right, before the text after it: " + answered);
		String tried = pages.get(2).raw().getString();
		ctx.assertTrue(tried.endsWith("Try again") && !tried.contains("✔"), "The note under its question: " + tried);
		ctx.succeed();
	}

	/** The CSV has a row per student and question; texts with commas, quotes and new lines are quoted. */
	@GameTest
	public void quizAnswersAsCsv(GameTestHelper ctx) {
		Villager npc = NpcManager.create(GameTests.player(ctx, true), "Csv \"test\"");
		NpcManager.setDialogue(npc, List.of("Hi, how many?\nReally?\n[ ] 3\n[x] 4", "Liked it?\n[ ] yes, a lot\n[ ] no"));
		ServerPlayer student = GameTests.player(ctx, false);
		NpcManager.choose(student, npc.getUUID(), 0, 0, 0);
		NpcManager.choose(student, npc.getUUID(), 0, 0, 1);
		NpcManager.choose(student, npc.getUUID(), 1, 0, 0);

		String csv = QuizExport.csv(QuizResults.get(ctx.getLevel().getServer()));
		ctx.assertTrue(csv.startsWith("npc,page,question,type,student,first_answer,"), "Header: " + csv.lines().findFirst());
		ctx.assertTrue(csv.contains("\"Csv \"\"test\"\"\",1,\"Hi, how many?\nReally?\",quiz,test-mock-player,A,3,false,true,2,"),
				"Quiz row, quoted:\n" + csv);
		ctx.assertTrue(csv.contains("\"Csv \"\"test\"\"\",2,Liked it?,poll,test-mock-player,A,\"yes, a lot\",,,,"),
				"Poll row, no right or wrong:\n" + csv);
		ctx.succeed();
	}

	/** Only teachers export; the file is saved in the world, in mine/exports. */
	@GameTest
	public void teachersExportTheAnswers(GameTestHelper ctx) {
		ServerPlayer player = GameTests.player(ctx, false);
		Villager npc = NpcManager.create(player, "Export");
		NpcManager.setDialogue(npc, List.of("Ok?\n[x] yes\n[ ] no"));
		NpcManager.choose(player, npc.getUUID(), 0, 0, 0);

		GameTests.execute(ctx, player.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS), "mine npc export", false);
		java.nio.file.Path folder = ctx.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
				.resolve("mine").resolve("exports");
		long before = count(folder);
		GameTests.execute(ctx, player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER), "mine npc export", true);
		ctx.assertTrue(count(folder) > before || before > 0, "A CSV file should be in " + folder);
		ctx.succeed();
	}

	private static long count(java.nio.file.Path folder) {
		try (var files = java.nio.file.Files.list(folder)) {
			return files.filter(file -> file.toString().endsWith(".csv")).count();
		} catch (java.io.IOException e) {
			return 0;
		}
	}

	/**
	 * With two questions on a page, each has its own answers, and the result goes under the
	 * question just answered.
	 */
	@GameTest
	public void twoQuestionsOnAPage(GameTestHelper ctx) {
		Villager npc = NpcManager.create(GameTests.player(ctx, true), "Two");
		NpcManager.setDialogue(npc, List.of("2+2?\n[ ] 3\n[x] 4\n\n3+3?\n[x] 6\n[ ] 7", "Bye!"));
		String page = NpcManager.dialogueBook(npc, List.of(0), (p, q, quiz) -> q == 0 ? Component.literal("WRONG") : null, false)
				.get(DataComponents.WRITTEN_BOOK_CONTENT).pages().getFirst().raw().getString();
		ctx.assertTrue(page.indexOf("B) 4") < page.indexOf("WRONG") && page.indexOf("WRONG") < page.indexOf("3+3?"),
				"The result under the first question: " + page);

		ServerPlayer student = GameTests.player(ctx, false);
		ctx.assertTrue(NpcManager.choose(student, npc.getUUID(), 0, 1, 1) == NpcManager.Choice.WRONG, "7 is wrong");
		ctx.assertTrue(NpcManager.choose(student, npc.getUUID(), 0, 0, 1) == NpcManager.Choice.RIGHT, "4 is right");
		ctx.assertTrue(NpcManager.choose(student, npc.getUUID(), 0, 1, 0) == NpcManager.Choice.RIGHT, "6 is right");
		ctx.assertTrue(NpcManager.choose(student, npc.getUUID(), 0, 2, 0) == NpcManager.Choice.INVALID, "No third question");
		QuizResults results = QuizResults.get(ctx.getLevel().getServer());
		ctx.assertTrue(results.of(npc.getUUID()).stream().map(QuizResults.Question::text).toList().equals(List.of("2+2?", "3+3?")),
				"Both questions saved, in order");
		QuizResults.Answer second = results.answer(npc.getUUID(), "3+3?", student.getUUID());
		ctx.assertTrue(second != null && !second.right() && second.solved() && second.tries() == 2, "Second: " + second);
		ctx.succeed();
	}

	/** Lines starting with @ on top of a page say who it is for; the rest is what it says. */
	@GameTest
	public void pagesForSomeStudents(GameTestHelper ctx) {
		NpcCondition.Page page = NpcCondition.parse("@lesson loops 2\n@done loops\n@lesson loops 1-3\n@lesson none\n@lesso x\nHint!\n@not a condition");
		ctx.assertTrue(page.conditions().size() == 4 && page.errors().equals(List.of("@lesso x")), "Parsed: " + page);
		ctx.assertTrue(page.body().equals("Hint!\n@not a condition"), "Body: " + page.body());
		NpcCondition step = page.conditions().get(0);
		ctx.assertTrue(step.kind() == NpcCondition.Kind.LESSON && step.lesson().equals("loops") && step.fromStep() == 2
				&& step.toStep() == 2, "Step: " + step);
		ctx.assertTrue(page.conditions().get(1).kind() == NpcCondition.Kind.DONE, "Done");
		ctx.assertTrue(page.conditions().get(2).fromStep() == 1 && page.conditions().get(2).toStep() == 3, "Range");
		ctx.assertTrue(page.conditions().get(3).kind() == NpcCondition.Kind.NONE, "None");
		ctx.assertTrue(NpcCondition.parse("Hello").forEverybody(), "No conditions: for everybody");
		ctx.assertTrue(NpcCondition.parse("@lesson nope\nx").conditions().getFirst().problem() != null, "Unknown lesson");
		ctx.assertTrue(NpcCondition.parse("@lesson loops 9\nx").conditions().getFirst().problem() != null, "No step 9");
		ctx.assertTrue(NpcCondition.parse("@lesson loops 3\nx").conditions().getFirst().problem() == null, "Step 3 is fine");
		ctx.succeed();
	}

	/**
	 * The NPC follows the student's lesson: each student sees the pages for everybody and those for
	 * where they are. Whoever wrote the NPC sees every page, with who it is for.
	 */
	@GameTest
	public void theNpcFollowsTheLesson(GameTestHelper ctx) {
		ServerPlayer teacher = GameTests.player(ctx, true);
		Villager npc = NpcManager.create(teacher, "Guide");
		NpcManager.setDialogue(npc, List.of("Hi all", "@lesson loops 2\nHint for step 2", "@lesson loops\nDoing loops",
				"@done loops\nWell done", "@lesson none\nStart a lesson", "@lesso loops\nBroken"));
		ServerPlayer student = GameTests.player(ctx, false);
		LessonProgress progress = LessonProgress.get(ctx.getLevel().getServer());
		long now = System.currentTimeMillis();

		ctx.assertTrue(NpcManager.pagesFor(student, npc).equals(List.of(0, 4)), "No lesson: " + NpcManager.pagesFor(student, npc));
		progress.put(student.getUUID(), new LessonProgress.Entry("loops", 0, 0, now, 0));
		ctx.assertTrue(NpcManager.pagesFor(student, npc).equals(List.of(0, 2)), "Step 1: " + NpcManager.pagesFor(student, npc));
		progress.put(student.getUUID(), new LessonProgress.Entry("loops", 1, 0, now, 0));
		ctx.assertTrue(NpcManager.pagesFor(student, npc).equals(List.of(0, 1, 2)), "Step 2: " + NpcManager.pagesFor(student, npc));
		progress.put(student.getUUID(), new LessonProgress.Entry("loops", 3, 0, now, now));
		ctx.assertTrue(NpcManager.pagesFor(student, npc).equals(List.of(0, 3, 4)), "Completed: " + NpcManager.pagesFor(student, npc));
		// Completed lessons are remembered when the student moves on.
		progress.put(student.getUUID(), new LessonProgress.Entry("debugging", 0, 0, now, 0));
		ctx.assertTrue(NpcManager.pagesFor(student, npc).equals(List.of(0, 3)), "Next lesson: " + NpcManager.pagesFor(student, npc));
		progress.remove(student.getUUID());

		var pages = NpcManager.dialogueBook(npc, NpcManager.allPages(npc), NpcText.Notes.NONE, true)
				.get(DataComponents.WRITTEN_BOOK_CONTENT).pages();
		ctx.assertTrue(pages.size() == 6, "The writer sees every page");
		String hint = pages.get(1).raw().getString();
		ctx.assertTrue(hint.startsWith("For:") && hint.contains("loops") && hint.endsWith("Hint for step 2") && !hint.contains("@"),
				"Who it is for, then the page without its @ lines: " + hint);
		ctx.assertTrue(pages.get(5).raw().getString().contains("⚠ @lesso loops"), "Mistakes are shown to the writer");
		ctx.assertTrue(!pages.get(0).raw().getString().contains("For:"), "Pages for everybody have no label");
		ctx.succeed();
	}

	/** Questions on a page for some students work like any other. */
	@GameTest
	public void questionsOnPagesForSomeStudents(GameTestHelper ctx) {
		Villager npc = NpcManager.create(GameTests.player(ctx, true), "Conditional quiz");
		NpcManager.setDialogue(npc, List.of("@lesson none\nOk?\n[x] yes\n[ ] no"));
		ServerPlayer student = GameTests.player(ctx, false);
		ctx.assertTrue(NpcManager.choose(student, npc.getUUID(), 0, 0, 0) == NpcManager.Choice.RIGHT, "Right");
		ctx.assertTrue(QuizResults.get(ctx.getLevel().getServer()).answer(npc.getUUID(), "Ok?", student.getUUID()) != null,
				"The question is saved without its @ line");
		ctx.succeed();
	}
}
