# Lesson 14, step 2: two kinds of events. A train closes the crossing; when it has passed (the
# lever on Byte's left), it opens again.
from mine import redstone
import mine
import os


def crossing(closed):
    redstone.setOutput("front", not closed)
    redstone.setOutput("back", closed)


crossing(False)
while True:
    event = os.pullEvent()  # whatever happens first: ("mine_device", key, value) or ("redstone",)
    if event[0] == "mine_device" and event[1] == "treno":
        crossing(True)
    elif event[0] == "redstone" and redstone.getInput("left"):
        crossing(False)
