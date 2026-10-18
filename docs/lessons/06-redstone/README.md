**English** · [Italiano](README.it.md)

# Lesson 6 — Redstone: Byte listens and switches on

| | |
|---|---|
| **Duration** | 1 hour (50–60 minutes) |
| **Age** | 10–14 years old (also fine for the first year of secondary school) |
| **Concepts** | input and output, signal on/off, waiting for an event, logic gates AND / OR |
| **Prerequisites** | [lesson 2](../02-loops/README.md) (loops) and [lesson 4](../04-conditions/README.md) (conditions, `while`) |

## Goals

By the end of the lesson students can:

1. tell an **input** (lever, button, sensor) from an **output** (lamp, door, piston);
2. make Byte **switch on** an output with `redstone.setOutput`;
3. make Byte **listen** to an input with `redstone.getInput`, and wait for it to change with
   `os.pullEvent("redstone")`;
4. build an **AND gate**: an output that is on only when two inputs are.

It also prepares for external boards (micro:bit, Arduino): there the lever becomes a real button.

## MINE's scene

When the lesson starts, MINE builds the same scene around every Byte, which stays for the whole
lesson:

- a **redstone lamp behind** Byte (`"back"`);
- an **iron door in front** (`"front"`);
- on the **left** and on the **right** a stone post with a **lever** on its outer side: the lever
  powers the stone, and Byte feels the signal on that side (`"left"`, `"right"`).

The lamp is behind, not on top, on purpose: a lamp lit by Byte also powers the blocks next to
it, and on top of Byte it would touch the door.

Students can pull the levers with a right click, to try. In steps 2 and 3 **MINE** moves the
levers, while the program runs (see below).

## Preparation (teacher, before the lesson)

- A **superflat world in Creative mode**, students side by side, with room for the scene: one free
  block behind Byte, two in front (the door, with solid ground under it), two on the left and
  two on the right.
