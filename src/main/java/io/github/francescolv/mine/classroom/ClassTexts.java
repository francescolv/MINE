package io.github.francescolv.mine.classroom;

import io.github.francescolv.mine.world.ClassWorlds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/** What students and teachers are told about joining the class. */
final class ClassTexts {

	private ClassTexts() {
	}

	/** For a student who isn't in the class, when there's no code to type. */
	static Component notInClass() {
		return Component.translatableWithFallback("mine.class.not_in_class",
				"You're not in this class yet.\n\nAsk your teacher for the class code, or to let you in.");
	}

	/** For a student who isn't in the class and whose game has no MINE to type the code. */
	static Component noMine() {
		return Component.translatableWithFallback("mine.class.no_mine",
				"You're not in this class yet.\n\nYour Minecraft doesn't have MINE, to type the class code: ask your teacher to let you in.");
	}

	static Component tooMany() {
		return Component.translatableWithFallback("mine.class.too_many",
				"Too many wrong codes. Ask your teacher to let you in.");
	}

	static Component closed() {
		return Component.translatableWithFallback("mine.class.closed",
				"The class code is closed. Ask your teacher to open it again.");
	}

	static Component gaveUp() {
		return Component.translatableWithFallback("mine.class.gave_up", "You didn't type the class code.");
	}

	/** For teachers: a student is knocking, with a button to let them in. */
	static Component trying(String name, boolean codeOpen) {
		MutableComponent button = Component.translatableWithFallback("mine.class.let_in", "[Let in]")
				.withStyle(style -> style.withColor(ChatFormatting.GREEN)
						.withClickEvent(new ClickEvent.RunCommand("/mine class admit " + name))
						.withHoverEvent(new HoverEvent.ShowText(Component.literal("/mine class admit " + name))));
		MutableComponent message = codeOpen
				? Component.translatableWithFallback("mine.class.trying_code", "%s is trying to join the class with the code. ", name)
				: Component.translatableWithFallback("mine.class.trying", "%s is trying to join the class but isn't on it yet. ", name);
		return message.withStyle(ChatFormatting.YELLOW).append(button);
	}

	static Component joined(String name, String world) {
		return (ClassWorlds.MAIN.equals(world)
				? Component.translatableWithFallback("mine.class.joined", "%s joined the class with the code.", name)
				: Component.translatableWithFallback("mine.class.joined_world", "%s joined the class with the code of %s.", name, world))
				.withStyle(ChatFormatting.GREEN);
	}

	static Component gaveUpTrying(String name) {
		return Component.translatableWithFallback("mine.class.too_many_teacher",
				"%s typed too many wrong codes. Let them in with /mine class admit %s", name, name)
				.withStyle(ChatFormatting.YELLOW);
	}
}
