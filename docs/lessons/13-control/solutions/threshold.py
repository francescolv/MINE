# Lesson 13, step 1: the fan (the lamp behind Byte) is on from 28 degrees up.
from mine import redstone
import mine

while True:
    key, temperature = mine.device.wait("temperatura")
    redstone.setOutput("back", temperature >= 28)
