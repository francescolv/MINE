package io.github.francescolv.mine.blocks;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.francescolv.mine.MineMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /mine blocks}: opens the block editor in the student's browser. The server only knows
 * its port; the student's MINE knows the address it joined the server with, and opens the page
 * there with the student's personal code.
 */
public final class BlocksCommand {

	private BlocksCommand() {
	}

	/** Open the editor at this port, with this personal code. */
	public record Open(int port, String code) implements CustomPacketPayload {
		public static final Type<Open> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "blocks_open"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Open::port, ByteBufCodecs.stringUtf8(64), Open::code, Open::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	/** Start {@code name} when the student is back in the game (they pressed Run in the browser). */
	public record RunWhenBack(String name) implements CustomPacketPayload {
		public static final Type<RunWhenBack> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "blocks_run_when_back"));
		public static final StreamCodec<RegistryFriendlyByteBuf, RunWhenBack> CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(64), RunWhenBack::name, RunWhenBack::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	/** The student is back in the game: start {@code name} (empty: the last program sent from the blocks). */
	public record Go(String name) implements CustomPacketPayload {
		public static final Type<Go> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "blocks_go"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Go> CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(64), Go::name, Go::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(Open.ID, Open.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RunWhenBack.ID, RunWhenBack.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Go.ID, Go.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Go.ID, (payload, context) -> BlockEditor.go(context.player(), payload.name()));
		BlockEditor.register();
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("blocks").executes(ctx -> open(ctx.getSource()));
	}

	/** {@code /mine python}: the student's code for Python on their PC (Thonny), to copy with a click. */
	public static LiteralArgumentBuilder<CommandSourceStack> python() {
		return Commands.literal("python").executes(ctx -> {
			ServerPlayer player = ctx.getSource().getPlayerOrException();
			String key = ThonnyKeys.get(player.level().getServer()).keyFor(player.getUUID());
			Component code = Component.literal(key).withStyle(style -> style.withColor(ChatFormatting.YELLOW).withBold(true)
					.withClickEvent(new ClickEvent.CopyToClipboard(key))
					.withHoverEvent(new HoverEvent.ShowText(Component.translatableWithFallback("mine.python.copy", "Click to copy"))));
			ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("mine.python.key",
					"Your code for Python on your PC (Thonny): %s. It's yours for good: mine.py asks for it the first time. How to start: /mine blocks, Python, Thonny…",
					code), false);
			return 1;
		});
	}

	private static int open(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		int port = BlockEditor.port();
		if (port < 0) {
			source.sendFailure(Component.translatableWithFallback("mine.blocks.not_running",
					"The block editor runs on a class server (MINE's server package), not in a single-player world."));
			return 0;
		}
		if (!ServerPlayNetworking.canSend(player, Open.ID)) {
			source.sendFailure(Component.translatableWithFallback("mine.blocks.needs_mine",
					"The block editor needs MINE in your game (the MINE modpack)."));
			return 0;
		}
		ServerPlayNetworking.send(player, new Open(port, BlockSessions.codeFor(player.getUUID())));
		source.sendSuccess(() -> Component.translatableWithFallback("mine.blocks.opening",
				"The block editor opens in your browser.").withStyle(ChatFormatting.GREEN), false);
		return 1;
	}
}
