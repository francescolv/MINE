**English** · [Italiano](README.it.md)

# Lesson 9 — Python: data from the board, as charts

| | |
|---|---|
| **Duration** | 1 hour (50–60 minutes) |
| **Age** | 14–18 years old (and able younger students) |
| **Concepts** | Python: lists (`append`, `max`, `index`), `for` over a list, functions, integer division (`//`), scaling a measurement |
| **Prerequisites** | some Python, or lessons 1–5 in Lua or blocks; [lesson 8](../08-board/README.md) for the board; a **micro:bit** (or an Arduino, or a Raspberry Pi Pico) per student or pair |
| **Language** | **Python**, in MINE's [Python editor](../../guide/python.md) (or in [Thonny](../../guide/python.md#thonny)) |

## Goals

By the end of the lesson students can:

1. collect measurements from a board in a **list**;
2. turn a list into a **bar chart** built by Byte, column by column;
3. find the **largest** value and its **position** (`max`, `index`, counting from 0);
4. **scale** a measurement to the space they have (`value * 8 // 255`).

These are the first steps of data science: measure, store, look, summarise.

## The board's program

Each press of **A** throws a die (1 to 6) and sends it as `dato`; each press of **B** sends the
light as `luce` (0 to 255). MakeCode's JavaScript:

```javascript
serial.writeLine("programma:lezione9")
input.onButtonPressed(Button.A, function () {
    let n = randint(1, 6)
    basic.showNumber(n)
    serial.writeLine("dato:" + n)
})
input.onButtonPressed(Button.B, function () {
    let light = input.lightLevel()
    basic.showNumber(light)
    serial.writeLine("luce:" + light)
})
```

With a **Raspberry Pi Pico** and MicroPython the same lines are written with `print("dato:" + str(n))`
(see [Boards](../../guide/devices.md)). No boards? MINE plays the board when it tests the programs.

## Missions: MINE checks the steps

```
/mine lesson start charts @a
```

| Step | Done when… |
|---|---|
| 1 · `bar_chart` | **MINE sends 5 numbers** (1 to 6) as `dato`; Byte builds one column per number, as tall as the number, the first in front of where it started and the others one by one to its right, then goes back where it started |
| 2 · `largest` | MINE sends 5 numbers (1 to 9); Byte **says** the largest and its position, from 1 (e.g. "the largest is 8, number 2") |
| 3 · `light_chart` | MINE sends 5 light readings (0 to 255) as `luce`; columns `luce * 8 // 255` tall, and Byte back where it started |

MINE chooses **new numbers every time**: a program that builds the same chart always doesn't pass.
MINE gives 64 planks at steps 1 and 3, and takes away the previous try's chart.

## Lesson plan

### Introduction — 5 minutes

A list of numbers says little; a chart says a lot. Show a bar chart of something real (the
temperature this week, the class's heights). What's on each axis? What does the tallest bar say?

### Activity 1 — Numbers in a list (10 minutes)

`/mine lesson start charts @a`, `/mine device`, then in the Python editor:

```python
import mine

numbers = []
for i in range(5):
    key, value = mine.device.wait("dato")
    numbers.append(value)
print(numbers)
```

Press A five times: the list appears on the right of the editor. `mine.device.wait` gives back
**two** values, the key and the value: `key, value = ...` takes both.

### Activity 2 — The bar chart (15 minutes)

```python
for n in numbers:
    for h in range(n):
        byte.place()
        byte.up()
    for h in range(n):
        byte.down()
    byte.turnRight()
    byte.forward()
    byte.turnLeft()

byte.turnLeft()
for i in range(5):
    byte.forward()
byte.turnRight()
```

- `for n in numbers` goes through the list, one number at a time.
- The last four lines bring Byte back: in the lab the lanes are 8 blocks apart, and the next chart
  needs room.

Solution: [`solutions/chart.py`](solutions/chart.py).

### Activity 3 — The largest (10 minutes)

```python
top = max(numbers)
mine.say("the largest is " + str(top) + ", number " + str(numbers.index(top) + 1))
```

Why `+ 1`? Lists count from 0, people from 1. Ask what `numbers[0]` is, and `numbers[5]` (an
error: there's no sixth number). Solution: [`solutions/largest.py`](solutions/largest.py).

### Activity 4 — Scaling the light (15 minutes)

255 blocks wouldn't fit: `value * 8 // 255` turns 0–255 into 0–8. `//` divides and drops the
decimals (`7 // 2` is 3). A function keeps it tidy:

```python
def column(h):
    for i in range(h):
        byte.place()
        byte.up()
    for i in range(h):
        byte.down()
```

Cover the board, press B, uncover it, press B again: the chart shows the light. Solution:
[`solutions/light.py`](solutions/light.py).

### Extra challenge

- Byte says the **average** too (`sum(numbers) / len(numbers)`), and builds a gold block on top of
  the tallest column.
- Ten throws of the die: how many 6s? A chart of how many times each number came out.

### Wrap-up — 5 minutes

The reflection question MINE shows at the end: *Your program turned numbers into a chart you can
walk around. Where have you seen charts made from measurements? What did they help people see?*

## Common problems

| What happens | Why | What to do |
|---|---|---|
| MINE: "your program ended before the board had sent its 5 numbers" | the program didn't wait for each number | `mine.device.wait("dato")` in the loop |
| `TypeError` at `range`, or `('dato', 3)` in the printed list | `value = mine.device.wait(...)` takes both key and value | `key, value = mine.device.wait("dato")` |
| MINE: "the chart is right! Now bring Byte back" | Byte stays at the last column | `turnLeft`, 5 × `forward`, `turnRight` |
| MINE: "column 3 is 2 tall" | the columns are built in the wrong place, or Byte didn't go back down | each column in front of Byte, then down again before moving right |
| The columns are too tall in step 3 | `/` instead of `//`, or no scaling | `value * 8 // 255` |

## References

- [MINE's Python editor and Thonny](../../guide/python.md)
- [Boards: micro:bit, Arduino and Byte](../../guide/devices.md)
- The solutions are in [`solutions/`](solutions/) (Italian in [`solutions/it/`](solutions/it/)), and are
  tested automatically, in MicroPython, every time MINE changes.
