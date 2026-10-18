package io.github.francescolv.mine.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.francescolv.mine.agent.AgentCommand;
import io.github.francescolv.mine.blocks.BlocksCommand;
import io.github.francescolv.mine.device.DeviceCommand;
import io.github.francescolv.mine.board.BoardCommand;
import io.github.francescolv.mine.classroom.ClassCommand;
import io.github.francescolv.mine.lab.LabCommand;
import io.github.francescolv.mine.lesson.LessonCommand;
import io.github.francescolv.mine.npc.NpcCommand;
import io.github.francescolv.mine.permission.BuilderCommand;
import io.github.francescolv.mine.portfolio.PortfolioCommand;
import io.github.francescolv.mine.permission.ViewCommand;
import io.github.francescolv.mine.world.WorldCommand;
import io.github.francescolv.mine.zone.ZoneCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Root of all MINE commands: {@code /mine <module> ...}.
 * Each module adds its own sub-command here.
 */
public final class MineCommands {

	private MineCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("mine");
			root.then(AgentCommand.build("byte"));
			root.then(AgentCommand.build("agent"));
			root.then(BlocksCommand.build());
			root.then(BlocksCommand.python());
			root.then(DeviceCommand.build());
			root.then(LessonCommand.build());
			root.then(NpcCommand.build());
			root.then(BoardCommand.build());
			root.then(PortfolioCommand.camera());
			root.then(PortfolioCommand.portfolio());
			root.then(PortfolioCommand.gallery());
			root.then(LabCommand.build());
			root.then(WorldCommand.build());
			root.then(ZoneCommand.build());
			root.then(ClassCommand.build());
			root.then(BuilderCommand.build());
			root.then(ViewCommand.build());
			dispatcher.register(root);
		});
	}
}
