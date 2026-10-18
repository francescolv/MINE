package io.github.francescolv.mine.mixin;

import io.github.francescolv.mine.zone.ZoneProtection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Protected zones: a player using an item on a block (placing it, a tool, fire...) is stopped
 * there. Only the item's own use: the block's (a door, a lever, a chest) has already happened.
 */
@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {

	@Inject(method = "useItemOn", cancellable = true, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
	private void mine$protectedZone(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (!ZoneProtection.mayChange(player, level, hit.getBlockPos())
				|| !ZoneProtection.mayChange(player, level, hit.getBlockPos().relative(hit.getDirection()))) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
