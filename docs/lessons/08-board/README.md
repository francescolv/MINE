**English** · [Italiano](README.it.md)

# Lesson 8 — The board: from your desk into the world

| | |
|---|---|
| **Duration** | 1 hour (50–60 minutes) |
| **Age** | 10–14 years old |
| **Concepts** | input and output between two computers, messages `key:value`, a sensor's number and a threshold (`< 50`), `if … else` |
| **Prerequisites** | [lesson 7](../07-sensors/README.md) (waiting, timers, `not`); a **micro:bit** (or an Arduino) for each student or pair, with its USB cable |

## Goals

By the end of the lesson students can:

1. make a **real button** change the game world: button A on the board opens a door;
2. use a **sensor's number** with a **threshold**: the board's light below 50 means "dark";
3. send a message **the other way**: a lever in the game lights a heart on the board;
4. explain that the board and Byte are **two computers with two programs**, which talk with
   short messages (`pulsante_a:1`, `luce:120`, `cuore:1`).

These are the ideas behind remote controls, smart homes and the sensors of a weather station.

## MINE's station

At each step MINE builds its part around Byte, and takes away the part of the step before:

| Step | Around Byte |
|---|---|
| 1 | an **iron door in front** (`"front"`) |
| 2 | a **redstone lamp behind** (`"back"`) |
| 3 | a **lever** on the ground on the **left** (`"left"`) |

## Preparation (teacher, before the lesson)

- As in lessons 6 and 7: a flat world, room around each Byte and solid ground around it. The
  **standard lab** is fine (`/mine lab build <students>`, `/mine lab assign`).
- **The boards.** On each micro:bit, the program below (MakeCode, *Download*, then copy the `.hex`
  onto the MICROBIT drive). One program for the whole lesson: it writes the button and the light,
  and shows the heart when Byte asks.
