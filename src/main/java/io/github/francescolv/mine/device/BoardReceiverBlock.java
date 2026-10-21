package io.github.francescolv.mine.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The board receiver: it gives redstone when the board says so. A value of its key from 1 up
 * powers it (15 at most, like redstone), 0 switches it off; "on"/"off" work too. Like a button
 * ({@link BoardBlockEntity#pulse}), any value above 0 powers it for one second.
 */
public final class BoardReceiverBlock extends BoardBlock {

	public static final IntegerProperty POWER = BlockStateProperties.POWER;
	/** How long a value keeps it on, like a stone button. */
	public static final int PULSE_TICKS = 20;

	public BoardReceiverBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(POWER, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWER);
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(POWER);
	}

	/** The end of a button-like pulse. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(POWER) > 0) {
			level.setBlock(pos, state.setValue(POWER, 0), UPDATE_ALL);
		}
	}
}
