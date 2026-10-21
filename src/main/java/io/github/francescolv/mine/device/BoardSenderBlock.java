package io.github.francescolv.mine.device;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;

/**
 * The board sender: when redstone reaches it, it tells the board {@code key:power} (0 to 15),
 * every time the power changes. A lever next to it switches an LED on the student's board.
 */
public final class BoardSenderBlock extends BoardBlock {

	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public BoardSenderBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWERED);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		if (level instanceof ServerLevel server) {
			BoardBlocks.senderChanged(server, pos);
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
		if (level instanceof ServerLevel server && !old.is(this)) {
			BoardBlocks.senderChanged(server, pos);
		}
	}
}
