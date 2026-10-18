package io.github.francescolv.mine;

import io.github.francescolv.mine.agent.AgentManager;
import io.github.francescolv.mine.blocks.BlocksCommand;
import io.github.francescolv.mine.device.BoardBlocks;
import io.github.francescolv.mine.device.Devices;
import io.github.francescolv.mine.board.BoardManager;
import io.github.francescolv.mine.classroom.ClassDay;
import io.github.francescolv.mine.classroom.JoinGate;
import io.github.francescolv.mine.classroom.LanAnnouncer;
import io.github.francescolv.mine.classroom.VersionCheck;
import io.github.francescolv.mine.command.MineCommands;
import io.github.francescolv.mine.config.CcTweakedDefaults;
import io.github.francescolv.mine.lesson.LessonManager;
import io.github.francescolv.mine.lesson.Lessons;
import io.github.francescolv.mine.npc.ExportPayload;
import io.github.francescolv.mine.npc.NpcManager;
import io.github.francescolv.mine.npc.PagePayload;
import io.github.francescolv.mine.npc.Templates;
import io.github.francescolv.mine.permission.TeacherBadge;
import io.github.francescolv.mine.portfolio.PortfolioPayloads;
import io.github.francescolv.mine.portfolio.Portfolios;
import io.github.francescolv.mine.world.ClassWorlds;
import io.github.francescolv.mine.zone.ZoneProtection;
import io.github.francescolv.mine.zone.Zones;
import io.github.francescolv.mine.writing.Writing;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MineMod implements ModInitializer {

	public static final String MOD_ID = "mine";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final String VERSION = FabricLoader.getInstance().getModContainer(MOD_ID)
			.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
			.orElse("dev");

	@Override
	public void onInitialize() {
		LOGGER.info("[MINE] Mine Is Not Education — core initialisation started.");

		if (FabricLoader.getInstance().isModLoaded(AgentManager.CC_MOD_ID)) {
			CcTweakedDefaults.apply();
		}
		DataFormat.register();
		AgentManager.register();
		Lessons.register();
		LessonManager.register();
		Templates.register();
		NpcManager.register();
		BoardManager.register();
		PortfolioPayloads.register();
		Portfolios.register();
		ExportPayload.register();
		PagePayload.register();
		VersionCheck.register();
		JoinGate.register();
		TeacherBadge.register();
		Writing.register();
		BlocksCommand.register();
		Devices.register();
		BoardBlocks.register();
		LanAnnouncer.register();
		ClassDay.register();
		ClassWorlds.register();
		Zones.register();
		ZoneProtection.register();
		MineCommands.register();
	}
}
