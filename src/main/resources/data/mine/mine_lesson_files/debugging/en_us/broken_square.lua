-- A 4 x 4 square: no errors. Right?
byte.up()
for row = 1, 4 do
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

mine.say("Square done!")
