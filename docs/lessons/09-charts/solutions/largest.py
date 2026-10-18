# Lesson 9, activity 3: the largest number, and where it is in the list.
import mine

numbers = []
for i in range(5):
    key, value = mine.device.wait("dato")
    numbers.append(value)

top = max(numbers)
position = numbers.index(top) + 1  # lists count from 0, people from 1
mine.say("the largest is " + str(top) + ", number " + str(position))
