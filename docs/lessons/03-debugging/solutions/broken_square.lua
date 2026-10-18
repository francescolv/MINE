-- Lesson 3, step 3, fixed: both loops of a row must repeat 4 times, not 3.
byte.up()
for row = 1, 4 do
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

mine.say("Square done!")
