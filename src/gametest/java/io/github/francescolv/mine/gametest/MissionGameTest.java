package io.github.francescolv.mine.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.agent.AgentManager;
import io.github.francescolv.mine.agent.AgentProgram;
import io.github.francescolv.mine.blocks.BlockPrograms;
import io.github.francescolv.mine.lesson.Lesson;
import io.github.francescolv.mine.lesson.LessonManager;
import io.github.francescolv.mine.lesson.LessonProgress;
import io.github.francescolv.mine.lesson.Lessons;
import io.github.francescolv.mine.text.ServerLanguage;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * Lesson missions: the server moves a student to the next step only when one of their programs
 * really did what the step asks, on their own Agent.
 */
public class MissionGameTest {

	private static final int TICKS = LessonGameTest.TICKS;
	/**
	 * Live tests take real seconds (MINE pulls a lever, waits for the Agent, looks), while the test
	 * server runs ticks as fast as it can: many more ticks.
	 */
	private static final int LIVE_TICKS = 20 * TICKS;

	/** Characters that fit on one line of the Agent's screen in the editor, leaving room for the cursor. */
	private static final int SCREEN_WIDTH = 38;

	/** Where the Agent appears: the student stands north of it, facing south (+Z). */
	private static final BlockPos AGENT = new BlockPos(2, 1, 1);
	private static final Path SOLUTIONS = LessonGameTest.LESSONS.resolve("01-first-steps").resolve("solutions");
	private static final Path LOOPS_SOLUTIONS = LessonGameTest.LESSONS.resolve("02-loops").resolve("solutions");
	private static final Path DEBUGGING_SOLUTIONS = LessonGameTest.LESSONS.resolve("03-debugging").resolve("solutions");
	private static final Path CONDITIONS_SOLUTIONS = LessonGameTest.LESSONS.resolve("04-conditions").resolve("solutions");
	private static final Path FUNCTIONS_SOLUTIONS = LessonGameTest.LESSONS.resolve("05-functions").resolve("solutions");
	private static final Path REDSTONE_SOLUTIONS = LessonGameTest.LESSONS.resolve("06-redstone").resolve("solutions");
	private static final Path SENSORS_SOLUTIONS = LessonGameTest.LESSONS.resolve("07-sensors").resolve("solutions");
	private static final Path BOARD_SOLUTIONS = LessonGameTest.LESSONS.resolve("08-board").resolve("solutions");
	private static final Path MAZE_SOLUTIONS = LessonGameTest.LESSONS.resolve("10-maze").resolve("solutions");
	private static final Path TREASURE_SOLUTIONS = LessonGameTest.LESSONS.resolve("11-treasure").resolve("solutions");
	private static final Path FARM_SOLUTIONS = LessonGameTest.LESSONS.resolve("15-farm").resolve("solutions");
	private static final Path CIPHER_SOLUTIONS = LessonGameTest.LESSONS.resolve("16-cipher").resolve("solutions");

	/** How many programs each computer has finished: tells a test when a program is over. */
	private static final Map<Integer, Integer> FINISHED = new ConcurrentHashMap<>();

	/** The last program each computer has finished. */
	private static final Map<Integer, AgentProgram> LAST = new ConcurrentHashMap<>();

	static {
		// Registered after LessonManager's listener: the lesson has already had its say.
		AgentProgram.addListener(program -> {
			LAST.put(program.computerId(), program);
			FINISHED.merge(program.computerId(), 1, Integer::sum);
		});
	}

	/** A student with an Agent that is turned on, and a few programs on its disk ({@code dir}). */
	private record Student(ServerPlayer player, TurtleBlockEntity turtle, ServerComputer computer, Path dir) {

		int computerId() {
			return computer.getID();
		}

		UUID uuid() {
			return player.getUUID();
		}
	}

	private static Student student(GameTestHelper ctx, BlockPos agent, Map<String, String> programs) {
		ServerPlayer player = ctx.makeMockServerPlayerInLevel();
		BlockPos feet = ctx.absolutePos(agent.north());
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0f, 0.0f);
		ctx.assertTrue(AgentManager.spawn(player) == AgentManager.Result.SPAWNED, "Agent not created");

