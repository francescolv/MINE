package io.github.francescolv.mine.lesson;

import com.mojang.serialization.MapCodec;
import io.github.francescolv.mine.agent.AgentProgram;
import io.github.francescolv.mine.device.Devices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Lesson 8, the board and the Agent. Each step builds its part around the Agent, taking away the
 * parts of the other steps:
 * <ul>
 * <li>an iron door in front: button A on the board ({@code pulsante_a:1}) opens it for 3 seconds;</li>
 * <li>a redstone lamp behind: on when the board's light sensor says it's dark ({@code luce} below 50);</li>
 * <li>a lever on the ground at the Agent's left: the board's heart ({@code cuore}) follows it.</li>
 * </ul>
 * Tested live without the board: MINE plays the student's board ({@link Devices#play}), pulls the
 * lever, and looks at the door, the lamp and what the Agent tells the board. The student tries it
 * with the real board too: the program is the same.
 */
final class BoardChecks {

	static final String BUTTON = "pulsante_a";
	static final String LIGHT = "luce";
	static final String HEART = "cuore";

	private BoardChecks() {
	}

	enum Part { DOOR, LAMP, HEART }

	/** The scene around the Agent at {@code agent}, facing {@code ahead}, with one step's part. */
	record Scene(ServerLevel world, BlockPos agent, Direction ahead, Map<BlockPos, BlockState> blocks) implements Checks.Challenge {

		BlockPos door() {
			return agent.relative(ahead);
		}

		BlockPos lamp() {
			return agent.relative(ahead.getOpposite());
		}

		BlockPos lever() {
			return agent.relative(ahead.getCounterClockWise());
		}

		static Map<BlockPos, BlockState> part(BlockPos agent, Direction ahead, Part part) {
			Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
			switch (part) {
				case DOOR -> {
					Scenes.ironDoor(blocks, agent.relative(ahead), ahead);
				}
				case LAMP -> blocks.put(agent.relative(ahead.getOpposite()), Blocks.REDSTONE_LAMP.defaultBlockState());
				case HEART -> blocks.put(agent.relative(ahead.getCounterClockWise()), Blocks.LEVER.defaultBlockState()
						.setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, ahead));
			}
			return blocks;
		}

		/** Builds the step's parts, taking away the other parts; or says it needs more room. */
		static Object build(ServerLevel world, BlockPos agent, Direction ahead, Part... parts) {
			List<Part> wanted = List.of(parts);
			for (Part other : Part.values()) {
				if (!wanted.contains(other)) {
					take(world, agent, ahead, other);
				}
			}
			Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
			List<BlockPos> grounded = new java.util.ArrayList<>();
			for (Part part : wanted) {
				blocks.putAll(part(agent, ahead, part));
				switch (part) {
					case DOOR -> grounded.add(agent.relative(ahead));
					case HEART -> grounded.add(agent.relative(ahead.getCounterClockWise()));
					case LAMP -> {
					}
				}
			}
			if (!Scenes.fits(world, blocks, grounded)) {
				return new NoRoom();
			}
			Scenes.place(world, blocks);
			// A door or a lamp only follows redstone when it changes: one built next to a signal
			// already on (Byte's, left by its last program) starts open or lit, as it would be.
			BlockPos door = agent.relative(ahead);
			if (wanted.contains(Part.DOOR) && world.getBlockState(door).is(Blocks.IRON_DOOR)) {
				boolean powered = world.hasNeighborSignal(door) || world.hasNeighborSignal(door.above());
				for (BlockPos half : List.of(door, door.above())) {
					BlockState state = world.getBlockState(half);
					if (state.is(Blocks.IRON_DOOR) && state.getValue(DoorBlock.OPEN) != powered) {
						world.setBlock(half, state.setValue(DoorBlock.OPEN, powered).setValue(DoorBlock.POWERED, powered), Block.UPDATE_CLIENTS);
					}
				}
			}
			BlockPos lamp = agent.relative(ahead.getOpposite());
			if (wanted.contains(Part.LAMP) && world.getBlockState(lamp).is(Blocks.REDSTONE_LAMP)
					&& world.getBlockState(lamp).getValue(RedstoneLampBlock.LIT) != world.hasNeighborSignal(lamp)) {
				world.setBlock(lamp, world.getBlockState(lamp).setValue(RedstoneLampBlock.LIT, world.hasNeighborSignal(lamp)), Block.UPDATE_CLIENTS);
			}
			return new Scene(world, agent, ahead, blocks);
		}

