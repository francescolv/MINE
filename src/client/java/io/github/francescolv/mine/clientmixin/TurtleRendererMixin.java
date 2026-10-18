package io.github.francescolv.mine.clientmixin;

import dan200.computercraft.client.render.TurtleBlockEntityRenderer;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import io.github.francescolv.mine.agent.AgentLabels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Agents' name tags: CC: Tweaked shows a turtle's label only while the player looks at it; MINE
 * shows it over every labelled turtle nearby, like players' names, so in the lab it's clear whose
 * Agent is whose. Nothing happens without CC: Tweaked.
 */
@Pseudo
@Mixin(targets = "dan200.computercraft.client.render.TurtleBlockEntityRenderer", remap = false)
abstract class TurtleRendererMixin {

	/** How far an Agent's name shows, in blocks. */
	private static final double NAME_DISTANCE = 24;

	@Inject(method = "extractRenderState(Ldan200/computercraft/shared/turtle/blocks/TurtleBlockEntity;"
			+ "Ldan200/computercraft/client/render/TurtleBlockEntityRenderer$State;FLnet/minecraft/world/phys/Vec3;"
			+ "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
			at = @At("TAIL"), remap = false, require = 0)
	private void mine$alwaysShowName(TurtleBlockEntity turtle, TurtleBlockEntityRenderer.State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.CrumblingOverlay breakProgress, CallbackInfo ci) {
		String label = turtle.getLabel();
		if (label != null && camera.distanceToSqr(Vec3.atCenterOf(turtle.getBlockPos())) < NAME_DISTANCE * NAME_DISTANCE
				&& Minecraft.getInstance().player != null) {
			// "Alex's Byte" in the player's language: "Byte di Alex".
			((TurtleRenderStateAccessor) (Object) state).mine$setLabel(AgentLabels.shown(label).getString());
		}
	}
}
