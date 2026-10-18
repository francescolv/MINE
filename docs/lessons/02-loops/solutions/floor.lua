-- Lesson 2, activity 2: a floor 5 blocks long and 4 wide.
-- Area = 5 x 4 = 20: put at least 20 blocks in Byte's highlighted slot.
byte.up()
for row = 1, 4 do
  -- one row of 5 blocks
  for i = 1, 5 do
    byte.placeDown()
    byte.forward()
  end
  -- back to the start of the row, then one step to the right
  for i = 1, 5 do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end

mine.say("Floor done: 20 blocks!")
