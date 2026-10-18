package io.github.francescolv.mine.lesson;

import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.agent.AgentKit;
import io.github.francescolv.mine.agent.AgentManager;
import io.github.francescolv.mine.agent.AgentProgram;
import io.github.francescolv.mine.blocks.BlockPrograms;
import io.github.francescolv.mine.text.ServerLanguage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lessons with missions: starts them, and when a student's program ends checks whether it
 * completed the current step. Only the server decides: there is no way to report a step as done.
 */
public final class LessonManager {

	/** What a running program is being checked against, decided when it started. */
	private record Pending(String lesson, int step, Check check, @Nullable Object memo, boolean[] stopped) {
	}

	/** A challenge built as soon as a step began, in front of the Agent, waiting for its first program. */
	private record Preview(String lesson, int step, AgentManager.Placement at, Checks.Challenge challenge, Object memo) {
	}

	/** By student. Only touched on the server thread; lost on restart, like the challenges' memos. */
	private static final Map<UUID, Preview> PREVIEWS = new HashMap<>();

	private LessonManager() {
	}

	public static void register() {
		AgentProgram.addListener(new AgentProgram.Listener() {
			@Override
			public void started(AgentProgram program) {
				LessonManager.started(program);
			}

			@Override
			public void tick(AgentProgram program) {
				LessonManager.tick(program);
			}

			@Override
			public void finished(AgentProgram program) {
				LessonManager.finished(program);
			}
		});
		AgentKit.setProvider(LessonManager::prepareKit);
		AgentKit.setStatus(LessonManager::statusLine);
	}

	/** Starts (or starts over) a lesson for the student, from the first step. */
	public static void start(ServerPlayer student, Lesson lesson) {
		LessonProgress.get(student.level().getServer()).put(student.getUUID(),
				new LessonProgress.Entry(lesson.id(), 0, 0, System.currentTimeMillis(), 0));
		// Programs made with blocks go straight among the student's: the editor shows them.
		BlockPrograms.get(student.level().getServer()).give(student.getUUID(), student.getName().getString(),
				lesson.blocksFor(student.clientInformation().language()));
		MineMod.LOGGER.info("[MINE] {} started lesson {}", student.getName().getString(), lesson.id());
		LessonFeedback.started(student, lesson);
		AgentManager.Placement agent = agentFor(student);
		preview(student.getUUID(), lesson, 0, agent);
		AgentManager.offerKit(student);
	}

	/**
	 * The student's Agent, created in front of them if they don't have one yet: a lesson needs
	 * nothing from the previous ones. {@code null} if it couldn't be created here.
	 */
	@Nullable
	private static AgentManager.Placement agentFor(ServerPlayer student) {
		AgentManager.Placement agent = AgentManager.placement(student);
		if (agent != null) {
			return agent;
		}
		switch (AgentManager.spawn(student)) {
			case SPAWNED, REPLACED -> student.sendSystemMessage(Component.translatableWithFallback("mine.agent.spawned",
					"Here is your Byte! Right-click it to program it.").withStyle(ChatFormatting.GREEN));
			case BLOCKED -> student.sendSystemMessage(Component.translatableWithFallback("mine.lesson.agent_blocked",
					"You don't have a Byte yet, and there's no room for it here: turn towards a free space and type /mine byte.")
					.withStyle(ChatFormatting.GOLD));
			default -> {
			}
		}
		return AgentManager.placement(student);
	}

	/** Starts the student's current lesson over; {@code false} if they don't have one. */
	public static boolean restart(ServerPlayer student) {
		Lesson lesson = current(student.level().getServer(), student.getUUID());
		if (lesson == null) {
			return false;
		}
		start(student, lesson);
		return true;
	}

	/** Longest answer kept: about what fits in a chat message. */
	static final int ANSWER_MAX_LENGTH = 256;

	/**
	 * Saves the student's answer to the reflection question of the lesson they have completed,
	 * replacing the previous one; {@code false} if they haven't completed a lesson.
	 */
	public static boolean answer(ServerPlayer student, String text) {
		LessonProgress progress = LessonProgress.get(student.level().getServer());
		Lesson lesson = current(student.level().getServer(), student.getUUID());
		if (lesson == null || progress.status(student.getUUID()) != LessonProgress.Status.COMPLETED) {
			return false;
		}
		String answer = text.strip();
		if (answer.length() > ANSWER_MAX_LENGTH) {
			answer = answer.substring(0, ANSWER_MAX_LENGTH) + "…";
		}
		progress.putAnswer(student.getUUID(), lesson.id(),
				new LessonProgress.Answer(student.getName().getString(), answer, System.currentTimeMillis()));
		MineMod.LOGGER.info("[MINE] {} answered the reflection of lesson {}: {}", student.getName().getString(), lesson.id(), answer);
		return true;
	}

	/** Forgets the student's lesson progress (not their answers). */
	public static void reset(ServerPlayer student) {
		LessonProgress.get(student.level().getServer()).remove(student.getUUID());
		discardPreview(student.getUUID());
	}

