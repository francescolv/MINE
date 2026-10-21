package io.github.francescolv.mine.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * What a board block remembers: whose board it listens to or talks to (the student who placed
 * it, or whom a teacher chose), the key, and for a receiver whether it works like a button.
 */
public final class BoardBlockEntity extends BlockEntity {

	@Nullable
	UUID owner;
	String ownerName = "";
	String key = "";
	/** Receiver: a value turns it on for a second, like a button (for boards that only say "pressed"). */
	boolean pulse;
	/** Sender: the power last told to the board, -1 if never. Not saved: after a restart it is told again. */
	int sent = -1;

	public BoardBlockEntity(BlockPos pos, BlockState state) {
		super(BoardBlocks.ENTITY, pos, state);
	}

	/** Whose board, and which key: for teachers placing blocks with commands, and for tests. */
	public void configure(@Nullable UUID owner, String ownerName, String key, boolean pulse) {
		BoardBlocks.untrack(this);
		this.owner = owner;
		this.ownerName = ownerName;
		this.key = key;
		this.pulse = pulse;
		this.sent = -1;
		setChanged();
		if (!isRemoved()) {
			BoardBlocks.track(this);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		output.putString("owner_name", ownerName);
		output.putString("key", key);
		output.putBoolean("pulse", pulse);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		ownerName = input.getStringOr("owner_name", "");
		key = input.getStringOr("key", "");
		pulse = input.getBooleanOr("pulse", false);
	}

	@Override
	public void clearRemoved() {
		super.clearRemoved();
		if (level instanceof ServerLevel) {
			BoardBlocks.track(this);
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		BoardBlocks.untrack(this);
	}
}
