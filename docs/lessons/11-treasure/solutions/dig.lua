-- Lesson 11, step 2: to the treasure, dig it up, and home again the same way, backwards.
local forward, right = mine.challenge()
for i = 1, forward do
  byte.forward()
end
byte.turnRight()
for i = 1, right do
  byte.forward()
end
byte.digDown()
for i = 1, right do
  byte.back()
end
byte.turnLeft()
for i = 1, forward do
  byte.back()
end
mine.say("Treasure found, and back home!")