- **The lab PCs.** On Linux the student's user must be in the `dialout` group (see
  [Boards](../../guide/devices.md#if-it-doesnt-work)). Try one PC before the lesson:
  `/mine device`, choose the board, press A: `pulsante_a:1` appears in the window.
- No boards? The lesson still works: MINE plays the board when it tests the programs. Students
  only miss seeing it with their own hands.

### The board's program (MakeCode, micro:bit)

```
on start
  serial write line "programma:lezione8"
on button A pressed
  serial write line "pulsante_a:1"
every 500 ms
  serial write line join "luce:" (light level)
on serial data received (new line)
  set line to serial read line
  if line starts with "cuore:"
    if (the number after "cuore:") > 0 then show icon heart
    else clear screen
```

In MakeCode's JavaScript:

```javascript
serial.writeLine("programma:lezione8")
input.onButtonPressed(Button.A, function () {
    serial.writeLine("pulsante_a:1")
})
loops.everyInterval(500, function () {
    serial.writeLine("luce:" + input.lightLevel())
})
serial.onDataReceived(serial.delimiters(Delimiters.NewLine), function () {
    let line = serial.readLine()
    if (line.indexOf("cuore:") == 0) {
        // cuore:1 from Byte, cuore:15 from a Board Sender: on; cuore:0: off
        if (parseInt(line.substr(6)) > 0) {
            basic.showIcon(IconNames.Heart)
        } else {
            basic.clearScreen()
        }
    }
})
```

With an **Arduino**: a button on pin 2 writes `pulsante_a:1`, a light sensor (LDR) on A0 writes
`luce:` with `analogRead(A0) / 4`, an LED on pin 13 follows `cuore` (see
[Boards → Arduino](../../guide/devices.md#arduino)).

## Missions: MINE checks the steps

```
/mine lesson start board @a
```

| Step | Done when… |
|---|---|
| 1 · `board_door` | **MINE presses A on the student's board** (it plays `pulsante_a:1`), twice: each time the door opens, is still open 2 seconds later, and **closes by itself** about 3 seconds after |
| 2 · `board_lamp` | **MINE plays the board's light**: 200, 10, 180, 20. The lamp must be on exactly when the light is below 50 |
| 3 · `board_heart` | **MINE pulls the lever** on and off twice: each time Byte must tell the board `cuore:1` (on) or `cuore:0` (off) |

All three are **live checks**: the program listens forever, MINE tries it, stops it and says how
it went. **MINE doesn't need the board** for its test: it plays the board's part. The students'
own board works with the same program, at the same time.

## With blocks

The whole lesson works in the [block editor](../../guide/blocks.md) with the **Board** category,
a comparison from **Logic** and **if … else**.

| Activity | File |
|---|---|
| 2 · The door with button A | [door.json](solutions/blocks/door.json) |
| 3 · The night light | [lamp.json](solutions/blocks/lamp.json) |
| 4 · The heart | [heart.json](solutions/blocks/heart.json) |

```
redstone signal in front set to false       repeat while true
repeat while true                               wait for a redstone change
    wait for "pulsante_a" from the board        if signal from left?
    redstone signal in front set to true            send to the board "cuore" 1
    wait 3 seconds                              else
    redstone signal in front set to false           send to the board "cuore" 0

repeat while true
    redstone signal behind set to  board value "luce" < 50
    wait 0.5 seconds
```

The Italian solutions are in [`solutions/it/blocks`](solutions/it/blocks).

## Lesson plan

### Introduction — 5 minutes, no computers

Show a micro:bit. It's a small **computer** too: it has buttons, a light sensor, 25 LEDs, and a
program inside. Today it talks with Byte through the USB cable, with short messages: a word,
a colon, a number. `pulsante_a:1` means "button A was pressed". Who writes it? Who reads it?

### Activity 1 — Connecting the board (5 minutes)

`/mine lesson start board @a`, then `/mine device` and the board's port. Press A: the window shows
`pulsante_a:1`. Cover the board with a hand: `luce` goes down. The window also says which program is
on the board (`lezione8`).

### Activity 2 — The door with button A (15 minutes)

**Task:** pressing A on the board opens the door, which closes by itself 3 seconds later.

```lua
redstone.setOutput("front", false)
while true do
  mine.device.wait("pulsante_a")
  redstone.setOutput("front", true)
  os.sleep(3)
  redstone.setOutput("front", false)
end
```

Things to explain:

- `mine.device.wait("pulsante_a")` makes Byte **wait for a message**, like `os.pullEvent`
  waits for redstone in lesson 7.
- The key must be the **same word** in both programs: `pulsante_a` and `pulsanteA` are different.

Solution: [`solutions/door.lua`](solutions/door.lua).

### Activity 3 — The night light (15 minutes)

**Task:** the lamp behind Byte is on only when the board is in the dark: light below 50.

```lua
while true do
  redstone.setOutput("back", mine.device.read("luce") < 50)
  os.sleep(0.5)
end
```

- `mine.device.read("luce")` is the **last** number the board wrote (it writes one every half second).
- `< 50` turns a number into **true or false**: that's a **threshold**. Ask students to find a good
  one for their classroom: cover the board, watch the numbers in `/mine device`.
- Without `os.sleep(0.5)` Byte checks without ever stopping, and CC: Tweaked stops it ("Too
  long without yielding").

Solution: [`solutions/lamp.lua`](solutions/lamp.lua).

### Activity 4 — The heart (15 minutes)

**Task:** the lever on Byte's left lights the heart on the board; off, the heart goes out.

```lua
while true do
  os.pullEvent("redstone")
  if redstone.getInput("left") then
    mine.device.send("cuore", 1)
  else
    mine.device.send("cuore", 0)
  end
end
```

Now the message goes **the other way**: Byte writes, the board reads. The `else` is needed:
without it the heart never goes out (MINE says so).

Solution: [`solutions/heart.lua`](solutions/heart.lua).

### Without code: the board blocks

To finish, or for early finishers: the **Board Receiver** and the **Board Sender** in the creative
inventory (Redstone) do the same without Byte
([guide](../../guide/devices.md#without-code-the-board-blocks)). A Board Receiver *like a button*
with the key `pulsante_a`, under a door; a lever next to a Board Sender with the key `cuore`. What
can Byte do that the blocks can't? (Wait 3 seconds, compare the light with 50, remember.)

### Extra challenge — For early finishers

- Button B closes the door at once (`pulsante_b:1` on the board, a second `if` in Byte).
- The lamp has three levels: Byte writes `luce` in the chat with `mine.say` when it changes a lot.
- The heart blinks while the lever is on.

### Wrap-up — 5 minutes

- Which messages went from the board to Byte? Which the other way?
- The reflection question MINE shows at the end: *Your button opened a door in the game, and a
  lever in the game lit up your board. Where in real life does a button, a sensor or a light talk
  to a computer far away?*

## Common problems

| What happens | Why | What to do |
|---|---|---|
| The door doesn't open with A, but MINE's test passes | the board isn't connected, or its program doesn't write `pulsante_a` | `/mine device`: press A and look for `pulsante_a:1` |
| MINE: "the door stayed open" | the program never closes it | `os.sleep(3)` then `redstone.setOutput("front", false)` |
| MINE: "the board said luce:200, and the lamp was on" | the comparison is the wrong way (`> 50`) | `< 50`: on when it's dark |
| "Too long without yielding" | a loop that never waits | `os.sleep(0.5)` in the loop |
| MINE: "Byte didn't tell the board cuore:0" | no `else` | `else mine.device.send("cuore", 0)` |
| The heart doesn't light on the board | the board's program doesn't read `cuore`, or reads another word | the *on serial data received* part of the board's program |
| MINE: "MINE needs room around Byte" | blocks around Byte | `/mine byte recall` from an open space |

## For the teacher: what to look for

- Can they say **which program** runs on the board and which on Byte?
- Do they choose a **threshold** by looking at real numbers, not by guessing?
- Do they keep the **same key** in both programs, and spot it when it's different?

## References

- [Boards: micro:bit, Arduino and Byte](../../guide/devices.md)
- [Lesson 7 — Sensors](../07-sensors/README.md)
- [MINE Lua API](../../guide/lua-api.md); in game, `help mine` in Byte's terminal.
- The solutions are in [`solutions/`](solutions/) (Italian in [`solutions/it/`](solutions/it/)), with
  and without blocks, and are tested automatically every time MINE changes.
