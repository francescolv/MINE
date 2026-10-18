# Lesson 9, activity 2: 5 numbers from the board in a list, then a bar chart of them.
from mine import byte
import mine

numbers = []
for i in range(5):
    key, value = mine.device.wait("dato")  # waits for the board's next number
    numbers.append(value)
print("numbers:", numbers)

for n in numbers:
    for h in range(n):
        byte.place()
        byte.up()
    for h in range(n):
        byte.down()
    byte.turnRight()
    byte.forward()
    byte.turnLeft()

# Back where Byte started: the next chart has room.
byte.turnLeft()
for i in range(5):
    byte.forward()
byte.turnRight()
