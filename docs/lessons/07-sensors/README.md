**English** · [Italiano](README.it.md)

# Lesson 7 — Sensors: Byte waits and remembers

| | |
|---|---|
| **Duration** | 1 hour (50–60 minutes) |
| **Age** | 10–14 years old |
| **Concepts** | sensors, timers (waiting), the opposite of a signal (`not`), memory (a variable that changes) |
| **Prerequisites** | [lesson 6](../06-redstone/README.md) (input, output, `os.pullEvent("redstone")`) and [lesson 5](../05-functions/README.md) (variables) |

## Goals

By the end of the lesson students can:

1. react to a **sensor**: a pressure plate someone steps on, a daylight sensor;
2. use a **timer**: do something, **wait**, then undo it (`os.sleep`);
3. use the **opposite** of a signal (`not`): a light that is on when it's dark;
4. **remember** something between two events with a variable: a button that switches on and off.

These are the ideas behind automatic doors, street lights and the lights on a staircase.

## MINE's station

At each step MINE builds its part of a small station around Byte, and takes away the part
of the step before:

| Step | Around Byte |
|---|---|
| 1 | a wooden **pressure plate** on the ground on the **left** (`"left"`), an **iron door in front** (`"front"`) |
| 2 | a **daylight sensor on top** of Byte (`"top"`), a **redstone lamp behind** (`"back"`) |
| 3 | a wooden **button** on the ground on the **right** (`"right"`), in front a **sticky piston** with a **yellow walkway** (`"front"`) |

Plate and button are on the ground next to Byte: students walk on the plate and press the
button themselves, to try.

## Preparation (teacher, before the lesson)

- As in lesson 6: a flat world, room around each Byte (one free block on each side, three in
  front, one behind and one on top) and solid ground around it. The **standard lab** is fine
  (`/mine lab build <students>`, `/mine lab assign`).
- The daylight sensor needs to see the sky: don't hold the lesson under a roof.
- Nothing else: MINE creates the missing Bytes and builds the station.

## Missions: MINE checks the steps

```
/mine lesson start sensors @a
```

| Step | Done when… |
|---|---|
| 1 · `plate_door` | **MINE steps on the plate** for a second, twice: each time the door opens, **stays open** after the plate is free, and **closes by itself** about 3 seconds later |
| 2 · `night_lamp` | **MINE makes night fall on the sensor** (it inverts it) and day again: the lamp must be on exactly when the sensor says it's dark |
| 3 · `button_bridge` | **MINE presses the button** three times: each press moves the walkway out if it's in, back in if it's out, and it stays there when the button pops up |

All three are **live checks**, as in lesson 6: the program listens forever, MINE tries it, stops
it and says how it went. A door that just follows the plate, a lamp that is on by day, or a
walkway that follows the button don't pass, and MINE says why.

## With blocks

The whole lesson works in the [block editor](../../guide/blocks.md) too, with the **Redstone**
blocks, **wait N seconds** (Loops), **not** (Logic) and a variable.

| Activity | File |
|---|---|
| 2 · The door with a timer | [door.json](solutions/blocks/door.json) |
| 3 · The night light | [lamp.json](solutions/blocks/lamp.json) |
| 4 · The walkway | [bridge.json](solutions/blocks/bridge.json) |

```
redstone signal in front set to false       set out to false
repeat while true                           redstone signal in front set to out
    wait for a redstone change              repeat while true
    if signal from left?                        wait for a redstone change
        redstone signal in front set to true    if signal from right?
        wait 3 seconds                              set out to not out
        redstone signal in front set to false       redstone signal in front set to out

repeat while true
    redstone signal behind set to not signal from on top?
    wait for a redstone change
```

The Italian solutions are in [`solutions/it/blocks`](solutions/it/blocks).

## Lesson plan

### Introduction — 5 minutes, no computers

Ask where machines **feel** something and react: the automatic door at the supermarket, the
street light that switches on at dusk, the stair light that goes off by itself, the button of a
lift. What do they feel (a person, the dark, a press)? What do they do? And how do they know when
to stop?

### Activity 1 — The plate (5 minutes)

