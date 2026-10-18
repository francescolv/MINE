package io.github.francescolv.mine.clientmixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The label CC: Tweaked draws over a turtle. */
@Pseudo
@Mixin(targets = "dan200.computercraft.client.render.TurtleBlockEntityRenderer$State", remap = false)
public interface TurtleRenderStateAccessor {

	@Accessor(value = "label", remap = false)
	void mine$setLabel(String label);
}
