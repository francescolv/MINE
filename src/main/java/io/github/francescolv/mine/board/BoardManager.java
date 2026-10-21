package io.github.francescolv.mine.board;

import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.npc.NpcManager;
import io.github.francescolv.mine.writing.Placed;
import io.github.francescolv.mine.writing.Writing;
import io.github.francescolv.mine.writing.WritingPayloads;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/**
 * Boards: large text in the world, like the boards of Minecraft Education, for titles,
 * instructions and rules in a lesson's area. Whoever made one writes it in a book and quill, as
 * for NPCs: right-click the board with the book; sneak with an empty hand to get the text back.
 *
 * <p>A board is two vanilla entities, so students don't need anything installed: a text display
 * that shows the text, and an invisible interaction entity in its middle that can be clicked.
 * The interaction entity keeps the text as it was written, its size and its maker.
 */
public final class BoardManager {

	static final String TAG = "mine_board";
	static final String TEXT_TAG = "mine_board_text";

	/** Line width in pixels before the text wraps: about 25 letters. */
	static final int LINE_WIDTH = 150;
	/** Dark green with a little transparency: a school board. */
	private static final int BACKGROUND = 0xE0203828;

	public static final float DEFAULT_SIZE = 2;
	public static final float MIN_SIZE = 0.5f;
	public static final float MAX_SIZE = 8;