`/mine lesson start sensors @a`. Students step on the plate, and see its signal reach Byte: in Byte's terminal, `lua` then `redstone.getInput("left")` says `true` while someone
stands on it.

### Activity 2 — The door with a timer (15 minutes)

**Task:** when someone steps on the plate the door opens, and closes by itself 3 seconds later.

```lua
redstone.setOutput("front", false)
while true do
  os.pullEvent("redstone")
  if redstone.getInput("left") then
    redstone.setOutput("front", true)
    os.sleep(3)
    redstone.setOutput("front", false)
  end
end
```

Things to explain:

- `os.sleep(3)` makes Byte **wait**: during the 3 seconds it does nothing else.
- Why not `redstone.setOutput("front", redstone.getInput("left"))` as in lesson 6? Try it: the
  door closes as soon as you step off. MINE says so too.
- The `if`: the plate sends a signal when you step on it **and** when you step off. Only the first
  one opens the door.

Solution: [`solutions/door.lua`](solutions/door.lua).

### Activity 3 — The night light (15 minutes)

**Task:** the lamp behind Byte is on only when it's dark.

The daylight sensor on top sends a signal **when it's light**. The lamp must do the opposite: `not`.

```lua
while true do
  redstone.setOutput("back", not redstone.getInput("top"))
  os.pullEvent("redstone")
end
```

To see it work without waiting for the night, **right-click the sensor**: it turns blue and
inverted, as if night had come. That's also how MINE tests it.

Solution: [`solutions/lamp.lua`](solutions/lamp.lua).

### Activity 4 — The walkway (15 minutes)

**Task:** each press of the button moves the walkway out, or back in.

A button is on **only for a moment**: Byte can't look at it to know where the walkway is. It
has to **remember**, in a variable:

```lua
local out = false
redstone.setOutput("front", out)
while true do
  os.pullEvent("redstone")
  if redstone.getInput("right") then
    out = not out
    redstone.setOutput("front", out)
  end
end
```

`out = not out` is the heart of it: true becomes false, false becomes true. Ask what happens
without the variable (the walkway comes out while the button is down, and goes back with it).

Solution: [`solutions/bridge.lua`](solutions/bridge.lua).

### Extra challenge — For early finishers

- A door that stays open **as long as** someone is on the plate, and 3 seconds more.
- A walkway that comes out with the button and goes back by itself after 5 seconds.
- A counter: Byte says how many times the button has been pressed (`mine.say`).

### Wrap-up — 5 minutes

- What does a sensor do? What does a timer do? What does a variable remember?
- The reflection question MINE shows at the end: *The night light works by itself, all night.
  Which machines in your home or at school react to a sensor, wait a while, or remember what
  happened?*

## Common problems

| What happens | Why | What to do |
|---|---|---|
| MINE: "the door closed as soon as the plate was free" | the door follows the plate | open, `os.sleep(3)`, close |
| MINE: "the door stayed open" | the program doesn't close it, or waits too long | `redstone.setOutput("front", false)` after the wait |
| The door opens twice | no `if`: stepping off is a change too | `if redstone.getInput("left") then` |
| MINE: "the sensor said it was light, and the lamp was on" | the lamp follows the sensor instead of the opposite | `not redstone.getInput("top")` |
| The sensor never changes | it can't see the sky (a roof, a block above) | an open space; right-click it to invert it |
| MINE: "the walkway moved back by itself" | the piston follows the button | a variable, changed only when the button is pressed |
| MINE: "MINE needs room around Byte" | blocks or the lesson 6 scene around Byte | `/mine byte recall` from an open space |
| The program ended before the test | no `while true do ... end` | keep it listening |

## For the teacher: what to look for

- **Do they predict** what a program will do before MINE tests it (the door that follows the
  plate, the walkway that follows the button)?
- **Do they read MINE's message** and change the one thing it points to?
- Can they say, in their words, why the button needs a variable and the lever of lesson 6 didn't?

## References

- [Lesson 6 — Redstone](../06-redstone/README.md)
- [MINE Lua API](../../guide/lua-api.md); in game, `help redstone` in Byte's terminal.
- The solutions are in [`solutions/`](solutions/) (Italian in [`solutions/it/`](solutions/it/)), with
  and without blocks, and are tested automatically every time MINE changes.
