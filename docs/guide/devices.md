**English** · [Italiano](devices.it.md)

# Boards: micro:bit, Arduino and Byte

A student plugs a **board** (micro:bit, Arduino, Raspberry Pi Pico…) into their PC with its USB
cable, and the board and their Byte talk to each other: a button on the micro:bit makes
Byte walk, Byte lights a heart on the micro:bit's display.

There are **two** programs, one per machine, and each is written and loaded from its own editor:

| | Written with | Loaded with | Runs on |
|---|---|---|---|
| The **board's** program | [MakeCode](https://makecode.microbit.org) (blocks, JavaScript or Python), MicroPython, the Arduino IDE | MakeCode: **Download**, then the `.hex` file onto the **MICROBIT** drive; Arduino: **Upload** | the board |
| The **Byte's** program | MINE's [block editor](blocks.md) (`/mine blocks`) or Lua | **Run ▶** or **Send to Byte** | Byte, in Minecraft |

Each stays where it is: the board keeps its program when unplugged and starts it again whenever
it gets power; Byte keeps its own on its disk (run it again with **R**). Load again only
what changes.

> Byte's blocks ("move forward", "turn") are **not** in MakeCode: the board doesn't move the
> Byte, it **tells** it something ("A was pressed") and Byte's program decides what to do.
> "MakeCode for Minecraft", with the Agent's blocks, is another editor, Minecraft Education's:
> with MINE you use the micro:bit's MakeCode.

## How they talk: `key:value` lines

The board and MINE exchange **one line of text per message**, on the USB serial port at
**115200 baud**:

```
pulsante_a:1
luce:120
programma:semaforo
```

| | |
|---|---|
| Key | lower-case letters, digits and `_`, up to 32 characters (capitals become lower case) |
| Value | a whole number from −32768 to 32767 (beyond, it is limited) or a text |
| Line | at most 256 characters; lines that aren't `key:value` are ignored |
| Rate | at most 40 lines a second per student, more are lost: write **only when the value changes** |

