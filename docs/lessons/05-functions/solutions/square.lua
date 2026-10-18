-- Lesson 5, step 2: a square as big as MINE asks.
local side = mine.challenge()
mine.say("Side: " .. side)

byte.up()
for row = 1, side do
  for i = 1, side do
    byte.placeDown()
    byte.forward()
  end
  for i = 1, side do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end
