# Lesson 9, activity 4: 5 light readings (0 to 255), as columns of 8 blocks at most.
from mine import byte
import mine


def column(h):
    """A column h blocks tall in front of Byte; Byte back on the ground after."""
    for i in range(h):
        byte.place()
        byte.up()
    for i in range(h):
        byte.down()


def next_column():
    byte.turnRight()
    byte.forward()
    byte.turnLeft()


readings = []
for i in range(5):
    key, value = mine.device.wait("luce")
    readings.append(value)

for value in readings:
    column(value * 8 // 255)  # // divides and drops the decimals
    next_column()

# Back where Byte started.
byte.turnLeft()
for i in range(5):
    byte.forward()
byte.turnRight()
