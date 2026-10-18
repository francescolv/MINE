**English** · [Italiano](README.it.md)

# Lesson 14 — Python: the level crossing (Arduino)

| | |
|---|---|
| **Length** | 1–2 hours |
| **Age** | 14–18 |
| **Concepts** | **Python**: **state machine**, events from two sources (`os.pullEvent`), functions, a counter as state |
| **Prerequisites** | [lesson 8](../08-board/README.md) (the board) and [lesson 13](../13-control/README.md) (state); an Arduino is recommended, not needed |

## Objectives

By the end of the lesson students can:

1. describe a system as a **state machine**: states, events, transitions;
2. wait for **events from several sources** (the board, the world's redstone) in the same loop;
3. gather everything that changes with the state in a **function**;
4. use a **counter** as the state, when "closed" depends on how many trains there are.

## How it works

Around Byte, MINE builds a level crossing:

- the **iron door in front** of Byte is the **barrier**: open while Byte sends it redstone
  (`redstone.setOutput("front", True)`);
- the **lamp behind** it is the flashing **light** (`redstone.setOutput("back", ...)`);
- the **lever on its left** is the **sensor on the track**: it goes on when the train has passed
  (`redstone.getInput("left")`).

The **Arduino** has a button: press it and `treno:1` arrives (a train is coming); and an LED, lit by
`led:1`. To check the steps **MINE plays the Arduino** and moves the lever.

## Preparation (teacher, before the lesson)

- The Python editor (`/mine python`), see the [Python guide](../../guide/python.md).
- Room around Byte, with solid ground in front and on the left (the standard lab is fine).
- If you have Arduinos: a button between pin 2 and GND, the LED on the board (pin 13), connected
  with `/mine device`, with this program:

```cpp
// MINE: the level crossing's Arduino. The button on pin 2 says a train is coming (treno:1);
// the LED on pin 13 follows led:1 / led:0.
const int BUTTON = 2;
const int LED = 13;
String line;

void setup() {
  Serial.begin(115200);
  pinMode(BUTTON, INPUT_PULLUP);
  pinMode(LED, OUTPUT);
  Serial.println("programma:level_crossing");
}

void loop() {
  static int before = HIGH;
  int now = digitalRead(BUTTON);
  if (before == HIGH && now == LOW) {   // just pressed
    Serial.println("treno:1");
  }
  before = now;
  while (Serial.available()) {          // from Byte, e.g. "led:1"
    char c = Serial.read();
    if (c == '\n') {
      if (line.startsWith("led:")) digitalWrite(LED, line.substring(4).toInt() > 0 ? HIGH : LOW);
      line = "";
    } else if (c != '\r') {
      line += c;
    }
  }
  delay(20);
}
```

## Missions: MINE checks the steps

```
/mine lesson start crossing @a
```

| Step | MINE… | Done when… |
|---|---|---|
| 1 · `close` | sends `treno:1` | to begin with the barrier is open and the light off; after the train, barrier closed and light on |
| 2 · `reopen` | sends a train, then switches the lever on (the train has passed); twice | the train closes it, the lever opens it |
| 3 · `trains` | sends **two** trains, then two passings; then another train | it opens only when they have all passed; the Arduino gets `led:1` while closed, `led:0` when open |

The tests are live; when something is wrong MINE tells the story: *"after a train, a train, a train
passed the barrier should be closed and the light on"*.

## Lesson plan

### Introduction — 10 minutes, no computers

Draw the **states** (open, closed) on the board as circles and the **events** (a train comes, the
train has passed) as arrows. It's a **state machine**: a lift, a vending machine, a traffic light
work like this. What happens if a train comes when it's already closed?

### Activity 1 — Close (15 minutes)

```python
from mine import redstone
import mine


def crossing(closed):
    redstone.setOutput("front", not closed)  # the barrier
    redstone.setOutput("back", closed)  # the light


crossing(False)
while True:
    mine.device.wait("treno")
    crossing(True)
```

The function `crossing` keeps together everything that changes with the state: the light doesn't
get forgotten.

Solution: [`solutions/close.py`](solutions/close.py).

### Activity 2 — Reopen: two kinds of events (20 minutes)

`mine.device.wait` waits **only** for the board: while it waits, it can't hear the lever. It takes
`os.pullEvent()`, which waits for **any** event and says which:

```python
event = os.pullEvent()   # ("mine_device", key, value) or ("redstone",)
if event[0] == "mine_device" and event[1] == "treno":
    crossing(True)
elif event[0] == "redstone" and redstone.getInput("left"):
    crossing(False)
```

Why `redstone.getInput("left")`? (The `redstone` event also comes when the lever goes off: that's not
a train passing.)

Solution: [`solutions/reopen.py`](solutions/reopen.py).

### Activity 3 — More trains (20 minutes)

With two tracks, a second train can come before the first has passed: if the barrier opened at the
first passing, it would be a disaster. The state isn't "open or closed" any more but **how many
trains** there are: `trains > 0` means closed. And the Arduino lights its LED while it's closed.

Solution: [`solutions/trains.py`](solutions/trains.py).

### Wrap-up — 5 minutes

MINE's reflection question: *Your crossing was always in one state, and events moved it from one to
the other. Which machines around you work like this? What are their states and their events?*

## Common problems

| What happens | Why | What to do |
|---|---|---|
| *"when the program starts, the barrier must be open"* | the starting state isn't set | `crossing(False)` before the loop |
| The lever does nothing | the program only waits for the board (`mine.device.wait`) | `os.pullEvent()` for every event |
| It opens when the lever goes off | the `redstone.getInput("left")` check is missing | open only when the lever is on |
| With two trains it opens too early | the state is a yes/no instead of a counter | `trains` + 1 and − 1 |

## References

- [Lesson 8 — The board](../08-board/README.md), [boards](../../guide/devices.md), [Python guide](../../guide/python.md).
- Solutions in [`solutions/`](solutions/) (in Italian in [`solutions/it/`](solutions/it/)), tested
  automatically in MicroPython.