		TurtleBlockEntity turtle = ctx.getBlockEntity(agent, TurtleBlockEntity.class);
		ServerComputer computer = turtle.createServerComputer();
		Path dir = ServerContext.get(ctx.getLevel().getServer()).storageDir()
				.resolve("computer").resolve(Integer.toString(computer.getID()));
		try {
			Files.createDirectories(dir);
			for (Map.Entry<String, String> program : programs.entrySet()) {
				Files.writeString(dir.resolve(program.getKey() + ".lua"), program.getValue());
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		computer.turnOn();
		return new Student(player, turtle, computer, dir);
	}

	private static Lesson lesson(String id) {
		Lesson lesson = Lessons.get(id);
		if (lesson == null) {
			throw new IllegalStateException("Lesson " + id + " was not loaded from the data files");
		}
		return lesson;
	}

	private static String solution(String name) {
		return solution(SOLUTIONS, name);
	}

	private static String solution(Path folder, String name) {
		try {
			return Files.readString(folder.resolve(name + ".lua"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** Steps run one after the other, a few per tick; the test passes when all are done. */
	private static final class Script {

		private final Deque<BooleanSupplier> steps = new ArrayDeque<>();

		Script then(BooleanSupplier step) {
			steps.add(step);
			return this;
		}

		Script check(Runnable check) {
			return then(() -> {
				check.run();
				return true;
			});
		}

		/** Starts a program from chat, as soon as the Agent is ready, and waits for it to end. */
		Script run(Student student, String program) {
			int[] before = { -1 };
			return then(() -> {
				if (before[0] < 0) {
					if (AgentManager.run(student.player(), program) != AgentManager.Result.STARTED) {
						return false;
					}
					before[0] = FINISHED.getOrDefault(student.computerId(), 0);
				}
				return FINISHED.getOrDefault(student.computerId(), 0) > before[0];
			});
		}

		void start(GameTestHelper ctx) {
			boolean[] done = { false };
			ctx.failIfEver(() -> {
				while (!done[0] && !steps.isEmpty() && steps.peek().getAsBoolean()) {
					steps.poll();
				}
				if (!done[0] && steps.isEmpty()) {
					done[0] = true;
					ctx.succeed();
				}
			});
		}
	}

	private static void expectStep(GameTestHelper ctx, Student student, int step) {
		LessonProgress.Entry entry = LessonProgress.get(ctx.getLevel().getServer()).get(student.uuid());
		ctx.assertTrue(entry != null && entry.status() == LessonProgress.Status.IN_PROGRESS && entry.step() == step,
				"Expected step " + step + ", found " + entry);
	}

	private static void expectStatus(GameTestHelper ctx, Student student, LessonProgress.Status status) {
		LessonProgress.Status actual = LessonProgress.get(ctx.getLevel().getServer()).status(student.uuid());
		ctx.assertTrue(actual == status, "Expected " + status + ", found " + actual);
	}

	/** The whole of lesson 1, done with the solutions published in the docs. */
	@GameTest(maxTicks = TICKS)
	public void firstStepsWithTheSolutions(GameTestHelper ctx) {
		Student s = student(ctx, AGENT, Map.of(
				"hello", solution("hello"), "roundtrip", solution("roundtrip"), "tower", solution("tower")));
		LessonManager.start(s.player(), lesson("first_steps"));
		s.turtle().getAccess().getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 3));

		new Script()
				.run(s, "hello").check(() -> expectStep(ctx, s, 1))
				.run(s, "roundtrip").check(() -> expectStep(ctx, s, 2))
				.run(s, "tower").check(() -> {
					expectStatus(ctx, s, LessonProgress.Status.COMPLETED);
					expectTold(ctx, s, "mine.lesson.answer_prompt_terminal");
				})
				.start(ctx);
	}

	/**
	 * After the lesson, the student answers the reflection question; the teacher reads every
	 * answer, also later. Answers survive moving on to another lesson, a reset, and saving.
	 */
	@GameTest
	public void reflectionAnswersAreKept(GameTestHelper ctx) {
		Student s = student(ctx, AGENT, Map.of());
		CommandSourceStack asStudent = s.player().createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS);
		CommandSourceStack asTeacher = s.player().createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER);
		LessonProgress progress = LessonProgress.get(ctx.getLevel().getServer());

		execute(ctx, asStudent, "mine lesson start first_steps", true);
		execute(ctx, asStudent, "mine lesson answer Too early", false);
		progress.put(s.uuid(), new LessonProgress.Entry("first_steps", 3, 0, 1, 2));
		execute(ctx, asStudent, "mine lesson answer Because I can run it again", true);
		execute(ctx, asStudent, "mine lesson answer Because I can run it again, without typing it", true);
		ctx.assertTrue("Because I can run it again, without typing it".equals(progress.answer(s.uuid(), "first_steps").text()),
				"The latest answer should be kept: " + progress.answer(s.uuid(), "first_steps"));

		execute(ctx, asStudent, "mine lesson answers first_steps", false);
		execute(ctx, asTeacher, "mine lesson answers first_steps", true);
		execute(ctx, asTeacher, "mine lesson answers loops", false);

		execute(ctx, asStudent, "mine lesson start loops", true);
		execute(ctx, asTeacher, "mine lesson reset @s", true);
		var registries = ctx.getLevel().registryAccess();
		LessonProgress loaded = LessonProgress.fromNbt(progress.save());
		// The test world is kept between runs: other students' answers may be there too.
		ctx.assertTrue(loaded.answer(s.uuid(), "first_steps") != null
						&& loaded.answers("first_steps").contains(loaded.answer(s.uuid(), "first_steps")),
				"The answer should survive a new lesson, a reset and saving");
		ctx.succeed();
	}

	/** MINE's lessons are read from its data files, in teaching order. */
	@GameTest
	public void lessonsAreLoadedFromData(GameTestHelper ctx) {
		List<String> ids = Lessons.all().stream().map(Lesson::id).toList();
		ctx.assertTrue(ids.equals(List.of("first_steps", "loops", "debugging", "conditions", "functions", "redstone", "sensors", "board", "charts", "maze", "treasure", "greenhouse", "control", "crossing", "farm", "cipher")), "Loaded lessons: " + ids);
		ctx.assertTrue(lesson("loops").steps().size() == 3, "Lesson 2 should have 3 steps");
		var italian = lesson("debugging").filesFor("it_it").keySet();
		ctx.assertTrue(italian.equals(java.util.Set.of("fila_rotta.lua", "pavimento_rotto.lua", "quadrato_rotto.lua")),
				"Lesson 3 Italian programs: " + italian);
		ctx.assertTrue(lesson("debugging").filesFor("xx_xx").containsKey("broken_row.lua"),
				"Other languages should get the English programs");
		// The Agent's screen is 39 characters wide and the editor doesn't wrap: keep lines shorter.
		for (Lesson lesson : Lessons.all()) {
			lesson.files().forEach((language, files) -> files.forEach((name, content) -> content.lines()
					.filter(line -> line.length() > SCREEN_WIDTH)
					.forEach(line -> ctx.assertTrue(false, lesson.id() + "/" + language + "/" + name
							+ " has a line wider than the Agent's screen: " + line))));
		}
		ctx.succeed();
	}

	/**
	 * The whole of lesson 2, done with the solutions published in the docs. Between steps the
	 * student moves to free space and recalls the Agent, as they would in class.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void loopsWithTheSolutions(GameTestHelper ctx) {
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of(
				"row", solution(LOOPS_SOLUTIONS, "row"),
				"floor", solution(LOOPS_SOLUTIONS, "floor"),
				"cube", solution(LOOPS_SOLUTIONS, "cube")));
		LessonManager.start(s.player(), lesson("loops"));

		new Script()
				.run(s, "row").check(() -> expectStep(ctx, s, 1))
				.check(() -> recallTo(ctx, s, new BlockPos(13, 1, 2)))
				.run(s, "floor").check(() -> expectStep(ctx, s, 2))
				.check(() -> recallTo(ctx, s, new BlockPos(13, 1, 9)))
				.run(s, "cube").check(() -> expectStatus(ctx, s, LessonProgress.Status.COMPLETED))
				.start(ctx);
	}

	/** Lesson 1 done with blocks: the Lua MINE's block editor makes from the solutions in the docs. */
	@GameTest(maxTicks = TICKS)
	public void firstStepsWithBlocks(GameTestHelper ctx) {
		firstStepsWithBlocks(ctx, SOLUTIONS.resolve("blocks"), "hello", "roundtrip", "tower");
	}

	@GameTest(maxTicks = TICKS)
	public void firstStepsWithBlocksItalian(GameTestHelper ctx) {
		firstStepsWithBlocks(ctx, SOLUTIONS.resolve("it").resolve("blocks"), "saluta", "andata", "torre");
	}

	private static void firstStepsWithBlocks(GameTestHelper ctx, Path blocks, String hello, String roundtrip, String tower) {
		Student s = student(ctx, AGENT, Map.of(hello, solution(blocks, hello), roundtrip, solution(blocks, roundtrip),
				tower, solution(blocks, tower)));
		LessonManager.start(s.player(), lesson("first_steps"));
		s.turtle().getAccess().getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 3));