It is [SerialCraft](https://github.com/LeonardoAliaga/SerialCraft/blob/HEAD/docs/en/protocol.md)'s
format, plus negative numbers and texts: sketches written for SerialCraft work with MINE too.

Each board only talks to Byte **of the student who plugged it in**: 25 boards in a lab
don't get in each other's way.

## Connecting the board

1. Plug the board into the PC with its USB cable (for the micro:bit: a **data** cable, not a
   charging-only one).
2. In the game, open the chat (**T**) and type `/mine device`.
3. In the window, click your board (the micro:bit shows as `/dev/ttyACM0` on Linux, `COM3` or
   similar on Windows). If it isn't there, plug it in and click **Look again**.

The window shows live what the board writes (→) and what Byte sends it (←), and below how
Byte reads the last value. Close it with **Done**: the board stays connected until the
student clicks **Disconnect** or leaves the game.

When it connects, MINE **restarts** the board (the micro:bit with a signal on the serial port;
Arduino Uno and Nano restart by themselves when the port opens), so its program starts afresh.

### Which program is on the board?

Loading a new program onto the board can't be seen from the serial port: to let MINE know, the
board's program **writes its name when it starts**, with the key `programma` (or `program`):

```
programma:semaforo
```

The window then shows "On the board: semaforo (started at 09:36:11)"; with the window closed,
"Your board is running semaforo" appears above the hotbar. Every time the program is loaded the
name and time are updated: that's how to be sure the new version is on the board.

## The board's program

### micro:bit with MakeCode

On [makecode.microbit.org](https://makecode.microbit.org): **New project**. The blocks you need:

| Block | Category |
|---|---|
| **on start** (already in the workspace) | Basic |
| **serial write line** "…" | Advanced → Serial |
| **on button** A **pressed** | Input |
| **serial on data received** "new line", with **serial read until** "new line" | Advanced → Serial |
| **show icon** | Basic |

A **traffic light** (keys in Italian, as in the Italian lessons): A turns it green, B red.

```
on start
  serial write line "programma:semaforo"
  show icon ✗
  serial write line "semaforo:0"

on button A pressed
  show icon ✓
  serial write line "semaforo:1"

on button B pressed
  show icon ✗
  serial write line "semaforo:0"
```

Then **Download** and drag the `.hex` file onto the **MICROBIT** drive (the yellow light on the
back blinks while it loads). The same in JavaScript, in MakeCode's JavaScript tab:

```js
serial.writeLine("programma:semaforo")
basic.showIcon(IconNames.No)
serial.writeLine("semaforo:0")
input.onButtonPressed(Button.A, function () {
    basic.showIcon(IconNames.Yes)
    serial.writeLine("semaforo:1")
})
input.onButtonPressed(Button.B, function () {
    basic.showIcon(IconNames.No)
    serial.writeLine("semaforo:0")
})
```

To receive what Byte sends (e.g. `cuore:1`):

```js
serial.onDataReceived(serial.delimiters(Delimiters.NewLine), function () {
    let line = serial.readUntil(serial.delimiters(Delimiters.NewLine))
    if (line == "cuore:1") {
        basic.showIcon(IconNames.Heart)
    } else if (line == "cuore:0") {
        basic.clearScreen()
    }
})
```

> Load the program with the `.hex` file on the MICROBIT drive. Loading it straight from the
> browser (**Connect device**, WebUSB) hasn't been tried together with MINE yet.

### micro:bit with MicroPython

```python
from microbit import *

print("programma:pulsanti")
while True:
    if button_a.was_pressed():
        print("pulsante_a:1")
    if button_b.was_pressed():
        print("pulsante_b:1")
    sleep(50)
```

### Raspberry Pi Pico with MicroPython

Tested with a Pico and MicroPython 1.15 and 1.29. Every second it sends the temperature of its chip;
`led:1` / `led:0` switch the LED on the board. Save it on the Pico as `mine_prova.py` (Thonny:
*File → Save as → Raspberry Pi Pico*), and as `main.py` a single line, `import mine_prova`, so it
starts by itself whenever the Pico is plugged in.

```python
# MINE: a Raspberry Pi Pico that talks with Byte, one line key:value at a time.
# Every second it sends its temperature (temperatura:NN); led:1 / led:0 switch its LED.
import machine
import select
import sys
import time

led = machine.Pin(25, machine.Pin.OUT)
sensor = machine.ADC(4)  # the temperature sensor inside the RP2040
poll = select.poll()
poll.register(sys.stdin, select.POLLIN)

print("programma:pico_prova")
last = time.ticks_ms()
line = ""
while True:
    if time.ticks_diff(time.ticks_ms(), last) >= 1000:
        last = time.ticks_ms()
        volts = sensor.read_u16() * 3.3 / 65535
        print("temperatura:%d" % round(27 - (volts - 0.706) / 0.001721))
    if poll.poll(20):
        char = sys.stdin.read(1)
        if char in "\r\n":
            if line.startswith("led:"):
                try:
                    led.value(1 if int(line[4:]) > 0 else 0)
                except ValueError:
                    pass
            line = ""
        else:
            line += char
```

- The Pico doesn't restart when MINE opens its port (the micro:bit does): its `programma:` line,
  written at power-on, may not show in `/mine device`. Everything else works the same.
- The chip's temperature is approximate (a few degrees off): good to see it rise when you blow
  on it, not as a thermometer.
- Close Thonny's connection to the Pico (or `/mine device`'s) before using the other: one
  program at a time can use the port.

### Raspberry Pi Pico 2 W over Wi-Fi

A Pico 2 W (or Pico W) talks to the MINE server by itself, with no cable to the PC: it only needs
power, even from a power bank. Tested with a Pico 2 W and MicroPython 1.29. The lines are the same
`key:value` as over the cable, and reach only the Byte of the student whose code it has.

1. Install MicroPython for the **Pico 2 W** (hold BOOTSEL while plugging it in and copy the `.uf2`
   file to the RP2350 drive).
2. Download `mine_wifi.py` from the server, `http://<server address>:25580/python/pico/mine_wifi.py`,
   and save it on the Pico (Thonny: *File → Save as → Raspberry Pi Pico*).
3. Save on the Pico a file `mine.txt`:

   ```
   wifi=the network's name
   password=its password
   code=the student's code (in the game: /mine python)
   ```

   It finds the server by itself on the school's network (like the Multiplayer list); if it
   can't, add `server=192.168.1.20`. `lang=it` gives its messages in Italian.
4. Save the program as `mine_prova_wifi.py` and, as `main.py`, the line `import mine_prova_wifi`.

```python
# MINE: a Raspberry Pi Pico 2 W that talks with Byte over Wi-Fi, no cable to the PC.
# Every second it sends its temperature (temperatura:NN); led:1 / led:0 switch its LED.
import time

import mine_wifi as mine
from machine import ADC, Pin

led = Pin("LED", Pin.OUT)
sensor = ADC(4)  # the temperature sensor inside the chip

mine.connect()
mine.send("programma", "pico_wifi_prova")
while True:
    volts = sensor.read_u16() * 3.3 / 65535
    mine.send("temperatura", round(27 - (volts - 0.706) / 0.001721))
    for key, value in mine.receive():
        if key == "led":
            led.value(1 if value else 0)
    time.sleep(1)
```

- The LED blinks while it gets on the Wi-Fi; then it's the program's. Its messages (`MINE: …`) show
  in Thonny when the Pico is plugged into the PC.
- The board talks to Byte only while the student is in the game: until then it waits, by itself.
- Call `mine.receive()` often, at least every few seconds: that's when the board asks the server
  for its lines. If it's silent for 10 seconds, MINE takes it as disconnected (and connects it
  again at the next ask).
- `/mine device` says in chat that the board is connected over Wi-Fi, and which program it runs.
- Before buying many, try one: some school Wi-Fi networks don't let devices talk to each other
  (client isolation). If the board can't find the server even with `server=`, ask the network
  technician.

### Arduino

A button on pin 2, an LED on pin 9:

```cpp
void setup() {
  Serial.begin(115200);
  pinMode(2, INPUT_PULLUP);
  pinMode(9, OUTPUT);
  Serial.println("programma:pulsante");
}

void loop() {
  static int last = -1;
  int pressed = digitalRead(2) == LOW ? 1 : 0;
  if (pressed != last) {           // only when it changes
    Serial.println(String("pulsante:") + pressed);
    last = pressed;
  }
  if (Serial.available()) {        // from Byte, e.g. "led:255"
    String line = Serial.readStringUntil('\n');
    if (line.startsWith("led:")) analogWrite(9, line.substring(4).toInt());
  }
}
```

## Byte's program

### With blocks

In the [block editor](blocks.md), the **Board** category:

| Block | What it does |
|---|---|
| **wait for** "pulsante_a" **from the board** | Byte stops until the board writes that key |
| **board value** "luce" | the last value that arrived for that key (0 until the board has written it) |
| **send to the board** "cuore" 1 | writes `cuore:1` to the board |
| **board connected?** | true if the student has connected a board with `/mine device` |

With the traffic light above, Byte only walks on green:

```
repeat while true
  if  board value "semaforo" = 1
    move forward
  wait 0.5 seconds
```

> Don't take out **wait 0.5 seconds** (in Loops): a loop that checks without ever stopping
> blocks Byte with the error "Too long without yielding". The **wait for … from the board**,
> **move** and **turn** blocks already wait by themselves.

Press A, Byte takes a step and the micro:bit shows a heart for a second:

```
repeat while true
  wait for "pulsante_a" from the board
  send to the board "cuore" 1
  move forward
  wait 1 seconds
  send to the board "cuore" 0
```

### In Lua

The same with `mine.device` (see the [Lua API](lua-api.md#minedevice-the-board)):

```lua
while true do
  local key, value = mine.device.wait()   -- the next line, any key
  if key == "pulsante_a" then
    mine.device.send("cuore", 1)
    byte.forward()
    mine.device.send("cuore", 0)
  end
end
```

## Without code: the board blocks

Two blocks, in the **Redstone** tab of the creative inventory, join the board and redstone with
no Byte program at all:

| Block | What it does |
|---|---|
| **Board Receiver** (green, a heart) | gives redstone when the board writes its key: `pulsante_a:1` switches it on, `pulsante_a:0` off. Numbers from 1 to 15 are the redstone's strength (beyond 15, 15); `on`/`off` work too |
| **Board Sender** (blue, an arrow) | when redstone reaches it, it writes `key:power` on the board: `led:15` with a lever next to it, `led:0` when the lever goes back |

When you place one, a window asks for the **key** (the same word the board's program uses). The
block belongs to **your** board: another student's board doesn't touch it. Right-click it to see
or change the key; others see it but can't change it.

- A board that only says "pressed" (MakeCode's *on button A pressed* writing `pulsante_a:1`):
  set the receiver to **Like a button**, and it stays on for one second each time.
- A light value from 0 to 255 is 15 for anything above 15: send `luce:1`/`luce:0` from the board
  (with an *if*), or let Byte decide (`mine.device.read("luce") > 100`).
- When the board connects, after a couple of seconds (boards restart) the senders tell it how they are.
- Teachers can give a block to a student: in its window, **Board of** with the student's name
  (they must be online). This is how a teacher's world can have a door for each student's micro:bit.

Examples: a door that opens with button A (receiver *Like a button* under the door); a lever that
lights the micro:bit's heart (sender next to the lever, and on the board *if `led` is 15, show a
heart*); a daylight sensor next to a sender, so the board knows when it's night in the game.

## During a lesson

[Lesson 8](../lessons/08-board/README.md) is the lesson with the board: a door, a night light and
a heart, with the program for the micro:bit ready to copy.

If the student has a lesson in progress, MINE tries **every** program that starts against the
lesson's step, and stops it when it has seen enough ("Not yet…"): a board program too, even if it
has nothing to do with the step. To try boards outside lessons, the teacher resets the student's
progress with `/mine lesson reset <student>`.

## If it doesn't work

| What happens | What to do |
|---|---|
| The window says "No board found" | The cable is charging-only, or the board isn't plugged in well: try another cable or USB port, then **Look again**. |
| "Can't open /dev/ttyACM0" (Linux) | The PC's user must be in the `dialout` group (`uucp` on some distributions): `sudo usermod -aG dialout <user>`, then log out and in again. This also applies to Prism Launcher from Flatpak. |
| "Can't open COM3" (Windows) | Another program is using the port (the Arduino IDE with its serial monitor open, MakeCode connected with WebUSB): close it. |
| The board is connected but nothing arrives | The board's program doesn't write on the serial port, or at another speed: it must be **115200 baud** (MakeCode already is). |
| Lines arrive grey, "ignored" | They aren't `key:value`: the colon is missing, or the key has spaces or accented letters. |
| Byte doesn't react | Byte's program isn't running (run it again with **R**), or the key is spelt differently in the two programs (`pulsante_a` and `pulsanteA` are different keys). |
| "Not yet…" and the program stops | A lesson is in progress: see above. |