	/**
	 * Shows the step's challenge (if its check has one) in front of the Agent straight away,
	 * replacing the previous one; nothing if there's no room, the program will say so.
	 */
	private static void preview(UUID owner, Lesson lesson, int step, @Nullable AgentManager.Placement agent) {
		discardPreview(owner);
		if (agent == null) {
			return;
		}
		Object memo = lesson.steps().get(step).check().prepare(agent.world(), agent.pos(), agent.facing());
		if (memo instanceof Checks.Challenge challenge) {
			PREVIEWS.put(owner, new Preview(lesson.id(), step, agent, challenge, memo));
		}
	}

	private static void discardPreview(UUID owner) {
		Preview preview = PREVIEWS.remove(owner);
		if (preview != null) {
			preview.challenge().remove();
		}
	}

	/** The next hint for the current step, or {@code null} if there are no more (or no lesson). */
	@Nullable
	public static Component nextHint(ServerPlayer student) {
		return nextHint(student, false);
	}

	/**
	 * Reveals the next hint of the student's step, in the blocks' words if {@code blocks} (the
	 * block editor asks); {@code null} if there are no more. Lua and block hints are counted
	 * together: hint 2 is the second one in either words.
	 */
	@Nullable
	public static Component nextHint(ServerPlayer student, boolean blocks) {
		return nextHint(student, blocks ? Lesson.Language.BLOCKS : Lesson.Language.LUA);
	}

	/** As {@link #nextHint(ServerPlayer, boolean)}, in {@code language}'s words. */
	@Nullable
	public static Component nextHint(ServerPlayer student, Lesson.Language language) {
		LessonProgress progress = LessonProgress.get(student.level().getServer());
		LessonProgress.Entry entry = progress.get(student.getUUID());
		Lesson lesson = current(student.level().getServer(), student.getUUID());
		if (lesson == null || entry.status() != LessonProgress.Status.IN_PROGRESS) {
			return null;
		}
		int total = lesson.hintCount(entry.step(), language);
		if (entry.hints() >= total) {
			return null;
		}
		progress.put(student.getUUID(), entry.withHints(entry.hints() + 1));
		return Component.translatableWithFallback("mine.lesson.hint", "Hint %s/%s: %s",
				entry.hints() + 1, total, lesson.hintText(entry.step(), entry.hints(), language));
	}

	/** The lesson the student has started (in progress or completed), if it still exists. */
	@Nullable
	public static Lesson current(MinecraftServer server, UUID student) {
		LessonProgress.Entry entry = LessonProgress.get(server).get(student);
		return entry == null ? null : Lessons.get(entry.lesson());
	}

	/**
	 * What the student's Agent needs for their lesson: the items of the current step (once per
	 * step, in the highlighted slot), and the lesson's programs (copied once each time the lesson
	 * is started: the start time tells a restart apart).
	 */
	@Nullable
	private static AgentKit.Kit prepareKit(MinecraftServer server, UUID owner, String language,
			Container inventory, int selectedSlot) {
		LessonProgress progress = LessonProgress.get(server);
		LessonProgress.Entry entry = progress.get(owner);
		Lesson lesson = current(server, owner);
		if (lesson == null || entry.status() != LessonProgress.Status.IN_PROGRESS) {
			return null;
		}

		List<Component> lines = new ArrayList<>();
		if (entry.given() != entry.step()) {
			lesson.steps().get(entry.step()).give().ifPresent(give -> {
				int count = give(give, inventory, selectedSlot, server.overworld().getRandom());
				if (count > 0) {
					lines.add(Component.translatableWithFallback("mine.lesson.given",
							"MINE has put %s new items in your Byte's highlighted slot.", count));
				}
			});
			progress.put(owner, entry.withGiven(entry.step()));
		}

		Map<String, String> files = lesson.filesFor(language);
		String token = files.isEmpty() ? null : lesson.id() + "@" + entry.startedAt();
		List<Component> filesLines = files.isEmpty() ? List.of() : List.of(Component.translatableWithFallback("mine.lesson.files",
				"New programs for the lesson: %s",
				files.keySet().stream().map(name -> name.replaceFirst("\\.lua$", "")).collect(Collectors.joining(", "))));
		if (token == null && lines.isEmpty()) {
			return null;
		}
		return new AgentKit.Kit(token, files, filesLines, lines);
	}

	/** The student's lesson and step, for the Agent's welcome; {@code null} if they have none. */
	@Nullable
	private static Component statusLine(MinecraftServer server, UUID owner) {
		LessonProgress.Entry entry = LessonProgress.get(server).get(owner);
		Lesson lesson = current(server, owner);
		if (lesson == null) {
			return null;
		}
		if (entry.status() == LessonProgress.Status.COMPLETED) {
			return Component.translatableWithFallback("mine.lesson.welcome_completed", "Lesson: %s, completed",
					lesson.titleText());
		}
		return Component.translatableWithFallback("mine.lesson.welcome_step", "Lesson: %s, step %s of %s",
				lesson.titleText(), entry.step() + 1, lesson.steps().size());
	}