		new Script()
				.run(s, hello).check(() -> expectStep(ctx, s, 1))
				.run(s, roundtrip).check(() -> expectStep(ctx, s, 2))
				.run(s, tower).check(() -> expectStatus(ctx, s, LessonProgress.Status.COMPLETED))
				.start(ctx);
	}

	/** Lesson 2 done with blocks, as the lesson with the Lua solutions. */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void loopsWithBlocks(GameTestHelper ctx) {
		loopsWithBlocks(ctx, LOOPS_SOLUTIONS.resolve("blocks"), "row", "floor", "cube");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void loopsWithBlocksItalian(GameTestHelper ctx) {
		loopsWithBlocks(ctx, LOOPS_SOLUTIONS.resolve("it").resolve("blocks"), "fila", "pavimento", "cubo");
	}

	private static void loopsWithBlocks(GameTestHelper ctx, Path blocks, String row, String floor, String cube) {
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of(row, solution(blocks, row), floor, solution(blocks, floor),
				cube, solution(blocks, cube)));
		LessonManager.start(s.player(), lesson("loops"));

		new Script()
				.run(s, row).check(() -> expectStep(ctx, s, 1))
				.check(() -> recallTo(ctx, s, new BlockPos(13, 1, 2)))
				.run(s, floor).check(() -> expectStep(ctx, s, 2))
				.check(() -> recallTo(ctx, s, new BlockPos(13, 1, 9)))
				.run(s, cube).check(() -> expectStatus(ctx, s, LessonProgress.Status.COMPLETED))
				.start(ctx);
	}

	/**
	 * The whole of lesson 6, done with the solutions in the docs: the lamp, then the door and the
	 * AND gate, which MINE tests live by pulling the levers, and stops once it has seen enough.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = LIVE_TICKS)
	public void redstoneWithTheSolutions(GameTestHelper ctx) {
		redstone(ctx, REDSTONE_SOLUTIONS, "lamp", "door", "and");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = LIVE_TICKS)
	public void redstoneWithTheSolutionsItalian(GameTestHelper ctx) {
		redstone(ctx, REDSTONE_SOLUTIONS.resolve("it"), "lampada", "porta", "e");
	}

	/** Lesson 6 done with blocks: the Lua MINE's block editor makes from the solutions in the docs. */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = LIVE_TICKS)
	public void redstoneWithBlocks(GameTestHelper ctx) {
		redstone(ctx, REDSTONE_SOLUTIONS.resolve("blocks"), "lamp", "door", "and");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = LIVE_TICKS)
	public void redstoneWithBlocksItalian(GameTestHelper ctx) {
		redstone(ctx, REDSTONE_SOLUTIONS.resolve("it").resolve("blocks"), "lampada", "porta", "e");
	}

	private static void redstone(GameTestHelper ctx, Path folder, String lamp, String door, String and) {
		groundForTheDoor(ctx);
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of(lamp, solution(folder, lamp), door, solution(folder, door),
				and, solution(folder, and)));
		LessonManager.start(s.player(), lesson("redstone"));

		new Script()
				.run(s, lamp).check(() -> expectStep(ctx, s, 1))
				.run(s, door).check(() -> expectStep(ctx, s, 2))
				.run(s, and).check(() -> expectStatus(ctx, s, LessonProgress.Status.COMPLETED))
				.start(ctx);
	}

	/**
	 * Lesson 7, with the published solutions: MINE steps on the plate, tricks the daylight sensor
	 * into night and back, and presses the button, while the programs run.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = LIVE_TICKS)
	public void sensorsWithTheSolutions(GameTestHelper ctx) {
		sensors(ctx, SENSORS_SOLUTIONS, "door", "lamp", "bridge");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = LIVE_TICKS)
	public void sensorsWithTheSolutionsItalian(GameTestHelper ctx) {
		sensors(ctx, SENSORS_SOLUTIONS.resolve("it"), "porta", "lampione", "ponte");
	}

	/** Lesson 7 done with blocks: waiting, "not" and a variable, from the block editor. */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = LIVE_TICKS)
	public void sensorsWithBlocks(GameTestHelper ctx) {
		sensors(ctx, SENSORS_SOLUTIONS.resolve("blocks"), "door", "lamp", "bridge");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = LIVE_TICKS)
	public void sensorsWithBlocksItalian(GameTestHelper ctx) {
		sensors(ctx, SENSORS_SOLUTIONS.resolve("it").resolve("blocks"), "porta", "lampione", "ponte");
	}

	private static void sensors(GameTestHelper ctx, Path folder, String door, String lamp, String bridge) {
		floor(ctx);
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of(door, solution(folder, door), lamp, solution(folder, lamp),
				bridge, solution(folder, bridge)));
		LessonManager.start(s.player(), lesson("sensors"));

		new Script()
				.run(s, door).check(() -> expectStep(ctx, s, 1))
				.run(s, lamp).check(() -> expectStep(ctx, s, 2))
				.run(s, bridge).check(() -> expectStatus(ctx, s, LessonProgress.Status.COMPLETED))
				.start(ctx);
	}

	/** Programs that don't wait, don't invert or don't remember don't pass. */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = LIVE_TICKS)
	public void sensorsNeedTimerAndMemory(GameTestHelper ctx) {
		floor(ctx);
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of(
				"follow", "while true do redstone.setOutput(\"front\", redstone.getInput(\"left\")) os.pullEvent(\"redstone\") end",
				"lamp", "while true do redstone.setOutput(\"back\", redstone.getInput(\"top\")) os.pullEvent(\"redstone\") end",
				"press", "while true do redstone.setOutput(\"front\", redstone.getInput(\"right\")) os.pullEvent(\"redstone\") end"));
		LessonManager.start(s.player(), lesson("sensors"));
		LessonProgress progress = LessonProgress.get(ctx.getLevel().getServer());

		new Script()
				.run(s, "follow").check(() -> {
					expectStep(ctx, s, 0);
					expectTold(ctx, s, "mine.lesson.check.plate_too_soon");
					progress.put(s.uuid(), new LessonProgress.Entry("sensors", 1, 0, 1, 0));
				})
				.run(s, "lamp").check(() -> {
					expectStep(ctx, s, 1);
					expectTold(ctx, s, "mine.lesson.check.night_lamp");
					progress.put(s.uuid(), new LessonProgress.Entry("sensors", 2, 0, 1, 0));
				})
				.run(s, "press").check(() -> {
					expectStep(ctx, s, 2);
					expectTold(ctx, s, "mine.lesson.check.bridge_back");
				})
				.start(ctx);
	}

	/**
	 * Lesson 8, with the published solutions: MINE plays the student's board (button A, the light)
	 * and pulls the lever, while the programs run; no board is connected.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = LIVE_TICKS)
	public void boardWithTheSolutions(GameTestHelper ctx) {
		board(ctx, BOARD_SOLUTIONS, "door", "lamp", "heart");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = LIVE_TICKS)
	public void boardWithTheSolutionsItalian(GameTestHelper ctx) {
		board(ctx, BOARD_SOLUTIONS.resolve("it"), "porta", "lampione", "cuore");
	}

	/** Lesson 8 done with blocks: the Board category, a comparison and an if/else. */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = LIVE_TICKS)
	public void boardWithBlocks(GameTestHelper ctx) {
		board(ctx, BOARD_SOLUTIONS.resolve("blocks"), "door", "lamp", "heart");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = LIVE_TICKS)
	public void boardWithBlocksItalian(GameTestHelper ctx) {
		board(ctx, BOARD_SOLUTIONS.resolve("it").resolve("blocks"), "porta", "lampione", "cuore");
	}

	private static void board(GameTestHelper ctx, Path folder, String door, String lamp, String heart) {
		floor(ctx);
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of(door, solution(folder, door), lamp, solution(folder, lamp),
				heart, solution(folder, heart)));
		LessonManager.start(s.player(), lesson("board"));

		new Script()
				.run(s, door).check(() -> expectStep(ctx, s, 1))
				.run(s, lamp).check(() -> expectStep(ctx, s, 2))
				.run(s, heart).check(() -> expectStatus(ctx, s, LessonProgress.Status.COMPLETED))
				.start(ctx);
	}

	/** A door that never closes, a lamp on in the light, a heart never switched off don't pass. */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = LIVE_TICKS)
	public void boardProgramsAreTested(GameTestHelper ctx) {
		floor(ctx);
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of(
				"open", "while true do mine.device.wait(\"pulsante_a\") redstone.setOutput(\"front\", true) end",
				"lamp", "while true do redstone.setOutput(\"back\", mine.device.read(\"luce\") > 50) os.sleep(0.5) end",
				"heart", "while true do os.pullEvent(\"redstone\") mine.device.send(\"cuore\", 1) end"));
		LessonManager.start(s.player(), lesson("board"));
		LessonProgress progress = LessonProgress.get(ctx.getLevel().getServer());

		new Script()
				.run(s, "open").check(() -> {
					expectStep(ctx, s, 0);
					expectTold(ctx, s, "mine.lesson.check.board_door_stays_open");
					progress.put(s.uuid(), new LessonProgress.Entry("board", 1, 0, 1, 0));
				})
				.run(s, "lamp").check(() -> {
					expectStep(ctx, s, 1);
					expectTold(ctx, s, "mine.lesson.check.board_lamp");
					progress.put(s.uuid(), new LessonProgress.Entry("board", 2, 0, 1, 0));
				})
				.run(s, "heart").check(() -> {
					expectStep(ctx, s, 2);
					expectTold(ctx, s, "mine.lesson.check.board_heart_off");
				})
				.start(ctx);
	}

	/** In the empty test structure the Agent floats: the iron door in front needs ground under it. */
	private static void groundForTheDoor(GameTestHelper ctx) {
		ctx.setBlock(LessonGameTest.LAB_AGENT.south().below(), Blocks.STONE);
	}

	/** A lamp that is always on isn't an AND gate: MINE's live test finds the case where it's wrong. */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = LIVE_TICKS)
	public void anAlwaysOnLampIsNotAnAndGate(GameTestHelper ctx) {
		groundForTheDoor(ctx);
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of("cheat",
				"redstone.setOutput(\"top\", true)\nwhile true do os.pullEvent(\"redstone\") end"));
		LessonManager.start(s.player(), lesson("redstone"));
		LessonProgress.get(ctx.getLevel().getServer()).put(s.uuid(), new LessonProgress.Entry("redstone", 2, 0, 1, 0));

		new Script()
				.run(s, "cheat")
				.check(() -> {
					expectStep(ctx, s, 2);
					expectTold(ctx, s, "mine.lesson.check.and_gate");
				})
				.start(ctx);
	}

	/** A floor of the wrong size doesn't count, and the student is told how big theirs is. */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void wrongSizeIsMeasured(GameTestHelper ctx) {
		String square = solution(LOOPS_SOLUTIONS, "floor").replace("for row = 1, 4", "for row = 1, 3");
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of("floor", square));
		LessonManager.start(s.player(), lesson("loops"));
		LessonProgress.get(ctx.getLevel().getServer()).put(s.uuid(), new LessonProgress.Entry("loops", 1, 0, 1, 0));
		s.turtle().getAccess().getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 64));

		new Script()
				.run(s, "floor")
				.check(() -> {
					expectStep(ctx, s, 1);
					expectTold(ctx, s, "mine.lesson.check.wrong_shape");
				})
				.start(ctx);
	}

	/**
	 * Lesson 3 copies its broken programs onto the Agent. They really are broken (the first two
	 * stop with an error, the third builds the wrong size), and fixing them completes the lesson.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void debuggingFromBrokenToFixed(GameTestHelper ctx) {
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of());
		Lesson lesson = lesson("debugging");
		Map<String, String> broken = lesson.filesFor("en_us");
		ctx.assertTrue(broken.keySet().equals(java.util.Set.of("broken_row.lua", "broken_floor.lua", "broken_square.lua")),
				"Lesson 3 programs: " + broken.keySet());
		LessonManager.start(s.player(), lesson);

		new Script()
				// The marker is written after all the programs.
				.then(() -> Files.exists(s.dir().resolve(".mine_lesson")))
				.check(() -> broken.forEach((name, content) ->
						ctx.assertTrue(content.equals(read(s.dir().resolve(name))), name + " was not copied as it is")))
				.run(s, "broken_row").check(() -> expectFailed(ctx, s, "broken_row"))
				.run(s, "broken_floor").check(() -> expectFailed(ctx, s, "broken_floor"))
				.check(() -> {
					write(s.dir().resolve("broken_row.lua"), solution(DEBUGGING_SOLUTIONS, "broken_row"));
					write(s.dir().resolve("broken_floor.lua"), solution(DEBUGGING_SOLUTIONS, "broken_floor"));
					recallTo(ctx, s, new BlockPos(13, 1, 2));
				})
				.run(s, "broken_row").check(() -> expectStep(ctx, s, 1))
				.check(() -> recallTo(ctx, s, new BlockPos(13, 1, 10)))
				.run(s, "broken_floor").check(() -> expectStep(ctx, s, 2))
				.check(() -> recallTo(ctx, s, new BlockPos(6, 1, 2)))
				.run(s, "broken_square").check(() -> {
					dumpTerminal(s, "broken_square");
					expectStep(ctx, s, 2);
					expectTold(ctx, s, "mine.lesson.check.wrong_shape");
					write(s.dir().resolve("broken_square.lua"), solution(DEBUGGING_SOLUTIONS, "broken_square"));
					recallTo(ctx, s, new BlockPos(6, 1, 8));
				})
				.run(s, "broken_square").check(() -> expectStatus(ctx, s, LessonProgress.Status.COMPLETED))
				.start(ctx);
	}

	/**
	 * Lesson 3 with blocks: starting it puts the broken block programs among the student's, in
	 * their language. Their Lua (as the block editor makes it) really is broken, and the fixed
	 * versions in the docs complete the lesson.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void debuggingWithBlocks(GameTestHelper ctx) {
		debuggingWithBlocks(ctx, "en_us", "blocks", "broken_row", "broken_floor", "broken_square");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void debuggingWithBlocksItalian(GameTestHelper ctx) {
		debuggingWithBlocks(ctx, "it_it", "it/blocks", "fila_rotta", "pavimento_rotto", "quadrato_rotto");
	}

	private static void debuggingWithBlocks(GameTestHelper ctx, String language, String fixed, String row, String floor,
			String square) {
		Lesson lesson = lesson("debugging");
		Map<String, String> given = lesson.blocksFor(language);
		ctx.assertTrue(given.keySet().equals(java.util.Set.of(row, floor, square)), "Lesson 3 block programs: " + given.keySet());
		Path blocks = DEBUGGING_SOLUTIONS.resolve(fixed);
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of(
				"row", lessonBlocks("debugging", language, row), "fixed_row", solution(blocks, row),
				"floor", lessonBlocks("debugging", language, floor), "fixed_floor", solution(blocks, floor),
				"square", lessonBlocks("debugging", language, square), "fixed_square", solution(blocks, square)));
		LessonManager.start(s.player(), lesson);
		if (language.equals("en_us")) {
			// The mock player's game is in English.
			BlockPrograms programs = BlockPrograms.get(ctx.getLevel().getServer());
			given.forEach((name, workspace) -> ctx.assertTrue(workspace.equals(programs.program(s.uuid(), name)),
					name + " was not given to the student"));
		}

		new Script()
				// "in front" instead of "below": one block, in the air.
				.run(s, "row").check(() -> expectStep(ctx, s, 0))
				.check(() -> recallTo(ctx, s, new BlockPos(13, 1, 2)))
				.run(s, "fixed_row").check(() -> expectStep(ctx, s, 1))
				// "up" inside the loop of the rows: a staircase.
				.check(() -> recallTo(ctx, s, new BlockPos(13, 1, 12)))
				.run(s, "floor").check(() -> {
					expectStep(ctx, s, 1);
					expectTold(ctx, s, "mine.lesson.check.wrong_shape");
					recallTo(ctx, s, new BlockPos(6, 1, 2));
				})
				.run(s, "fixed_floor").check(() -> expectStep(ctx, s, 2))
				.check(() -> recallTo(ctx, s, new BlockPos(4, 1, 8)))
				.run(s, "square").check(() -> {
					expectStep(ctx, s, 2);
					expectTold(ctx, s, "mine.lesson.check.wrong_shape");
					recallTo(ctx, s, new BlockPos(10, 1, 8));
				})
				.run(s, "fixed_square").check(() -> expectStatus(ctx, s, LessonProgress.Status.COMPLETED))
				.start(ctx);
	}

	/** The Lua of a block program a lesson gives, as tools/blocks-to-lua.js made it. */
	private static String lessonBlocks(String lesson, String language, String name) {
		String resource = "/mine_lesson_blocks/" + lesson + "/" + language + "/" + name + ".lua";
		try (java.io.InputStream in = MissionGameTest.class.getResourceAsStream(resource)) {
			if (in == null) {
				throw new IllegalStateException(resource + " is missing: run node tools/blocks-to-lua.js");
			}
			return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Lesson 4, with the published solutions: MINE builds a new challenge each time a program
	 * starts (a wall at a random distance, a row with gold in random places), so only programs
	 * that look at the world pass. The Agent digs the gold with the pickaxe it is created with.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void conditionsWithTheSolutions(GameTestHelper ctx) {
		conditions(ctx, "wall", "gold", "tower");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void conditionsWithTheItalianSolutions(GameTestHelper ctx) {
		conditions(ctx, "it/muro", "it/oro", "it/torre");
	}

	/** Lesson 4 done with blocks: "block below same as the slot's?" and "blocks in the slot". */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void conditionsWithBlocks(GameTestHelper ctx) {
		conditions(ctx, "blocks/wall", "blocks/gold", "blocks/tower");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void conditionsWithBlocksItalian(GameTestHelper ctx) {
		conditions(ctx, "it/blocks/muro", "it/blocks/oro", "it/blocks/torre");
	}

	private static void conditions(GameTestHelper ctx, String wall, String gold, String tower) {
		floor(ctx);
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of(
				"wall", solution(CONDITIONS_SOLUTIONS, wall),
				"gold", solution(CONDITIONS_SOLUTIONS, gold),
				"tower", solution(CONDITIONS_SOLUTIONS, tower)));
		LessonManager.start(s.player(), lesson("conditions"));

		new Script()
				.run(s, "wall").check(() -> expectStep(ctx, s, 1))
				.check(() -> recallTo(ctx, s, new BlockPos(13, 1, 2)))
				.run(s, "gold").check(() -> expectStep(ctx, s, 2))
				.check(() -> recallTo(ctx, s, new BlockPos(4, 1, 2)))
				// MINE gave the Agent a random number of planks for the tower: the program doesn't know how many.
				.run(s, "tower").check(() -> {
					expectStatus(ctx, s, LessonProgress.Status.COMPLETED);
					ctx.assertBlockPresent(net.minecraft.world.level.block.Blocks.OAK_PLANKS, new BlockPos(4, 3, 3));
				})
				.start(ctx);
	}

	/**
	 * Lesson 10, with the published solutions: a new corridor and new mazes every time, which the
	 * Agent leaves keeping a hand on the wall; at the last one it says how many steps it took.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void mazeWithTheSolutions(GameTestHelper ctx) {
		maze(ctx, "corridor", "maze", "steps");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void mazeWithTheItalianSolutions(GameTestHelper ctx) {
		maze(ctx, "it/corridoio", "it/labirinto", "it/passi");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void mazeWithBlocks(GameTestHelper ctx) {
		maze(ctx, "blocks/corridor", "blocks/maze", "blocks/steps");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void mazeWithBlocksItalian(GameTestHelper ctx) {
		maze(ctx, "it/blocks/corridoio", "it/blocks/labirinto", "it/blocks/passi");
	}

	private static void maze(GameTestHelper ctx, String corridor, String maze, String steps) {
		floor(ctx);
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of(
				"corridor", solution(MAZE_SOLUTIONS, corridor),
				"maze", solution(MAZE_SOLUTIONS, maze),
				"steps", solution(MAZE_SOLUTIONS, steps)));
		LessonManager.start(s.player(), lesson("maze"));

		new Script()
				.run(s, "corridor").check(() -> expectStep(ctx, s, 1))
				// The maze walked goes away: Byte comes back to the lane's start for the next one.
				.check(() -> recallTo(ctx, s, LessonGameTest.LAB_AGENT))
				.run(s, "maze").check(() -> expectStep(ctx, s, 2))
				.check(() -> recallTo(ctx, s, LessonGameTest.LAB_AGENT))
				.run(s, "steps").check(() -> expectStatus(ctx, s, LessonProgress.Status.COMPLETED))
				.start(ctx);
	}

	/**
	 * Lesson 11, with the published solutions: treasure buried in new places every time, found
	 * with the numbers of {@code mine.challenge()}, dug up, and Byte back home.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void treasureWithTheSolutions(GameTestHelper ctx) {
		treasure(ctx, "map", "dig", "three");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void treasureWithTheItalianSolutions(GameTestHelper ctx) {
		treasure(ctx, "it/mappa", "it/scava", "it/tre");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void treasureWithBlocks(GameTestHelper ctx) {
		treasure(ctx, "blocks/map", "blocks/dig", "blocks/three");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void treasureWithBlocksItalian(GameTestHelper ctx) {
		treasure(ctx, "it/blocks/mappa", "it/blocks/scava", "it/blocks/tre");
	}

	private static void treasure(GameTestHelper ctx, String map, String dig, String three) {
		floor(ctx);
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of(
				"map", solution(TREASURE_SOLUTIONS, map),
				"dig", solution(TREASURE_SOLUTIONS, dig),
				"three", solution(TREASURE_SOLUTIONS, three)));
		LessonManager.start(s.player(), lesson("treasure"));

		new Script()
				.run(s, "map").check(() -> expectStep(ctx, s, 1))
				.check(() -> recallTo(ctx, s, LessonGameTest.LAB_AGENT))
				.run(s, "dig").check(() -> expectStep(ctx, s, 2))
				.run(s, "three").check(() -> {
					expectStatus(ctx, s, LessonProgress.Status.COMPLETED);
					// The ground is put back as it was.
					LessonGameTest.expectBox(ctx, net.minecraft.world.level.block.Blocks.STONE, new BlockPos(4, 0, 2), new BlockPos(8, 0, 8));
				})
				.start(ctx);
	}

	/**
	 * Lesson 15, with the published solutions: a field dug in the ground, sown by Byte flying over
	 * it; then fields with ripe wheat in new places, harvested and sown again, and counted.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void farmWithTheSolutions(GameTestHelper ctx) {
		farm(ctx, "sow", "harvest", "count");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void farmWithTheItalianSolutions(GameTestHelper ctx) {
		farm(ctx, "it/semina", "it/raccolta", "it/conta");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void farmWithBlocks(GameTestHelper ctx) {
		farm(ctx, "blocks/sow", "blocks/harvest", "blocks/count");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void farmWithBlocksItalian(GameTestHelper ctx) {
		farm(ctx, "it/blocks/semina", "it/blocks/raccolta", "it/blocks/conta");
	}

	private static void farm(GameTestHelper ctx, String sow, String harvest, String count) {
		floor(ctx);
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of(
				"sow", solution(FARM_SOLUTIONS, sow),
				"harvest", solution(FARM_SOLUTIONS, harvest),
				"count", solution(FARM_SOLUTIONS, count)));
		LessonManager.start(s.player(), lesson("farm"));

		new Script()
				.run(s, "sow").check(() -> expectStep(ctx, s, 1))
				.check(() -> recallTo(ctx, s, LessonGameTest.LAB_AGENT))
				.run(s, "harvest").check(() -> expectStep(ctx, s, 2))
				.check(() -> recallTo(ctx, s, LessonGameTest.LAB_AGENT))
				.run(s, "count").check(() -> {
					expectStatus(ctx, s, LessonProgress.Status.COMPLETED);
					// The ground is put back as it was.
					LessonGameTest.expectBox(ctx, net.minecraft.world.level.block.Blocks.STONE, new BlockPos(6, 0, 3), new BlockPos(9, 0, 7));
				})
				.start(ctx);
	}

	/** Lesson 16 in Lua: a new secret message every time, decoded with the key 3, with MINE's key, then without. */
	@GameTest(maxTicks = TICKS)
	public void cipherWithTheSolutions(GameTestHelper ctx) {
		cipher(ctx, "caesar", "key", "crack");
	}

	@GameTest(maxTicks = TICKS)
	public void cipherWithTheItalianSolutions(GameTestHelper ctx) {
		cipher(ctx, "it/cesare", "it/chiave", "it/forza");
	}

	private static void cipher(GameTestHelper ctx, String caesar, String key, String crack) {
		Student s = student(ctx, AGENT, Map.of(
				"caesar", solution(CIPHER_SOLUTIONS, caesar),
				"key", solution(CIPHER_SOLUTIONS, key),
				"crack", solution(CIPHER_SOLUTIONS, crack),
				"wrong", "mine.say(mine.challenge())\n"));
		LessonManager.start(s.player(), lesson("cipher"));

		new Script()
				.run(s, "wrong").check(() -> {
					expectStep(ctx, s, 0);
					expectTold(ctx, s, "mine.lesson.check.cipher_wrong");
				})
				.run(s, "caesar").check(() -> expectStep(ctx, s, 1))
				.run(s, "key").check(() -> expectStep(ctx, s, 2))
				.run(s, "crack").check(() -> expectStatus(ctx, s, LessonProgress.Status.COMPLETED))
				.start(ctx);
	}

	/** A treasure missed is told with where it was and where Byte stopped, both counted from the start. */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void aMissedTreasureIsMeasured(GameTestHelper ctx) {
		floor(ctx);
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of("nothing", "turtle.turnLeft()\nturtle.turnRight()\n"));
		LessonManager.start(s.player(), lesson("treasure"));

		// Treasures are never where Byte starts: at least one block forward.
		new Script()
				.run(s, "nothing").check(() -> {
					expectStep(ctx, s, 0);
					expectTold(ctx, s, "mine.lesson.check.treasure_where");
					recallTo(ctx, s, LessonGameTest.LAB_AGENT);
					LessonProgress.get(ctx.getLevel().getServer()).put(s.uuid(), new LessonProgress.Entry("treasure", 1, 0, 1, 0));
				})
				.run(s, "nothing").check(() -> {
					expectStep(ctx, s, 1);
					expectTold(ctx, s, "mine.lesson.check.treasure_left");
				})
				.start(ctx);
	}

	/**
	 * In the maze only walking counts: climbing over the walls, breaking them or stopping short
	 * doesn't pass, and MINE says which; a wrong count of steps is told with the right one.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void theMazeIsWalkedNotClimbed(GameTestHelper ctx) {
		floor(ctx);
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of(
				"climb", "turtle.up()\nturtle.up()\nturtle.forward()\nturtle.down()\n",
				"dig", "turtle.turnLeft()\nturtle.dig()\n",
				"short", "turtle.forward()\n",
				"lie", solution(MAZE_SOLUTIONS, "maze").replace("mine.say(\"Out of the maze!\")", "mine.say(\"I took 1000 steps\")")));
		LessonManager.start(s.player(), lesson("maze"));
		LessonProgress.get(ctx.getLevel().getServer()).put(s.uuid(), new LessonProgress.Entry("maze", 2, 0, 1, 0));

		new Script()
				.run(s, "climb").check(() -> {
					expectStep(ctx, s, 2);
					expectTold(ctx, s, "mine.lesson.check.maze_over");
					recallTo(ctx, s, LessonGameTest.LAB_AGENT);
				})
				.run(s, "dig").check(() -> {
					expectStep(ctx, s, 2);
					expectTold(ctx, s, "mine.lesson.check.maze_broken");
					// The maze that wasn't passed is taken away.
					ctx.assertBlockPresent(net.minecraft.world.level.block.Blocks.AIR, LessonGameTest.LAB_AGENT.east());
					recallTo(ctx, s, LessonGameTest.LAB_AGENT);
				})
				.run(s, "short").check(() -> {
					expectStep(ctx, s, 2);
					expectTold(ctx, s, "mine.lesson.check.maze_exit");
					recallTo(ctx, s, LessonGameTest.LAB_AGENT);
				})
				.run(s, "lie").check(() -> {
					expectStep(ctx, s, 2);
					expectTold(ctx, s, "mine.lesson.check.maze_count");
				})
				.start(ctx);
	}

	/**
	 * A program that ignores the gold doesn't pass, is told how it went, and the row is taken
	 * away so the student can try again in the same place. No room in front: MINE says so.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void challengesAreMeasuredAndTakenAway(GameTestHelper ctx) {
		floor(ctx);
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of("nothing", "turtle.turnLeft()\nturtle.turnRight()\n"));
		LessonManager.start(s.player(), lesson("conditions"));
		LessonProgress.get(ctx.getLevel().getServer()).put(s.uuid(), new LessonProgress.Entry("conditions", 1, 0, 1, 0));

		new Script()
				.run(s, "nothing").check(() -> {
					expectStep(ctx, s, 1);
					expectTold(ctx, s, "mine.lesson.check.gold");
					LessonGameTest.expectBox(ctx, net.minecraft.world.level.block.Blocks.AIR,
							LessonGameTest.LAB_AGENT.south(), LessonGameTest.LAB_AGENT.south(8));
					ctx.setBlock(LessonGameTest.LAB_AGENT.south(3), net.minecraft.world.level.block.Blocks.DIRT);
				})
				.run(s, "nothing").check(() -> {
					expectStep(ctx, s, 1);
					expectTold(ctx, s, "mine.lesson.check.no_room");
				})
				.start(ctx);
	}

	/**
	 * A lesson needs nothing from the previous ones: starting it creates the student's Agent if
	 * they have none, and shows the first challenge in front of it straight away.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void startingALessonPreparesEverything(GameTestHelper ctx) {
		ServerPlayer player = ctx.makeMockServerPlayerInLevel();
		BlockPos feet = ctx.absolutePos(LessonGameTest.LAB_AGENT.north());
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0f, 0.0f);
		ctx.assertTrue(AgentManager.placement(player) == null, "The student shouldn't have an Agent yet");

		LessonManager.start(player, lesson("conditions"));
		AgentManager.Placement agent = AgentManager.placement(player);
		ctx.assertTrue(agent != null && agent.pos().equals(ctx.absolutePos(LessonGameTest.LAB_AGENT)),
				"Starting the lesson should have created the Agent in front of the student");
		int bricks = 0;
		for (int distance = 2; distance <= 6; distance++) {
			if (ctx.getBlockState(LessonGameTest.LAB_AGENT.south(distance)).is(net.minecraft.world.level.block.Blocks.BRICKS)) {
				bricks++;
			}
		}
		ctx.assertTrue(bricks == 1, "The wall should be in front of the Agent as soon as the lesson starts");
		ctx.succeed();
	}

	/**
	 * Each step gives the Agent what it needs, once, in the highlighted slot, moving whatever was
	 * there; and an Agent without tools (created before MINE gave them a pickaxe) gets one.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void theAgentGetsItsKit(GameTestHelper ctx) {
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of());
		var access = s.turtle().getAccess();
		access.setUpgrade(dan200.computercraft.api.turtle.TurtleSide.LEFT, null);
		access.getInventory().setItem(access.getSelectedSlot(), new ItemStack(Items.DIRT, 5));
		LessonManager.start(s.player(), lesson("loops"));

		new Script()
				.then(() -> access.getInventory().getItem(access.getSelectedSlot()).is(Items.OAK_PLANKS))
				.check(() -> {
					ctx.assertTrue(access.getInventory().getItem(access.getSelectedSlot()).getCount() == 64,
							"Lesson 2 gives 64 planks");
					boolean dirtKept = false;
					for (int slot = 0; slot < access.getInventory().getContainerSize(); slot++) {
						dirtKept |= access.getInventory().getItem(slot).is(Items.DIRT);
					}
					ctx.assertTrue(dirtKept, "What was in the highlighted slot should have been moved, not lost");
					ctx.assertTrue(access.getUpgrade(dan200.computercraft.api.turtle.TurtleSide.LEFT) != null,
							"The Agent should have been given its pickaxe");
					access.getInventory().setItem(access.getSelectedSlot(), ItemStack.EMPTY);
					AgentManager.offerKit(s.player());
				})
				// Offered again for the same step: nothing more is given.
				.then(new java.util.function.BooleanSupplier() {
					int ticks;

					@Override
					public boolean getAsBoolean() {
						return ++ticks > 40;
					}
				})
				.check(() -> ctx.assertTrue(access.getInventory().getItem(access.getSelectedSlot()).isEmpty(),
						"The items of a step should be given only once"))
				.start(ctx);
	}

	/**
	 * Lesson 5, with the published solutions: count the steps to a wall at a random distance,
	 * build a square as big as mine.challenge() says, then a pyramid with a function.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void functionsWithTheSolutions(GameTestHelper ctx) {
		functions(ctx, "count", "square", "pyramid");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void functionsWithTheItalianSolutions(GameTestHelper ctx) {
		functions(ctx, "it/passi", "it/quadrato", "it/piramide");
	}

	/** Lesson 5 done with blocks: a variable, the challenge's number, and a function with a parameter. */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void functionsWithBlocks(GameTestHelper ctx) {
		functions(ctx, "blocks/count", "blocks/square", "blocks/pyramid");
	}

	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void functionsWithBlocksItalian(GameTestHelper ctx) {
		functions(ctx, "it/blocks/passi", "it/blocks/quadrato", "it/blocks/piramide");
	}

	private static void functions(GameTestHelper ctx, String count, String square, String pyramid) {
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of(
				"count", solution(FUNCTIONS_SOLUTIONS, count),
				"square", solution(FUNCTIONS_SOLUTIONS, square),
				"pyramid", solution(FUNCTIONS_SOLUTIONS, pyramid)));
		LessonManager.start(s.player(), lesson("functions"));

		new Script()
				.run(s, "count").check(() -> expectStep(ctx, s, 1))
				// The wall stays, at x 7–9: a square up to 5 wide from x = 14 grows west only to x = 10.
				.check(() -> recallTo(ctx, s, new BlockPos(14, 1, 2)))
				.run(s, "square").check(() -> expectStep(ctx, s, 2))
				.check(() -> recallTo(ctx, s, new BlockPos(6, 1, 8)))
				.run(s, "pyramid").check(() -> {
					expectStatus(ctx, s, LessonProgress.Status.COMPLETED);
					ctx.assertBlockPresent(net.minecraft.world.level.block.Blocks.OAK_PLANKS, new BlockPos(4, 3, 10));
				})
				.start(ctx);
	}

	/** Saying every number, to be sure to say the right one, doesn't count. */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = TICKS)
	public void guessingTheNumberDoesNotCount(GameTestHelper ctx) {
		Student s = student(ctx, LessonGameTest.LAB_AGENT, Map.of("guess",
				"while not turtle.detect() do turtle.forward() end\nfor i = 1, 9 do mine.say(i) end\n"));
		LessonManager.start(s.player(), lesson("functions"));

		new Script()
				.run(s, "guess").check(() -> {
					expectStep(ctx, s, 0);
					expectTold(ctx, s, "mine.lesson.check.count_say");
				})
				.start(ctx);
	}

	/** Stone under the whole lab, so the Agent can come back "to the ground". */
	private static void floor(GameTestHelper ctx) {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				ctx.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.STONE);
			}
		}
	}

	/** Restarting the lesson copies the programs again, over the student's changes. */
	@GameTest(maxTicks = TICKS)
	public void restartCopiesTheProgramsAgain(GameTestHelper ctx) {
		Student s = student(ctx, AGENT, Map.of());
		LessonManager.start(s.player(), lesson("debugging"));
		Path row = s.dir().resolve("broken_row.lua");
		String original = lesson("debugging").filesFor("en_us").get("broken_row.lua");

		new Script()
				.then(() -> Files.exists(row) && original.equals(read(row)))
				.check(() -> {
					write(row, "-- my changes\n");
					ctx.assertTrue(LessonManager.restart(s.player()), "The lesson should restart");
				})
				.then(() -> original.equals(read(row)))
				.start(ctx);
	}

	private static void expectFailed(GameTestHelper ctx, Student student, String program) {
		dumpTerminal(student, program);
		AgentProgram last = LAST.get(student.computerId());
		ctx.assertTrue(last != null && !last.succeeded(), program + " should have stopped with an error");
		expectStep(ctx, student, 0);
	}

	/** Writes the Agent's screen to the test log: what a student sees when a program breaks. */
	private static void dumpTerminal(Student student, String program) {
		StringBuilder screen = new StringBuilder();
		screen(student).forEach(line -> screen.append("\n|").append(line));
		MineMod.LOGGER.info("[MINE test] Terminal after {}:{}", program, screen);
	}

	/** The lines on the Agent's screen, without trailing spaces. */
	private static List<String> screen(Student student) {
		try {
			java.lang.reflect.Field field = ServerComputer.class.getDeclaredField("terminal");
			field.setAccessible(true);
			dan200.computercraft.core.terminal.Terminal terminal =
					(dan200.computercraft.core.terminal.Terminal) field.get(student.computer());
			List<String> lines = new java.util.ArrayList<>();
			for (int y = 0; y < terminal.getHeight(); y++) {
				lines.add(terminal.getLine(y).toString().stripTrailing());
			}
			return lines;
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Can't read the Agent's terminal", e);
		}
	}

	/** When the Agent starts, it shows MINE's welcome instead of CraftOS's banner and tip of the day. */
	@GameTest(maxTicks = TICKS)
	public void theAgentWelcomesTheStudent(GameTestHelper ctx) {
		Student s = student(ctx, AGENT, Map.of());
		new Script()
				.then(() -> screen(s).stream().anyMatch(line -> line.startsWith("> ") || line.equals(">")))
				.check(() -> {
					List<String> screen = screen(s);
					ctx.assertTrue(screen.get(0).startsWith("MINE ") && screen.get(0).contains("'s Byte"),
							"First line: " + screen.get(0));
					ctx.assertTrue(screen.stream().noneMatch(line -> line.contains("CraftOS") || line.contains("tweaked.cc")),
							"CraftOS's banner should be gone: " + screen);
					ctx.assertTrue(screen.stream().anyMatch(line -> line.contains("help mine")), "Help line missing: " + screen);
				})
				.start(ctx);
	}

	private static String read(Path file) {
		try {
			return Files.exists(file) ? Files.readString(file) : "";
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void write(Path file, String content) {
		try {
			Files.writeString(file, content);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** Moves the student so that their Agent comes back in front of them at {@code agent}. */
	private static void recallTo(GameTestHelper ctx, Student student, BlockPos agent) {
		BlockPos feet = ctx.absolutePos(agent.north());
		student.player().snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0f, 0.0f);
		ctx.assertTrue(AgentManager.recall(student.player()) == AgentManager.Result.RECALLED, "Agent not recalled");
	}

	/** Programs that fail, or don't do what the step asks, never complete it. */
	@GameTest(maxTicks = TICKS)
	public void onlyRealWorkCompletesAStep(GameTestHelper ctx) {
		Student s = student(ctx, AGENT, Map.of(
				"broken", "mine.say('Hi!')\nerror('oops')\n",
				"spin", "turtle.turnLeft()\nturtle.turnRight()\n",
				"hello", "mine.say('Hi!')\n",
				"nothing", "\n"));
		LessonManager.start(s.player(), lesson("first_steps"));

		new Script()
				.run(s, "broken").check(() -> expectStep(ctx, s, 0))
				.run(s, "spin").check(() -> expectStep(ctx, s, 0))
				.run(s, "hello").check(() -> expectStep(ctx, s, 1))
				// Roundtrip: an empty program ends where it started, but never went anywhere.
				.run(s, "nothing").check(() -> expectStep(ctx, s, 1))
				.run(s, "spin").check(() -> expectStep(ctx, s, 1))
				.start(ctx);
	}

	/** A program on someone else's Agent doesn't count for me. */
	@GameTest(maxTicks = TICKS)
	public void anotherStudentsAgentDoesNotCount(GameTestHelper ctx) {
		Student me = student(ctx, AGENT, Map.of());
		Student other = student(ctx, AGENT.east(3), Map.of("hello", "mine.say('Hi!')\n"));
		LessonManager.start(me.player(), lesson("first_steps"));

		new Script()
				.run(other, "hello")
				.check(() -> {
					expectStep(ctx, me, 0);
					expectStatus(ctx, other, LessonProgress.Status.NOT_STARTED);
				})
				.start(ctx);
	}

	/**
	 * Blocks spread one per slot: turtle.place() only uses the selected one, so the tower stops at
	 * the first block. The student is told why, in the Agent's terminal too.
	 */
	@GameTest(maxTicks = TICKS)
	public void towerWithBlocksSpreadOut(GameTestHelper ctx) {
		Student s = student(ctx, AGENT, Map.of("tower", solution("tower")));
		LessonManager.start(s.player(), lesson("first_steps"));
		LessonProgress.get(ctx.getLevel().getServer()).put(s.uuid(), new LessonProgress.Entry("first_steps", 2, 0, 1, 0));
		for (int slot = 0; slot < 3; slot++) {
			s.turtle().getAccess().getInventory().setItem(slot, new ItemStack(Items.OAK_PLANKS));
		}

		new Script()
				.run(s, "tower")
				.check(() -> {
					expectStep(ctx, s, 2);
					expectTold(ctx, s, "mine.lesson.check.blocks_elsewhere");
				})
				.start(ctx);
	}

	/** Completing a step is written in the terminal too: the chat is hidden while it is open. */
	@GameTest(maxTicks = TICKS)
	public void outcomeIsWrittenInTheTerminal(GameTestHelper ctx) {
		Student s = student(ctx, AGENT, Map.of("hello", "mine.say('Hi!')\n", "nothing", "\n"));
		LessonManager.start(s.player(), lesson("first_steps"));

		new Script()
				.run(s, "hello").check(() -> expectTold(ctx, s, "mine.lesson.step_done"))
				.run(s, "nothing").check(() -> expectTold(ctx, s, "mine.lesson.first_steps.roundtrip.not_yet"))
				.start(ctx);
	}

	/** Terminal lines are translated on the server, in the student's language. */
	@GameTest
	public void terminalLinesAreTranslated(GameTestHelper ctx) {
		Component goal = Component.translatableWithFallback("mine.lesson.goal", "Step %s/%s: %s",
				1, 3, Component.translatableWithFallback("mine.lesson.first_steps.title", "First steps with Byte"));
		String italian = ServerLanguage.render(goal, "it_it");
		ctx.assertTrue(italian.equals("Tappa 1/3: Primi passi con Byte"), "Italian: " + italian);
		String english = ServerLanguage.render(goal, "xx_xx");
		ctx.assertTrue(english.equals("Step 1/3: First steps with Byte"), "English: " + english);
		ctx.succeed();
	}

	private static void expectTold(GameTestHelper ctx, Student student, String key) {
		AgentProgram program = LAST.get(student.computerId());
		boolean told = program != null && program.told().stream().anyMatch(line ->
				line.getContents() instanceof TranslatableContents t && t.getKey().equals(key));
		ctx.assertTrue(told, "The terminal should have shown " + key);
	}

	/** Progress is saved with the world and read back unchanged. */
	@GameTest
	public void progressSurvivesSaving(GameTestHelper ctx) {
		UUID student = UUID.randomUUID();
		LessonProgress saved = new LessonProgress();
		LessonProgress.Entry entry = new LessonProgress.Entry("first_steps", 2, 1, 1000, 0);
		saved.put(student, entry);

		var registries = ctx.getLevel().registryAccess();
		LessonProgress loaded = LessonProgress.fromNbt(saved.save());
		ctx.assertTrue(entry.equals(loaded.get(student)), "Loaded " + loaded.get(student) + " instead of " + entry);
		ctx.succeed();
	}

	/** Students restart their lesson and ask for hints; only teachers can reset others. */
	@GameTest
	public void commandsAndPermissions(GameTestHelper ctx) {
		Student s = student(ctx, AGENT, Map.of());
		CommandSourceStack asStudent = s.player().createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS);
		CommandSourceStack asTeacher = s.player().createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER);
		LessonProgress progress = LessonProgress.get(ctx.getLevel().getServer());

		execute(ctx, asStudent, "mine lesson hint", false);
		execute(ctx, asStudent, "mine lesson start first_steps", true);
		expectStep(ctx, s, 0);

		execute(ctx, asStudent, "mine lesson hint", true);
		execute(ctx, asStudent, "mine lesson hint", true);
		execute(ctx, asStudent, "mine lesson hint", false);
		ctx.assertTrue(progress.get(s.uuid()).hints() == 2, "Two hints should have been shown");

		progress.put(s.uuid(), new LessonProgress.Entry("first_steps", 1, 0, 1, 0));
		execute(ctx, asStudent, "mine lesson restart", true);
		expectStep(ctx, s, 0);

		execute(ctx, asStudent, "mine lesson reset @s", false);
		expectStep(ctx, s, 0);
		execute(ctx, asTeacher, "mine lesson reset @s", true);
		expectStatus(ctx, s, LessonProgress.Status.NOT_STARTED);
		execute(ctx, asStudent, "mine lesson start nonexistent", false);
		ctx.succeed();
	}

	private static void execute(GameTestHelper ctx, CommandSourceStack source, String command, boolean succeeds) {
		int result;
		try {
			result = ctx.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
		} catch (CommandSyntaxException e) {
			result = 0;
		}
		ctx.assertTrue((result > 0) == succeeds, "/" + command + (succeeds ? " should succeed" : " should fail"));
	}
}
