**English** · [Italiano](lesson-format.it.md)

# Writing a lesson with missions

MINE's lessons are **data files**, not code: a lesson is a JSON file in a datapack, so a teacher
can write one without touching Java and try it on their server straight away. MINE's own
lessons work exactly the same way: you can read them in
[`src/main/resources/data/mine/mine_lesson/`](../../src/main/resources/data/mine/mine_lesson/).

## Where the file goes

A datapack is a folder inside the world's `datapacks` folder:

```
world/datapacks/my_school/
├── pack.mcmeta
└── data/
    └── my_school/
        └── mine_lesson/
            └── bridges.json
```

`pack.mcmeta` is the same for every datapack on Minecraft 26.3:

```json
{
	"pack": {
		"description": "Our school's MINE lessons",
		"min_format": 121,
		"max_format": 121
	}
}
```

The file name is the lesson's id, the one you type in `/mine lesson start bridges`: use only
lowercase letters, digits and `_`. The folder under `data/` (`my_school` here) can be anything.

After saving the file, type `/reload` on the server: `/mine lesson list` shows the new lesson.
If something is wrong in the file, the lesson is skipped and the **server log** says why, in a
line that starts with `[MINE] Skipping lesson my_school:bridges` — for example, a required
field is missing.

## What a lesson contains

```json
{
	"format": 1,
	"order": 10,
	"title": "Bridges",
	"reflection": "Which part of your program would you change to build a longer bridge?",
	"steps": [
		{
			"id": "deck",
			"goal": "Build a bridge deck 8 blocks long and 2 wide.",
			"not_yet": "Not yet: the deck has to be 8 blocks long and 2 wide.",
			"hints": [
				"It's a floor like in lesson 2: 2 rows of 8 blocks.",
				"Put at least 16 blocks in Byte's highlighted slot."
			],
			"check": { "type": "build", "length": 8, "width": 2 }
		}
	]
}
```

