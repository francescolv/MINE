package io.github.francescolv.mine.agent;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.function.Function;

/**
 * {@code /mine byte}: creates the student's Byte, their Agent (replacing the old one).
 * {@code /mine byte recall}: brings it back in front of the student.
 * {@code /mine byte run <program>}: starts one of its programs without opening its terminal.
 * {@code /mine agent} is the same, for whoever learnt it before Byte had its name.
 */
public final class AgentCommand {

	private AgentCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
		return Commands.literal(name)
				.executes(ctx -> run(ctx.getSource(), AgentManager::spawn))
				.then(Commands.literal("recall")
						.executes(ctx -> run(ctx.getSource(), AgentManager::recall)))
				.then(Commands.literal("run")
						.then(Commands.argument("program", StringArgumentType.greedyString())
								.executes(ctx -> {
									String program = StringArgumentType.getString(ctx, "program").trim();
									return run(ctx.getSource(), player -> AgentManager.runOrStart(player, program));
								})));
	}

	private static int run(CommandSourceStack source,
			Function<ServerPlayer, AgentManager.Result> action)
			throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		AgentManager.Result result = action.apply(player);

		switch (result) {
			case STARTED, STARTING -> {
				source.sendSuccess(() -> message(result), false);
				return 1;
			}
			case SPAWNED, REPLACED, RECALLED -> {
				ServerLevel world = player.level();
				world.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
						SoundSource.BLOCKS, 1.0f, 1.2f);
				source.sendSuccess(() -> message(result), false);
				return 1;
			}
			case BLOCKED -> {
				var inFront = player.level().getBlockState(AgentManager.targetFor(player));
				source.sendFailure(inFront.canBeReplaced()
						? Component.translatableWithFallback("mine.agent.cant_move",
								"Your Byte can't come here: create a new one with /mine byte (its programs stay).")
						: Component.translatableWithFallback("mine.agent.blocked",
								"There's %s in front of you: turn towards a free space and try again.", inFront.getBlock().getName()));
				return 0;
			}
			default -> {
				source.sendFailure(message(result));
				return 0;
			}
		}
	}

	/** What the student is told about a result of the Byte's commands (also when the block editor runs a program). */
	public static Component message(AgentManager.Result result) {
		return switch (result) {
			case SPAWNED -> Component.translatableWithFallback("mine.agent.spawned",
					"Here is your Byte! Right-click it to program it.");
			case REPLACED -> Component.translatableWithFallback("mine.agent.replaced",
					"Here is your new Byte! The old one has been removed.");
			case RECALLED -> Component.translatableWithFallback("mine.agent.recalled",
					"Your Byte is back.");
			case STARTING -> Component.translatableWithFallback("mine.agent.run.starting",
					"Your Byte is turning on: the program starts when it is ready.");
			case BLOCKED, SENT -> throw new IllegalArgumentException("not a result of these commands");
			case NO_AGENT -> Component.translatableWithFallback("mine.agent.none",
					"You don't have a Byte yet: create one with /mine byte.");
			case NO_CC -> Component.translatableWithFallback("mine.agent.no_cc",
					"Byte requires CC: Tweaked, which is not installed on this server.");
			case STARTED -> Component.translatableWithFallback("mine.agent.run.started",
					"Your Byte is running the program.");
			case OFF -> Component.translatableWithFallback("mine.agent.run.off",
					"Your Byte is turned off: right-click it once to turn it on, then try again.");
			case BUSY -> Component.translatableWithFallback("mine.agent.run.busy",
					"Your Byte is busy (a program is running, or the editor is open). Stop it by holding Ctrl+T for a second in its terminal.");
		};
	}
}
