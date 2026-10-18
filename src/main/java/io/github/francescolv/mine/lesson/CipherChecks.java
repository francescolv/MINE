package io.github.francescolv.mine.lesson;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.francescolv.mine.agent.AgentProgram;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Lesson 16, secret messages: the Caesar cipher, like Minecraft Education's lessons on ciphers.
 * Each time a program starts, MINE writes a new secret message in capital letters, each letter
 * moved {@code shift} places forward in the alphabet (Z goes round to A; spaces stay). The program
 * reads it with {@code mine.challenge()}, decodes it and says it.
 */
final class CipherChecks {

	/** Words that are the same in English and Italian: Minecraft's. */
	private static final List<String> WORDS = List.of("CREEPER", "ZOMBIE", "ENDERMAN", "REDSTONE", "NETHER", "PORTAL",
			"OBSIDIAN", "VILLAGER", "MINECRAFT", "SKELETON", "PIGLIN", "BEACON", "ELYTRA", "WITHER", "AXOLOTL");

	private CipherChecks() {
	}

	/** Each letter of {@code text} moved {@code shift} places forward (backward if negative). */
	static String shift(String text, int shift) {
		StringBuilder out = new StringBuilder();
		for (char c : text.toCharArray()) {
			out.append(c >= 'A' && c <= 'Z' ? (char) ('A' + Math.floorMod(c - 'A' + shift, 26)) : c);
		}
		return out.toString();
	}

	/**
	 * {@code shift}: the key, the same every time (0: a new one every time, from 1 to 25). With
	 * {@code tell_key}, {@code mine.challenge()} also gives the key; otherwise the program has to
	 * find it, knowing that the message contains the word BYTE.
	 */
	record Cipher(int shift, boolean tellKey) implements Check {

		static final MapCodec<Cipher> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.intRange(0, 25).fieldOf("shift").forGetter(Cipher::shift),
				Codec.BOOL.optionalFieldOf("tell_key", false).forGetter(Cipher::tellKey)
		).apply(instance, Cipher::new));

		/** The message, its key, and how it was given to the program. */
		private record Secret(String message, int key, String coded) {
		}

		@Override
		public Object start(AgentProgram program) {
			RandomSource random = program.world().getRandom();
			String first = WORDS.get(random.nextInt(WORDS.size()));
			String second;
			do {
				second = WORDS.get(random.nextInt(WORDS.size()));
			} while (second.equals(first));
			// Without the key, the message has BYTE in it: that's how the program knows it found the key.
			String message = shift == 0 && !tellKey ? "BYTE " + first + " " + second : first + " " + second;
			int key = shift > 0 ? shift : 1 + random.nextInt(25);
			Secret secret = new Secret(message, key, CipherChecks.shift(message, key));
			if (tellKey) {
				program.setChallenge(secret.coded(), key);
			} else {
				program.setChallenge(secret.coded());
			}
			return secret;
		}

		@Override
		public boolean passed(AgentProgram program, @Nullable Object memo) {
			return memo instanceof Secret secret && program.messages().stream()
					.anyMatch(message -> message.toUpperCase(Locale.ROOT).contains(secret.message()));
		}

		@Override
		public @Nullable Component whyNot(AgentProgram program, @Nullable Object memo) {
			if (!(memo instanceof Secret secret)) {
				return null;
			}
			if (program.messages().isEmpty()) {
				return Component.translatableWithFallback("mine.lesson.check.cipher_say",
						"Not yet: Byte has to say the decoded message with mine.say.");
			}
			return Component.translatableWithFallback("mine.lesson.check.cipher_wrong",
					"Not yet: the secret message was %s, and Byte said %s. Try decoding the first letter on paper: which way, and how many places?",
					secret.coded(), program.messages().getLast());
		}
	}
}
