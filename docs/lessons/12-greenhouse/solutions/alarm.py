# Lesson 12, step 2: the alarm. Above 30 degrees the Pico's LED goes on (allarme:1).
import mine

while True:
    key, temperature = mine.device.wait("temperatura")
    if temperature > 30:
        mine.device.send("allarme", 1)
    else:
        mine.device.send("allarme", 0)
