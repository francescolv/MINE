package io.github.francescolv.mine.classroom;

import io.github.francescolv.mine.DataFormat;
import io.github.francescolv.mine.MineMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * A class server is always at midday with a clear sky: no night to see nothing in, no rain over
 * the lessons. Done once, the first time the world starts on a dedicated server; then it's the
 * teacher's (/gamerule advance_time, advance_weather).
 */
public final class ClassDay extends SavedData {

	private static final String KEY = "class_day";
	private static final SavedDataType<ClassDay> TYPE = DataFormat.saved(KEY, ClassDay::new, ClassDay::fromNbt, ClassDay::save);

	private boolean done;

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			if (server.isDedicatedServer()) {
				apply(server);
			}
		});
	}

	/** Midday and clear, for good, unless done before in this world. Returns whether it did it now. */
	public static boolean apply(MinecraftServer server) {
		ClassDay state = server.getDataStorage().computeIfAbsent(TYPE);
		if (state.done) {
			return false;
		}
		server.getGameRules().set(GameRules.ADVANCE_TIME, false, server);
		server.getGameRules().set(GameRules.ADVANCE_WEATHER, false, server);
		CommandSourceStack console = server.createCommandSourceStack().withSuppressedOutput();
		server.getCommands().performPrefixedCommand(console, "time set noon");
		server.getCommands().performPrefixedCommand(console, "weather clear");
		state.done = true;
		state.setDirty();
		MineMod.LOGGER.info("[MINE] Class world: always midday, no rain (change it with /gamerule advance_time, advance_weather)");
		return true;
	}

	/** As if this world had never started on a class server: the next {@link #apply} does it again. */
	public static void forget(MinecraftServer server) {
		ClassDay state = server.getDataStorage().computeIfAbsent(TYPE);
		state.done = false;
		state.setDirty();
	}

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		nbt.putBoolean("Done", done);
		return nbt;
	}

	private static ClassDay fromNbt(CompoundTag nbt) {
		ClassDay state = new ClassDay();
		state.done = nbt.getBooleanOr("Done", false);
		return state;
	}
}