- The quickest way: the **standard lab** (`/mine lab build <students>`, then `/mine lab assign`),
  whose lanes are 8 blocks wide ([guide](../../guide/server.md#3-the-standard-lab)).
- Nothing else: MINE creates Byte for whoever doesn't have one and builds the scene.

## Missions: MINE checks the steps

At the start of activity 1:

```
/mine lesson start redstone @a
```

| Step | Done when… |
|---|---|
| 1 · `lamp` | when the program ends, the lamp behind Byte is on |
| 2 · `door` | **while the program runs, MINE pulls the left lever**, off and on again: the door must open and close with it every time |
| 3 · `and` | **MINE tries the two levers in all four ways**: the lamp must be on only when both are on |

Steps 2 and 3 are a **live check**. The students' program listens forever (`while true do ... end`)
and MINE tries every case: a program that always switches the lamp on, or looks at one lever only,
doesn't pass by luck. When the test is over, **MINE stops the program itself** and says how it
went, in the terminal and in the chat. If the program ends by itself before the test, MINE
explains that it has to keep listening.

The other commands are the same as in the other lessons: `/mine lesson status @a`,
`/mine lesson hint`, `/mine lesson restart`, `/mine lesson reset <students>` (see the
[lesson 1 guide](../01-first-steps/README.md#missions-mine-checks-the-steps)).

## With blocks

The whole lesson works in the [block editor](../../guide/blocks.md) too (`/mine blocks`, then
**Run ▶**), with the **Redstone** category: *redstone signal [side] set to …*, *signal from
[side]?*, *wait for a redstone change*; *wait N seconds* is in **Loops**.

| Activity | With blocks | File |
|---|---|---|
| 1 · The lamp | `redstone signal behind set to true` | [lamp.json](solutions/blocks/lamp.json) |
| 3 · The door | see below | [door.json](solutions/blocks/door.json) |
| 4 · AND | see below | [and.json](solutions/blocks/and.json) |

```
repeat while true                           repeat while true
    redstone signal in front set to             redstone signal behind set to
        signal from left?                           signal from left? and signal from right?
    wait for a redstone change                  wait for a redstone change
```

## Lesson plan

### Introduction — 5 minutes, no computers

Inputs and outputs around us: the doorbell (button → sound), the street lamp (light sensor →
light), the automatic gate (remote → motor). What decides what the output does? Sometimes nothing
(the doorbell's wire), sometimes a program (the gate).

In Minecraft the redstone **inputs** are levers, buttons, pressure plates, light sensors; the
**outputs** are lamps, iron doors, pistons. Byte can be both: a program in between.

### Activity 1 — Byte switches on the lamp (10 minutes)

```lua
redstone.setOutput("back", true)
```

The first value is Byte's **side**: `"back"`, `"front"`, `"left"`, `"right"`, `"top"`,
`"bottom"`. The second: `true` on, `false` off. The signal stays on when the program ends too: to
switch it off it takes `false`.

Solution: [`solutions/lamp.lua`](solutions/lamp.lua).

### Activity 2 — Blink! (10 minutes)

Loops again, with no mission: the lamp goes on and off 5 times.

```lua
for i = 1, 5 do
  redstone.setOutput("back", true)
  os.sleep(0.5)
  redstone.setOutput("back", false)
  os.sleep(0.5)
end
```

`os.sleep(0.5)` waits half a second. Questions: how do we make it faster? And if we take away the
second `os.sleep`? (The lamp goes off and back on at once: it looks always on.)

Solution: [`solutions/blink.lua`](solutions/blink.lua).

### Activity 3 — The door that listens (15 minutes)

**Task:** the door follows the left lever.

```lua
while true do
  redstone.setOutput("front", redstone.getInput("left"))
  os.pullEvent("redstone") -- waits until a redstone signal changes
end
```

`redstone.getInput("left")` answers `true` when the left lever is on. The value goes straight to
the door. `os.pullEvent("redstone")` **waits** for a signal to change, instead of checking all the
time: the program costs nothing while it waits.

Before MINE runs its test, let students pull the lever themselves. The program never ends: to stop
it, hold **Ctrl+T** in the terminal **for a second** (a quick press is not enough): *Terminated*
appears, then the `>` prompt.

> **Watching the scene:** the terminal covers the lamp and the door. A program that listens keeps
> running when the terminal is closed with **Esc**: pull the lever and watch. After stopping it
> (Ctrl+T), **R** starts Byte's last program again without opening the terminal.

Solution: [`solutions/door.lua`](solutions/door.lua).

### Activity 4 — Two levers: AND and OR (15 minutes)

**Task:** the lamp is on only when the left lever **and** the right one are on.

```lua
while true do
  local both = redstone.getInput("left") and redstone.getInput("right")
  redstone.setOutput("back", both)
  os.pullEvent("redstone")
end
```

Then the `or` version (one lever is enough), to try by hand. Discussion: is a safe with two keys AND
or OR? And the doorbell that rings from the front door and from the gate?

Solution: [`solutions/and.lua`](solutions/and.lua).

### Extra challenge — For early finishers

- **Automatic door:** put a pressure plate on top of the left post: whoever steps on it powers the
  stone like the lever, and the door opens.
- **Counter:** Byte counts how many times the lever was switched on and says it with
  `mine.say`.
- **OR gate:** the lamp on with at least one of the two levers.

### Wrap-up — 5 minutes

- What is an input, and what is an output? Where have you seen them outside Minecraft?
- Why does the program never end in steps 2 and 3?
- The reflection question MINE shows at the end: *Where in real life do you find an input and an
  output like Byte's levers and lamp? Think of a device that needs two things at once (AND)
  to work.*

## Common problems

| What happens | Why | What to do |
|---|---|---|
| MINE says it needs room around Byte | something is behind, in front or on the sides, or there's no ground under the door | `/mine byte recall` from a free spot, on flat ground |
| `bad argument #1 (unknown option black)` | the side's name is misspelt (here `"black"` instead of `"back"`) | the sides are `"back"`, `"front"`, `"left"`, `"right"`, `"top"`, `"bottom"` |
| The lamp doesn't switch on | wrong side (`"top"` instead of `"back"`) | the lamp is **behind**: `"back"` |
| The lamp stays on after the program | that's normal: the signal stays until a `false` switches it off | `redstone.setOutput("back", false)` |
| The door opens once and then doesn't follow the lever | there's no `while true`: the program ends at once | `while true do ... end` |
| MINE says the program ended before the test | as above | the program must keep listening |
| Ctrl+T doesn't stop the program | it was only tapped | open the terminal, click inside it and **hold** Ctrl+T for a second, until *Terminated* appears |
| A student broke Byte or part of the scene | in Creative one click is enough | `/mine byte` from a free spot: the new Byte has the old one's programs, and the scene is built again around it when the program starts |
| The door doesn't open with the AND program | `and` only drives the lamp (`"back"`): the door is `door`'s job | stop `and` with Ctrl+T, then run `door` |
| The lamp goes on with one lever only | it's an OR gate (`or`), not AND | `and` |

## For the teacher: what to look for

- **Can they say** which side of Byte is an input and which an output, in each activity?
- **Do they try** the lever by hand before MINE runs its test?
- **Do they explain** the difference between AND and OR with a real-life example?

## References

- [Lesson 4 — Conditions](../04-conditions/README.md)
- [CC: Tweaked's `redstone` API](https://tweaked.cc/module/redstone.html); in the game,
  `help redstone` in Byte's terminal.
- The solutions are in [`solutions/`](solutions/) (in Italian in [`solutions/it/`](solutions/it/)),
  with blocks too, and are tested automatically at every change to MINE, with MINE pulling the levers.
