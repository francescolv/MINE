# Lesson 13, step 3: button A switches manual mode on and off; in manual mode the fan is on.
from mine import redstone
import mine

on = False
manual = False
while True:
    key, value = mine.device.wait()  # any key: the temperature or the button
    if key == "pulsante_a":
        manual = not manual
    elif key == "temperatura":
        if value >= 28:
            on = True
        elif value <= 24:
            on = False
    redstone.setOutput("back", on or manual)
