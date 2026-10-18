package io.github.francescolv.mine.mixin;

import io.github.francescolv.mine.classroom.JoinGate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

/** The login check: students not on the whitelist may join with the class code ({@link JoinGate}). */
@Mixin(PlayerList.class)
abstract class PlayerManagerMixin {

	@Shadow
	@Final
	private MinecraftServer server;

	@Redirect(method = "canPlayerLogin", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/players/PlayerList;isWhiteListed(Lnet/minecraft/server/players/NameAndId;)Z"))
	private boolean mine$passWhitelist(PlayerList players, NameAndId player) {
		return JoinGate.passWhitelist(players, player);
	}

	@Inject(method = "canPlayerLogin", at = @At("RETURN"), cancellable = true)
	private void mine$refused(SocketAddress address, NameAndId player, CallbackInfoReturnable<Component> cir) {
		cir.setReturnValue(JoinGate.refused(server, player, cir.getReturnValue()));
	}
}
