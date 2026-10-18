package io.github.francescolv.mine.gametest;

import io.github.francescolv.mine.zone.Zones;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Zones: marked with two corners, a snapshot of their blocks, and put back as they were. */
public class ZoneGameTest {

	/**
	 * A zone comes back as it was: broken blocks, new blocks, a chest's contents, dropped items.
	 * Agents are left out: none is copied back where one was, and the one there now stays.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = 100)
	public void aZoneComesBackAsItWas(GameTestHelper ctx) {
		MinecraftServer server = ctx.getLevel().getServer();
		ServerLevel level = ctx.getLevel();
		Block turtle = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("computercraft", "turtle_normal"));
		BlockPos stone = new BlockPos(3, 1, 3);
		BlockPos chest = new BlockPos(5, 1, 3);
		BlockPos agentBefore = new BlockPos(3, 1, 6);
		BlockPos agentNow = new BlockPos(6, 1, 6);
		ctx.setBlock(stone, Blocks.STONE);
		ctx.setBlock(chest, Blocks.CHEST);
		((ChestBlockEntity) ctx.getBlockEntity(chest, ChestBlockEntity.class)).setItem(0, new ItemStack(Items.DIAMOND, 3));
		ctx.setBlock(agentBefore, turtle);

		// Marked with the two corner blocks, as a teacher does.
		ServerPlayer teacher = ctx.makeMockServerPlayerInLevel();
		Zones.corner(teacher, ctx.absolutePos(new BlockPos(2, 1, 2)));
		Zones.corner(teacher, ctx.absolutePos(new BlockPos(7, 1, 7)));
		String name = "z" + Long.toString(System.nanoTime() % 1_000_000_000L, 36);
		ctx.assertTrue(Zones.create(teacher, name, 1, 3) == Zones.Result.DONE, "Zone made");
		ctx.assertTrue(Zones.create(teacher, name) == Zones.Result.NO_CORNERS, "The corners are used up");
		BoundingBox box = Zones.get(server).zone(name).box();
		ctx.assertTrue(box.getXSpan() == 6 && box.getZSpan() == 6, "The corners' area: " + box);
		ctx.assertTrue(box.minY() == ctx.absolutePos(new BlockPos(2, 0, 2)).getY() && box.getYSpan() == 5,
				"Into the ground and above the corners: " + box);
		BoundingBox usual = Zones.box(level, BlockPos.ZERO, new BlockPos(5, 0, 5), Zones.BELOW, Zones.ABOVE);
		ctx.assertTrue(usual.minY() == -Zones.BELOW && usual.maxY() == Zones.ABOVE, "Usually 10 down and 30 up: " + usual);

		// A lesson goes by.
		ctx.setBlock(stone, Blocks.AIR);
		BlockPos ground = new BlockPos(4, 0, 4);
		BlockState groundBefore = ctx.getBlockState(ground);
		ctx.setBlock(ground, Blocks.AIR);
		ctx.setBlock(new BlockPos(4, 2, 4), Blocks.DIRT);
		((ChestBlockEntity) ctx.getBlockEntity(chest, ChestBlockEntity.class)).setItem(0, ItemStack.EMPTY);
		ctx.setBlock(agentBefore, Blocks.AIR);
		ctx.setBlock(agentNow, turtle);
		ItemEntity dropped = new ItemEntity(level, ctx.absolutePos(stone).getX() + 0.5, ctx.absolutePos(stone).getY(), ctx.absolutePos(stone).getZ() + 0.5,
				new ItemStack(Items.APPLE));
		level.addFreshEntity(dropped);

		ctx.assertTrue(Zones.restore(server, name) == Zones.Result.DONE, "Put back");
		ctx.assertBlockPresent(Blocks.STONE, stone);
		ctx.assertTrue(ctx.getBlockState(ground) == groundBefore, "The hole in the ground is filled: " + ctx.getBlockState(ground));
		ctx.assertBlockPresent(Blocks.AIR, new BlockPos(4, 2, 4));
		ctx.assertTrue(((ChestBlockEntity) ctx.getBlockEntity(chest, ChestBlockEntity.class)).getItem(0).is(Items.DIAMOND),
				"The chest has its diamonds again");
		ctx.assertBlockPresent(Blocks.AIR, agentBefore);
		ctx.assertBlockPresent(turtle, agentNow);
		ctx.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(ctx.absolutePos(stone)).inflate(2)).isEmpty(), "Dropped items gone");

		ctx.assertTrue(Zones.remove(server, name), "Forgotten");
		ctx.assertTrue(Zones.restore(server, name) == Zones.Result.NO_ZONE, "No more");
		ctx.setBlock(agentNow, Blocks.AIR);
		ctx.succeed();
	}

	/**
	 * A protected zone: a student can't break or place blocks in it, but opens its door; a
	 * teacher can change it; an explosion next to it breaks only what is outside.
	 */
	@GameTest(structure = LessonGameTest.LAB, maxTicks = 100)
	public void aProtectedZone(GameTestHelper ctx) {
		MinecraftServer server = ctx.getLevel().getServer();
		ServerLevel level = ctx.getLevel();
		String name = "p" + Long.toString(System.nanoTime() % 1_000_000_000L, 36);
		ctx.assertTrue(Zones.create(level, name, BoundingBox.fromCorners(ctx.absolutePos(new BlockPos(1, 0, 1)),
				ctx.absolutePos(new BlockPos(6, 6, 6)))) == Zones.Result.DONE, "Zone made");
		ctx.assertTrue(Zones.protect(server, name, true), "Protected");

		BlockPos wall = new BlockPos(3, 1, 3);
		ctx.setBlock(wall, Blocks.STONE);
		ServerPlayer student = ctx.makeMockServerPlayerInLevel();
		ctx.assertFalse(student.gameMode.destroyBlock(ctx.absolutePos(wall)), "The student can't break it");
		ctx.assertBlockPresent(Blocks.STONE, wall);

		BlockPos air = new BlockPos(4, 1, 3);
		student.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 4));
		BlockHitResult onTop = new BlockHitResult(Vec3.atCenterOf(ctx.absolutePos(wall)), Direction.UP, ctx.absolutePos(wall), false);
		ctx.assertTrue(student.gameMode.useItemOn(student, level, student.getMainHandItem(), InteractionHand.MAIN_HAND, onTop)
				== InteractionResult.FAIL, "The student can't place a block");
		ctx.assertBlockPresent(Blocks.AIR, wall.above());

		BlockPos door = new BlockPos(5, 1, 5);
		ctx.setBlock(door, Blocks.OAK_DOOR.defaultBlockState());
		ctx.setBlock(door.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		student.gameMode.useItemOn(student, level, student.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(ctx.absolutePos(door)), Direction.NORTH, ctx.absolutePos(door), false));
		ctx.assertTrue(ctx.getBlockState(door).getValue(DoorBlock.OPEN), "But the door opens");

		ServerPlayer teacher = ctx.makeMockServerPlayerInLevel();
		server.getPlayerList().getOps().add(new ServerOpListEntry(teacher.nameAndId(), LevelBasedPermissionSet.GAMEMASTER, false));
		ctx.assertTrue(teacher.gameMode.destroyBlock(ctx.absolutePos(wall)), "The teacher can");
		server.getPlayerList().getOps().remove(teacher.nameAndId());

		// An explosion at the edge: the wall inside stays, the one outside goes.
		BlockPos inside = new BlockPos(6, 2, 3);
		BlockPos outside = new BlockPos(8, 2, 3);
		ctx.setBlock(inside, Blocks.STONE);
		ctx.setBlock(outside, Blocks.STONE);
		BlockPos center = ctx.absolutePos(new BlockPos(7, 2, 3));
		level.explode(null, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, 3.0f, Level.ExplosionInteraction.BLOCK);
		ctx.assertBlockPresent(Blocks.STONE, inside);
		ctx.assertBlockPresent(Blocks.AIR, outside);

		Zones.remove(server, name);
		ctx.assertTrue(student.gameMode.destroyBlock(ctx.absolutePos(inside)), "Without the zone, the student can break it");
		ctx.succeed();
	}
}
