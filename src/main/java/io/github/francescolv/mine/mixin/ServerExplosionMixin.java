package io.github.francescolv.mine.mixin;

import io.github.francescolv.mine.zone.ZoneProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/** Protected zones: an explosion breaks nothing in them, and lights no fire there. */
@Mixin(ServerExplosion.class)
abstract class ServerExplosionMixin {

	@Shadow
	@Final
	private ServerLevel level;

	@Inject(method = "calculateExplodedPositions", at = @At("RETURN"), cancellable = true)
	private void mine$protectedZones(CallbackInfoReturnable<List<BlockPos>> cir) {
		List<BlockPos> positions = cir.getReturnValue();
		if (positions.stream().anyMatch(pos -> ZoneProtection.isProtected(level, pos))) {
			List<BlockPos> left = new ArrayList<>(positions);
			left.removeIf(pos -> ZoneProtection.isProtected(level, pos));
			cir.setReturnValue(left);
		}
	}
}
