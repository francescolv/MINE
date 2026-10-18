**English** · [Italiano](README.it.md)

# Lesson 4 — Conditions: Byte decides

| | |
|---|---|
| **Duration** | 1 hour (50–60 minutes) |
| **Age** | 10–14 years old (also fine for the first year of secondary school) |
| **Concepts** | true and false, `if`, `while`, sensors (`byte.detect`, `byte.compareDown`), instructions that answer `false` |
| **Prerequisites** | [lesson 2](../02-loops/README.md) (`for` loops); [lesson 3](../03-debugging/README.md) helps |

## Goals

By the end of the lesson students can:

1. explain that some instructions **answer** `true` or `false`, and use that answer;
2. use `if ... then ... end` to do something **only when** a condition is true;
3. use `while ... do ... end` to repeat **as long as** a condition is true, when you can't know
   in advance how many times;
4. write a program that works in situations it has never seen.

## Why MINE changes the challenge every time

Turtle instructions are forgiving: `byte.forward()` against a wall, or `byte.placeDown()`
on the ground, just answer `false` and the program goes on. So a program with no conditions at
all could often "pass" by luck, for example by walking 20 steps and stopping against the wall.

That's why, in this lesson, **MINE builds the challenge itself**, in front of Byte, and it
is different every time: the wall is at a different distance, the gold is in different places.
The challenge appears as soon as the step begins, and the first program faces that one. If a
program fails, MINE takes the challenge away and builds a new, different one when the next
program starts (also if Byte has been moved); when it succeeds, the challenge stays. The
number "5" that worked last time won't work next time: Byte has to **look** and
**decide**.

## Preparation (teacher, before the lesson)

- A **superflat world in Creative mode**, students side by side, all facing the same way, about
  **8 blocks apart**, with at least **10 free blocks in front** of each Byte.
