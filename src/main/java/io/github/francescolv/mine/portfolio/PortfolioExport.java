package io.github.francescolv.mine.portfolio;

import io.github.francescolv.mine.lesson.Lesson;
import io.github.francescolv.mine.lesson.LessonProgress;
import io.github.francescolv.mine.lesson.Lessons;
import io.github.francescolv.mine.npc.NpcQuiz;
import io.github.francescolv.mine.npc.QuizResults;
import io.github.francescolv.mine.text.ServerLanguage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Portfolios and galleries as web pages in a zip, to open in any browser, print or keep: a page
 * for each student (photos, notes, answers to the lessons' questions and to the NPCs' quizzes),
 * one for each class gallery, and an index. Nothing to install, no internet needed.
 */
public final class PortfolioExport {

	private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
	private static final String STYLE = """
			body{font-family:system-ui,sans-serif;margin:0 auto;max-width:1100px;padding:16px;color:#1d2a22;background:#f6f7f2}
			h1{color:#203828}h2{margin-top:32px;border-bottom:2px solid #203828}a{color:#2b6a45}
			.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:16px}
			.card{background:#fff;border-radius:8px;box-shadow:0 1px 3px #0003;overflow:hidden}
			.card img{width:100%;display:block}.card p{margin:8px 12px}.note{white-space:pre-wrap}
			.details{color:#667;font-size:.85em}.right{color:#2b6a45}.wrong{color:#a33}
			table{border-collapse:collapse}td,th{border:1px solid #ccd;padding:4px 8px;text-align:left;vertical-align:top}
			@media print{.card{break-inside:avoid;box-shadow:none;border:1px solid #ccc}}
			""";

	private PortfolioExport() {
	}

	/** The file name for an export made now. */
	public static String fileName(String what) {
		return "portfolio_" + what.replaceAll("[^A-Za-z0-9_-]", "_") + "_" + FILE_TIME.format(LocalDateTime.now()) + ".zip";
	}

	/** Every portfolio and gallery, for the teacher. */
	public static byte[] everything(MinecraftServer server, String language) throws IOException {
		PortfolioStore store = PortfolioStore.get(server);
		List<PortfolioStore.Album> students = store.portfolios().stream()
				.sorted(Comparator.comparing(PortfolioStore.Album::title, String.CASE_INSENSITIVE_ORDER)).toList();
		return zip(server, language, students, store.galleries());
	}

	/** One student's portfolio, for them. */
	public static byte[] of(MinecraftServer server, PortfolioStore.Album portfolio, String language) throws IOException {
		return zip(server, language, List.of(portfolio), List.of());
	}

