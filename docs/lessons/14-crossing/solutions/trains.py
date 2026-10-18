# Lesson 14, step 3: a state machine. Byte counts the trains between the barriers: it opens only
# when they have all passed. The Arduino's LED is on while it's closed.
from mine import redstone
import mine
import os

trains = 0


def show():
    closed = trains > 0
    redstone.setOutput("front", not closed)
    redstone.setOutput("back", closed)
    mine.device.send("led", 1 if closed else 0)


show()
while True:
    event = os.pullEvent()
    if event[0] == "mine_device" and event[1] == "treno":
        trains = trains + 1
        show()
    elif event[0] == "redstone" and redstone.getInput("left") and trains > 0:
        trains = trains - 1
        show()
