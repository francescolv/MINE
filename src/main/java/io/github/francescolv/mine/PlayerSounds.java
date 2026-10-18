package io.github.francescolv.mine;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Sounds only one player hears: a notice for them, not for the whole lab. */
public final class PlayerSounds {

	private PlayerSounds() {
	}

	public static void play(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch) {
		player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source,
				player.getX(), player.getY(), player.getZ(), volume, pitch, player.getRandom().nextLong()));
	}
}
