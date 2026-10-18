**English** · [Italiano](README.it.md)

# Lesson 2 — Loops: rows, floors and cubes

| | |
|---|---|
| **Duration** | 1 hour (50–60 minutes) |
| **Age** | 10–14 years old (also fine for the first year of secondary school) |
| **Concepts** | loop (`for`), loop counter, nested loops; area and volume |
| **Prerequisites** | [lesson 1](../01-first-steps/README.md): writing, saving and running a program |

## Goals

By the end of the lesson students can:

1. explain why a **loop** is better than writing the same line many times;
2. use `for i = 1, n do ... end` to repeat instructions a set number of times;
3. put one loop **inside** another to cover a surface, and a third one to fill a volume;
4. link the program to maths: a floor of 5 × 4 blocks has an **area** of 20, a cube of
   3 × 3 × 3 has a **volume** of 27 — and the loops multiply exactly like that.

## Preparation (teacher, before the lesson)

- The same setup as lesson 1: a **superflat world in Creative mode**, with MINE and CC: Tweaked.
- Builds take room. Line students up side by side, **all facing the same way**, about
  **8 blocks apart**, with at least **12 free blocks in front** of each Byte. Floors and cubes
  grow towards Byte's right.
- The quickest way: the **standard lab** (`/mine lab build <students>`, then `/mine lab assign`)
  has exactly these lanes ([guide](../../guide/server.md#3-the-standard-lab)); `/mine lab reset`
  clears it after the lesson.
- Nothing else to prepare: when the lesson starts MINE creates a Byte for any student who
  doesn't have one, and at every step it puts **64 planks** in Byte's highlighted slot
  (whatever was there is moved to another slot).
- Try the three programs yourself, from [`solutions/`](solutions/): 10 minutes.
- Optional: place the lesson's guide and board where students can see them,
  `/mine npc place mine:loops_guide` and `/mine board place mine:loops`. Ada offers to start the
  lesson, gives a tip at each step, and asks a question at the end: `/mine npc results` next to
  her shows the answers ([guide](../../guide/npc.md#npcs-and-boards-in-datapacks)).

## Missions: MINE checks the steps

At the start of activity 2 (after the warm-up), type:

```
/mine lesson start loops @a
```

| Step | Done when… |
|---|---|
| 1 · `row` | the program built a straight row of 10 blocks, one block high |
| 2 · `floor` | the program built a solid floor 5 blocks long and 4 wide |
| 3 · `cube` | the program built a solid cube 3 × 3 × 3 |

MINE looks at the **new blocks** the program placed near where Byte started: blocks that
were already there don't count, and the shape may face any direction. It doesn't read the code,
so in theory a student could build the row with ten lines instead of a loop — the goal asks for
a loop, and that's something to check by walking around the room.

When a build has the wrong size, MINE measures it, for example: *"Not yet: your Byte built 15
blocks, 5 long, 3 wide and 1 tall. Compare it with the goal: what's different?"* That's a small
area problem in itself: let students work out what's missing before helping.

The commands are the same as in lesson 1: `/mine lesson status @a`, `/mine lesson hint`,
`/mine lesson restart`, `/mine lesson reset <students>` (see the
[lesson 1 guide](../01-first-steps/README.md#missions-mine-checks-the-steps)).

## With blocks

The whole lesson works in MINE's [block editor](../../guide/blocks.md) too (`/mine blocks`, then
**Run ▶**): the missions check what was built, not how. It's a good lesson to compare blocks and
Lua: the `repeat 10 times` block becomes `for count = 1, 10 do`, the editor shows it next to the
blocks.

| Activity | With blocks | File |
|---|---|---|
| 1 · The counter speaks | `count with i from 1 to 5 by 1` with `say` and the variable `i` inside | |
| 2 · Row | see below | [row.json](solutions/blocks/row.json) |
| 3 · Floor | see below | [floor.json](solutions/blocks/floor.json) |
| 4 · Cube | the floor of activity 3 inside `repeat 3 times`, with `move up` before it | [cube.json](solutions/blocks/cube.json) |

```
move up                             move up
repeat 10 times                     repeat 4 times
    place a block below                 repeat 5 times
    move forward                            place a block below
say "Row done!"                             move forward
                                        repeat 5 times
                                            move back
                                        turn right
                                        move forward
                                        turn left
                                    say "Floor done: 20 blocks!"
```

A loop inside a loop is easy to see with blocks: one block holds the other. Ask students which
block repeats the rows and which one the blocks of a row. The Italian solutions are in
[`solutions/it/blocks`](solutions/it/blocks).

## Lesson plan

### Introduction — 5 minutes, no computers

Go back to the extra challenge of lesson 1: a tower 10 blocks tall. Write the program on the
board, line after line, and stop halfway: *"Do we really have to write all of it?"*

Ask how they would say it to a friend: "**ten times**: place a block and go up". That sentence
is a loop. In Lua:

```lua
for i = 1, 10 do
  byte.place()
  byte.up()
end
```

Point out that:

- the lines between `do` and `end` are **repeated**: they are the *body* of the loop;
- `i` is a **counter**: it is 1 the first time, 2 the second time… up to 10;
- changing 10 into 100 costs nothing. Writing 100 lines by hand would.

### Activity 1 — The counter speaks (5 minutes)

To see the loop at work before building anything: `edit count`, then

```lua
for i = 1, 5 do
  mine.say(i)
end
```

Run it with `count`. Then ask them to **predict** before trying: what does it say with
`for i = 1, 3`? And with `for i = 2, 10, 2`? (Count in twos: 2, 4, 6, 8, 10.)

### Activity 2 — A row of 10 blocks (10 minutes)

**Task:** with a loop, Byte builds a straight row of 10 blocks.

The new trick: `byte.placeDown()` places a block **under** Byte. If Byte goes up one
block first, it can "paint" the ground as it moves forward:

```lua
byte.up()
for i = 1, 10 do
  byte.placeDown()
  byte.forward()
end
```

The blocks are already there: MINE put them in Byte's highlighted slot.

Discussion questions:

- What happens if you swap `placeDown()` and `forward()` inside the loop? (The first block
  ends up one step further on: the row still has 10 blocks, it just starts later.)
- And if you forget `byte.up()`? (`placeDown()` can't place: there's already ground under
  Byte. It returns `false` without an error, and Byte just walks forward.)

Solution: [`solutions/row.lua`](solutions/row.lua).

### Activity 3 — The floor, and its area (15 minutes)

**Task:** a solid floor, 5 blocks long and 4 blocks wide.

**First, without computers:** how many blocks does it take? Draw it on squared paper. The answer
is the **area**: 5 × 4 = 20. That's also how many blocks Byte will use.

A floor is 4 rows of 5 blocks: **a loop inside a loop**. The outer loop counts the rows; the
inner loop builds one row. Between two rows Byte must go back to the start of the row and
move one step sideways:

```lua
byte.up()
for row = 1, 4 do
  for i = 1, 5 do
    byte.placeDown()
    byte.forward()
  end
  for i = 1, 5 do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end
```

Things to explain:

- **indentation** (the spaces at the start of the lines) doesn't change what Lua does, but it
  shows which lines belong to which loop. Ask students to indent like this.
- the counters have different names (`row`, `i`) so we can tell them apart.
- how many times does `byte.placeDown()` run? 4 times the inner loop, 5 times each: 4 × 5 = 20.
  **The loops multiply, just like the area does.**

Solution: [`solutions/floor.lua`](solutions/floor.lua).

### Activity 4 — The cube, and its volume (15 minutes)

**Task:** a solid cube, 3 blocks long, 3 wide and 3 tall.

Again, predict first: how many blocks? 3 × 3 × 3 = 27: the **volume**. A cube is three 3 × 3
floors stacked on top of each other, so the floor goes inside **a third loop**, one turn per
layer. At the start of each layer Byte goes up one block; at the end it goes back to the
first row of the layer.

Hint, only if needed: "build one 3 × 3 floor first. When it works, wrap it in a loop that
repeats it 3 times."

Solution: [`solutions/cube.lua`](solutions/cube.lua).

### Extra challenge — For early finishers

- Build a cube 4 × 4 × 4. Which numbers do you change? How many blocks will it take? (64: a full
  stack!)
- A staircase: each step is one block higher and one block further forward.
- A **hollow** cube, like a room. (It needs ideas from the next lessons: let them try and talk
  about why it's harder.)

### Wrap-up — 5 minutes

- What is a loop? (a way to repeat instructions without writing them again)
- Why does the floor take 20 blocks, and the cube 27? (area and volume: the loops multiply)
- The reflection question MINE shows at the end: *How many lines would the cube take without
  loops? What would you change in your program to build a cube 10 blocks wide?*

## Common problems

| What happens | Why | What to do |
|---|---|---|
| `'do' expected` or `'end' expected` | a `do` or an `end` is missing: every `for` needs both | count the `for`s and the `end`s: there must be as many |
| `'=' expected near ...` | a comma or a space in the wrong place: `for i 1, 10` | the exact form is `for i = 1, 10 do` |
| Byte walks but places no blocks | it didn't go up first: there's ground under it | `byte.up()` before the loop |
| The row stops early or has gaps | Byte ran out of blocks, or something blocked it | MINE says so if the slot is empty; otherwise look for an obstacle in front |
| The floor is all in one line, or diagonal | the sideways step between rows is missing or in the wrong place | the turn-forward-turn goes **after** the inner loop, but **inside** the outer one |
| The floor grows to the left instead of the right | `turnLeft` and `turnRight` are swapped | it's fine: MINE accepts the floor in any direction |
| MINE says "5 long, 3 wide" or similar | the outer loop runs the wrong number of times | compare the numbers in the message with the goal |
| The cube is 3 floors side by side | Byte doesn't go up between layers | `byte.up()` at the start of each layer |
| The cube's layers are shifted | after a layer Byte doesn't go back to the first row | turn left, forward 3, turn right at the end of each layer |
| Byte bumps into the previous build | not enough space | `/mine byte recall` from a free spot |

## For the teacher: what to look for

- **Do they predict the number of blocks** before running the program? That's where the maths is.
- **Do they read MINE's measurements** ("5 long, 3 wide") and fix the right number?
- **Do they build the cube step by step**, starting from a floor that works, instead of writing
  everything at once?

## References

- [Lesson 1 — First steps with Byte](../01-first-steps/README.md)
- [MINE Lua API](../../guide/lua-api.md); in game, `help byte` in Byte's terminal.
- The solutions are in [`solutions/`](solutions/) (Italian versions in [`solutions/it/`](solutions/it/))
  and are tested automatically every time MINE changes.