		/** Takes away a part MINE built, where it is still there (and only what MINE builds). */
		private static void take(ServerLevel world, BlockPos agent, Direction ahead, Part part) {
			Set<Block> mine = Set.of(Blocks.IRON_DOOR, Blocks.REDSTONE_LAMP, Blocks.LEVER);
			List<BlockPos> places = List.copyOf(part(agent, ahead, part).keySet());
			// Top first: the door stands on its lower half.
			for (int i = places.size() - 1; i >= 0; i--) {
				BlockPos pos = places.get(i);
				if (mine.contains(world.getBlockState(pos).getBlock())) {
					world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
		}

		boolean open() {
			return Scenes.open(world, door());
		}

		boolean lit() {
			return Scenes.lit(world, lamp());
		}

		/** The lever on ({@code true}) or off, as a student would pull it. */
		void pull(boolean on) {
			BlockState lever = world.getBlockState(lever());
			if (lever.is(Blocks.LEVER) && lever.getValue(LeverBlock.POWERED) != on) {
				world.setBlock(lever(), lever.setValue(LeverBlock.POWERED, on), Block.UPDATE_ALL);
				world.updateNeighborsAt(lever(), Blocks.LEVER);
				world.updateNeighborsAt(lever().below(), Blocks.LEVER);
			}
		}

		@Override
		public boolean untouched() {
			return Scenes.untouched(world, blocks);
		}

		@Override
		public void remove() {
			for (Part part : Part.values()) {
				take(world, agent, ahead, part);
			}
		}
	}

	/** There's no room for the scene around the Agent. */
	private record NoRoom() {

		Component text() {
			return noRoom();
		}
	}

	/** What the student is told when there's no room for the scene. */
	static Component noRoom() {
		return Component.translatableWithFallback("mine.lesson.check.board_room",
				"Not yet: MINE needs room around Byte, and solid ground in front and on its left. Move it to an open space with /mine byte recall.");
	}

	/** Whether a value told to the board means "on": anything but nothing, 0, false or off. */
	static boolean on(@Nullable String value) {
		return value != null && !Set.of("", "0", "false", "off").contains(value.strip().toLowerCase(java.util.Locale.ROOT));
	}

	/**
	 * A scene and its live test. The test plays one student's board, so it's made when their
	 * program starts: the scene may have been built before, when the step began.
	 */
	private static final class Run implements Checks.Challenge {

		final Scene scene;
		@Nullable
		LiveTest timeline;

		Run(Scene scene) {
			this.scene = scene;
		}

		@Override
		public boolean untouched() {
			return scene.untouched();
		}

		@Override
		public void remove() {
			scene.remove();
		}
	}

	/** What the three checks share: a scene, a live test with the student's board played by MINE. */
	private abstract static class Live implements Check {

		abstract Part part();

		abstract LiveTest test(Scene scene, UUID student);

		@Override
		public Object prepare(ServerLevel world, BlockPos start, Direction ahead) {
			Object built = Scene.build(world, start, ahead, part());
			return built instanceof Scene scene ? new Run(scene) : built;
		}

		@Override
		public boolean watch(AgentProgram program, @Nullable Object memo) {
			if (memo instanceof Run run) {
				if (run.timeline == null) {
					run.timeline = test(run.scene, program.owner());
				}
				return run.timeline.tick();
			}
			return memo instanceof NoRoom;
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return memo instanceof Run run && run.timeline != null && run.timeline.passed;
		}

		@Override
		public void ended(AgentProgram program, @Nullable Object memo, boolean passed) {
			if (memo instanceof Run run) {
				run.scene.pull(false);
			}
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (memo instanceof NoRoom noRoom) {
				return noRoom.text();
			}
			if (memo instanceof Run run && run.timeline != null && run.timeline.failure != null) {
				return run.timeline.failure;
			}
			return Checks.keepListening();
		}
	}

	/**
	 * Tested live: MINE presses button A on the student's board twice. Each time the door must
	 * open, still be open 2 seconds later, and close by itself about 3 seconds after it opened.
	 */
	static final class BoardDoor extends Live {

		static final MapCodec<BoardDoor> CODEC = MapCodec.unit(BoardDoor::new);

		@Override
		Part part() {
			return Part.DOOR;
		}

		@Override
		LiveTest test(Scene s, UUID student) {
			Runnable press = () -> Devices.play(s.world().getServer(), student, BUTTON, "1");
			Supplier<Component> stayedClosed = () -> Component.translatableWithFallback("mine.lesson.check.board_door_closed",
					"Not yet: MINE pressed A on your board (pulsante_a:1), but the door didn't open.");
			// Ticks from the press: open by 16, still open at 44 (2.2 s), closed at 110 (5.5 s).
			return LiveTest.inGameTime(s.world(), s)
					.then(null, null, press, 16)
					.then(s::open, stayedClosed, null, 28)
					.then(s::open, () -> Component.translatableWithFallback("mine.lesson.check.board_door_too_soon",
							"Not yet: the door closed at once. Keep it open for 3 seconds: wait, then close it."), null, 66)
					.then(() -> !s.open(), () -> Component.translatableWithFallback("mine.lesson.check.board_door_stays_open",
							"Not yet: the door stayed open. Close it again 3 seconds after it opened."), press, 16)
					.then(s::open, stayedClosed, null, 0);
		}
	}

	/**
	 * Tested live: MINE plays the board's light sensor, light and dark twice. Each time the lamp
	 * must be on exactly when the light is below 50.
	 */
	static final class BoardLamp extends Live {

		static final MapCodec<BoardLamp> CODEC = MapCodec.unit(BoardLamp::new);

		@Override
		Part part() {
			return Part.LAMP;
		}

		@Override
		LiveTest test(Scene s, UUID student) {
			int[] light = { 0 };
			LiveTest timeline = LiveTest.inGameTime(s.world(), s);
			for (int value : new int[] { 200, 10, 180, 20 }) {
				// The board sends its light now and again: twice, half a second apart, then MINE looks.
				Runnable send = () -> {
					light[0] = value;
					Devices.play(s.world().getServer(), student, LIGHT, Integer.toString(value));
				};
				timeline.then(null, null, send, 10)
						.then(null, null, send, 30)
						.then(() -> s.lit() == light[0] < 50, () -> wrongLamp(light[0], s.lit()), null, 0);
			}
			return timeline;
		}

		private static Component wrongLamp(int light, boolean lit) {
			Component lamp = Checks.onOff(lit);
			return Component.translatableWithFallback("mine.lesson.check.board_lamp",
					"Not yet: the board said luce:%s, and the lamp was %s. It must be on only when the light is below 50.", light, lamp);
		}
	}

	/**
	 * Tested live: MINE pulls the lever on and off twice. Each time the Agent must tell the board
	 * the heart's new state: {@code cuore:1} (or any "on" value) with the lever on, {@code cuore:0} off.
	 */
	static final class BoardHeart extends Live {

		static final MapCodec<BoardHeart> CODEC = MapCodec.unit(BoardHeart::new);

		@Override
		Part part() {
			return Part.HEART;
		}

		@Override
		LiveTest test(Scene s, UUID student) {
			Supplier<Component> notOn = () -> Component.translatableWithFallback("mine.lesson.check.board_heart_on",
					"Not yet: MINE pulled the lever on, but Byte didn't tell the board cuore:1.");
			Supplier<Component> notOff = () -> Component.translatableWithFallback("mine.lesson.check.board_heart_off",
					"Not yet: MINE put the lever back, but Byte didn't tell the board cuore:0.");
			return LiveTest.inGameTime(s.world(), s)
					.then(null, null, () -> s.pull(true), 30)
					.then(() -> on(Devices.lastSent(student, HEART)), notOn, () -> s.pull(false), 30)
					.then(() -> !on(Devices.lastSent(student, HEART)), notOff, () -> s.pull(true), 30)
					.then(() -> on(Devices.lastSent(student, HEART)), notOn, () -> s.pull(false), 30)
					.then(() -> !on(Devices.lastSent(student, HEART)), notOff, null, 0);
		}
	}
}
