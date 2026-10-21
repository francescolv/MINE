package io.github.francescolv.mine.lesson;

import com.mojang.serialization.MapCodec;
import io.github.francescolv.mine.agent.AgentProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Lesson 7, sensors. Each step builds its part of a small station around the Agent, taking away
 * the parts of the other steps:
 * <ul>
 * <li>a pressure plate on the ground at the Agent's left, and an iron door in front: the door
 * opens when someone steps on the plate and closes by itself 3 seconds later (a timer);</li>
 * <li>a daylight sensor on top of the Agent and a redstone lamp behind it: the lamp is on only when
 * the sensor says it's dark (a night light);</li>
 * <li>a button on the ground at the Agent's right, and in front a sticky piston with a yellow
 * walkway: each press moves the walkway out, or back in (the Agent remembers, in a variable).</li>
 * </ul>
 * Plate and button are on the ground, next to the Agent: students walk on the plate and press the
 * button without climbing, and they power the Agent directly.
 * All three are tested live: MINE steps on the plate, tricks the sensor into "night" (it inverts
 * it) and presses the button while the program runs, and looks at what the Agent does.
 */
final class SensorChecks {

	/**
	 * How long MINE keeps a plate or a button down, as a player would (a second); MINE looks at
	 * what the Agent did then. All the test's times are game ticks, as the Agent's own clock
	 * (os.sleep, os.startTimer): a program that waits 3 seconds waits 60 ticks.
	 */
	static final int PRESS_TICKS = 24;

	private SensorChecks() {
	}

	enum Part { PLATE_DOOR, NIGHT_LAMP, BUTTON_BRIDGE }

	/** The station around the Agent at {@code agent}, facing {@code ahead}, with one step's part. */
	record Station(ServerLevel world, BlockPos agent, Direction ahead, Map<BlockPos, BlockState> blocks) implements Checks.Challenge {

		Direction left() {
			return ahead.getCounterClockWise();
		}

		Direction right() {
			return ahead.getClockWise();
		}

		BlockPos door() {
			return agent.relative(ahead);
		}

		BlockPos plate() {
			return agent.relative(left());
		}

		BlockPos lamp() {
			return agent.relative(ahead.getOpposite());
		}

		BlockPos sensor() {
			return agent.above();
		}

		BlockPos button() {
			return agent.relative(right());
		}

		BlockPos piston() {
			return agent.relative(ahead);
		}

		/** What each part is made of, from the bottom up, at its places. */
		static Map<BlockPos, BlockState> part(BlockPos agent, Direction ahead, Part part) {
			Direction left = ahead.getCounterClockWise();
			Direction right = ahead.getClockWise();
			Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
			switch (part) {
				case PLATE_DOOR -> {
					Scenes.ironDoor(blocks, agent.relative(ahead), ahead);
					blocks.put(agent.relative(left), Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
				}
				case NIGHT_LAMP -> {
					blocks.put(agent.relative(ahead.getOpposite()), Blocks.REDSTONE_LAMP.defaultBlockState());
					blocks.put(agent.above(), Blocks.DAYLIGHT_DETECTOR.defaultBlockState());
				}
				case BUTTON_BRIDGE -> {
					blocks.put(agent.relative(right), Blocks.OAK_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.FLOOR)
							.setValue(ButtonBlock.FACING, ahead));
					blocks.put(agent.relative(ahead), Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, ahead));
					blocks.put(agent.relative(ahead, 2), Blocks.CONCRETE.yellow().defaultBlockState());
				}
			}
			return blocks;
		}

		/** Builds the step's part, taking away the other parts; or says it needs more room. */
		static Object build(ServerLevel world, BlockPos agent, Direction ahead, Part part) {
			for (Part other : Part.values()) {
				if (other != part) {
					take(world, agent, ahead, other);
				}
			}
			Map<BlockPos, BlockState> blocks = part(agent, ahead, part);
			// The door, the plate and the button stand on the ground.
			List<BlockPos> grounded = switch (part) {
				case PLATE_DOOR -> List.of(agent.relative(ahead), agent.relative(ahead.getCounterClockWise()));
				case BUTTON_BRIDGE -> List.of(agent.relative(ahead.getClockWise()));
				case NIGHT_LAMP -> List.of();
			};
			if (!Scenes.fits(world, blocks, grounded)) {
				return new NoRoom();
			}
			blocks.forEach((pos, state) -> {
				BlockState there = world.getBlockState(pos);
				// The sensor is put back as it should be: MINE may have left it inverted.
				if (!there.is(state.getBlock()) || there.is(Blocks.DAYLIGHT_DETECTOR) && there.getValue(DaylightDetectorBlock.INVERTED)) {
					world.setBlock(pos, state, Block.UPDATE_ALL);
				}
			});
			return new Station(world, agent, ahead, blocks);
		}

