**English** · [Italiano](README.it.md)

# Lesson 11 — Treasure hunt: coordinates

| | |
|---|---|
| **Length** | 1 hour (50–60 minutes) |
| **Age** | 10–14 |
| **Concepts** | coordinates (forward, right), variables, `for` with a variable, a function with parameters |
| **Prerequisites** | [lesson 2](../02-loops/README.md) (loops); for step 3, [lesson 5](../05-functions/README.md) (functions) |

## Objectives

By the end of the lesson students can:

1. say where a place is with **two numbers**, counted from a starting point;
2. keep numbers in **variables** and use them to decide how many steps to take;
3. use variables as a **memory**, to find the way home;
4. write a **function with parameters** that takes Byte anywhere.

## How it works

Every time a program starts, **MINE buries a treasure**, a gold block in the ground, in a new
place near Byte: 1 to 6 blocks **forward** and 0 to 4 **to the right**. The program can't see it:
it finds it with the numbers of `mine.challenge()`.

```lua
local forward, right = mine.challenge()   -- for example 4 and 2
```

With blocks, *the challenge's number 1* is how far forward, *the challenge's number 2* how far to
the right (in Lua `mine.challenge(1)` and `mine.challenge(2)`). When the program ends, MINE puts
the ground back as it was.

When Byte misses, MINE says so **in coordinates**: *"the treasure was 4 forward and 2 to the
right; Byte stopped 4 forward and 0 to the right"*. The best moment to talk about it at the board.

## Preparation (teacher, before the lesson)

- The **standard lab** (`/mine lab build <students>`, then `/mine lab assign`), or a superflat
  world with **7 free blocks ahead** and **5 to the right** of each Byte, on solid ground.
- Nothing else: Byte already has its pickaxe to dig.
- Try the three steps yourself with [`solutions/`](solutions/).

## Missions: MINE checks the steps

```
/mine lesson start treasure @a
```

| Step | `mine.challenge()` gives… | Done when… |
|---|---|---|
| 1 · `map` | forward and right of the treasure | Byte is right on top of the treasure |
| 2 · `dig` | as above | the treasure is dug up (`byte.digDown()`) and Byte is back where it started |
| 3 · `three` | 6 numbers: forward and right of three treasures | all three dug up, and Byte home |

After each step, `/mine byte recall` from the start of the lane brings Byte back to its place.

## With blocks

| Step | File |
|---|---|
| 1 · The map | [map.json](solutions/blocks/map.json) |
| 2 · Dig and go home | [dig.json](solutions/blocks/dig.json) |
| 3 · Three treasures | [three.json](solutions/blocks/three.json) |

They use *the challenge's number …* (Byte), *set … to* and the variable (Variables), *repeat* with
the variable instead of the number; step 3 a function with two inputs (Functions, the gear to add
them).

## Lesson plan

### Introduction — 10 minutes, no computers

Battleships on the board, or in the playground: "the treasure is **4 steps forward and 2 to the
right**". One student is Byte and walks. Then: *and to come back?* (2 steps back, turn, 4 steps
back: the same numbers, the other way round).

### Activity 1 — The map (15 minutes)

```lua
local forward, right = mine.challenge()
mine.say("The treasure is " .. forward .. " forward and " .. right .. " to the right")
for i = 1, forward do
  byte.forward()
end
byte.turnRight()
for i = 1, right do
  byte.forward()
end
```

Things to explain:

- a **variable** is a box with a name: `forward` holds a number, different every time;
- `for i = 1, forward do` repeats as many times as `forward` says;
- and if `right` is 0? The loop doesn't run at all: that's fine.

Solution: [`solutions/map.lua`](solutions/map.lua).

### Activity 2 — Dig and go home (10 minutes)

Variables **remember**: after digging with `byte.digDown()`, the way home is made of the same
numbers, backwards (`byte.back()`), in the opposite order.

Solution: [`solutions/dig.lua`](solutions/dig.lua).

### Activity 3 — Three treasures (15 minutes)

Now there are three treasures, and between one and the next Byte can't start from home again: it
has to know **where it is**. Two variables, `f` and `r`, keep its position; a function takes it
anywhere:

```lua
local f, r = 0, 0

local function goTo(toF, toR)
  while f < toF do byte.forward() f = f + 1 end
  while f > toF do byte.back() f = f - 1 end
  byte.turnRight()
  while r < toR do byte.forward() r = r + 1 end
  while r > toR do byte.back() r = r - 1 end
  byte.turnLeft()
end

for i = 1, 3 do
  goTo(mine.challenge(2 * i - 1), mine.challenge(2 * i))
  byte.digDown()
end
goTo(0, 0)
```

Questions: why does `goTo(0, 0)` bring Byte home? What happens if the second treasure is further
back than the first? (`f > toF`: Byte goes back.)

Solution: [`solutions/three.lua`](solutions/three.lua).

### Wrap-up — 5 minutes

- Where are coordinates like these used? (Maps, GPS, battleships, spreadsheets: B3!)
- MINE's reflection question: *Byte found treasure it couldn't see, with just some numbers. Where
  are coordinates like these used in real life? Why did Byte need variables to get home?*

## Common problems

| What happens | Why | What to do |
|---|---|---|
| `mine.challenge()` gives `nil` | the program started outside the lesson, or the student isn't at a step of the hunt | `/mine lesson status`; run the program from Byte's terminal or with `/mine byte run` |
| Byte stops beside the treasure | it turned before going forward, or left instead of right | forward first, then `byte.turnRight()` |
| Byte doesn't get home | the steps back are in the wrong order | first the steps to the right, then turn, then the forward ones |
| MINE says it needs free room | Byte is still where the last program left it | `/mine byte recall` from the start of the lane |

## References

- [Lesson 5 — Variables and functions](../05-functions/README.md); [Lua API](../../guide/lua-api.md) (`mine.challenge`).
- Solutions in [`solutions/`](solutions/) (in Italian in [`solutions/it/`](solutions/it/)), tested
  automatically on treasure buried at random.
