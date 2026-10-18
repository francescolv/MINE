# Lesson 12, step 3: every 5 temperatures, their average goes back to the Pico (media).
import mine

group = []
while True:
    key, temperature = mine.device.wait("temperatura")
    group.append(temperature)
    if len(group) == 5:
        average = round(sum(group) / len(group), 1)
        mine.device.send("media", average)
        print("average of", group, "=", average)
        group = []
