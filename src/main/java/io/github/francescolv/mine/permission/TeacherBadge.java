package io.github.francescolv.mine.permission;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Who the teacher is, at a glance. A teacher who joins is told so in chat, with the commands that
 * run the class; and teachers have a gold {@code [Teacher]} before their name, over their head and
 * in the player list, for everyone to see. Without it, a teacher in the world looks like any
 * other player with lessons to do.
 *
 * <p>The label is a scoreboard team, kept up to date every few seconds: it follows /op, /deop and
 * the student view. A player already on another team (e.g. one a teacher made for a game) keeps it.
 */
public final class TeacherBadge {

	private static final String TEAM = "mine_teachers";
	private static final int EVERY_TICKS = 100;

	private TeacherBadge() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
			ServerPlayer player = handler.getPlayer();
			update(server, player);
			if (Permissions.isTeacher(player)) {
				welcome(player);
			}
		}));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % EVERY_TICKS == 0) {
				server.getPlayerList().getPlayers().forEach(player -> update(server, player));
			}
		});
	}

	private static void update(MinecraftServer server, ServerPlayer player) {
		Scoreboard scoreboard = server.getScoreboard();
		String name = player.getScoreboardName();
		PlayerTeam current = scoreboard.getPlayersTeam(name);
		boolean onBadge = current != null && current.getName().equals(TEAM);
		if (Permissions.isTeacher(player)) {
			if (current == null) {
				scoreboard.addPlayerToTeam(name, team(scoreboard));
			}
		} else if (onBadge) {
			scoreboard.removePlayerFromTeam(name, current);
		}
	}

	private static PlayerTeam team(Scoreboard scoreboard) {
		PlayerTeam team = scoreboard.getPlayerTeam(TEAM);
		if (team == null) {
			team = scoreboard.addPlayerTeam(TEAM);
			team.setDisplayName(Component.translatableWithFallback("mine.teacher.team", "Teachers"));
			team.setPlayerPrefix(Component.translatableWithFallback("mine.teacher.prefix", "[Teacher] ")
					.withStyle(ChatFormatting.GOLD));
		}
		return team;
	}

	/** In chat: you are the teacher, and the commands to run the class (click to write them). */
	private static void welcome(ServerPlayer player) {
		player.sendSystemMessage(Component.translatableWithFallback("mine.teacher.welcome",
				"You are a teacher in this class: students do the lessons, you run them.").withStyle(ChatFormatting.GOLD));
		player.sendSystemMessage(command("/mine class code", "mine.teacher.welcome.code",
				"show the class code, for students to join"));
		player.sendSystemMessage(command("/mine lesson start first_steps @a", "mine.teacher.welcome.start",
				"start a lesson for every student (not the teachers)"));
		player.sendSystemMessage(command("/mine lesson status @a", "mine.teacher.welcome.status",
				"where each student is"));
		player.sendSystemMessage(command("/mine view student", "mine.teacher.welcome.view",
				"try MINE as a student sees it"));
	}

	private static Component command(String command, String key, String fallback) {
		return Component.literal(" ").append(Component.literal(command).withStyle(style -> style
						.withColor(ChatFormatting.AQUA)
						.withUnderlined(true)
						.withClickEvent(new ClickEvent.SuggestCommand(command))
						.withHoverEvent(new HoverEvent.ShowText(Component.translatableWithFallback("mine.teacher.welcome.click",
								"Click to write it in the chat")))))
				.append(Component.literal(" — ").withStyle(ChatFormatting.GRAY))
				.append(Component.translatableWithFallback(key, fallback).withStyle(ChatFormatting.GRAY));
	}
}