	static final AttachmentType<List<String>> SOURCE = AttachmentRegistry.createPersistent(
			Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "board_source"), Codec.STRING.listOf());
	/** The text display, also known to MINE's client: it clicks boards wherever their text is. */
	public static final AttachmentType<UUID> DISPLAY = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "board_display"), builder -> builder
					.persistent(UUIDUtil.AUTHLIB_CODEC).syncWith(UUIDUtil.STREAM_CODEC, AttachmentSyncPredicate.all()));
	static final AttachmentType<UUID> MAKER = AttachmentRegistry.createPersistent(
			Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "board_maker"), UUIDUtil.AUTHLIB_CODEC);
	public static final AttachmentType<Float> SIZE = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "board_size"), builder -> builder
					.persistent(Codec.FLOAT).syncWith(ByteBufCodecs.FLOAT, AttachmentSyncPredicate.all()));

	private BoardManager() {
	}

	public static void register() {
		Placed.onClick(Interaction.class, BoardManager::isBoard, BoardManager::interact);
	}

	public static boolean isBoard(@Nullable Entity entity) {
		return entity instanceof Interaction && entity.entityTags().contains(TAG);
	}

	/**
	 * Creates a board two blocks in front of its maker, facing them, with {@code pages} as text;
	 * {@code null} if there's no room.
	 */
	@Nullable
	public static Interaction create(ServerPlayer maker, float size, List<String> pages) {
		BlockPos pos = Placed.inFront(maker);
		Interaction board = create(maker.level(), pos, maker.getDirection().getOpposite(), maker.getUUID(), size, pages);
		if (board != null) {
			MineMod.LOGGER.info("[MINE] {} created a board at {}", maker.getName().getString(), pos.toShortString());
		}
		return board;
	}

	/** Creates a board at {@code pos}, its text facing {@code facing}; {@code null} if there's no room. */
	@Nullable
	public static Interaction create(ServerLevel world, BlockPos pos, Direction facing, UUID maker, float size,
			List<String> pages) {
		if (!world.getBlockState(pos).canBeReplaced()) {
			return null;
		}
		float yaw = facing.toYRot();
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.1;
		double z = pos.getZ() + 0.5;

		Display.TextDisplay display = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, world);
		display.snapTo(x, y, z, yaw, 0);
		display.setBillboardConstraints(Display.BillboardConstraints.FIXED);
		display.setLineWidth(LINE_WIDTH);
		display.setBackgroundColor(BACKGROUND);
		// Readable at night and in caves too.
		display.setBrightnessOverride(new Brightness(15, 15));
		display.addTag(TEXT_TAG);

		Interaction board = new Interaction(EntityTypes.INTERACTION, world);
		board.snapTo(x, y, z, yaw, 0);
		board.setResponse(true);
		board.addTag(TAG);
		board.setAttached(DISPLAY, display.getUUID());
		board.setAttached(MAKER, maker);

		world.addFreshEntity(display);
		world.addFreshEntity(board);
		update(board, display, size, pages);
		return board;
	}

	/** Teachers can change every board; builders only the ones they made. */
	public static boolean canChange(ServerPlayer player, Interaction board) {
		return Placed.canChange(player, board.getAttached(MAKER));
	}

	public static List<String> source(Interaction board) {
		return board.getAttachedOrElse(SOURCE, List.of());
	}

	public static float size(Interaction board) {
		return board.getAttachedOrElse(SIZE, DEFAULT_SIZE);
	}

	/** The text display showing the board's text; {@code null} if it has been removed by other means. */
	@Nullable
	public static Display.TextDisplay display(Interaction board) {
		UUID id = board.getAttached(DISPLAY);
		return id != null && board.level() instanceof ServerLevel world
				&& world.getEntity(id) instanceof Display.TextDisplay display ? display : null;
	}

	/** {@code pages} become the board's text, as from a book: the writer is told. */
	public static boolean write(ServerPlayer player, Interaction board, List<String> pages) {
		setText(board, pages);
		player.sendSystemMessage(Component.translatableWithFallback("mine.board.written", "The board now shows your book.")
				.withStyle(ChatFormatting.GREEN));
		return true;
	}

	public static void setText(Interaction board, List<String> pages) {
		update(board, display(board), size(board), pages);
	}

	public static void setSize(Interaction board, float size) {
		update(board, display(board), size, source(board));
	}

	public static void remove(Interaction board) {
		Display.TextDisplay display = display(board);
		if (display != null) {
			display.discard();
		}
		board.discard();
	}

	private static void update(Interaction board, @Nullable Display.TextDisplay display, float size,
			List<String> pages) {
		board.setAttached(SOURCE, List.copyOf(pages));
		board.setAttached(SIZE, size);
		String text = String.join("\n", pages).strip();
		if (display != null) {
			// An empty board would be invisible: say how to write it.
			display.setText(text.isEmpty()
					? Component.translatableWithFallback("mine.board.empty", "Empty board: right-click it with a book and quill.")
							.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
					: render(text));
			display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
					new Vector3f(size, size, size), new Quaternionf()));
		}
		// A thin box in the middle of the text, as tall as it is: wide enough to click, not so wide
		// that it gets in the way of the blocks around the board.
		board.setWidth(0.8f);
		board.setHeight(Math.max(0.5f, lines(text) * 10 * BoardHit.PIXEL * size + 2 * BoardHit.PIXEL * size));
	}

	/** About how many lines the text takes once wrapped (letters are about 6 pixels wide). */
	static int lines(String text) {
		int lines = 0;
		for (String line : text.split("\n", -1)) {
			lines += Math.max(1, (int) Math.ceil(line.length() * 6.0 / LINE_WIDTH));
		}
		return lines;
	}

	/** The board's text: lines starting with "# " are titles. */
	public static Component render(String text) {
		MutableComponent rendered = Component.empty();
		String[] lines = text.split("\n", -1);
		for (int i = 0; i < lines.length; i++) {
			if (i > 0) {
				rendered.append("\n");
			}
			String line = lines[i];
			if (line.startsWith("# ")) {
				rendered.append(Component.literal(line.substring(2).strip()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
			} else {
				rendered.append(Component.literal(line).withStyle(ChatFormatting.WHITE));
			}
		}
		return rendered;
	}

	/** Whoever may change the board writes it with a book, or gets its text back by sneaking with an empty hand. */
	public static void interact(ServerPlayer player, Interaction board) {
		if (!canChange(player, board)) {
			return;
		}
		ItemStack held = player.getMainHandItem();
		List<String> pages = NpcManager.pagesOf(held);
		if (pages != null && pages.isEmpty()) {
			// Clicking the board with a new book doesn't open it: say how to write it first.
			player.sendSystemMessage(Component.translatableWithFallback("mine.board.empty_book",
					"Your book is empty: hold it and right-click in the air to write the board's text, then right-click the board with it.")
					.withStyle(ChatFormatting.GOLD));
		} else if (pages != null) {
			write(player, board, pages);
		} else if (held.isEmpty() && !player.isShiftKeyDown() && Writing.open(player, WritingPayloads.Kind.BOARD, board.getUUID(),
				Component.translatableWithFallback("mine.board.book_name", "Board"), source(board))) {
			// The text opens in MINE's window.
		} else if (held.isEmpty() && player.isShiftKeyDown()) {
			player.addItem(book(board));
			player.sendSystemMessage(Component.translatableWithFallback("mine.board.book", "Here is the board's text: change it and give it back.")
					.withStyle(ChatFormatting.GREEN));
		} else {
			player.sendSystemMessage(Component.translatableWithFallback("mine.board.how_to_write",
					"Write the board's text in a book and quill (lines starting with # are titles), then right-click the board with it.")
					.withStyle(ChatFormatting.GOLD));
		}
	}

	/** A book and quill with the board's text, ready to be changed. */
	public static ItemStack book(Interaction board) {
		return Placed.book(source(board), Component.translatableWithFallback("mine.board.book_name", "Board"));
	}

	/** The board closest to the player within {@code distance} blocks, among those they may change (all if {@code teacher}). */
	@Nullable
	static Interaction nearest(ServerPlayer player, double distance, boolean teacher) {
		return Placed.nearest(player, Interaction.class, distance, board -> isBoard(board) && (teacher || canChange(player, board)));
	}
}
