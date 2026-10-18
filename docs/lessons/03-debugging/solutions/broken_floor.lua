-- Lesson 3, step 2, fixed: the inner loop that goes forward was missing its end.
byte.up()
for row = 1, 3 do
  for i = 1, 4 do
    byte.placeDown()
    byte.forward()
  end
  for i = 1, 4 do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end

mine.say("Floor done!")
