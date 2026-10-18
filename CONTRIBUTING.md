# Contributing to MINE

Thanks for your interest! MINE is a community project: it needs many different skills,
not just code.

## How you can help

- **Teachers** — propose lessons, tell us what you use in Minecraft Education and what you miss.
  Open an issue with the *Lesson proposal* template (`lesson.yml`).
- **Java/Fabric developers** — core mod modules (worlds, portfolio, NPCs, permissions).
- **Lua and Python programmers** — programs, libraries and lesson solutions for Byte.
- **Testers / lab technicians** — try MINE on real lab machines (Linux, Windows, old PCs) and report problems.
- **Translators and artists** — texts, textures, icons (original assets only!).

## Development environment

1. Install a full JDK 25 (Fedora: `java-25-openjdk-devel`, Debian/Ubuntu: `openjdk-25-jdk`).
2. Clone the repo and run `./gradlew runClient`.
3. Recommended IDEs: IntelliJ IDEA (with the Minecraft Development plugin) or VS Code with the Java extensions.

## Language

MINE is meant to be an international project:

- **Code, comments, identifiers and developer docs** (README, ROADMAP, this file, issues and PRs)
  are in **English**.
- **Teacher and student docs** (`docs/guide`, `docs/lessons`) are written in English, with
  translations next to the English file as `<name>.<lang>.md` — for example `README.it.md`
  beside `README.md`. Italian is maintained today; other languages are welcome.
- **Text players see in game** lives only in the translation files (`assets/mine/lang/`):
  `en_us.json` is the reference, every other language follows it. Don't hard-code it in Java or Lua.

## Rules

- **No assets or code from Minecraft Education**, nor from other projects with incompatible licenses.
  Everything you contribute must be your own or compatible with Apache 2.0.
- One PR = one change. Describe what it changes and how you tested it: `./gradlew runGametest` runs
  the in-game tests (also every lesson's solutions), the same ones CI runs.
- Respect the scope in [README.md](README.md#what-it-is-not) and the [ROADMAP](ROADMAP.md):
  for big features, open an issue to discuss it first.
- Behaviour: see [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md). The project is meant for schools:
  always keep a respectful tone.

## Licensing of contributions

By submitting a PR you agree that your contribution is released under the project's
Apache 2.0 license.