	/**
	 * Puts a step's items in the highlighted slot, moving whatever was there to a free slot.
	 * Returns how many were given: 0 if the Agent's inventory is full.
	 */
	private static int give(Lesson.Give give, Container inventory, int selectedSlot, RandomSource random) {
		int count = give.maxCount() > give.count()
				? give.count() + random.nextInt(give.maxCount() - give.count() + 1)
				: give.count();
		ItemStack current = inventory.getItem(selectedSlot);
		if (!current.isEmpty() && !current.is(give.item())) {
			int free = -1;
			for (int slot = 0; slot < inventory.getContainerSize() && free < 0; slot++) {
				if (slot != selectedSlot && inventory.getItem(slot).isEmpty()) {
					free = slot;
				}
			}
			if (free < 0) {
				return 0;
			}
			inventory.setItem(free, current);
		}
		inventory.setItem(selectedSlot, new ItemStack(give.item(), count));
		inventory.setChanged();
		return count;
	}

	private static void started(AgentProgram program) {
		MinecraftServer server = program.world().getServer();
		LessonProgress.Entry entry = LessonProgress.get(server).get(program.owner());
		Lesson lesson = current(server, program.owner());
		if (lesson == null || entry.status() != LessonProgress.Status.IN_PROGRESS) {
			return;
		}
		Check check = lesson.steps().get(entry.step()).check();
		// The challenge shown when the step began, if the Agent is still in front of it untouched.
		Preview preview = PREVIEWS.remove(program.owner());
		Object memo;
		if (preview != null && preview.lesson().equals(lesson.id()) && preview.step() == entry.step()
				&& preview.at().world() == program.world() && preview.at().pos().equals(program.startPos())
				&& preview.at().facing() == program.startFacing() && preview.challenge().untouched()) {
			memo = preview.memo();
			check.reuse(program, memo);
		} else {
			if (preview != null) {
				preview.challenge().remove();
			}
			memo = check.start(program);
		}
		program.setAttachment(new Pending(lesson.id(), entry.step(), check, memo, new boolean[1]));
	}

	/** A check that tests the program live has seen enough: the program is stopped, once. */
	private static void tick(AgentProgram program) {
		if (program.attachment() instanceof Pending pending && !pending.stopped()[0]
				&& pending.check().watch(program, pending.memo())) {
			pending.stopped()[0] = true;
			program.stop();
		}
	}

	private static void finished(AgentProgram program) {
		if (!(program.attachment() instanceof Pending pending)) {
			return;
		}
		boolean passed = program.succeeded() && evaluate(program, pending);
		pending.check().ended(program, pending.memo(), passed);
	}

	/** Moves the student on if the program completed their step; tells them why not otherwise. */
	private static boolean evaluate(AgentProgram program, Pending pending) {
		MinecraftServer server = program.world().getServer();
		LessonProgress progress = LessonProgress.get(server);
		LessonProgress.Entry entry = progress.get(program.owner());
		Lesson lesson = Lessons.get(pending.lesson());
		// The lesson may have been restarted or reset while the program was running.
		if (lesson == null || entry == null || !entry.lesson().equals(pending.lesson())
				|| entry.step() != pending.step() || entry.status() != LessonProgress.Status.IN_PROGRESS) {
			return false;
		}

		ServerPlayer student = server.getPlayerList().getPlayer(program.owner());
		Check check = pending.check();
		if (!check.passed(program, pending.memo())) {
			Component why = check.whyNot(program, pending.memo());
			Component told = why != null ? why : lesson.notYetText(entry.step());
			// In the log too: what the student was told, for the teacher (and the tests) to see.
			MineMod.LOGGER.info("[MINE] {} didn't complete step {} of lesson {}: {}",
					student != null ? student.getName().getString() : program.owner(),
					lesson.steps().get(entry.step()).id(), lesson.id(), ServerLanguage.render(told, "en_us"));
			LessonFeedback.notYet(program, student, told);
			return false;
		}

		boolean last = entry.step() + 1 == lesson.steps().size();
		progress.put(program.owner(), last ? entry.completed(System.currentTimeMillis()) : entry.nextStep());
		if (!last) {
			preview(program.owner(), lesson, entry.step() + 1,
					new AgentManager.Placement(program.world(), program.pos(), program.facing()));
		}
		if (last) {
			LessonFeedback.completed(program, student, lesson);
		} else {
			LessonFeedback.stepDone(program, student, lesson, entry.step() + 1);
		}
		if (student != null) {
			MineMod.LOGGER.info("[MINE] {} completed step {} of lesson {}", student.getName().getString(),
					lesson.steps().get(entry.step()).id(), lesson.id());
		}
		return true;
	}
}
