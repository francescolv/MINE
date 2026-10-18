**English** · [Italiano](README.it.md)

# Lesson 13 — Python: automatic control (micro:bit)

| | |
|---|---|
| **Length** | 1–2 hours |
| **Age** | 14–18 |
| **Concepts** | **Python**: threshold, **hysteresis**, state variables, events from several sources; control systems |
| **Prerequisites** | [lesson 8](../08-board/README.md) (the board and Byte) and [lesson 9](../09-charts/README.md) (Python); a micro:bit is recommended, not needed |

## Objectives

By the end of the lesson students can:

1. switch an actuator with a **threshold**, from a measurement;
2. explain why a single threshold makes the actuator flicker, and fix it with **hysteresis**;
3. use a **state variable** that remembers what happened before;
4. handle **two sources** of events (the temperature, a button) in the same loop.

## How it works

The lamp **behind Byte** is the greenhouse's **fan**: Byte switches it on with
`redstone.setOutput("back", True)`. The micro:bit measures the temperature (`temperatura`) and has
a button (`pulsante_a`), which reach Byte as in [lesson 8](../08-board/README.md).

To check the steps **MINE plays the micro:bit**: it sends temperatures and button presses, and
after each one looks whether the fan is on or off as it should be.

## Preparation (teacher, before the lesson)

- The Python editor (`/mine python`), see the [Python guide](../../guide/python.md).
- Room around Byte: MINE puts the lamp behind it (the standard lab is fine).
- If you have micro:bits: connected with `/mine device`, with this program (MicroPython):

```python
# MINE: the greenhouse's micro:bit. The temperature every second; button A when you press it.
from microbit import *

print("programma:serra")
while True:
    if button_a.was_pressed():
        print("pulsante_a:1")
    print("temperatura:%d" % temperature())
    sleep(1000)
```

The micro:bit's sensor measures its chip's temperature: holding it raises it a few degrees. To try
it in class, lower the thresholds (for example 26 and 24).

## Missions: MINE checks the steps

```
/mine lesson start control @a
```

| Step | Done when… |
|---|---|
| 1 · `threshold` | the fan is on from 28 degrees up, off below |
| 2 · `hysteresis` | it switches on at 28 or more, off at 24 or less, and **in between it stays as it was** |
| 3 · `manual` | also, button A switches **manual mode** on and off, where the fan is always on |

The tests are live: the program keeps listening (`while True:`). When something is wrong, MINE
tells what it sent and what it saw: *"after 22°, 26°, 29°, 26° the fan should be on, and it was
off"*.

## Lesson plan

### Introduction — 10 minutes

A **control system**: a sensor measures, a program decides, an actuator acts. Examples: the
thermostat, the fridge, street lights that switch on in the dark. Draw temperature and fan over
time on the board, with a single threshold at 28: while the temperature wobbles between 27 and 28,
the fan switches on and off all the time. How do we avoid it?

### Activity 1 — The threshold (10 minutes)

```python
from mine import redstone
import mine

while True:
    key, temperature = mine.device.wait("temperatura")
    redstone.setOutput("back", temperature >= 28)
```

The comparison `temperature >= 28` is already `True` or `False`: no `if` needed.

Solution: [`solutions/threshold.py`](solutions/threshold.py).

### Activity 2 — Hysteresis (20 minutes)

Two thresholds: on at 28, off at 24; **in between nothing changes**. The program has to remember
whether the fan is on: a **state variable**.

```python
on = False
while True:
    key, temperature = mine.device.wait("temperatura")
    if temperature >= 28:
        on = True
    elif temperature <= 24:
        on = False
    redstone.setOutput("back", on)
```

Draw the chart again with the two thresholds: how many times does it switch on now?

Solution: [`solutions/hysteresis.py`](solutions/hysteresis.py).

### Activity 3 — The gardener's button (20 minutes)

`mine.device.wait()` **without a key** waits for anything from the board: the key says what.

```python
on = False
manual = False
while True:
    key, value = mine.device.wait()
    if key == "pulsante_a":
        manual = not manual
    elif key == "temperatura":
        if value >= 28:
            on = True
        elif value <= 24:
            on = False
    redstone.setOutput("back", on or manual)
```

Now there are four states (automatic or manual, on or off): draw them.

Solution: [`solutions/manual.py`](solutions/manual.py).

### Wrap-up — 5 minutes

MINE's reflection question: *Your greenhouse switched its fan on and off by itself, without
flickering. Where else does a machine decide by itself with a threshold? Why do they need a gap
between on and off?*

## Common problems

| What happens | Why | What to do |
|---|---|---|
| The fan goes off between 24 and 28 | the program doesn't remember the state: it works everything out from the temperature | a variable `on` outside the loop, changed only at the thresholds |
| The button does nothing | the program only waits for `"temperatura"` | `mine.device.wait()` without a key, then look at `key` |
| MINE says it needs room | something is behind Byte | `/mine byte recall` in a free place |

## References

- [Lesson 8 — The board](../08-board/README.md), [boards](../../guide/devices.md), [Python guide](../../guide/python.md).
- Solutions in [`solutions/`](solutions/) (in Italian in [`solutions/it/`](solutions/it/)), tested
  automatically in MicroPython.
