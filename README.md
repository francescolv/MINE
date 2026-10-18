# MINE — *Mine Is Not Education*

**English** · [Italiano](README.it.md)

[![build](https://github.com/francescolv/MINE/actions/workflows/build.yml/badge.svg)](https://github.com/francescolv/MINE/actions/workflows/build.yml)
[![License: Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

A Fabric mod for Minecraft Java Edition that brings the useful teaching features of
Minecraft Education to school — **on Linux too**, where Minecraft Education doesn't exist.

🌐 Project page: **https://francescolv.github.io/MINE/**

📦 For schools: [server](https://github.com/francescolv/MINE/releases/latest/download/MINE-server.zip) · [modpack for the lab PCs](https://github.com/francescolv/MINE/releases/latest/download/MINE.mrpack) · [installation guide](docs/guide/server.md)

## Why

Minecraft Education runs on Windows, macOS, iPad and Chromebook, but not on Linux, and it
requires Microsoft 365 Education licenses. Many schools use (or would like to use) Linux in
their computer labs. MINE starts from Minecraft Java — which runs great on Linux — and adds
the tools you need to teach, as open source software under the school's control.

What you need to play: a copy of **Minecraft Java Edition** (bought with a regular
Microsoft/Mojang account). What you **don't** need: Microsoft 365 licenses, the Education
client, school tenants.

## What it is NOT

To avoid misunderstandings and out-of-scope issues:

- ❌ No Chemistry module (lab table, elements, reactions)
- ❌ No Classroom Mode (external teacher dashboard, mass teleport/freeze)
- ❌ Not a fork of Minecraft Education, and it uses none of its assets or code
- ❌ Not an official Minecraft product, nor approved by or associated with Mojang or Microsoft

## What's inside

| Module | Education equivalent | What MINE has |
|---|---|---|
| **Code Builder / Agent** | Code Builder, Agent | **Byte**, a robot for each student ([CC: Tweaked](https://github.com/cc-tweaked/CC-Tweaked) turtle), programmed in Lua, with [blocks](docs/guide/blocks.md) in the browser, or in [Python](docs/guide/python.md); [16 lessons](docs/lessons/README.md) whose missions the server checks by itself |
| **Teaching NPCs** | NPCs, boards | NPCs with dialogues and quizzes written in game, pages that follow the lesson, boards with large text, from datapacks too |
| **Portfolio / Camera** | Camera, Portfolio, Book & Quill | camera, portfolio window, class galleries, export as web pages |
| **Class worlds** | World library, lesson worlds | [a world per class](docs/guide/worlds.md), flat or a copy of a map ([Fantasy](https://github.com/NucleoidMC/fantasy)), class rules, a class code of symbols to join; [zones](docs/guide/zones.md) put back as they were, or protected |
| **Physical computing** | — | [micro:bit, Arduino and Raspberry Pi Pico](docs/guide/devices.md) talking with Byte and the world (on a cable, or the Pico 2 W over Wi-Fi), blocks that turn a board into redstone |

What's still missing, for anyone who'd like to help: [ROADMAP.md](ROADMAP.md).

## Requirements

| Software | Version |
|---|---|
| Minecraft Java | 26.3 |
| Fabric Loader | 0.19.5+ |
| Fabric API | 0.162.0+26.3 |
| CC: Tweaked (recommended) | 1.120.3 |
| JDK (development only) | 25 |

## Build

You need a full **JDK 25** (with `javac`). On Fedora: `sudo dnf install java-25-openjdk-devel`.

```bash
# Build the mod
./gradlew build

# Test environment (client with MINE + CC: Tweaked loaded)
./gradlew runClient
```

The compiled file is in `build/libs/mine-<version>.jar`. For schools:
`./gradlew modpack serverPack` builds the modpack (`.mrpack`) and the server package in
`build/distributions/` ([guide](docs/guide/server.md)).

```bash
# Automated in-game tests (headless server, the same ones CI runs)
./gradlew runGametest
```

## Commands

Each student has **Byte**, a robot they program in Lua or with blocks (Minecraft Education calls
it the Agent). A byte is a block of 8 bits: Byte works with the blocks of the world.

| Command | What it does |
|---|---|
| `/mine byte` | Creates your Byte (a CC: Tweaked turtle) in front of you. If you already had one, it replaces it. |
| `/mine byte recall` | Brings your Byte back in front of you, with its programs and inventory. |
| `/mine blocks` | Opens the block editor in your browser: program your Byte with blocks, see the Lua they make, run it ([guide](docs/guide/blocks.md)). |
| *Python* | In the block editor, **Python** opens the Python editor: real Python (MicroPython) with the same names as in Lua, run on your Byte; or [Thonny](docs/guide/python.md#thonny) on the PC ([guide](docs/guide/python.md)). |
| `/mine device` | Connects the board plugged into your PC (micro:bit, Arduino): it talks with your Byte in `key:value` lines ([guide](docs/guide/devices.md)). |
| `/mine byte run <program>` | Starts one of your Byte's programs from chat, so you can watch it work. When it finishes (or stops because of an error) it tells you in chat. |
| `/mine lesson list` | The lessons with missions that MINE checks by itself. |
| `/mine lesson start <lesson>` | Starts a lesson: MINE gives you one goal at a time and checks it when your programs end. Teachers can add students, e.g. `/mine lesson start first_steps @a`. |
| `/mine lesson status` | Your step and its goal. Teachers: `/mine lesson status @a` shows the whole class. |
| `/mine lesson hint` | The next hint for your current step. |
| `/mine lesson restart` | Starts your lesson over. |
| `/mine lesson reset <students>` | Teachers only: forgets the students' lesson progress (not their answers). |
| `/mine lesson answer <text>` | At the end of a lesson: your answer to its reflection question. |
| `/mine lesson answers <lesson>` | Teachers only: every student's answer to that lesson's question, also from students who are offline. |
| `/mine builder add\|remove <students>` | Teachers only: lets students build with MINE's tools (NPCs, boards) without being operators; `/mine builder list` shows who. |
| `/mine view student\|teacher` | Teachers only: try MINE as students see it (NPCs only to read, no teacher commands), then come back. |
| `/mine npc create <name>` | Teachers and builders: an NPC in front of you. Write its dialogue in a book and quill and right-click it with the book ([guide](docs/guide/npc.md)). |
| `/mine npc remove` | Teachers and builders: removes the nearest NPC (builders: their own). |
| `/mine npc place <id>` | Teachers and builders: an NPC from a datapack, e.g. `mine:loops_guide`, the guide of the loops lesson. |
| `/mine npc results [name]` | Teachers and builders: the answers to the quizzes of the nearest NPC (teachers: of any NPC, by name). `/mine npc clear` forgets them. |
| `/mine npc export` | Teachers only: every quiz answer in a CSV file, downloaded to your computer. |
| `/mine board create [size]` | Teachers and builders: a board with large text in front of you; write it with a book and quill ([guide](docs/guide/boards.md)). `place <id>`, `size <size>` and `remove` too. |
| `/mine lab build <lanes>` | Teachers only: the standard lab where you stand, a lane per student; `assign` puts the students on their lanes, `reset` clears it after the lesson ([guide](docs/guide/server.md#3-the-standard-lab)). |
| `/mine world` | Teachers: a world per class, flat or a copy of a map: `create`, `go`, `rules lesson\|free`, `list`, `remove` ([guide](docs/guide/worlds.md)). |
| `/mine zone` | Teachers: zones marked with two corner blocks, put back as they were (`restore`) or protected (`protect`) ([guide](docs/guide/zones.md)). |
| Command blocks | On in the class server, for teachers: buttons that start lessons, plates to other worlds ([guide](docs/guide/command-blocks.md)). |
| `/mine camera [students]` | A camera: right-click with it to take a photo for your portfolio (teachers: give one to students) ([guide](docs/guide/portfolio.md)). |
| `/mine portfolio [student]` | Your portfolio, in a window: photos, captions, notes, sharing (teachers: a student's). `/mine portfolio export`: web pages in a zip. |
| `/mine gallery` | The class gallery. Teachers: `create\|use\|remove <name>`; everybody: `open <name>`, `list`. |

"Teachers" are operators (permission level 2).

Inside Byte, besides the CC: Tweaked APIs, there is the `mine` API: `mine.say(...)` to
talk in chat, `mine.challenge()` for what a lesson asks this time, `mine.device` for the board.
Details in
[docs/guide/lua-api.md](docs/guide/lua-api.md), or `help mine` in Byte's terminal.

## Lessons

Lessons ready for a one-hour lab session, with a teacher's guide and solutions that are
tested automatically: [docs/lessons](docs/lessons/README.md).

1. [First steps with Byte](docs/lessons/01-first-steps/README.md): instructions and sequences
2. [Loops: rows, floors and cubes](docs/lessons/02-loops/README.md): `for` loops, area and volume
3. [Debugging: fix Byte's programs](docs/lessons/03-debugging/README.md): reading errors, fixing bugs
4. [Conditions: Byte decides](docs/lessons/04-conditions/README.md): `if`, `while` and Byte's sensors
5. [Variables and functions](docs/lessons/05-functions/README.md): counting, values from outside, a pyramid with a function
6. [Redstone](docs/lessons/06-redstone/README.md): a lamp, a door with a lever, an AND gate
7. [Sensors](docs/lessons/07-sensors/README.md): waiting, timers, memory
8. [The board](docs/lessons/08-board/README.md): a micro:bit or an Arduino and Byte
9. [Python: data from the board, as charts](docs/lessons/09-charts/README.md) (14–18)
10. [The maze](docs/lessons/10-maze/README.md): an algorithm, the hand on the wall
11. [Treasure hunt](docs/lessons/11-treasure/README.md): coordinates and variables
12. [Python: the connected greenhouse](docs/lessons/12-greenhouse/README.md) (Pico 2 W, 14–18)
13. [Python: automatic control](docs/lessons/13-control/README.md) (micro:bit, 14–18)
14. [Python: the level crossing](docs/lessons/14-crossing/README.md) (Arduino, 14–18)
15. [The farm](docs/lessons/15-farm/README.md): nested loops and conditions
16. [Secret messages](docs/lessons/16-cipher/README.md): Caesar's cipher

Teachers can write their own lessons with missions as JSON files in a datapack:
[docs/guide/lesson-format.md](docs/guide/lesson-format.md).

## Using MINE in the computer lab

**Installing**: the school server (one command: service, whitelist, nightly backups) and the
MINE modpack for the lab PCs (Prism Launcher): [docs/guide/server.md](docs/guide/server.md).

Practical tips for Linux labs (touchpads, Byte setup...):
[docs/guide/linux-lab.md](docs/guide/linux-lab.md).

## Contributing

Code and technical documentation are in English; teacher and student guides are also
translated into Italian. We are looking for Java/Fabric developers, Lua and Python programmers,
**teachers** who write lessons and translators. What's still to do is in [ROADMAP.md](ROADMAP.md);
how to start, in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Apache License 2.0 — see [LICENSE](LICENSE).

MINE's jar includes [Fantasy](https://github.com/NucleoidMC/fantasy) (LGPL-3.0), unchanged, as a
separate library.

"Minecraft" is a trademark of Mojang Synergies AB. MINE is not affiliated with Mojang or Microsoft.
