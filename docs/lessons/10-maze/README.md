**English** · [Italiano](README.it.md)

# Lesson 10 — The maze: a hand on the wall

| | |
|---|---|
| **Length** | 1 hour (50–60 minutes) |
| **Age** | 10–14 |
| **Concepts** | algorithm, `while` and `if` together, conditions with `and` and `not`, counter |
| **Prerequisites** | [lesson 4](../04-conditions/README.md) (`while`, `if`, `byte.detect`); for step 3, [lesson 5](../05-functions/README.md) (variables) |

## Objectives

By the end of the lesson students can:

1. describe an **algorithm** in words before writing it: "keep your right hand on the wall";
2. combine `while` and `if` in a program that decides at every step;
3. join two conditions with `and` and `not` ("there's a wall **and** it isn't the gold");
4. count what the program does with a variable.

## How it works

As in lesson 4, **MINE builds the challenge by itself** in front of Byte, different every time:
a maze of stone bricks two blocks high, with a **gold block** in the wall where the way out is.
Byte starts in the entrance. The maze spreads to Byte's right and ahead, never behind it, where
the student stands: 7 blocks wide and up to 13 deep, it fits in a lane of the
[standard lab](../../guide/server.md#3-the-standard-lab).

Every time a program ends, passed or not, **MINE takes the maze away**, and builds a new one for
the next program. The route that worked once won't work next time: it takes an algorithm that
works for **every** maze.

Only walking along the corridors counts: MINE checks that Byte never went up (no climbing over
the walls) and that no wall was broken (Byte has a pickaxe!).

## Preparation (teacher, before the lesson)

- The quickest way: the **standard lab** (`/mine lab build <students>`, then `/mine lab assign`).
  Otherwise a superflat world, students **8 blocks** apart, with **14 free blocks ahead** and
  **6 to the right** of each Byte.
- Nothing else. At each step MINE puts **a gold block** in Byte's highlighted slot: it's what
  `byte.compare()` uses to recognise the gold.
- Try the three steps yourself with the programs in [`solutions/`](solutions/): 10 minutes.

## Missions: MINE checks the steps

```
/mine lesson start maze @a
```

| Step | Every time a program starts, MINE… | Done when… |
|---|---|---|
| 1 · `corridor` | builds a winding corridor with no branches, the gold at its end | Byte is right in front of the gold, without having gone up or broken walls |
| 2 · `maze` | builds a real maze, with dead ends, the way out on the far side or on a side | as above |
| 3 · `steps` | as in step 2 | as above, and Byte **says** how many steps it took: MINE counts them and compares |

When something is wrong, MINE says what: *"Byte went over the walls"*, *"broke a wall"*,
*"stopped 4 blocks from the gold"*, *"took 23 steps, but said 20"*.

**After each step** Byte is at the way out and the maze disappears: the student types
`/mine byte recall` from the start of the lane to bring it back, ready for the next step. If
there's no room in front of Byte, MINE says so.

## With blocks

The whole lesson works in the [block editor](../../guide/blocks.md). The blocks it needs are
*block in front?* and *block in front same as the slot's?* (in Lua `byte.detect()` and
`byte.compare()`), *repeat until*, *repeat while*, *if / else*, *and*, *not* (in Logic).

| Step | File |
|---|---|
| 1 · The corridor | [corridor.json](solutions/blocks/corridor.json) |
| 2 · The maze | [maze.json](solutions/blocks/maze.json) |
| 3 · Count the steps | [steps.json](solutions/blocks/steps.json) |

```
repeat until block in front same as the slot's?
    turn right
    repeat while block in front? and not block in front same as the slot's?
        turn left
    if not block in front same as the slot's?
        move forward
say "Out of the maze!"
```

## Lesson plan

### Introduction — 10 minutes, no computers

Draw a maze on the board. Ask: *how would you get out of a maze in the dark, without seeing
anything?* Let them suggest. Then tell them the old trick: **always keep your right hand on the
wall and walk**. Try it together with a finger on the drawing: you get out, dead ends and all.

Write the algorithm together, in three lines:

1. turn right;
2. while there's a wall in front, turn left;
3. take a step. And again, until you see the way out.

### Activity 1 — The corridor (15 minutes)

**Task:** bring Byte to the end of a winding corridor, in front of the gold.

