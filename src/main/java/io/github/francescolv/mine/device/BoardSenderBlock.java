package io.github.francescolv.mine.device;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The board sender: when redstone reaches it, it tells the board {@code key:power} (0 to 15),
 * every time the power changes. A lever next to it switches an LED on the student's board.
 */
public final class BoardSenderBlock extends Block implements EntityBlock {

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
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BoardBlockEntity(pos, state);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		BoardBlocks.placed(level, pos, placer);
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

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BoardBlocks.open(player, pos, null);
		return InteractionResult.SUCCESS;
	}
}
