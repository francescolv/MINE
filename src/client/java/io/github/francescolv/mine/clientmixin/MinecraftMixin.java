package io.github.francescolv.mine.clientmixin;

import io.github.francescolv.mine.ClientBoards;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The right click: on a board's text, wherever it is, it's MINE's (see {@link ClientBoards}). */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void mine$clickBoard(CallbackInfo ci) {
		if (ClientBoards.click((Minecraft) (Object) this)) {
			ci.cancel();
		}
	}
}