		/** Takes away a part MINE built, where it is still there (and only what MINE builds). */
		private static void take(ServerLevel world, BlockPos agent, Direction ahead, Part part) {
			List<BlockPos> places = new ArrayList<>(part(agent, ahead, part).keySet());
			if (part == Part.BUTTON_BRIDGE) {
				// An extended piston: its head, and the walkway one block further.
				places.add(agent.relative(ahead, 3));
			}
			// Stone posts with a plate on top or a button or lever on the side (lesson 6's scene):
			// posts first in the list, so they go last.
			Direction left = ahead.getCounterClockWise();
			Direction right = ahead.getClockWise();
			places.addAll(0, List.of(agent.relative(left), agent.relative(right), agent.relative(left).above(),
					agent.relative(left, 2), agent.relative(right, 2)));
			// Top first: plates and buttons hang on posts, doors stand on their lower half.
			for (int i = places.size() - 1; i >= 0; i--) {
				BlockPos pos = places.get(i);
				BlockState there = world.getBlockState(pos);
				if (there.is(Blocks.IRON_DOOR) || there.is(Blocks.STONE_PRESSURE_PLATE) || there.is(Blocks.OAK_PRESSURE_PLATE)
						|| there.is(Blocks.REDSTONE_LAMP) || there.is(Blocks.DAYLIGHT_DETECTOR) || there.is(Blocks.STONE_BUTTON)
						|| there.is(Blocks.OAK_BUTTON) || there.is(Blocks.LEVER) || there.is(Blocks.STICKY_PISTON)
						|| there.is(Blocks.PISTON_HEAD) || there.is(Blocks.MOVING_PISTON) || there.is(Blocks.CONCRETE.yellow())
						|| there.is(Blocks.STONE) && (pos.equals(agent.relative(ahead.getCounterClockWise())) || pos.equals(agent.relative(ahead.getClockWise())))) {
					world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}

		boolean open() {
			return Scenes.open(world, door());
		}

		boolean lit() {
			return Scenes.lit(world, lamp());
		}

		/** Whether the sensor says it's dark: no signal. */
		boolean dark() {
			BlockState sensor = world.getBlockState(sensor());
			return !sensor.is(Blocks.DAYLIGHT_DETECTOR) || sensor.getValue(DaylightDetectorBlock.POWER) == 0;
		}

		boolean out() {
			BlockState piston = world.getBlockState(piston());
			return piston.is(Blocks.STICKY_PISTON) && piston.getValue(PistonBaseBlock.EXTENDED);
		}

		/** Someone steps on the plate ({@code true}), or off it. */
		void stand(boolean on) {
			BlockState plate = world.getBlockState(plate());
			if (plate.is(Blocks.OAK_PRESSURE_PLATE) && plate.getValue(PressurePlateBlock.POWERED) != on) {
				world.setBlock(plate(), plate.setValue(PressurePlateBlock.POWERED, on), Block.UPDATE_ALL);
				world.updateNeighborsAt(plate(), plate.getBlock());
				world.updateNeighborsAt(plate().below(), plate.getBlock());
			}
		}

		/** Night or day for the sensor: MINE inverts it, as a player clicking it would. */
		void invert(boolean inverted) {
			BlockState sensor = world.getBlockState(sensor());
			if (sensor.is(Blocks.DAYLIGHT_DETECTOR) && sensor.getValue(DaylightDetectorBlock.INVERTED) != inverted) {
				world.setBlock(sensor(), sensor.setValue(DaylightDetectorBlock.INVERTED, inverted), Block.UPDATE_ALL);
			}
		}

		/** The button pressed ({@code true}), or back up. */
		void push(boolean on) {
			BlockState button = world.getBlockState(button());
			if (button.is(Blocks.OAK_BUTTON) && button.getValue(ButtonBlock.POWERED) != on) {
				world.setBlock(button(), button.setValue(ButtonBlock.POWERED, on), Block.UPDATE_ALL);
				world.updateNeighborsAt(button(), button.getBlock());
				world.updateNeighborsAt(button().below(), button.getBlock());
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

	/** There's no room for the station around the Agent. */
	private record NoRoom() {

		Component text() {
			return Component.translatableWithFallback("mine.lesson.check.sensors_room",
					"Not yet: MINE needs room around Byte for the sensors, and solid ground in front. Move it to an open space with /mine byte recall.");
		}
	}

	/** What all three checks share: a station, a live test, its outcome. */
	private abstract static class Live implements Check {

		abstract Part part();

		abstract LiveTest test(Station station);

		@Override
		public Object prepare(ServerLevel world, BlockPos start, Direction ahead) {
			Object built = Station.build(world, start, ahead, part());
			return built instanceof Station station ? test(station) : built;
		}

		@Override
		public boolean watch(AgentProgram program, @Nullable Object memo) {
			// No room for the station: stop at once, so the student is told instead of waiting.
			return memo instanceof NoRoom || memo instanceof LiveTest timeline && timeline.tick();
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return memo instanceof LiveTest timeline && timeline.passed;
		}

		@Override
		public void ended(AgentProgram program, @Nullable Object memo, boolean passed) {
			// Nobody stays on the plate, no button stays down, whatever happened.
			if (memo instanceof LiveTest timeline) {
				Station station = (Station) timeline.scene();
				station.stand(false);
				station.push(false);
			}
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (memo instanceof NoRoom noRoom) {
				return noRoom.text();
			}
			if (memo instanceof LiveTest timeline && timeline.failure != null) {
				return timeline.failure;
			}
			return Checks.keepListening();
		}
	}

	/**
	 * Tested live: MINE steps on the plate twice. Each time the door must open, stay open after
	 * the plate is free again, and close by itself about 3 seconds later.
	 */
	static final class PlateDoor extends Live {

		static final MapCodec<PlateDoor> CODEC = MapCodec.unit(PlateDoor::new);

		@Override
		Part part() {
			return Part.PLATE_DOOR;
		}

		@Override
		LiveTest test(Station s) {
			Supplier<Component> stayedClosed = () -> Component.translatableWithFallback("mine.lesson.check.plate_closed",
					"Not yet: someone stepped on the plate, but the door didn't open.");
			// On the plate for a second: the door open by then, and still open after.
			// Ticks from MINE stepping on the plate: open by 16, plate free at 24, still open at 44
			// (2.2 s), closed at 110 (5.5 s).
			return LiveTest.inGameTime(s.world(), s)
					.then(null, null, () -> s.stand(true), 16)
					.then(s::open, stayedClosed, null, PRESS_TICKS - 16)
					.then(null, null, () -> s.stand(false), 20)
					.then(s::open, () -> Component.translatableWithFallback("mine.lesson.check.plate_too_soon",
							"Not yet: the door closed as soon as the plate was free. Keep it open for 3 seconds: wait, then close it."), null, 66)
					.then(() -> !s.open(), () -> Component.translatableWithFallback("mine.lesson.check.plate_stays_open",
							"Not yet: the door stayed open. Close it again 3 seconds after it opened."), () -> s.stand(true), 16)
					.then(s::open, stayedClosed, () -> s.stand(false), 0);
		}
	}

	/**
	 * Tested live: MINE inverts the sensor, as if night had come, and back to day. Each time the
	 * lamp must be on exactly when the sensor says it's dark.
	 */
	static final class NightLamp extends Live {

		static final MapCodec<NightLamp> CODEC = MapCodec.unit(NightLamp::new);

		@Override
		Part part() {
			return Part.NIGHT_LAMP;
		}

		@Override
		LiveTest test(Station s) {
			// The sensor updates once a second (20 ticks): then the Agent reacts.
			return LiveTest.inGameTime(s.world(), s)
					.then(() -> s.lit() == s.dark(), () -> wrongLamp(s), () -> s.invert(true), 50)
					.then(() -> s.lit() == s.dark(), () -> wrongLamp(s), () -> s.invert(false), 50)
					.then(() -> s.lit() == s.dark(), () -> wrongLamp(s), null, 0);
		}

		private static Component wrongLamp(Station s) {
			Component light = s.dark() ? Component.translatableWithFallback("mine.lesson.check.dark", "dark")
					: Component.translatableWithFallback("mine.lesson.check.light", "light");
			Component lamp = Checks.onOff(s.lit());
			return Component.translatableWithFallback("mine.lesson.check.night_lamp",
					"Not yet: the sensor said it was %s, and the lamp was %s. It must be on only when it's dark.", light, lamp);
		}

		@Override
		public void ended(AgentProgram program, @Nullable Object memo, boolean passed) {
			super.ended(program, memo, passed);
			// Back to a plain sensor, whatever happened.
			if (memo instanceof LiveTest timeline) {
				((Station) timeline.scene()).invert(false);
			}
		}
	}

	/**
	 * Tested live: MINE presses the button three times. Each press must move the walkway out if it
	 * was in, or back in if it was out, and it must stay there after the button pops up.
	 */
	static final class ButtonBridge extends Live {

		static final MapCodec<ButtonBridge> CODEC = MapCodec.unit(ButtonBridge::new);

		@Override
		Part part() {
			return Part.BUTTON_BRIDGE;
		}

		@Override
		LiveTest test(Station s) {
			// Wherever the walkway is when MINE starts, each press must change it.
			boolean[] was = new boolean[1];
			Supplier<Component> didntMove = () -> Component.translatableWithFallback("mine.lesson.check.bridge_still",
					"Not yet: MINE pressed the button, but the walkway didn't move.");
			Supplier<Component> wentBack = () -> Component.translatableWithFallback("mine.lesson.check.bridge_back",
					"Not yet: the walkway moved back by itself when the button popped up. Remember where it is in a variable, and change it only when the button is pressed.");
			Runnable press = () -> {
				was[0] = s.out();
				s.push(true);
			};
			Runnable release = () -> s.push(false);
			// Pressed for a second, then up: the walkway must have moved, and stay there.
			return LiveTest.inGameTime(s.world(), s)
					.then(null, null, press, PRESS_TICKS)
					.then(() -> s.out() != was[0], didntMove, release, 30)
					.then(() -> s.out() != was[0], wentBack, press, PRESS_TICKS)
					.then(() -> s.out() != was[0], didntMove, release, 30)
					.then(() -> s.out() != was[0], wentBack, press, PRESS_TICKS)
					.then(() -> s.out() != was[0], didntMove, release, 30)
					.then(() -> s.out() != was[0], wentBack, null, 0);
		}
	}
}
