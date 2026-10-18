# Lesson 12, step 1: six temperatures from the greenhouse's Pico, and their average.
import mine

temperatures = []
for i in range(6):
    key, value = mine.device.wait("temperatura")
    temperatures.append(value)
print("temperatures:", temperatures)

average = sum(temperatures) / len(temperatures)
mine.say("Average temperature: " + str(round(average, 1)))
