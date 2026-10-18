package io.github.francescolv.mine.gametest;

import com.google.gson.JsonParser;
import io.github.francescolv.mine.DataFormat;
import io.github.francescolv.mine.lesson.Lessons;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * Version numbers in what MINE reads and writes: a file for a newer MINE is skipped with a reason,
 * files without a number are the first format, saved data carries its format.
 */
public class DataFormatGameTest {

	@GameTest
	public void formatNumbers(GameTestHelper ctx) {
		ctx.assertTrue(DataFormat.tooNew(JsonParser.parseString("{\"title\": \"x\"}")) == null, "No number: the first format");
		ctx.assertTrue(DataFormat.tooNew(JsonParser.parseString("{\"format\": " + DataFormat.FILES + "}")) == null, "This MINE's format");
		String newer = DataFormat.tooNew(JsonParser.parseString("{\"format\": " + (DataFormat.FILES + 1) + "}"));
		ctx.assertTrue(newer != null && newer.contains("update MINE"), "A newer format is skipped, saying why: " + newer);
		ctx.assertTrue(Lessons.get("first_steps") != null, "The lessons with \"format\": 1 load");

		CompoundTag saved = DataFormat.write(new CompoundTag());
		ctx.assertTrue(saved.getIntOr("MineFormat", 0) == DataFormat.SAVED, "Saved data carries its format");
		ctx.succeed();
	}
}
