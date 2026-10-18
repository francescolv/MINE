-- Floor 4 x 3: Lua won't start it!
byte.up()
for row = 1, 3 do
  for i = 1, 4 do
    byte.placeDown()
    byte.forward()

  for i = 1, 4 do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end

mine.say("Floor done!")
