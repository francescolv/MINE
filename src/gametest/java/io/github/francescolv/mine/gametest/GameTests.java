package io.github.francescolv.mine.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import io.github.francescolv.mine.MineMod;
import io.github.francescolv.mine.agent.AgentManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;

/** What many GameTests do: run commands, make players, books and Agents, talk to MINE's web pages. */
final class GameTests {

	private GameTests() {
	}

	/** Runs {@code command} as {@code source}: the test fails if it doesn't succeed (or fail) as expected. */
	static void execute(GameTestHelper ctx, CommandSourceStack source, String command, boolean succeeds) {
		int result;
		try {
			result = ctx.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
		} catch (CommandSyntaxException e) {
			MineMod.LOGGER.info("[MINE test] /{} failed: {}", command, e.getMessage());
			result = 0;
		}
		ctx.assertTrue((result > 0) == succeeds, "/" + command + (succeeds ? " should succeed" : " should fail"));
	}

	/** A player in the test, a teacher if {@code teacher}. */
	static ServerPlayer player(GameTestHelper ctx, boolean teacher) {
		ServerPlayer player = ctx.makeMockServerPlayerInLevel();
		BlockPos feet = ctx.absolutePos(new BlockPos(3, 1, 1));
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0f, 0.0f);
		if (teacher) {
			// Level 4 explicitly: the test server gives operators level 0.
			ctx.getLevel().getServer().getPlayerList().getOps()
					.add(new ServerOpListEntry(player.nameAndId(), LevelBasedPermissionSet.OWNER, false));
		}
		return player;
	}

	/** A book and quill with {@code pages}. */
	static ItemStack bookWith(String... pages) {
		ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
		book.set(DataComponents.WRITABLE_BOOK_CONTENT,
				new WritableBookContent(List.of(pages).stream().map(Filterable::passThrough).toList()));
		return book;
	}

	/** The student's Byte at {@code agent}, the student standing behind it. */
	static TurtleBlockEntity spawnAgent(GameTestHelper ctx, ServerPlayer student, BlockPos agent) {
		BlockPos feet = ctx.absolutePos(agent.north());
		student.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0f, 0.0f);
		ctx.assertTrue(AgentManager.spawn(student) == AgentManager.Result.SPAWNED, "Byte not created");
		return ctx.getBlockEntity(agent, TurtleBlockEntity.class);
	}

	/** A POST of {@code json} to one of MINE's pages. */
	static HttpRequest post(String url, String json) {
		return HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json)).build();
	}
}
