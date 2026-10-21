package io.github.francescolv.mine.lesson;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.Collection;
import java.util.Map;

/**
 * What the lessons' scenes around the Agent (redstone, boards, sensors) have in common: checking
 * there's room, building what is missing, and reading doors and lamps.
 */
final class Scenes {

	private Scenes() {
	}

	/** An iron door at {@code door}, both halves, facing {@code ahead}. */
	static void ironDoor(Map<BlockPos, BlockState> blocks, BlockPos door, Direction ahead) {
		BlockState state = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, ahead);
		blocks.put(door, state.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		blocks.put(door.above(), state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/**
	 * Whether the scene can be built: solid ground under {@code grounded}, and each place either
	 * free or already holding its block.
	 */
	static boolean fits(ServerLevel world, Map<BlockPos, BlockState> blocks, Collection<BlockPos> grounded) {
		for (BlockPos pos : grounded) {
			if (!world.getBlockState(pos.below()).isFaceSturdy(world, pos.below(), Direction.UP)) {
				return false;
			}
		}
		for (Map.Entry<BlockPos, BlockState> block : blocks.entrySet()) {
			BlockState there = world.getBlockState(block.getKey());
			if (!there.is(block.getValue().getBlock()) && !there.canBeReplaced()) {
				return false;
			}
		}
		return true;
	}

	/** Builds the blocks that aren't there yet, keeping those that are. */
	static void place(ServerLevel world, Map<BlockPos, BlockState> blocks) {
		blocks.forEach((pos, state) -> {
			if (!world.getBlockState(pos).is(state.getBlock())) {
				world.setBlock(pos, state, Block.UPDATE_ALL);
			}
		});
	}

	/** Whether every block of the scene is still there. */
	static boolean untouched(ServerLevel world, Map<BlockPos, BlockState> blocks) {
		return blocks.entrySet().stream().allMatch(block -> world.getBlockState(block.getKey()).is(block.getValue().getBlock()));
	}

	static boolean open(ServerLevel world, BlockPos door) {
		BlockState state = world.getBlockState(door);
		return state.is(Blocks.IRON_DOOR) && state.getValue(DoorBlock.OPEN);
	}

	static boolean lit(ServerLevel world, BlockPos lamp) {
		BlockState state = world.getBlockState(lamp);
		return state.is(Blocks.REDSTONE_LAMP) && state.getValue(RedstoneLampBlock.LIT);
	}
}
