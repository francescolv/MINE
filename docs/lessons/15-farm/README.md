**English** · [Italiano](README.it.md)

# Lesson 15 — The farm: rows and fields

| | |
|---|---|
| **Length** | 1 hour (50–60 minutes) |
| **Age** | 10–14 |
| **Concepts** | nested loops (a loop inside a loop), conditions, counter; the area of a rectangle |
| **Prerequisites** | [lesson 2](../02-loops/README.md) (loops) and [lesson 4](../04-conditions/README.md) (conditions) |

## Objectives

By the end of the lesson students can:

1. go over a **field** (rows and columns) with a **loop inside a loop**;
2. do something **only where needed**, with a condition (`byte.ripeDown()`);
3. **count** what the program did with a variable;
4. link the number of turns of the loops to the **area**: 3 rows × 5 = 15 blocks.

## How it works

Every time a program starts, **MINE digs a field** in front of Byte: **3 rows of 5** farmland
blocks, with a water channel on the left to keep it wet. Byte **flies one block above the crops**,
as farming turtles do: first `byte.up()`, then it sows with `byte.placeDown()` and harvests with
`byte.digDown()`. When the program ends MINE puts the ground back as it was.

At each step MINE puts **64 wheat seeds** in Byte's highlighted slot. From step 2 the wheat has
already grown: some **ripe** (golden), some **young** (green), in different places every time.
`byte.ripeDown()` (with blocks *ripe crop below?*) says whether the wheat under Byte is ripe.

## Preparation (teacher, before the lesson)

- The **standard lab**, or a superflat world with **6 free blocks ahead**, **3 to the right** and
  **1 to the left** of each Byte, on solid ground.
- Try the three steps yourself with [`solutions/`](solutions/).

## Missions: MINE checks the steps

```
/mine lesson start farm @a
```

| Step | The field… | Done when… |
|---|---|---|
| 1 · `sow` | is empty | every farmland block has seeds |
| 2 · `harvest` | has ripe and young wheat | all the ripe wheat is harvested **and sown again**, the young wheat still where it was |
| 3 · `count` | as in step 2 | as above, and Byte says how much it harvested |

After each step, `/mine byte recall` from the start of the lane brings Byte back to its place.

## With blocks

| Step | File |
|---|---|
| 1 · Sow | [sow.json](solutions/blocks/sow.json) |
| 2 · Harvest | [harvest.json](solutions/blocks/harvest.json) |
| 3 · Count | [count.json](solutions/blocks/count.json) |

```
move up
repeat 3 times
    repeat 5 times
        move forward
        if ripe crop below?
            dig below
            place a block below
    repeat 5 times
        move back
    turn right
    move forward
    turn left
```

## Lesson plan

### Introduction — 5 minutes, no computers

A squared field on the board, 3 rows of 5. How does a tractor sow it? One row, back again, on to
the next row. How many times does it sow? (3 × 5 = 15: the area!)

### Activity 1 — Sow (15 minutes)

```lua
byte.up()
for row = 1, 3 do
  for i = 1, 5 do
    byte.forward()
    byte.placeDown()
  end
  for i = 1, 5 do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end
```

The inner loop does **one row**; the outer one repeats it for **every row**. How many times does
`byte.placeDown()` run?

Solution: [`solutions/sow.lua`](solutions/sow.lua).

### Activity 2 — Harvest only what's ripe (15 minutes)

The same route; on every block a question: is it ripe?

```lua
    byte.forward()
    if byte.ripeDown() then
      byte.digDown()
      byte.placeDown()
    end
```

The young wheat stays; where Byte harvests it sows again at once (with the seeds it harvested, which
go back into the slot).

Solution: [`solutions/harvest.lua`](solutions/harvest.lua).

### Activity 3 — Count the harvest (10 minutes)

A variable `harvested` starts at 0 and goes up by 1 inside the `if`. At the end Byte says it. MINE
counts too: no made-up numbers!

Solution: [`solutions/count.lua`](solutions/count.lua).

### Extra challenge

- The **snake** field: without going back at every row (one row forward, the next backwards). It
  takes knowing whether the row is even or odd: `row % 2 == 0`.
- How much **young** wheat was there? (15 minus what was harvested.)

### Wrap-up — 5 minutes

MINE's reflection question: *Byte sowed and harvested a whole field with a few lines, a loop inside
a loop. Where do farmers really use machines that follow a program?*

## Common problems

| What happens | Why | What to do |
|---|---|---|
| Byte can't move over the field | it didn't go up: the wheat is in its way | `byte.up()` at the start |
| *"N of the 15 farmland blocks have seeds"* | going back to the start of the row, or on to the next, is wrong | try a single row by hand, then add the outer loop |
| Byte cuts the young wheat too | the `if` is missing, or the condition is the wrong way round | `if byte.ripeDown() then` |
| It doesn't sow again | the highlighted slot is empty or holds something else | the seeds in the highlighted slot; `/mine lesson restart` gives them again |
| MINE says it needs room | Byte is still on the last field | `/mine byte recall` from the start of the lane |

## References

- [Lesson 2 — Loops](../02-loops/README.md), [lesson 4 — Conditions](../04-conditions/README.md); [Lua API](../../guide/lua-api.md) (`byte.ripeDown`).
- Solutions in [`solutions/`](solutions/) (in Italian in [`solutions/it/`](solutions/it/)), tested
  automatically on fields with ripe wheat in random places.
