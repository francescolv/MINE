# Lesson 1's tower, in Python: three blocks in front of Byte, then back to the ground.
from mine import byte
import mine

mine.say("A tower from Python")
for i in range(3):
    byte.place()
    byte.up()
for i in range(3):
    byte.down()
print("done", mine.version())