The first, most natural idea: "go forward while you can; at a corner look right; if it's closed,
turn the other way".

```lua
while not byte.compare() do
  if not byte.detect() then
    byte.forward()
  else
    byte.turnRight()
    if byte.detect() and not byte.compare() then
      byte.turnLeft()
      byte.turnLeft()
    end
  end
end
mine.say("Gold!")
```

Things to explain:

- `byte.compare()` is true when the block in front is the same as the one in the highlighted
  slot: the gold. The loop stops when Byte faces it.
- `and` joins two conditions: **both** true. Why is `not byte.compare()` needed? (If the gold is
  on the right, it's a "wall" too: without it, Byte would turn the other way.)

Run the program two or three times: a different corridor each time, and it always works.

Solution: [`solutions/corridor.lua`](solutions/corridor.lua).

### Activity 2 — A real maze (15 minutes)

**Task:** get out of a maze with dead ends.

Before writing, let them try the corridor program: in a dead end Byte goes back and forth, or
stops in the wrong place. It takes the hand-on-the-wall algorithm:

```lua
while not byte.compare() do
  byte.turnRight()
  while byte.detect() and not byte.compare() do
    byte.turnLeft()
  end
  if not byte.compare() then
    byte.forward()
  end
end
mine.say("Out of the maze!")
```

Questions for discussion:

- Why does Byte turn right **first**, even when it could go straight? (To "keep its hand" on the
  right wall: if there's a way to the right, it takes it.)
- What happens in a dead end? (It turns left three times and goes back: the dead end is explored
  and left.)
- Would it work with the left hand? (Yes: swap right and left. Try it!)

Solution: [`solutions/maze.lua`](solutions/maze.lua).

### Activity 3 — Count the steps (10 minutes)

**Task:** the same algorithm, and at the way out Byte says how many steps it took. MINE counts
them too and compares: no made-up numbers!

```lua
local steps = 0
-- ... the same loop, and after byte.forward():
    steps = steps + 1
-- ... at the end:
mine.say("Out of the maze in " .. steps .. " steps!")
```

Full solution: [`solutions/steps.lua`](solutions/steps.lua).

Question: do the right hand and the left hand take the same number of steps? Try it on the same
maze drawn on the board.

### Extra challenge — For early finishers

- Write the **left hand** version.
- Count the **turns** too, with a second variable.
- On the board: draw a maze where the hand on the wall does **not** find the way out. (It takes an
  "island": a way out in the middle, on walls that don't touch the edge. MINE's mazes have none.)

### Wrap-up — 5 minutes

- What's an **algorithm**? (A precise recipe that always works, not just once.)
- The reflection question MINE shows at the end: *Your program got Byte out of mazes it had never
  seen. Why does keeping a hand on the wall always work? When wouldn't it?*

## Common problems

| What happens | Why | What to do |
|---|---|---|
| MINE says it needs free blocks in front of Byte | Byte is still at the way out of the last maze, or facing the wrong way | `/mine byte recall` from the start of the lane |
| Byte stops at once, without moving | the highlighted slot is empty: `byte.compare()` with an empty slot facing air is true | the gold block in the **highlighted** slot; `/mine lesson restart` gives it again |
| Byte spins forever | `not byte.compare()` is missing in the inner loop, or the step forward is | hold Ctrl+T to stop it, then compare with the algorithm on the board |
| *"Byte went over the walls"* | the program uses `byte.up()` | in the maze, only forward and turns |
| *"Byte broke a wall"* | the program uses `byte.dig()` | as above |
| The count of steps is wrong | `steps = steps + 1` is outside the `if`, or turns are counted too | count only after `byte.forward()` |

## For the teacher: what to watch for

- Can they **say the algorithm in words** before writing it?
- Do they run the program **several times**, on different mazes, before saying it works?
- Do they understand **why** `and not byte.compare()` is needed?

## References

- [Lesson 4 — Conditions](../04-conditions/README.md), [lesson 5 — Variables and functions](../05-functions/README.md)
- [MINE's Lua API](../../guide/lua-api.md); in the game, `help byte` in Byte's terminal.
- The solutions are in [`solutions/`](solutions/) (in Italian in [`solutions/it/`](solutions/it/))
  and are tested automatically on every change to MINE, on mazes built at random.
