package io.github.francescolv.mine.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Who is joining, while their connection is still being set up. */
@Mixin(ServerConfigurationPacketListenerImpl.class)
public interface ServerConfigurationNetworkHandlerAccessor {

	@Accessor("gameProfile")
	GameProfile mine$getProfile();
}
