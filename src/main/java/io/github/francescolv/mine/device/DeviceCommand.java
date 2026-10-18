package io.github.francescolv.mine.device;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** {@code /mine device}: MINE's window to connect a board plugged into the student's PC. */
public final class DeviceCommand {

	private DeviceCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("device").executes(ctx -> open(ctx.getSource()));
	}

	private static int open(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (!Devices.canOpen(player)) {
			source.sendFailure(Component.translatableWithFallback("mine.device.needs_mine",
					"Boards are read by MINE in your game (the MINE modpack)."));
			return 0;
		}
		// A board on Wi-Fi isn't on the PC's ports, which the window shows: chat says it.
		if (Devices.onWifi(player.getUUID())) {
			Object program = Devices.read(player.getUUID(), "programma");
			source.sendSuccess(() -> Component.translatableWithFallback("mine.device.wifi", "Your board is connected over Wi-Fi.")
					.withStyle(ChatFormatting.GREEN), false);
			if (program != null) {
				source.sendSuccess(() -> Component.translatableWithFallback("mine.device.program_started",
						"Your board runs %s", program.toString()), false);
			}
			return 1;
		}
		ServerPlayNetworking.send(player, new DevicePayloads.Open());
		return 1;
	}
}