- The quickest way: the **standard lab** (`/mine lab build <students>`, then `/mine lab assign`)
  has exactly these lanes ([guide](../../guide/server.md#3-the-standard-lab)); `/mine lab reset`
  clears it after the lesson.
- Nothing else to prepare. When the lesson starts MINE creates a Byte for any student who
  doesn't have one; every Byte gets a **diamond pickaxe** to dig the gold (you can see it on its
  side), also Bytes created with older versions of MINE, without losing their programs. At
  each step MINE puts what's needed in Byte's highlighted slot: **a gold block** for step 2,
  **between 3 and 12 planks** for step 3, a different number for each student.
- Try the three steps yourself, with the programs in [`solutions/`](solutions/): 10 minutes.

## Missions: MINE checks the steps

At the start of the lesson, type:

```
/mine lesson start conditions @a
```

| Step | Each time a program starts, MINE… | Done when… |
|---|---|---|
| 1 · `wall` | builds a brick wall across Byte's way, 2 to 6 blocks ahead | Byte climbs over it and lands right behind it, back on the ground, without breaking it |
| 2 · `gold` | lays a row of 8 blocks in front of Byte: stone, with 2 to 5 gold blocks in random places | all the gold has been dug up and all the stone is still there |
| 3 · `tower` | — | there is a tower in front of Byte made of **all** the blocks in its highlighted slot (at least 3), and Byte is back on the ground |

When something isn't right, MINE says what happened, for example *"this time the wall was 4
blocks away"*, *"2 gold blocks are still in the row, and 2 stone blocks were dug up"* or *"
Byte had 7 blocks, and the tower is 5 tall"*. If there isn't enough free space in front of
Byte, MINE doesn't build anything and says so: `/mine byte recall` from an open spot.

The other commands are the same as in lesson 1 (see the
[lesson 1 guide](../01-first-steps/README.md#missions-mine-checks-the-steps)).

## With blocks

The whole lesson works in the [block editor](../../guide/blocks.md) too (`/mine blocks`, then
**Run ▶**). Two **Byte** blocks are made for it: *block [below] same as the slot's?* (Lua's
`byte.compareDown()`) and *blocks in the slot* (`byte.getItemCount()`).

| Activity | With blocks | File |
|---|---|---|
| 1 · Yes/no questions | `say` with `block in front?` inside: it says `true` or `false` | |
| 2 · Over the wall | see below | [wall.json](solutions/blocks/wall.json) |
| 3 · The gold row | see below | [gold.json](solutions/blocks/gold.json) |
| 4 · A tower with all the blocks | see below | [tower.json](solutions/blocks/tower.json) |

```
repeat while not block in front?        move up
    move forward                        repeat 8 times
move up                                     move forward
move forward                                if block below same as the slot's?
move forward                                    dig below
move down                               say "All the gold is mine!"
say "Over the wall!"

repeat while blocks in the slot > 0
    place a block in front
    move up
repeat until block below?
    move down
say "Tower done!"
```

The tower is written differently from Lua: in Lua `byte.place()` both places a block and
answers whether it could, a trick the blocks don't have. Instead, Byte looks at how many
blocks are left in its slot. A nice question: which version is easier to read? The Italian
solutions are in [`solutions/it/blocks`](solutions/it/blocks).

## Lesson plan

### Introduction — 5 minutes, no computers

Play "robot in the corridor": a student is the robot, eyes closed, and can only take one step
at a time. The class gives them **one** program that must work from any starting point:
"**as long as** there's nothing in front of you, take a step". They stop at the wall, wherever it is.

Then write on the board the two questions a program can ask:

- **if** something is true, then do this (once);
- **while** something is true, keep doing this (again and again).

### Activity 1 — Questions with a yes/no answer (5 minutes)

In the `lua` prompt, students try:

```lua
byte.detect()
```

It answers `false`. Then they put a block in front of Byte and try again: `true`.
Byte has **sensors**: `byte.detect()` looks in front, `byte.detectDown()` looks below.

Remind them of lesson 1: `byte.forward()` and `byte.place()` also answer `true` or `false`.
Every one of these answers can be used in a condition.

### Activity 2 — Over the wall (15 minutes)

**Task:** Byte walks to the wall, wherever it is, climbs over it and lands right behind it.

The wall is already there when the step begins. Let students run a program that just walks
(`for i = 1, 5 do byte.forward() end`) twice, and notice that after a failed attempt MINE
builds the wall somewhere else.

```lua
while not byte.detect() do
  byte.forward()
end
byte.up()
byte.forward()
byte.forward()
byte.down()
```

Things to explain:

- `while ... do ... end` repeats **as long as** the condition is true, and checks it **before**
  every turn: when Byte touches the wall, `byte.detect()` becomes `true`, so
  `not byte.detect()` becomes `false` and the loop ends.
- `not` turns `true` into `false` and the other way round.
- The difference from `for`: with `for` you know how many times; with `while` you don't, and
  you let the world decide.

Solution: [`solutions/wall.lua`](solutions/wall.lua).

### Activity 3 — The gold row (15 minutes)

**Task:** MINE lays a row of 8 blocks: stone, with some gold. Dig up only the gold.

`byte.compareDown()` answers `true` when the block below Byte is the same as the one in
its highlighted slot: MINE has put **one gold block** there, so Byte can tell gold from
stone. `byte.digDown()` digs the block below.

```lua
byte.up()
for i = 1, 8 do
  byte.forward()
  if byte.compareDown() then
    byte.digDown()
  end
end
```

Discussion questions:

- What happens without the `if`? (Byte digs everything, stone included: MINE says how
  many stone blocks were dug up.)
- Here we use a `for` and an `if` together: why `for` and not `while`? (The row is always 8
  blocks long: we know how many times.)

Solution: [`solutions/gold.lua`](solutions/gold.lua).

### Activity 4 — A tower with all the blocks (10 minutes)

**Task:** a tower with **all** the blocks in the highlighted slot — MINE gave each student a
different number, between 3 and 12, and the program must work for any of them — then back to
the ground.

In lesson 1 we noticed that `byte.place()` answers `false` when it can't place a block. Now
we use it: when the blocks run out, `byte.place()` is `false` and the loop stops. In the same
way, `byte.down()` is `false` when Byte touches the ground.

```lua
while byte.place() do
  byte.up()
end
while byte.down() do
end
```

The second loop has an empty body: all the work is done by the condition! Have students run
the same program with a different number of blocks: it always works.

Solution: [`solutions/tower.lua`](solutions/tower.lua).

### Extra challenge — For early finishers

- Change the wall program so that it also **says** how many steps it took to reach the wall.
  (You need a variable to count: that's lesson 5!)
- In the gold row, make Byte say "gold!" or "stone!" for every block it passes over:
  `if ... then ... else ... end`.

### Wrap-up — 5 minutes

- What's the difference between `if` and `while`?
- When do we use `for`, and when `while`?
- The reflection question MINE shows at the end: *MINE changed the challenge every time, but
  your programs still worked. How did Byte know what to do?*

## Common problems

| What happens | Why | What to do |
|---|---|---|
| MINE says it needs free blocks in front of Byte | something is in the way: a previous build, another Byte, a hill | `/mine byte recall` from an open spot |
| There's no wall in front of Byte | MINE builds it only during the `wall` step, and only where there is room | check with `/mine lesson status` that the student is on that step; `/mine byte recall` from an open spot, then run the program: the wall appears |
| Byte walks into the wall and stops, without climbing | the program has no `while`: `byte.forward()` against the wall just answers `false` | `while not byte.detect() do ... end` |
| Byte climbs too early or too late | the climb is inside the loop, or the loop condition is wrong | the climb goes **after** the `end` of the `while` |
| `byte.digDown()` does nothing | Byte hasn't got its pickaxe yet: MINE gives it when Byte is waiting for a command | open Byte's terminal so that it shows the `>` prompt |
| Byte digs the stone too | there's no `if`, or the student moved the gold out of the highlighted slot | the gold block goes back in the **highlighted** slot |
| Byte digs nothing | the highlighted slot is empty or has something else in it | the gold block goes in the highlighted slot; `/mine lesson restart` gives everything again |
| The tower is shorter than the number of blocks | some blocks are in other slots: `byte.place()` only uses the highlighted one | all the blocks in the highlighted slot |
| The program never ends | a `while` whose condition never changes, e.g. `while byte.detect() do end` in front of a wall | hold Ctrl+T in the terminal for a second to stop it, then fix the condition |

## For the teacher: what to look for

- **Do they run the program more than once** to check that it works with a different challenge?
- **Can they say in words** what the condition asks ("as long as there's nothing in front")?
- **Do they choose between `for` and `while`** for a reason?

## References

- [Lesson 3 — Debugging](../03-debugging/README.md)
- [MINE Lua API](../../guide/lua-api.md); in game, `help byte` in Byte's terminal.
- The solutions are in [`solutions/`](solutions/) (Italian versions in [`solutions/it/`](solutions/it/))
  and are tested automatically every time MINE changes, against challenges built at random.
