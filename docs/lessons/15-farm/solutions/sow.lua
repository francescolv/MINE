-- Lesson 15, step 1: sow the field, 3 rows of 5. Byte flies one block above it.
byte.up()
for row = 1, 3 do
  for i = 1, 5 do
    byte.forward()
    byte.placeDown()
  end
  -- back to the start of the row, then on to the next one
  for i = 1, 5 do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end
mine.say("All sown!")
