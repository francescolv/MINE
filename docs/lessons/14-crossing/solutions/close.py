# Lesson 14, step 1: open to begin with; a train from the Arduino (treno) closes the crossing.
from mine import redstone
import mine


def crossing(closed):
    redstone.setOutput("front", not closed)  # the barrier: the door is open while it gets redstone
    redstone.setOutput("back", closed)  # the light


crossing(False)
while True:
    mine.device.wait("treno")
    crossing(True)