| Field | Required | What it is |
|---|---|---|
| `format` | no | the file's format, today `1` (also when it's missing). A newer MINE still reads it; a file made for a newer MINE than yours is skipped, with the reason in the server log |
| `order` | no | where the lesson appears in `/mine lesson list` (MINE's lessons use 1, 2, 3…; the default is 1000) |
| `title` | yes | the lesson's name, shown when it starts |
| `reflection` | yes | the question shown when the student completes the lesson |
| `steps` | yes | the steps, in order (1 to 32) |

Each step:

| Field | Required | What it is |
|---|---|---|
| `id` | yes | a short name, used in the server log and in translations |
| `goal` | yes | what the student has to do |
| `not_yet` | yes | what MINE says when a program ends but the step isn't done |
| `hints` | no | hints revealed one at a time with `/mine lesson hint` |
| `block_goal` | no | the goal as the [block editor](blocks.md) shows it, when `goal` talks about Lua (e.g. "using a for loop") |
| `block_hints` | no | the hints in the blocks' words, for the block editor's **Hint** button. Give as many as `hints`: hints are counted together, so hint 2 is the second one in either words |
| `python_goal` | no | the goal as the [Python editor](python.md) shows it, when `goal` talks about Lua |
| `python_hints` | no | the hints in Python's words, for the Python editor's **Hint** button; as many as `hints` |
| `check` | yes | how MINE decides that the step is done: see below |
| `give` | no | items MINE puts in Byte's highlighted slot when the step begins: `{"item": "minecraft:oak_planks", "count": 64}`. With `"max_count"` too, each student gets a random number between `count` and `max_count` |

## Checks

MINE never reads the student's code: when one of their programs ends without errors, it looks at
what Byte really did. A check is a `type` plus its parameters.

| Type | Parameters | Done when… |
|---|---|---|
| `said` | — | the program made Byte speak with `mine.say` |
| `roundtrip` | `distance` | Byte went at least `distance` blocks away and came back to where it started, facing the same way |
| `tower` | `height` | there is a column of `height` blocks right in front of where Byte started, and Byte is back at its starting height |
| `build` | `length`, `width` (default 1), `height` (default 1) | the program built a solid box of new blocks near the start: a row, a floor, a wall, a cube… `length` and `width` are horizontal, in either direction |
| `over_the_wall` | `min_distance`, `max_distance` | *MINE builds a brick wall across Byte's way, at a random distance, each time a program starts.* Byte climbed over it and landed right behind it, at its starting height, without breaking it |
| `gold_row` | `length`, `min_gold`, `max_gold` | *MINE lays a row of stone in front of Byte, with gold in random places, each time a program starts.* All the gold was dug up and all the stone is still there |
| `count_to_wall` | `min_distance`, `max_distance` | *MINE builds a brick wall at a random distance, as for `over_the_wall`.* Byte stopped right in front of it and said with `mine.say` how many steps it took: that number and no other |
| `square_of_challenge` | `min_side`, `max_side` | each time a program starts MINE chooses a side, which the program reads with `mine.challenge()`; done when it built a solid square floor of that size |
| `pyramid` | `base` | a step pyramid near the start: a solid `base` × `base` square with squares 2 blocks smaller centred on top, up to the top |
| `redstone_lamp` | — | *MINE builds the redstone scene around Byte (lamp behind, iron door in front, a lever on each side), once for the whole lesson.* The lamp behind Byte is on when the program ends |
| `lever_door` | — | *Live, in the same scene: while the program runs MINE pulls the left lever on and off, then stops the program.* The door followed the lever every time |
| `lever_and` | — | *Live, in the same scene: MINE tries both levers in all four ways, then stops the program.* The lamp was on only with both levers on |
| `tower_of_all` | `min_blocks` (default 3) | there is a tower in front of where Byte started made of **all** the blocks it had in its highlighted slot, and Byte is back on the ground |
| `plate_door`, `night_lamp`, `button_bridge` | — | *Lesson 7, live: MINE builds its part of a station around Byte (a plate and an iron door, a daylight sensor and a lamp, a button and a piston with a walkway), steps on the plate, makes night fall, presses the button.* The door closes by itself 3 s later; the lamp is on only in the dark; each press moves the walkway |
| `board_door`, `board_lamp`, `board_heart` | — | *Lesson 8, live: MINE plays the student's board (button A, the light) and pulls a lever.* The door opens for 3 s; the lamp is on when `luce` is below 50; Byte tells the board `cuore:1`/`cuore:0` |
| `chart` | `key`, `count`, `min`, `max`, `height` (optional) | *MINE plays the board: `count` new numbers between `min` and `max` on `key`, while the program runs.* One column per number in front of where Byte started and to its right, as tall as the number (or `number * height // max`), and Byte back where it started |
| `chart_max` | `key`, `count`, `min`, `max` | *As `chart`.* Byte said the largest number and its position, counting from 1 |
| `maze` | `width`, `depth`, optional `corridor`, `say_steps` | *MINE builds a maze of stone bricks in front of Byte (`width` cells to its right, `depth` ahead; `corridor`: one winding way with no branches), a gold block in the wall at the way out, each time a program starts, and takes it away when the program ends.* Byte is right in front of the gold, never went up and broke no wall; with `say_steps`, it also said how many steps it took |
| `treasure` | `count`, optional `dig`, `home` | *MINE buries `count` gold blocks in the ground near Byte (1–6 forward, 0–4 to the right); `mine.challenge()` gives forward and right of each; the ground goes back as it was when the program ends.* Without `dig`, Byte is on top of the treasure; with it, all are dug up, and with `home` Byte is back where it started |
| `farm` | `width`, `length`, optional `harvest`, `count` | *MINE digs a field of farmland in front of Byte (`width` rows to its right, `length` long), with water on its left; with `harvest` it's planted, some wheat ripe in random places.* Without `harvest`, all sown; with it, the ripe wheat harvested and sown again and the young left; with `count`, Byte also said how many |
| `cipher` | `shift`, optional `tell_key` | *A new secret message every time, Caesar-shifted by `shift` (0: a random key); `mine.challenge()` gives it, and the key with `tell_key`; without the key the message contains BYTE.* Byte said the decoded message |
| `greenhouse_average` | `count` | *MINE plays `count` temperatures (`temperatura`).* The last number Byte said is their average, to half a degree |
| `greenhouse_alarm` | `threshold` | *Live: MINE plays temperatures.* After each one the board got `allarme:1` only above `threshold`, else `allarme:0` |
| `greenhouse_report` | `every` | *Live: MINE plays two groups of `every` temperatures.* After each group the board got their average as `media` |
| `fan` | `on`, `off`, optional `button` | *Live, with a lamp (the fan) behind Byte: MINE plays temperatures, and with `button` presses of `pulsante_a`.* The fan is on from `on`, off at `off` or less, unchanged in between; `pulsante_a` switches a manual mode where it's always on |
| `crossing` | optional `reopen`, `count` | *Live, with a door (the barrier) in front of Byte, a lamp behind and a lever on its left: MINE plays `treno:1` and pulls the lever.* Open and dark to begin with, closed and lit after a train; with `reopen` the lever opens it again; with `count` it opens when all trains have passed, and the board gets `led` |

The checks in *italics* build a challenge in front of Byte, only where there is free space
(otherwise they say how much room they need). The challenge appears as soon as the step begins;
if a program doesn't pass, MINE takes it away and builds a new one when the next program starts,
so the student can try again in the same place. Because the challenge changes every time, a
program only passes if it looks at the world instead of counting on fixed numbers.

## What MINE prepares

A lesson needs nothing from the previous ones. When it starts, MINE creates a Byte for every
student who doesn't have one (in front of them, if there's room). Every Byte gets a diamond
pickaxe if it has no tools, and each step's `give` items arrive in its highlighted slot: what
was there is moved to another slot, never thrown away. Items are given once per step;
`/mine lesson restart` gives them again. Byte receives all this when its shell is waiting
for a command — straight away if it is, otherwise as soon as it is turned on.

