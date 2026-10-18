package io.github.francescolv.mine.mixin;

import io.github.francescolv.mine.zone.ZoneProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Protected zones: fire burns nothing in them. */
@Mixin(FireBlock.class)
abstract class FireBlockMixin {

	@Inject(method = "checkBurnOut", at = @At("HEAD"), cancellable = true)
	private void mine$protectedZone(Level level, BlockPos pos, int chance, RandomSource random, int age, CallbackInfo ci) {
		if (ZoneProtection.isProtected(level, pos)) {
			ci.cancel();
		}
	}
}
