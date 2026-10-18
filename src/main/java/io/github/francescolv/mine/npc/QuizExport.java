package io.github.francescolv.mine.npc;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every answer to the NPCs' quizzes as a CSV file, one row per student and question, to open in a
 * spreadsheet. Saved in the world, in {@code mine/exports}.
 */
public final class QuizExport {

	static final List<String> COLUMNS = List.of("npc", "page", "question", "type", "student", "first_answer",
			"first_answer_text", "first_right", "solved", "tries", "time");

	private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
	private static final DateTimeFormatter CELL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private QuizExport() {
	}

	/** The CSV text: a header, then the answers by NPC, question and student. */
	public static String csv(QuizResults results) {
		StringBuilder csv = new StringBuilder();
		row(csv, COLUMNS);
		for (QuizResults.Question question : results.all()) {
			List<Map.Entry<UUID, QuizResults.Answer>> answers = question.answers().entrySet().stream()
					.sorted(Comparator.comparing(entry -> entry.getValue().name(), String.CASE_INSENSITIVE_ORDER))
					.toList();
			for (Map.Entry<UUID, QuizResults.Answer> entry : answers) {
				QuizResults.Answer answer = entry.getValue();
				boolean poll = question.poll();
				row(csv, List.of(question.npcName(), String.valueOf(question.page() + 1), question.text(),
						poll ? "poll" : "quiz", answer.name(), NpcQuiz.letter(answer.option()), answer.text(),
						poll ? "" : String.valueOf(answer.right()), poll ? "" : String.valueOf(answer.solved()),
						poll ? "" : String.valueOf(answer.tries()),
						CELL_TIME.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(answer.time()), ZoneId.systemDefault()))));
			}
		}
		return csv.toString();
	}

	/** The file name for an export made now, e.g. {@code quiz-answers_2026-10-07_09-30-00.csv}. */
	public static String fileName() {
		return "quiz-answers_" + FILE_TIME.format(LocalDateTime.now()) + ".csv";
	}

	/**
	 * The CSV as bytes: UTF-8 with a byte order mark, so that spreadsheets show accented letters
	 * right.
	 */
	public static byte[] bytes(String csv) {
		return ("﻿" + csv).getBytes(StandardCharsets.UTF_8);
	}

	/** Saves the file in the world's {@code mine/exports} folder and returns where. */
	public static Path save(MinecraftServer server, String fileName, byte[] content) throws IOException {
		Path folder = server.getWorldPath(LevelResource.ROOT).resolve("mine").resolve("exports");
		Files.createDirectories(folder);
		Path file = folder.resolve(fileName);
		Files.write(file, content);
		return file.normalize();
	}

	/** One CSV row (RFC 4180): fields with commas, quotes or line breaks are quoted. */
	private static void row(StringBuilder csv, List<String> fields) {
		for (int i = 0; i < fields.size(); i++) {
			String field = fields.get(i);
			if (i > 0) {
				csv.append(',');
			}
			if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
				csv.append('"').append(field.replace("\"", "\"\"")).append('"');
			} else {
				csv.append(field);
			}
		}
		csv.append("\r\n");
	}
}