Some checks explain *why* a step isn't done yet, instead of repeating `not_yet`: `tower` and
`build` notice when Byte ran out of blocks, and `build` measures what was built ("15
blocks, 5 long, 3 wide and 1 tall").

Need a check that doesn't exist yet? Open an issue and describe the activity: new checks are
added when a lesson needs them.

## Programs for the students

A lesson can give students programs, copied onto their Byte when the lesson starts — for
example the broken programs of [lesson 3](../lessons/03-debugging/README.md). Put them next to
the lesson, one folder per language:

```
data/my_school/mine_lesson_files/bridges/en_us/half_bridge.lua
data/my_school/mine_lesson_files/bridges/it_it/mezzo_ponte.lua
```

The folder after `mine_lesson_files` is the lesson id. Each student gets the programs in their
game's language, otherwise the English ones (`en_us`), otherwise whatever language there is.
File names follow the same rule as lesson ids, plus `.lua`.

Byte picks them up as soon as it is waiting for a command (or when it is next turned on),
and the terminal lists them. They are copied **again each time the lesson starts**, overwriting
files with the same name: `/mine lesson restart` gives a student a fresh copy.

Programs made with **blocks** go in the same folders, as `.json` files saved with the block
editor's **Save file** (names up to 24 characters). They don't go on Byte: they appear in the
student's **My programs** in the [block editor](blocks.md), again each time the lesson starts.

## Languages

The texts in the lesson file are what students see. To translate a lesson into several
languages, add its texts to a resource pack's language files with these keys
(MINE's lessons do this in [`assets/mine/lang/`](../../src/main/resources/assets/mine/lang/)):

```
mine.lesson.<lesson>.title
mine.lesson.<lesson>.reflection
mine.lesson.<lesson>.<step>.goal
mine.lesson.<lesson>.<step>.not_yet
mine.lesson.<lesson>.<step>.hint.1, .hint.2, ...
mine.lesson.<lesson>.<step>.block_goal
mine.lesson.<lesson>.<step>.block_hint.1, .block_hint.2, ...
mine.lesson.<lesson>.<step>.python_goal
mine.lesson.<lesson>.<step>.python_hint.1, .python_hint.2, ...
```

When a key has no translation, the text in the lesson file is used. The lines MINE writes in
Byte's terminal and in the block editor are translated on the server, which only knows MINE's own language files:
there, other datapacks' lessons always use the text in the lesson file.

## Sharing your lesson

A lesson works best with a teacher's guide and tested solutions, like those in
[`docs/lessons`](../lessons/README.md). If you'd like your lesson to become part of MINE, read
[CONTRIBUTING.md](../../CONTRIBUTING.md) — we are especially looking for teachers.
