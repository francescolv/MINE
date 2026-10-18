# Lesson 13, step 2: on at 28 or more, off at 24 or less, and in between it stays as it was.
from mine import redstone
import mine

on = False
while True:
    key, temperature = mine.device.wait("temperatura")
    if temperature >= 28:
        on = True
    elif temperature <= 24:
        on = False
    redstone.setOutput("back", on)
