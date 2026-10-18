**English** · [Italiano](README.it.md)

# Lesson 3 — Debugging: fix Byte's programs

| | |
|---|---|
| **Duration** | 1 hour (50–60 minutes) |
| **Age** | 10–14 years old (also fine for the first year of secondary school) |
| **Concepts** | bug, error message, line number; syntax errors, runtime errors, logic errors |
| **Prerequisites** | [lesson 2](../02-loops/README.md): `for` loops and loops inside loops |

## Goals

By the end of the lesson students can:

1. read an error message and find the line it points to;
2. tell apart the three kinds of bugs: Lua **won't start** the program (syntax), the program
   **stops halfway** (runtime), the program **runs but does the wrong thing** (logic);
3. fix a bug by changing one thing at a time and trying again;
4. see a mistake as information, not as a failure.

## Preparation (teacher, before the lesson)

- The same setup as lesson 2: a **superflat world in Creative mode**, students side by side,
  all facing the same way, about **8 blocks apart**, with at least **10 free blocks in front**
  of each Byte.
- The quickest way: the **standard lab** (`/mine lab build <students>`, then `/mine lab assign`)
  has exactly these lanes ([guide](../../guide/server.md#3-the-standard-lab)); `/mine lab reset`
  clears it after the lesson.
- Nothing else to prepare: when the lesson starts MINE creates a Byte for any student who
  doesn't have one, and at every step it puts **64 planks** in Byte's highlighted slot.
- Try the lesson yourself: start it with `/mine lesson start debugging` and fix the three
  programs. It takes 10 minutes. The fixed versions are in [`solutions/`](solutions/).

## Missions: MINE gives out the broken programs

At the start of the lesson, type:

```
/mine lesson start debugging @a
```

MINE copies **three broken programs** onto every student's Byte, and the terminal says so:
*"New programs for the lesson: broken_floor, broken_row, broken_square"*. Students whose game is
in Italian get them with Italian names and comments (`fila_rotta`, `pavimento_rotto`,
`quadrato_rotto`). If a Byte is off, the programs arrive when it is turned on.

| Step | Program | The bug | Done when… |
|---|---|---|---|
| 1 · `typo` | `broken_row` | a misspelled instruction: it **stops with an error** | the program builds a straight row of 6 blocks |
| 2 · `missing_end` | `broken_floor` | an `end` is missing: Lua **won't start it** | it builds a floor 4 long and 3 wide |
| 3 · `wrong_size` | `broken_square` | no error, but it builds **the wrong size** | it builds a square 4 × 4 |

As in lesson 2, MINE looks at what Byte builds, not at the code. The programs are copied
again each time the lesson starts: `/mine lesson restart` gives a student a fresh set of broken
programs, **overwriting their changes**. A student who creates a new Byte gets them again too.

The other commands are the same as in lesson 1 (see the
[lesson 1 guide](../01-first-steps/README.md#missions-mine-checks-the-steps)).

## With blocks

The lesson works in the [block editor](../../guide/blocks.md) too. Starting the lesson also puts
**three broken block programs** in every student's **My programs**, with the same names
(`broken_row`, `broken_floor`, `broken_square`; in Italian `fila_rotta`, `pavimento_rotto`,
`quadrato_rotto`): students open one, press **Run ▶**, watch, fix the blocks and run them again.
Like the Lua ones, they are given again when the lesson restarts, replacing the student's changes.

With blocks, Lua **can't refuse a program**: blocks only fit together in ways that make sense, so
there are no typos and no missing `end`s. That's a great discussion point (it's why blocks
exist), and it's why the block bugs are different ones:

| Step | Program | The bug with blocks | Fixed |
|---|---|---|---|
| 1 | `broken_row` | a **wrong choice**: `place a block in front` instead of `below`. Byte places one block in the air and stays there | [broken_row.json](solutions/blocks/broken_row.json) |
| 2 | `broken_floor` | a block **in the wrong place**: `move up` is inside the loop of the rows, so every row is one block higher: a staircase. MINE says *"12 blocks, 4 long, 3 wide and 3 tall"* | [broken_floor.json](solutions/blocks/broken_floor.json) |
| 3 | `broken_square` | the **wrong numbers**, as in Lua: each row repeats 3 times instead of 4 | [broken_square.json](solutions/blocks/broken_square.json) |

The detective's method stays the same, with *watch Byte* and *read MINE's measurements*
instead of reading an error message. Bug 2 is the blocks' version of the missing `end`: in both
cases a piece of the program is inside the wrong loop. The fixed programs (Italian ones in
[`solutions/it/blocks`](solutions/it/blocks)) open with **Open file**.

## Lesson plan

### Introduction — 5 minutes, no computers

Tell the story of the first "bug": in 1947 the team working on the Harvard Mark II computer
found a real moth stuck in one of its relays, and taped it into their logbook as the
"first actual case of bug being found". Engineers already called faults *bugs*; since then,
finding and fixing them has been called **debugging**.

Then: *"Today your Byte has three programs written by someone in a hurry. Each one has a bug.
Your job is to be the detective."*

Write the detective's method on the board — it's the real takeaway of the lesson:

1. **Run** the program and look at what happens.
2. **Read** the error message, all of it.
3. **Find** the line.
4. **Guess** what's wrong, and **change one thing**.
5. **Try again.**

### Activity 1 — `broken_row`: the program stops (10 minutes)

Students type `ls` to see the new programs, then run `broken_row`. Byte goes up one block
and stops. The terminal shows:

```
/broken_row.lua:5: attempt to call
field 'placedown' (a nil value)
Line 5
  byte.placedown()

Did you mean: placeDown?
```

Take this message apart together, on the board:

- `broken_row.lua:5` — **which program, which line**. Line 5, counting the comments too.
- `attempt to call field 'placedown' (a nil value)` — Lua looked for an instruction called
  `placedown` and found nothing (`nil`).
- `Did you mean: placeDown?` — sometimes the computer even suggests the fix!

Then `edit broken_row`, fix line 5, save, and run it again.

Discussion question: why did Byte go up before stopping? (Lua runs the lines in order:
`byte.up()` comes before the broken line. This is a **runtime error**: the program started,
and stopped when it reached the bug.)

### Activity 2 — `broken_floor`: Lua won't start it (15 minutes)

Run `broken_floor`. This time Byte **doesn't move at all**, not even up. The terminal says:

```
Unexpected end of file. Expected end or
another statement.

Line 3
for row = 1, 3 do
Block started here.

Line 17

Expected end of block here.
```

This is a **syntax error**: before running anything, Lua reads the whole program and checks
that it is written correctly — like checking that every open bracket is closed. It found a
`for` that is never closed.

The interesting part: Lua points at line 3 (the outer loop), but the missing `end` belongs to
the **first inner loop**, the one that goes forward. Lua can't know where we *meant* to close
it: it only knows that, at the end of the file, something is still open. Ask students to
match every `for` with its `end` with their finger on the screen. The indentation shows where
the `end` should be.

Solution: [`solutions/broken_floor.lua`](solutions/broken_floor.lua).

### Activity 3 — `broken_square`: no errors, wrong result (15 minutes)

Run `broken_square`. No error: Byte builds, says "Square done!"… but MINE answers:
*"Not yet: your Byte built 12 blocks, 4 long, 3 wide and 1 tall."*

This is a **logic error**, the hardest kind: the program does exactly what it says, but what it
says isn't what we wanted. No message tells you the line. Clues:

- MINE's measurement: 4 rows of **3**, instead of 4 rows of 4.
- Which numbers in the program are 3? Which ones should be 4?

A common trap, and a great discussion: a student who changes only the **first** `3` gets
*"16 blocks, 7 long, 4 wide"*: the right number of blocks, but each row starts one block
further forward, like stairs. Byte goes forward 4 but comes back only 3. Both loops of a
row must repeat the same number of times.

Solution: [`solutions/broken_square.lua`](solutions/broken_square.lua).

### Extra challenge — For early finishers

Write a short program **with one bug in it**, on paper or on your own Byte, and give it to a
classmate to fix. Which kind of bug is hardest to find?

### Wrap-up — 5 minutes

- Go back to the detective's method on the board: which step helped most?
- The three kinds of bugs: what does each one look like? (Lua won't start it / it stops
  halfway / it runs but does the wrong thing.)
- The reflection question MINE shows at the end: *Which bug was hardest to find: the ones with
  an error message, or the one without? Why?*

## Common problems

| What happens | Why | What to do |
|---|---|---|
| The new programs aren't there | Byte was off, or the lesson wasn't started | right-click Byte to turn it on and type `ls`; check with `/mine lesson status` |
| The programs have Italian names | the student's game is set to Italian | that's intended: `fila_rotta`, `pavimento_rotto`, `quadrato_rotto` |
| A student's fixes have disappeared | the lesson was restarted, which copies the programs again | fix them again (it's quicker the second time!) |
| The fixed row is floating one block up | the broken run had already gone up before stopping | it still counts; or `/mine byte recall` before running it again |
| `edit broken_row` opens an empty file | a typo in the name | `ls` shows the exact names |
| After a fix, a new, different error appears | that's progress: the first bug is gone, Lua now reaches the next problem | read the new message: which line now? |
| MINE says "16 blocks, 7 long, 4 wide" | only one of the two loops in a row was changed | both loops of a row must repeat 4 times |
| Byte bumps into the previous build | not enough space | `/mine byte recall` from a free spot |

## For the teacher: what to look for

- **Do they read the whole message**, including the line number, before changing anything?
- **Do they change one thing at a time**, or rewrite everything at random?
- **Do they use MINE's measurements** as clues for the logic error?

## References

- [Lesson 2 — Loops](../02-loops/README.md)
- The broken programs are in MINE's data files:
  [`mine_lesson_files/debugging/`](../../../src/main/resources/data/mine/mine_lesson_files/debugging/).
  The fixed versions are in [`solutions/`](solutions/) (Italian in [`solutions/it/`](solutions/it/))
  and are tested automatically every time MINE changes, as is the fact that the broken ones
  really are broken.
