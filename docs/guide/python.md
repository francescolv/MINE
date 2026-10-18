**English** · [Italiano](python.it.md)

# Python: programming Byte in Python

Byte runs Lua, and with blocks students write Lua without seeing it. Older students can write
**Python**: real Python (MicroPython, the same that runs on the micro:bit and the Raspberry Pi
Pico), with the **same names** as in Lua and in the lessons: `byte.forward()`, `redstone.getInput("left")`,
`mine.say("Hello!")`, `mine.device.wait("pulsante_a")`.

There are two ways:

- **in the browser**, in MINE's Python editor: nothing to install;
- **in [Thonny](#thonny)** on the PC, with the full Python, for those who want a real development
  environment.

## The Python editor

In the game type `/mine blocks`: the block editor opens. The **Python** button at the top opens
the Python editor, with the same session.

```python
from mine import byte
import mine

mine.say("Hello from Python!")
for i in range(3):
    byte.up()
    byte.placeDown()
    byte.forward()
```

| | |
|---|---|
| **My programs** | the student's Python programs, kept on the server (as the block ones) |
| **Save** | keeps the program without running it |
| **Run ▶** | sends the program to Byte; go back to Minecraft: after 3-2-1 it starts |
| **Stop ■** | stops Byte and the program |
| **Blocks** | back to the block editor |
| **Thonny…** | how to use Python on the PC with Thonny, the student's code and **Download mine.py** (see below) |
| **Students' programs** | teachers only: open a student's program, copy it to theirs |

On the right, what the program writes with `print`, and the errors: the line is highlighted, and
under Python's message MINE explains what it usually means ("Python doesn't know «x»: is it spelt
right?"). The lesson bar shows the step's goal and its hints, in Python's words when the lesson
has them.

In the block editor the code pane has **Lua | Python**: the same blocks in Python, and
**Continue in Python ▸** turns them into a new Python program, to carry on writing by hand.

## How it works

The Python program runs **in the browser**, with MicroPython. Each call to Byte (`byte.forward()`,
`redstone.getInput("left")`…) goes to the MINE server, which has Byte run it through a small Lua
program, the bridge, with the program's name: it is Byte's program like any other, so **lessons
check it**, Ctrl+T stops it, the **R** key runs it again (only with the editor open: the program
is in the browser). Keep the editor open while the program runs.

- `time.sleep(3)` and `os.sleep(3)` wait 3 seconds of the game, as in Lua.
- `os.pullEvent("redstone")` waits for a redstone change, as in Lua. Without an argument it waits
  for any event and always gives a tuple: `event[0]` is its name (`"redstone"`, `"mine_device"`…),
  as in [lesson 14](../lessons/14-crossing/README.md).
- `byte.forward()` gives `True` or `False` (without the reason Lua also gives).
- `mine.device.wait("dato")` gives two values: `key, value = mine.device.wait("dato")`.
- Python can only call Byte's functions: `byte`, `redstone`, `mine`, `mine.device`, the waits.

## Thonny

[Thonny](https://thonny.org) is a Python editor for schools (it's also used for the micro:bit and
the Pico): `sudo dnf install thonny` (Fedora) or `sudo apt install thonny` (Ubuntu, Debian).

1. **A folder for the Python programs** on the PC, e.g. `MINE-Python` in the home folder.
2. **`mine.py` in that folder**: in MINE's Python editor click **Thonny…**, then **Download
   mine.py**, and move it from *Downloads* to the folder. It's the same file for every student:
   in a lab whose PCs are restored at every start, put the folder with `mine.py` in the image.
3. **The programs in the same folder**: in Thonny write the program (`from mine import byte` …),
   save it there (e.g. `scale.py`) and press **F5**.
4. **The first time**, `mine.py` finds the school's server by itself (MINE announces it on the
   network, as for Minecraft's list) and asks for the student's **code**: six letters and digits,
   the student's for good, shown with `/mine python` in the game and in the **Thonny…** window.
   Both are remembered in `mine.txt`, in the folder. If the server isn't found (a network that
   blocks the announcement), `mine.py` asks for its address.
5. **Back to Minecraft**: after 3-2-1 Byte starts. Thonny's shell shows the `print`s and the errors.

The student must be in the game while the program runs (Byte listens while they play); the
program is kept with their Python programs in the editor too. A wrong code is asked again; on a
PC shared by several students, each one has their own folder, with their own `mine.txt`.

## For developers

- The browser side: `src/main/resources/web/python/` (`python.html`, `python.js`, the worker
  `python-worker.js`, `mine.py`; MicroPython 1.29, MIT); Thonny's module `web/python/thonny/mine.py` (the codes: `blocks/ThonnyKeys.java`, `data/mine/thonny.dat`).
- The bridge: `web/python/bridge.lua` and `agent/PythonBridge.java`; the editor's server
  `blocks/BlockEditor.java` (`/api/py/run|call|end|stop|save`).
- `node tools/python-run.mjs <server> <code> <name> <program.py>` runs a program the editor's way,
  without a browser: the GameTests use it for the Python solutions of the lessons.
