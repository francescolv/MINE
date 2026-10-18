**English** · [Italiano](README.it.md)

# Lesson 12 — Python: the connected greenhouse (Pico 2 W)

| | |
|---|---|
| **Length** | 1–2 hours |
| **Age** | 14–18 |
| **Concepts** | **Python**: lists, `sum`/`len`, average, threshold; data from a sensor over Wi-Fi (Internet of Things), summarising data |
| **Prerequisites** | [lesson 9](../09-charts/README.md) (Python with Byte); a [Raspberry Pi Pico 2 W](../../guide/devices.md#raspberry-pi-pico-2-w-over-wi-fi) is recommended, not needed |

## Objectives

By the end of the lesson students can:

1. collect measurements in a **list** and work out their **average**;
2. compare a measurement with a **threshold** and send a command to the device;
3. **summarise** data in groups, as a weather station does, instead of sending it all;
4. explain the path of the data: sensor → Wi-Fi → server → program → back to the device.

## How it works

A **Pico 2 W** in the greenhouse measures the temperature and sends it to the MINE server **over
Wi-Fi**, with no cable to the PC, with the key `temperatura`. Byte gets it in the student's Python
program (`mine.device.wait("temperatura")`) and answers the Pico: `allarme:1` lights its LED,
`media` reaches it as a number.

To check the steps **MINE plays the Pico**: it sends the temperatures itself, different every
time, and looks at what Byte answers. So the lesson works without the board, and with the board
the program is the same.

## Preparation (teacher, before the lesson)

- The Python editor (`/mine python`, or **Python** in the block editor): see the
  [Python guide](../../guide/python.md). Thonny on the PC is fine too.
- If you have Pico 2 Ws: MicroPython, `mine_wifi.py` and `mine.txt` (network, password, each
  student's code), as in the [boards guide](../../guide/devices.md#raspberry-pi-pico-2-w-over-wi-fi).
  First check with one Pico that the school network lets devices through.
- Try the three steps yourself with [`solutions/`](solutions/).

### The Pico's program

Every 2 seconds it sends its chip's temperature; `allarme` switches its LED; `media` is printed
(in Thonny). Save it on the Pico as `serra.py` and, as `main.py`, the line `import serra`.

```python
# MINE: the greenhouse's Pico 2 W. Every 2 seconds its temperature (temperatura:NN);
# allarme:1 / allarme:0 switch its LED on and off; media:NN is printed.
import time

import mine_wifi as mine
from machine import ADC, Pin

led = Pin("LED", Pin.OUT)
sensor = ADC(4)  # the temperature sensor inside the chip

mine.connect()
mine.send("programma", "serra")
while True:
    volts = sensor.read_u16() * 3.3 / 65535
    mine.send("temperatura", round(27 - (volts - 0.706) / 0.001721))
    for key, value in mine.receive():
        if key == "allarme":
            led.value(1 if value else 0)
        elif key == "media":
            print("average:", value)
    time.sleep(2)
```

To raise the temperature, hold the Pico between your fingers or breathe on it; to see the alarm in
class, lower the threshold to 28.

## Missions: MINE checks the steps

```
/mine lesson start greenhouse @a
```

| Step | MINE (as the Pico)… | Done when… |
|---|---|---|
| 1 · `average` | sends 6 temperatures between 16 and 34 | Byte says their average (rounded is fine) |
| 2 · `alarm` | sends temperatures below and above 30, and exactly 30 | after each one the Pico got `allarme:1` only **above** 30, else `allarme:0` |
| 3 · `report` | sends two groups of 5 temperatures | after each group the Pico got their average as `media` |

Steps 2 and 3 are tested **live**: the program keeps listening (`while True:`) and MINE stops it
when it has seen enough. When something is wrong, MINE says what: *"the temperature was 33, and
Byte told the board allarme:0"*.

## Lesson plan

### Introduction — 10 minutes

Draw a measurement's journey on the board: **sensor** (in the Pico) → **Wi-Fi** → MINE **server**
→ Byte's **program** → back to the **Pico** (the LED). This is the Internet of Things: thermostats,
weather stations, real greenhouses work like this. Ask: who decides, the Pico or the program?

### Activity 1 — The average (15 minutes)

```python
import mine

temperatures = []
for i in range(6):
    key, value = mine.device.wait("temperatura")
    temperatures.append(value)

average = sum(temperatures) / len(temperatures)
mine.say("Average temperature: " + str(round(average, 1)))
```

- `mine.device.wait` **waits** for the next measurement: the program goes at the sensor's pace.
- `sum` and `len`: the average is the sum divided by how many there are.

Solution: [`solutions/average.py`](solutions/average.py).

### Activity 2 — The alarm (15 minutes)

```python
while True:
    key, temperature = mine.device.wait("temperatura")
    if temperature > 30:
        mine.device.send("allarme", 1)
    else:
        mine.device.send("allarme", 0)
```

Question: `>` or `>=`? MINE also tries **exactly 30**, which isn't "above 30". Why does a real
greenhouse send `allarme:0` with every measurement and not only when it changes? (If a message
gets lost, the next one puts things right.)

Solution: [`solutions/alarm.py`](solutions/alarm.py).

### Activity 3 — The report (20 minutes)

Sending every measurement costs (battery, network). Send a **summary** instead: the average every
5 measurements.

```python
group = []
while True:
    key, temperature = mine.device.wait("temperatura")
    group.append(temperature)
    if len(group) == 5:
        mine.device.send("media", round(sum(group) / len(group), 1))
        group = []
```

Solution: [`solutions/report.py`](solutions/report.py).

### Extra challenge

- Also send the **lowest** and **highest** of each group (`min`, `max`).
- The **trend**: is the temperature going up or down compared with the group before? Send
  `tendenza:1` or `tendenza:-1`, and on the Pico light the LED only when it goes up.

### Wrap-up — 5 minutes

MINE's reflection question: *Your greenhouse measured, decided and sent back a summary by itself,
over Wi-Fi. Which devices around you work like this? What data do they send, and to whom?*

## Common problems

| What happens | Why | What to do |
|---|---|---|
| *"your program ended before MINE could try it"* | steps 2 and 3 have no `while True:` | the program keeps listening; MINE stops it |
| The average is way off | the list also gets other keys, or the sum is outside the loop | wait only for `"temperatura"` |
| The Pico gets nothing | Byte sends `allarme` with another key, or the Pico doesn't call `mine.receive()` | the same key on both sides; `mine.receive()` at least every few seconds |
| With the real Pico, MINE's test goes wrong | the Pico sends its temperatures while MINE tries its own | switch the Pico off (or unplug it) while MINE checks the step |

## References

- [Boards: Pico 2 W over Wi-Fi](../../guide/devices.md#raspberry-pi-pico-2-w-over-wi-fi), [Python guide](../../guide/python.md).
- Solutions in [`solutions/`](solutions/) (in Italian in [`solutions/it/`](solutions/it/)), tested
  automatically in MicroPython, as in the editor.
