package io.github.francescolv.mine.npc;

import com.mojang.serialization.Codec;
import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.PlayerSounds;
import io.github.francescolv.mine.permission.Permissions;
import io.github.francescolv.mine.writing.Writing;
import io.github.francescolv.mine.writing.WritingPayloads;
import io.github.francescolv.mine.writing.Placed;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Teaching NPCs: characters that talk to students, like the NPCs of Minecraft Education. Whoever
 * made one writes what it says in a book and quill, in game, and gives it to it; students
 * right-click them to read it in the chat, one page at a time.
 *
 * <p>Teachers and builders (see {@link Permissions}) create NPCs; builders can only change and
 * remove their own, teachers any of them.
 *
 * <p>An NPC is a vanilla villager that doesn't move, can't be hurt and doesn't trade, so
 * students don't need anything installed. Its dialogue and its maker are saved on the entity itself.
 */
public final class NpcManager {

	static final String TAG = "mine_npc";

	/** The dialogue: one entry per page of the book it was written in. */
	static final AttachmentType<List<String>> DIALOGUE = AttachmentRegistry.createPersistent(
			Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "npc_dialogue"), Codec.STRING.listOf());

	/**
	 * The datapack NPC ({@code mine_npc}) this one says the dialogue of, in each reader's language;
	 * dropped when someone gives it a dialogue of their own.
	 */
	static final AttachmentType<Identifier> TEMPLATE = AttachmentRegistry.createPersistent(
			Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "npc_template"), Identifier.CODEC);

	/** Who created the NPC: they may change it even if they are only a builder. */
	static final AttachmentType<UUID> MAKER = AttachmentRegistry.createPersistent(
			Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "npc_maker"), UUIDUtil.AUTHLIB_CODEC);

	private NpcManager() {
	}

	public static void register() {
		Placed.onClick(Villager.class, NpcManager::isNpc, NpcManager::interact);
		// Students play in Creative, where punching ignores invulnerability.
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
				isNpc(entity) ? InteractionResult.FAIL : InteractionResult.PASS);
	}

	public static boolean isNpc(@Nullable Entity entity) {
		return entity instanceof Villager && entity.entityTags().contains(TAG);
	}

	/** Creates an NPC in front of its maker, facing them; {@code null} if there's no room. */
	@Nullable
	public static Villager create(ServerPlayer maker, String name) {
		BlockPos pos = Placed.inFront(maker);
		Villager npc = create(maker.level(), pos, maker.getDirection().getOpposite(), maker.getUUID(), name);
		if (npc != null) {
			MineMod.LOGGER.info("[MINE] {} created the NPC {} at {}", maker.getName().getString(), name, pos.toShortString());
		}
		return npc;
	}

	/** Creates an NPC at {@code pos}, looking towards {@code facing}; {@code null} if there's no room. */
	@Nullable
	public static Villager create(ServerLevel world, BlockPos pos, Direction facing, UUID maker, String name) {
		if (!world.getBlockState(pos).canBeReplaced() || !world.getBlockState(pos.above()).canBeReplaced()) {
			return null;
		}
		Villager npc = new Villager(EntityTypes.VILLAGER, world);
		npc.setVillagerData(npc.getVillagerData().withProfession(world.registryAccess(), VillagerProfession.LIBRARIAN).withLevel(5));
		npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, facing.toYRot(), 0);
		npc.setYHeadRot(npc.getYRot());
		npc.setYBodyRot(npc.getYRot());
		npc.setNoAi(true);
		npc.setPermanentlyInvulnerable(true);
		npc.setSilent(true);
		npc.setPersistenceRequired();
		npc.setCustomName(Component.literal(name));
		npc.setCustomNameVisible(true);
		npc.addTag(TAG);
		npc.setAttached(DIALOGUE, List.of());
		npc.setAttached(MAKER, maker);
		world.addFreshEntity(npc);
		return npc;
	}

	/** Creates an NPC from a datapack at {@code pos}; {@code null} if there's no room. */
	@Nullable
	public static Villager place(ServerLevel world, BlockPos pos, Direction facing, UUID maker,
			Templates.NpcTemplate template) {
		Villager npc = create(world, pos, facing, maker, template.name());
		if (npc != null) {
			npc.setAttached(TEMPLATE, template.id());
		}
		return npc;
	}

	/** Teachers can change every NPC; builders only the ones they made. */
	public static boolean canChange(ServerPlayer player, Villager npc) {
		return Placed.canChange(player, npc.getAttached(MAKER));
	}

	/** The dialogue, in English if it comes from a datapack. */
	public static List<String> dialogue(Villager npc) {
		return dialogue(npc, null);
	}

	/** The dialogue as {@code reader} reads it: from a datapack, in their language. */
	public static List<String> dialogue(Villager npc, @Nullable ServerPlayer reader) {
		Templates.NpcTemplate template = template(npc);
		if (template != null) {
			return template.pages(reader == null ? Templates.DEFAULT_LANGUAGE : reader.clientInformation().language());
		}
		return npc.getAttachedOrElse(DIALOGUE, List.of());
	}

	/** The datapack NPC this one comes from; {@code null} if none, or if it is no longer in the datapacks. */
	@Nullable
	public static Templates.NpcTemplate template(Villager npc) {
		Identifier id = npc.getAttached(TEMPLATE);
		return id == null ? null : Templates.npc(id);
	}

	/** Gives the NPC its own dialogue, no longer the one of a datapack. */
	public static void setDialogue(Villager npc, List<String> pages) {
		npc.setAttached(DIALOGUE, List.copyOf(pages));
		npc.removeAttached(TEMPLATE);
	}

	/** Creates an NPC from a datapack in front of the maker; {@code null} if there's no room. */
	@Nullable
	public static Villager place(ServerPlayer maker, Templates.NpcTemplate template) {
		Villager npc = create(maker, template.name());
		if (npc != null) {
			npc.setAttached(TEMPLATE, template.id());
		}
		return npc;
	}

	/**
	 * Whoever may change the NPC writes its dialogue with a book, or gets it back in a book by
	 * sneaking with an empty hand; everybody else reads it.
	 */
	public static void interact(ServerPlayer player, Villager npc) {
		ItemStack held = player.getMainHandItem();
		if (canChange(player, npc)) {
			List<String> pages = pagesOf(held);
			if (pages != null && pages.isEmpty()) {
				// Clicking the NPC with a new book doesn't open it: say how to write it first.
				player.sendSystemMessage(Component.translatableWithFallback("mine.npc.empty_book",
						"Your book is empty: hold it and right-click in the air (not on %s) to write the dialogue, then give it to them.",
						npc.getDisplayName()).withStyle(ChatFormatting.GOLD));
				return;
			}
			if (pages != null) {
				write(player, npc, pages);
				return;
			}
			if (held.isEmpty() && player.isShiftKeyDown()) {
				giveBook(player, npc);
				return;
			}
		}
		talk(player, npc);
	}

	/** {@code pages} become what the NPC says, as from a book given to it: the writer is told. */
	public static boolean write(ServerPlayer player, Villager npc, List<String> written) {
		List<String> pages = new ArrayList<>(written);
		// Empty pages at the end are just unused paper.
		while (!pages.isEmpty() && pages.getLast().isBlank()) {
			pages.removeLast();
		}
		Templates.NpcTemplate template = template(npc);
		setDialogue(npc, pages);
		if (template != null) {
			player.sendSystemMessage(Component.translatableWithFallback("mine.npc.unlinked",
					"It no longer says the dialogue of %s: your book replaces it.", template.id().toString())
					.withStyle(ChatFormatting.GOLD));
		}
		player.sendSystemMessage(Component.translatableWithFallback("mine.npc.written", "%s will now say the %s pages of your book.",
				npc.getDisplayName(), pages.size()).withStyle(ChatFormatting.GREEN));
		checkConditions(player, pages);
		return true;
	}

	/** The NPC's dialogue to change: in MINE's window, or in a book for a game without MINE. */
	public static void edit(ServerPlayer player, Villager npc) {
		Component title = Component.translatableWithFallback("mine.npc.book_name", "Dialogue of %s", npc.getDisplayName());
		if (!Writing.open(player, WritingPayloads.Kind.NPC, npc.getUUID(), title, dialogue(npc, player))) {
			giveBook(player, npc);
		}
	}

	private static void giveBook(ServerPlayer player, Villager npc) {
		player.addItem(book(npc, player));
		player.sendSystemMessage(Component.translatableWithFallback("mine.npc.book", "Here is the dialogue of %s: change it and give it back.",
				npc.getDisplayName()).withStyle(ChatFormatting.GREEN));
	}

	/** Tells the writer about the pages' condition lines that MINE doesn't understand or can't use. */
	private static void checkConditions(ServerPlayer writer, List<String> pages) {
		for (int i = 0; i < pages.size(); i++) {
			NpcCondition.Page page = NpcCondition.parse(pages.get(i));
			for (String error : page.errors()) {
				writer.sendSystemMessage(Component.translatableWithFallback("mine.npc.condition.not_understood",
						"Page %s: I don't understand \"%s\". Write e.g. @lesson loops, @lesson loops 2, @done loops or @lesson none.",
						i + 1, error).withStyle(ChatFormatting.RED));
			}
			for (NpcCondition condition : page.conditions()) {
				Component problem = condition.problem();
				if (problem != null) {
					writer.sendSystemMessage(Component.translatableWithFallback("mine.npc.condition.problem", "Page %s: %s.",
							i + 1, problem).withStyle(ChatFormatting.RED));
				}
			}
		}
	}

	/** The pages of a book and quill or of a signed book; {@code null} if it isn't a book. */
	@Nullable
	public static List<String> pagesOf(ItemStack stack) {
		List<String> pages = new ArrayList<>();
		WritableBookContent writable = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
		WrittenBookContent written = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
		if (writable != null) {
			writable.pages().forEach(page -> pages.add(page.raw()));
		} else if (written != null) {
			written.pages().forEach(page -> pages.add(page.raw().getString()));
		} else {
			return null;
		}
		// Empty pages at the end are just unused paper.
		while (!pages.isEmpty() && pages.getLast().isBlank()) {
			pages.removeLast();
		}
		return pages;
	}

	/** A book and quill with the NPC's dialogue, ready to be changed. */
	public static ItemStack book(Villager npc, @Nullable ServerPlayer reader) {
		return Placed.book(dialogue(npc, reader), Component.translatableWithFallback("mine.npc.book_name", "Dialogue of %s",
				npc.getDisplayName()));
	}

	/**
	 * Opens the NPC's dialogue for the player in a book window, like the dialogue window of
	 * Minecraft Education's NPCs: pages turn with the book's arrows, and buttons can be clicked
	 * with the mouse (in the chat, they could only be clicked with the chat open).
	 *
	 * <p>The book is never given: the client is told for a moment that it holds it, opens it, and
	 * is then told again what it really holds. The player's inventory on the server never changes.
	 */
	public static void talk(ServerPlayer player, Villager npc) {
		talk(player, npc, 0, null);
	}

	/** A note written under a question of the dialogue, such as whether the answer was right. */
	public record Note(int page, int question, Component text) {
	}

	/**
	 * Opens the dialogue at {@code page}, with the player's notes under the questions they have
	 * answered and {@code note}, if any. The book has every page; a game with MINE is told to turn
	 * to {@code page}. Without MINE a book always opens at its first page, so it starts from
	 * {@code page} and the pages before it can't be read.
	 */
	public static void talk(ServerPlayer player, Villager npc, int page, @Nullable Note note) {
		List<String> pages = dialogue(npc, player);
		lookAt(npc, player.position());
		PlayerSounds.play(player, SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.6f, 1.0f);
		if (pages.isEmpty()) {
			// A new NPC: whoever may change it writes it straight away, in MINE's window.
			if (canChange(player, npc) && Writing.open(player, WritingPayloads.Kind.NPC, npc.getUUID(),
					Component.translatableWithFallback("mine.npc.book_name", "Dialogue of %s", npc.getDisplayName()), List.of())) {
				return;
			}
			player.sendSystemMessage(Component.translatableWithFallback("mine.npc.silent", "%s has nothing to say yet.",
					npc.getDisplayName()).withStyle(ChatFormatting.GRAY));
			if (canChange(player, npc)) {
				player.sendSystemMessage(Component.translatableWithFallback("mine.npc.how_to_write",
						"Write its dialogue in a book and quill (one page = one screen), then right-click it with the book.")
						.withStyle(ChatFormatting.GOLD));
			}
			return;
		}
		// Whoever may change the NPC sees every page, with who it is for; students only theirs.
		boolean writer = canChange(player, npc);
		List<Integer> visible = writer ? java.util.stream.IntStream.range(0, pages.size()).boxed().toList()
				: pagesFor(player, npc);
		if (visible.isEmpty()) {
			player.sendSystemMessage(Component.translatableWithFallback("mine.npc.nothing_now", "%s has nothing to say to you now.",
					npc.getDisplayName()).withStyle(ChatFormatting.GRAY));
			return;
		}
		int at = Math.max(0, visible.indexOf(page));
		int slot = player.getInventory().getSelectedSlot();
		player.connection.send(new ClientboundSetPlayerInventoryPacket(slot, dialogueBook(npc, pages, visible, notes(player, npc, note), writer)));
		player.connection.send(new ClientboundOpenBookPacket(InteractionHand.MAIN_HAND));
		player.connection.send(new ClientboundSetPlayerInventoryPacket(slot, player.getMainHandItem()));
		if (at > 0) {
			ServerPlayNetworking.send(player, new PagePayload(at));
		}
	}

	/** Every page of the dialogue, by number. */
	public static List<Integer> allPages(Villager npc) {
		return java.util.stream.IntStream.range(0, dialogue(npc).size()).boxed().toList();
	}

	/** The pages for the student: those for everybody, and those whose conditions match their lesson. */
	public static List<Integer> pagesFor(ServerPlayer student, Villager npc) {
		List<String> dialogue = dialogue(npc, student);
		return java.util.stream.IntStream.range(0, dialogue.size())
				.filter(i -> NpcCondition.parse(dialogue.get(i)).matches(student.level().getServer(), student.getUUID()))
				.boxed().toList();
	}

	/**
	 * The notes under the questions for {@code player}: {@code note} where it goes, "right" under
	 * the questions they have got right, "saved" under the polls they have answered.
	 */
	public static NpcText.Notes notes(ServerPlayer player, Villager npc, @Nullable Note note) {
		QuizResults results = QuizResults.get(player.level().getServer());
		return (page, question, quiz) -> {
			if (note != null && note.page() == page && note.question() == question) {
				return note.text();
			}
			QuizResults.Answer answer = results.answer(npc.getUUID(), quiz.question(), player.getUUID());
			if (answer == null) {
				return null;
			}
			return quiz.isPoll() ? noteText(Choice.SAVED) : answer.solved() ? noteText(Choice.RIGHT) : null;
		};
	}

	private static Component noteText(Choice choice) {
		return switch (choice) {
			case RIGHT -> Component.translatableWithFallback("mine.npc.quiz.right", "✔ Right!").withStyle(ChatFormatting.DARK_GREEN);
			case WRONG -> Component.translatableWithFallback("mine.npc.quiz.wrong", "✘ Not quite: try again.").withStyle(ChatFormatting.DARK_RED);
			default -> Component.translatableWithFallback("mine.npc.quiz.saved", "Answer saved, thank you!").withStyle(ChatFormatting.DARK_GREEN);
		};
	}

	/** A signed book with the NPC's whole dialogue: its name on the first page, buttons ready to click. */
	public static ItemStack dialogueBook(Villager npc) {
		return dialogueBook(npc, allPages(npc), NpcText.Notes.NONE, false);
	}

	/**
	 * A book with the {@code pages} of the dialogue, by number, and the NPC's name on top of the
	 * first one. Pages with questions show their answers as buttons, and the {@code notes} under
	 * them. With {@code labels}, pages for some students only say on top who they are for.
	 */
	public static ItemStack dialogueBook(Villager npc, List<Integer> pages, NpcText.Notes notes, boolean labels) {
		return dialogueBook(npc, dialogue(npc), pages, notes, labels);
	}

	private static ItemStack dialogueBook(Villager npc, List<String> dialogue, List<Integer> pages,
			NpcText.Notes notes, boolean labels) {
		List<Filterable<Component>> book = new ArrayList<>();
		for (int i : pages) {
			MutableComponent page = Component.empty();
			if (book.isEmpty()) {
				page.append(npc.getDisplayName().copy().withStyle(ChatFormatting.BOLD));
				if (labels) {
					// Whoever may change the NPC: the dialogue opens in MINE's window from here.
					page.append(" ").append(NpcText.editButton(npc.getUUID()));
				}
				page.append("\n\n");
			}
			NpcCondition.Page conditional = NpcCondition.parse(dialogue.get(i));
			if (labels && !conditional.forEverybody()) {
				page.append(NpcText.label(conditional)).append("\n");
			}
			NpcQuiz.Page quiz = NpcQuiz.parse(conditional.body());
			page.append(quiz == null ? NpcText.page(conditional.body()) : NpcText.quiz(quiz, npc.getUUID(), i, notes));
			book.add(Filterable.passThrough(page));
		}
		String name = npc.getDisplayName().getString();
		ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
		stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
				Filterable.passThrough(name.length() > 32 ? name.substring(0, 32) : name), name, 0, book, true));
		return stack;
	}

	/** How close a student must be to an NPC to answer its questions. */
	static final double ANSWER_DISTANCE = 8;

	/** What happened to a choice made in a quiz. */
	public enum Choice {
		RIGHT,
		WRONG,
		/** A poll: every answer is fine. */
		SAVED,
		/** Not an NPC nearby, not a question, or the question has changed since the book was opened. */
		INVALID
	}

	/**
	 * The student chose {@code option} in question {@code question} on page {@code page} of the
	 * NPC: the answer is saved and the dialogue opens again on the same page, with the result under
	 * the question.
	 */
	public static Choice choose(ServerPlayer student, UUID npcId, int page, int question, int option) {
		Entity entity = student.level().getEntity(npcId);
		if (!isNpc(entity) || entity.distanceToSqr(student) > ANSWER_DISTANCE * ANSWER_DISTANCE) {
			return Choice.INVALID;
		}
		Villager npc = (Villager) entity;
		List<String> dialogue = dialogue(npc, student);
		NpcQuiz.Page questions = page >= 0 && page < dialogue.size()
				? NpcQuiz.parse(NpcCondition.parse(dialogue.get(page)).body()) : null;
		if (questions == null || question < 0 || question >= questions.questions().size()) {
			return Choice.INVALID;
		}
		NpcQuiz quiz = questions.questions().get(question);
		if (option < 0 || option >= quiz.options().size()) {
			return Choice.INVALID;
		}
		QuizResults results = QuizResults.get(student.level().getServer());
		results.record(npc.getUUID(), npc.getDisplayName().getString(), page, question, quiz,
				student.getUUID(), student.getName().getString(), option, System.currentTimeMillis());
		MineMod.LOGGER.info("[MINE] {} answered {} to \"{}\" of {}", student.getName().getString(),
				NpcQuiz.letter(option), quiz.question(), npc.getDisplayName().getString());
		Choice choice = quiz.isPoll() ? Choice.SAVED : quiz.options().get(option).right() ? Choice.RIGHT : Choice.WRONG;
		// The same page again, with the result under the question: the student goes on with the book's arrow.
		talk(student, npc, page, new Note(page, question, noteText(choice)));
		if (choice == Choice.RIGHT) {
			PlayerSounds.play(student, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.2f);
		}
		return choice;
	}

	private static void lookAt(Villager npc, Vec3 target) {
		double dx = target.x - npc.getX();
		double dz = target.z - npc.getZ();
		float yaw = (float) (Mth.atan2(dz, dx) * (180 / Math.PI)) - 90;
		npc.setYRot(yaw);
		npc.setYHeadRot(yaw);
		npc.setYBodyRot(yaw);
	}

	/**
	 * The NPC closest to the player within {@code distance} blocks, among those they may change:
	 * all of them if {@code teacher} (e.g. a command run with a teacher's permission level).
	 */
	@Nullable
	static Villager nearest(ServerPlayer player, double distance, boolean teacher) {
		return Placed.nearest(player, Villager.class, distance, npc -> isNpc(npc) && (teacher || canChange(player, npc)));
	}
}
