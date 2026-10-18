-- Lesson 2, activity 3: a solid cube 3 x 3 x 3.
-- Volume = 3 x 3 x 3 = 27: put at least 27 blocks in Byte's highlighted slot.
for layer = 1, 3 do
  byte.up()
  -- one 3 x 3 floor, like in activity 2
  for row = 1, 3 do
    for i = 1, 3 do
      byte.placeDown()
      byte.forward()
    end
    for i = 1, 3 do
      byte.back()
    end
    byte.turnRight()
    byte.forward()
    byte.turnLeft()
  end
  -- back to the first row, ready for the next layer
  byte.turnLeft()
  for i = 1, 3 do
    byte.forward()
  end
  byte.turnRight()
end

mine.say("Cube done: 27 blocks!")
