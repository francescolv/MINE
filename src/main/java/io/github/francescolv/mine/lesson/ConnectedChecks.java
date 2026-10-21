package io.github.francescolv.mine.lesson;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.francescolv.mine.agent.AgentProgram;
import io.github.francescolv.mine.device.Devices;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The lessons for older students with a board, in Python: MINE plays the student's board (and the
 * world around Byte) and looks at what Byte does, so they work without the board too.
 * <ul>
 * <li>Lesson 12, the connected greenhouse (Pico 2 W over Wi-Fi): the average of the temperatures,
 * an alarm on the board above a threshold, the average of every 5 sent back to the board;</li>
 * <li>lesson 13, automatic control (micro:bit): a fan (the lamp behind Byte) on above a threshold,
 * then with hysteresis, then with a button for manual mode;</li>
 * <li>lesson 14, the level crossing (Arduino): a state machine, a train on the board closes the
 * barrier (the door in front of Byte) and lights the lamp, the train passing (the lever) opens it.</li>
 * </ul>
 */
final class ConnectedChecks {

	static final String TEMPERATURE = "temperatura";
	static final String ALARM = "allarme";
	static final String AVERAGE = "media";
	static final String TRAIN = "treno";
	static final String LED = "led";

	private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:[.,]\\d+)?");
	/** Between two numbers MINE plays, when it doesn't wait for an answer: a board writing twice a second. */
	private static final long PLAY_MS = 500;

	private ConnectedChecks() {
	}

	private static void play(AgentProgram program, String key, Object value) {
		Devices.play(program.world().getServer(), program.owner(), key, value.toString());
	}

	/** A number (with a point or a comma) from text; {@code null} if there's none. */
	@Nullable
	private static Double number(@Nullable String text) {
		if (text == null) {
			return null;
		}
		Matcher matcher = NUMBER.matcher(text);
		return matcher.find() ? Double.parseDouble(matcher.group().replace(',', '.')) : null;
	}

	private static double mean(List<Integer> values) {
		return values.stream().mapToInt(Integer::intValue).average().orElse(0);
	}

	private static String format(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	private static Component list(List<?> values) {
		return ComponentUtils.formatList(values.stream().map(value -> value instanceof Component text ? text
				: Component.literal(value.toString())).toList(), Component.literal(", "));
	}

	// Lesson 12, the connected greenhouse.

	/**
	 * MINE plays {@code count} temperatures; Byte says their average (to half a degree, rounded
	 * any way). The program ends by itself; the last number it said counts.
	 */
	record Average(int count) implements Check {

		static final MapCodec<Average> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.intRange(2, 20).fieldOf("count").forGetter(Average::count)
		).apply(instance, Average::new));

		private static final class Played {
			final List<Integer> values = new ArrayList<>();
			int sent;
			long next = -1;
		}

		@Override
		public Object start(AgentProgram program) {
			Played played = new Played();
			RandomSource random = program.world().getRandom();
			for (int i = 0; i < count; i++) {
				played.values.add(16 + random.nextInt(19));
			}
			return played;
		}

		@Override
		public boolean watch(AgentProgram program, @Nullable Object memo) {
			if (memo instanceof Played played && played.sent < played.values.size()) {
				long now = System.currentTimeMillis();
				if (played.next < 0) {
					played.next = now + LiveTest.SETTLE_MS;
				}
				if (now >= played.next) {
					play(program, TEMPERATURE, played.values.get(played.sent++));
					played.next = now + PLAY_MS;
				}
			}
			return false;
		}

		@Nullable
		private static Double said(AgentProgram program) {
			List<String> messages = program.messages();
			for (int i = messages.size() - 1; i >= 0; i--) {
				Double number = number(messages.get(i));
				if (number != null) {
					return number;
				}
			}
			return null;
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			Double said = said(program);
			return memo instanceof Played played && played.sent == played.values.size() && said != null
					&& Math.abs(said - mean(played.values)) <= 0.5;
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (!(memo instanceof Played played)) {
				return null;
			}
			if (played.sent < played.values.size()) {
				return Component.translatableWithFallback("mine.lesson.check.greenhouse_waiting",
						"Not yet: the program ended before all %s temperatures arrived.", count);
			}
			Double said = said(program);
			if (said == null) {
				return Component.translatableWithFallback("mine.lesson.check.greenhouse_say",
						"Not yet: at the end Byte has to say the average with mine.say.");
			}
			return Component.translatableWithFallback("mine.lesson.check.greenhouse_average",
					"Not yet: the temperatures were %s: their average is %s, and Byte said %s.",
					list(played.values), format(mean(played.values)), format(said));
		}
	}

	/** Tested live: the board must get {@code allarme:1} when the temperature is above {@code threshold}, else {@code allarme:0}. */
	record Alarm(int threshold) implements LiveTest.Check {

		static final MapCodec<Alarm> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.intRange(0, 100).fieldOf("threshold").forGetter(Alarm::threshold)
		).apply(instance, Alarm::new));

		@Override
		public Object test(AgentProgram program) {
			RandomSource random = program.world().getRandom();
			UUID student = program.owner();
			// Below and above, twice, and right on the threshold: "above" doesn't take it in.
			List<Integer> values = List.of(threshold - 2 - random.nextInt(8), threshold + 1 + random.nextInt(8),
					threshold - 2 - random.nextInt(8), threshold + 1 + random.nextInt(8), threshold);
			LiveTest test = new LiveTest();
			for (int value : values) {
				test.act(() -> play(program, TEMPERATURE, value))
						.expect(() -> BoardChecks.on(Devices.lastSent(student, ALARM)) == value > threshold,
								() -> Component.translatableWithFallback("mine.lesson.check.greenhouse_alarm",
										"Not yet: the temperature was %s, and Byte told the board allarme:%s. It must be 1 only above %s, else 0.",
										value, String.valueOf(Devices.lastSent(student, ALARM)), threshold));
			}
			return test;
		}
	}

	/** Tested live: after every {@code every} temperatures, the board gets their average as {@code media}. */
	record Report(int every) implements LiveTest.Check {

		static final MapCodec<Report> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.intRange(2, 10).fieldOf("every").forGetter(Report::every)
		).apply(instance, Report::new));

		@Override
		public Object test(AgentProgram program) {
			RandomSource random = program.world().getRandom();
			UUID student = program.owner();
			LiveTest test = new LiveTest();
			for (int group = 0; group < 2; group++) {
				List<Integer> values = new ArrayList<>();
				for (int i = 0; i < every; i++) {
					values.add(16 + random.nextInt(19));
				}
				for (int i = 0; i < every; i++) {
					int value = values.get(i);
					Runnable send = () -> play(program, TEMPERATURE, value);
					if (i < every - 1) {
						test.then(null, null, send, PLAY_MS);
					} else {
						test.act(send);
					}
				}
				test.expect(() -> {
					Double sent = number(Devices.lastSent(student, AVERAGE));
					return sent != null && Math.abs(sent - mean(values)) <= 0.5;
				}, () -> Component.translatableWithFallback("mine.lesson.check.greenhouse_report",
						"Not yet: after the temperatures %s, Byte told the board media:%s. Their average is %s.",
						list(values), String.valueOf(Devices.lastSent(student, AVERAGE)), format(mean(values))));
			}
			return test;
		}
	}

	// Lesson 13, automatic control.

	/**
	 * Tested live: the fan (the lamp behind Byte) switches on when the temperature is
	 * {@code on} or more, off when it is {@code off} or less, and in between stays as it was (for a
	 * plain threshold, {@code off} is {@code on - 1}). With {@code button}, {@code pulsante_a:1}
	 * switches manual mode on and off: in manual mode the fan is always on.
	 */
	record Fan(int on, int off, boolean button) implements LiveTest.Check {

		static final MapCodec<Fan> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.intRange(-50, 100).fieldOf("on").forGetter(Fan::on),
				Codec.intRange(-50, 100).fieldOf("off").forGetter(Fan::off),
				Codec.BOOL.optionalFieldOf("button", false).forGetter(Fan::button)
		).apply(instance, Fan::new));

		@Override
		public Object test(AgentProgram program) {
			Object built = BoardChecks.Scene.build(program.world(), program.startPos(), program.startFacing(), BoardChecks.Part.LAMP);
			if (!(built instanceof BoardChecks.Scene scene)) {
				return BoardChecks.noRoom();
			}
			// A temperature (a number) or the button (null): what happens, in order.
			List<Integer> events;
			if (button) {
				events = java.util.Arrays.asList(off - 2, null, off - 1, null, on + 2, null, off - 4, null);
			} else if (off < on - 1) {
				// Hysteresis: in between, it stays as it was, coming from below and from above.
				int middle = (on + off) / 2;
				events = List.of(off - 2, middle, on + 1, middle, off + 1 == on ? middle : off + 1, off - 1, middle);
			} else {
				events = List.of(off - 3, on + 2, off, on, on - 1 == off ? off - 5 : off - 1);
			}
			LiveTest test = new LiveTest(scene);
			List<Component> history = new ArrayList<>();
			boolean[] state = { false, false };
			for (Integer event : events) {
				Component label = event == null ? Component.translatableWithFallback("mine.lesson.check.event_button", "button A")
						: Component.literal(event + "°");
				Runnable act = event == null ? () -> play(program, BoardChecks.BUTTON, 1) : () -> play(program, TEMPERATURE, event);
				// What the fan must be now, worked out as the test is written.
				if (event == null) {
					state[1] = !state[1];
				} else if (event >= on) {
					state[0] = true;
				} else if (event <= off) {
					state[0] = false;
				}
				boolean expected = state[0] || state[1];
				history.add(label);
				List<Component> sofar = List.copyOf(history);
				test.act(act).expect(() -> scene.lit() == expected, () -> Component.translatableWithFallback("mine.lesson.check.control_fan",
						"Not yet: after %s the fan should be %s, and it was %s.", list(sofar), Checks.onOff(expected), Checks.onOff(scene.lit())));
			}
			return test;
		}
	}

	// Lesson 14, the level crossing.

	/**
	 * Tested live. Open (the door in front of Byte open, the lamp behind it off) to begin with; a
	 * train from the board ({@code treno:1}) closes it and lights the lamp. With {@code reopen},
	 * the train passing (MINE pulls the lever on Byte's left) opens it again; with {@code count},
	 * trains are counted (it opens when all have passed) and the board's LED ({@code led}) shows
	 * when it is closed.
	 */
	record Crossing(boolean reopen, boolean count) implements LiveTest.Check {

		static final MapCodec<Crossing> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.BOOL.optionalFieldOf("reopen", false).forGetter(Crossing::reopen),
				Codec.BOOL.optionalFieldOf("count", false).forGetter(Crossing::count)
		).apply(instance, Crossing::new));

		private enum Event { TRAIN, PASSED, LEVER_BACK }

		@Override
		public Object test(AgentProgram program) {
			Object built = BoardChecks.Scene.build(program.world(), program.startPos(), program.startFacing(),
					BoardChecks.Part.DOOR, BoardChecks.Part.LAMP, BoardChecks.Part.HEART);
			if (!(built instanceof BoardChecks.Scene scene)) {
				return BoardChecks.noRoom();
			}
			UUID student = program.owner();
			List<Event> events = new ArrayList<>();
			if (!reopen) {
				events.add(Event.TRAIN);
			} else if (!count) {
				events.addAll(List.of(Event.TRAIN, Event.PASSED, Event.LEVER_BACK, Event.TRAIN, Event.PASSED, Event.LEVER_BACK));
			} else {
				events.addAll(List.of(Event.TRAIN, Event.TRAIN, Event.PASSED, Event.LEVER_BACK, Event.PASSED, Event.LEVER_BACK,
						Event.TRAIN, Event.PASSED, Event.LEVER_BACK));
			}
			LiveTest test = new LiveTest(scene);
			// To begin with, open: the program sets it up when it starts.
			test.expect(() -> scene.open() && !scene.lit(), () -> Component.translatableWithFallback("mine.lesson.check.crossing_start",
					"Not yet: when the program starts, the barrier must be open (redstone in front on) and the light off."));
			List<Component> history = new ArrayList<>();
			int trains = 0;
			for (Event event : events) {
				Runnable act = switch (event) {
					case TRAIN -> () -> play(program, TRAIN, 1);
					case PASSED -> () -> scene.pull(true);
					case LEVER_BACK -> () -> scene.pull(false);
				};
				switch (event) {
					case TRAIN -> trains = count ? trains + 1 : 1;
					case PASSED -> trains = count ? Math.max(0, trains - 1) : 0;
					case LEVER_BACK -> {
					}
				}
				if (event == Event.LEVER_BACK) {
					test.act(act);
					continue;
				}
				history.add(event == Event.TRAIN ? Component.translatableWithFallback("mine.lesson.check.event_train", "a train")
						: Component.translatableWithFallback("mine.lesson.check.event_passed", "a train passed"));
				boolean closed = trains > 0;
				List<Component> sofar = List.copyOf(history);
				Component barrier = closed ? Component.translatableWithFallback("mine.lesson.check.closed", "closed")
						: Component.translatableWithFallback("mine.lesson.check.open", "open");
				test.act(act).expect(() -> scene.open() != closed && scene.lit() == closed,
						() -> Component.translatableWithFallback("mine.lesson.check.crossing",
								"Not yet: after %s the barrier should be %s and the light %s.", list(sofar), barrier, Checks.onOff(closed)));
				if (count) {
					test.expect(() -> BoardChecks.on(Devices.lastSent(student, LED)) == closed,
							() -> Component.translatableWithFallback("mine.lesson.check.crossing_led",
									"Not yet: after %s the board should have got led:%s: led:1 while the barrier is closed, led:0 when it's open.",
									list(sofar), closed ? 1 : 0));
				}
			}
			return test;
		}

		@Override
		public void ended(AgentProgram program, @Nullable Object memo, boolean passed) {
			if (memo instanceof LiveTest test && test.scene() instanceof BoardChecks.Scene scene) {
				scene.pull(false);
			}
			LiveTest.Check.super.ended(program, memo, passed);
		}
	}
}