	private static byte[] zip(MinecraftServer server, String language, List<PortfolioStore.Album> students,
			List<PortfolioStore.Album> galleries) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
			StringBuilder index = new StringBuilder();
			index.append("<h1>").append(esc(t(language, "mine.portfolio.html.title", "Portfolios"))).append("</h1>");
			index.append("<p class=details>").append(esc(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))))
					.append("</p>");
			if (!students.isEmpty()) {
				index.append("<h2>").append(esc(t(language, "mine.portfolio.html.students", "Students"))).append("</h2><ul>");
				for (PortfolioStore.Album album : students) {
					String page = "students/" + slug(album.title()) + "_" + album.key().substring(0, 8) + ".html";
					index.append("<li><a href=\"").append(page).append("\">").append(esc(album.title())).append("</a> (")
							.append(album.entries().size()).append(")</li>");
					write(zip, page, page(album.title(), studentBody(server, album, language, zip)));
				}
				index.append("</ul>");
			}
			if (!galleries.isEmpty()) {
				index.append("<h2>").append(esc(t(language, "mine.portfolio.html.galleries", "Class galleries"))).append("</h2><ul>");
				for (PortfolioStore.Album album : galleries) {
					String page = "galleries/" + slug(album.title()) + ".html";
					index.append("<li><a href=\"").append(page).append("\">").append(esc(album.title())).append("</a> (")
							.append(album.entries().size()).append(")</li>");
					write(zip, page, page(album.title(),
							"<h1>" + esc(t(language, "mine.portfolio.html.gallery", "Class gallery: %s", album.title())) + "</h1>"
									+ entries(server, album, language, zip, true)));
				}
				index.append("</ul>");
			}
			write(zip, "index.html", page("MINE", index.toString()));
		}
		return bytes.toByteArray();
	}

	private static String studentBody(MinecraftServer server, PortfolioStore.Album album, String language, ZipOutputStream zip)
			throws IOException {
		StringBuilder body = new StringBuilder();
		body.append("<p><a href=\"../index.html\">←</a></p><h1>")
				.append(esc(t(language, "mine.portfolio.html.portfolio", "Portfolio of %s", album.title()))).append("</h1>");
		body.append(entries(server, album, language, zip, false));

		UUID student = UUID.fromString(album.key());
		Map<String, LessonProgress.Answer> answers = LessonProgress.get(server).answersOf(student);
		if (!answers.isEmpty()) {
			body.append("<h2>").append(esc(t(language, "mine.portfolio.html.reflections", "Answers to the lessons' questions")))
					.append("</h2><table>");
			answers.forEach((id, answer) -> {
				Lesson lesson = Lessons.get(id);
				body.append("<tr><th>").append(esc(lesson == null ? id : ServerLanguage.render(lesson.reflectionText(), language)))
						.append("</th><td>").append(esc(answer.text())).append("</td></tr>");
			});
			body.append("</table>");
		}

		StringBuilder quizzes = new StringBuilder();
		for (QuizResults.Question question : QuizResults.get(server).all()) {
			QuizResults.Answer answer = question.answers().get(student);
			if (answer == null) {
				continue;
			}
			String result = question.poll() ? ""
					: answer.right() ? "<span class=right>✔</span>"
					: "<span class=wrong>✘</span>" + (answer.solved()
					? " " + esc(t(language, "mine.portfolio.html.tries", "right after %s tries", answer.tries())) : "");
			quizzes.append("<tr><td>").append(esc(question.npcName())).append("</td><td>").append(esc(question.text()))
					.append("</td><td>").append(esc(NpcQuiz.letter(answer.option()) + ") " + answer.text())).append(' ')
					.append(result).append("</td></tr>");
		}
		if (!quizzes.isEmpty()) {
			body.append("<h2>").append(esc(t(language, "mine.portfolio.html.quizzes", "Answers to the NPCs' questions")))
					.append("</h2><table>").append(quizzes).append("</table>");
		}
		return body.toString();
	}

	/** The album's entries as cards; photos are added to the zip next to the page. */
	private static String entries(MinecraftServer server, PortfolioStore.Album album, String language, ZipOutputStream zip,
			boolean withAuthor) throws IOException {
		if (album.entries().isEmpty()) {
			return "<p>" + esc(t(language, "mine.portfolio.html.empty", "Nothing here yet.")) + "</p>";
		}
		StringBuilder cards = new StringBuilder("<div class=grid>");
		Path folder = PortfolioStore.folder(server, album.key());
		for (PortfolioStore.Entry entry : album.entries()) {
			cards.append("<div class=card>");
			if (entry.isPhoto()) {
				Path file = folder.resolve(entry.photo());
				if (Files.isRegularFile(file)) {
					String name = "photos/" + album.key() + "/" + entry.photo();
					zip.putNextEntry(new ZipEntry(name));
					Files.copy(file, zip);
					zip.closeEntry();
					cards.append("<a href=\"../").append(name).append("\"><img loading=lazy src=\"../").append(name)
							.append("\" alt=\"").append(esc(entry.text())).append("\"></a>");
				}
				if (!entry.text().isEmpty()) {
					cards.append("<p>").append(esc(entry.text())).append("</p>");
				}
			} else {
				cards.append("<p class=note>").append(esc(entry.text())).append("</p>");
			}
			cards.append("<p class=details>").append(esc(Portfolios.details(entry, withAuthor, language))).append("</p></div>");
		}
		return cards.append("</div>").toString();
	}

	private static String page(String title, String body) {
		return "<!doctype html><html><head><meta charset=utf-8><meta name=viewport content=\"width=device-width,initial-scale=1\">"
				+ "<title>" + esc(title) + "</title><style>" + STYLE + "</style></head><body>" + body + "</body></html>";
	}

	private static void write(ZipOutputStream zip, String name, String content) throws IOException {
		zip.putNextEntry(new ZipEntry(name));
		zip.write(content.getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
	}

	private static String t(String language, String key, String fallback, Object... args) {
		return ServerLanguage.render(Component.translatableWithFallback(key, fallback, args), language);
	}

	static String slug(String name) {
		String slug = name.replaceAll("[^A-Za-z0-9_-]", "_");
		return slug.isEmpty() ? "_" : slug;
	}

	static String esc(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

}
