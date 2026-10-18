package io.github.francescolv.mine.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The board receiver: it gives redstone when the board says so. A value of its key from 1 up
 * powers it (15 at most, like redstone), 0 switches it off; "on"/"off" work too. Like a button
 * ({@link BoardBlockEntity#pulse}), any value above 0 powers it for one second.
 */
public final class BoardReceiverBlock extends Block implements EntityBlock {

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
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BoardBlockEntity(pos, state);
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(POWER);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		BoardBlocks.placed(level, pos, placer);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BoardBlocks.open(player, pos, null);
		return InteractionResult.SUCCESS;
	}

	/** The end of a button-like pulse. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(POWER) > 0) {
			level.setBlock(pos, state.setValue(POWER, 0), UPDATE_ALL);
		}
	}
}
