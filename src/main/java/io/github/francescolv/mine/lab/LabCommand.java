package io.github.francescolv.mine.lab;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.francescolv.mine.permission.Permissions;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /mine lab build <lanes>}, {@code assign [students]} and {@code reset} (teachers): build
 * the standard lab where you stand, put the students on their lanes, clear what they built. Each
 * world has its own lab: the commands are about the one of the world the teacher is in, and
 * {@code assign} without names puts the students who are in that world.
 */
public final class LabCommand {

	private LabCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("lab")
				.requires(Permissions::isTeacher)
				.then(Commands.literal("build")
						.then(Commands.argument("lanes", IntegerArgumentType.integer(1, Lab.MAX_LANES))
								.executes(ctx -> build(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "lanes")))))
				.then(Commands.literal("assign")
						.executes(ctx -> assign(ctx.getSource(), Lab.students(ctx.getSource().getLevel())))
						.then(Commands.argument("students", EntityArgument.players())
								.executes(ctx -> assign(ctx.getSource(), List.copyOf(EntityArgument.getPlayers(ctx, "students"))))))
				.then(Commands.literal("reset")
						.executes(ctx -> reset(ctx.getSource())));
	}

	private static int build(CommandSourceStack source, int lanes) throws CommandSyntaxException {
		ServerPlayer teacher = source.getPlayerOrException();
		Lab lab = Lab.get(teacher.level());
		lab.build(teacher, lanes);
		source.sendSuccess(() -> Component.translatableWithFallback("mine.lab.built",
				"Lab built: %s lanes, in front of you and to your right. Now put the students on them: /mine lab assign", lanes)
				.withStyle(ChatFormatting.GREEN), true);
		return lanes;
	}

	private static int assign(CommandSourceStack source, List<ServerPlayer> students) {
		Lab lab = Lab.get(source.getLevel());
		if (!lab.exists()) {
			return noLab(source);
		}
		if (students.isEmpty()) {
			source.sendFailure(Component.translatableWithFallback("mine.lab.no_students", "There are no students online."));
			return 0;
		}
		List<ServerPlayer> left = lab.assign(students);
		int placed = students.size() - left.size();
		source.sendSuccess(() -> Component.translatableWithFallback("mine.lab.assigned", "%s students on their lanes.", placed)
				.withStyle(ChatFormatting.GREEN), true);
		if (!left.isEmpty()) {
			source.sendFailure(Component.translatableWithFallback("mine.lab.full",
					"No lane left for: %s. Build a lab with more lanes (/mine lab build).",
					left.stream().map(student -> student.getName().getString()).collect(Collectors.joining(", "))));
		}
		return placed;
	}

	private static int reset(CommandSourceStack source) {
		Lab lab = Lab.get(source.getLevel());
		ServerLevel world = lab.world(source.getServer());
		if (!lab.exists() || world == null) {
			return noLab(source);
		}
		lab.clear(world);
		source.sendSuccess(() -> Component.translatableWithFallback("mine.lab.reset",
				"The lab is clear again: builds, Bytes and dropped items removed. Bytes come back with /mine byte or when a lesson starts.")
				.withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int noLab(CommandSourceStack source) {
		source.sendFailure(Component.translatableWithFallback("mine.lab.none",
				"There's no lab yet: stand where it should start, facing the way students will face, and type /mine lab build <lanes>."));
		return 0;
	}
}
