package io.github.francescolv.mine.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.francescolv.mine.lab.Lab;
import io.github.francescolv.mine.npc.NpcManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The standard lab: lanes for the students, built where the teacher stands, the students put on
 * them, and cleared for the next class. Built high in the sky, away from the other tests.
 */
public class LabGameTest {

	@GameTest(maxTicks = 400)
	public void theStandardLab(GameTestHelper ctx) {
		ServerPlayer teacher = ctx.makeMockServerPlayerInLevel();
		BlockPos sky = ctx.absolutePos(new BlockPos(3, 150, 1));
		// Facing south (yaw 0).
		teacher.snapTo(sky.getX() + 0.5, sky.getY(), sky.getZ() + 0.5, 0.0f, 0.0f);
		ctx.getLevel().getServer().getPlayerList().getOps()
				.add(new net.minecraft.server.players.ServerOpListEntry(teacher.nameAndId(), LevelBasedPermissionSet.OWNER, false));
		CommandSourceStack asTeacher = teacher.createCommandSourceStack();
		// The lab reaches 30 blocks ahead: its chunks loaded, as around a teacher in the game, so
		// its labels and board are found in a test world that has never been there.
		setLoaded(ctx, sky, true);

		// Loading takes effect over the next ticks: the lab is built once the chunks are there.
		ctx.runAfterDelay(20, () -> {
			execute(ctx, player(ctx).createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS), "mine lab build 2", false);
			execute(ctx, asTeacher, "mine lab build 2", true);
			Lab lab = Lab.get(ctx.getLevel().getServer());
			ctx.assertTrue(lab.exists() && lab.lanes() == 2 && lab.facing() == Direction.SOUTH, "Built, facing south");
			BlockPos first = lab.start(0);
			BlockPos second = lab.start(1);
			ctx.assertTrue(first.getY() == sky.getY() && first.getZ() > sky.getZ(), "Lanes start in front of the teacher: " + first);
			ctx.assertTrue(Math.abs(second.getX() - first.getX()) == 8 && second.getZ() == first.getZ(), "Lanes 8 blocks apart: " + second);
			ctx.assertTrue(ctx.getLevel().getBlockState(first.below()).is(Blocks.CONCRETE.lime()), "The start square");
			ctx.assertTrue(ctx.getLevel().getBlockState(first.below().south(5)).is(Blocks.CONCRETE.lightGray()), "The lane");
			ctx.assertTrue(ctx.getLevel().getBlockState(first.south(12)).isAir(), "Free space in front");
			List<Entity> labEntities = ctx.getLevel().getEntities((Entity) null, new AABB(sky).inflate(60), e -> e.entityTags().contains("mine_lab"));
			ctx.assertTrue(labEntities.stream().anyMatch(NpcManager::isNpc), "The guide at the entrance");
			ctx.assertTrue(labEntities.size() >= 4, "Labels, board and guide: " + labEntities.stream()
					.map(e -> e.getType().toShortString() + "@" + e.blockPosition().toShortString()).toList() + " (lab from " + sky.toShortString() + ")");

			// Three students, two lanes: one is left out, and told.
			ServerPlayer alex = player(ctx);
			ServerPlayer sam = player(ctx);
			ServerPlayer kim = player(ctx);
			List<ServerPlayer> left = lab.assign(List.of(alex, sam, kim));
			ctx.assertTrue(left.size() == 1, "One student without a lane");
			ServerPlayer placed = left.contains(alex) ? sam : alex;
			Integer lane = lab.laneOf(placed.getUUID());
			ctx.assertTrue(lane != null && placed.blockPosition().equals(lab.start(lane)), "On their start square: " + placed.blockPosition());
			ctx.assertTrue(placed.getRespawnConfig().respawnData().pos() != null && placed.getRespawnConfig().respawnData().pos().equals(lab.start(lane)), "Their spawn point");
			lab.assign(List.of(placed));
			ctx.assertTrue(lane.equals(lab.laneOf(placed.getUUID())), "The same lane next time");

			// Builds go, the floor stays.
			ctx.getLevel().setBlockAndUpdate(first.south(3), Blocks.OAK_PLANKS.defaultBlockState());
			execute(ctx, asTeacher, "mine lab reset", true);
			ctx.assertTrue(ctx.getLevel().getBlockState(first.south(3)).isAir(), "The build is gone");
			ctx.assertTrue(ctx.getLevel().getBlockState(first.below()).is(Blocks.CONCRETE.lime()), "The floor stays");

			// Built again: no second guide, lanes given again.
			execute(ctx, asTeacher, "mine lab build 2", true);
			long guides = ctx.getLevel().getEntities((Entity) null, new AABB(sky).inflate(60),
					e -> e.entityTags().contains("mine_lab") && NpcManager.isNpc(e)).size();
			ctx.assertTrue(guides == 1, "One guide after building again: " + guides);
			ctx.assertTrue(lab.laneOf(placed.getUUID()) == null, "A new lab gives lanes again");

			// A student new to the class: a lane to come back to, but they arrive in front of the guide.
			ctx.assertTrue(lab.welcome(kim), "Kim gets a lane");
			Integer kimLane = lab.laneOf(kim.getUUID());
			ctx.assertTrue(kimLane != null && kim.getRespawnConfig().respawnData().pos() != null && kim.getRespawnConfig().respawnData().pos().equals(lab.start(kimLane)),
					"Kim comes back to their lane");
			ctx.assertTrue(kim.blockPosition().equals(sky), "Kim arrives at the entrance: " + kim.blockPosition());
			setLoaded(ctx, sky, false);
			ctx.succeed();
		});
	}

	private static void setLoaded(GameTestHelper ctx, BlockPos corner, boolean loaded) {
		for (int x = (corner.getX() - 16) >> 4; x <= (corner.getX() + 48) >> 4; x++) {
			for (int z = (corner.getZ() - 16) >> 4; z <= (corner.getZ() + 48) >> 4; z++) {
				ctx.getLevel().setChunkForced(x, z, loaded);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper ctx) {
		return ctx.makeMockServerPlayerInLevel();
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
