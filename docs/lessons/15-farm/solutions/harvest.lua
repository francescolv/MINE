-- Lesson 15, step 2: harvest only the ripe wheat, and plant again where Byte harvested.
byte.up()
for row = 1, 3 do
  for i = 1, 5 do
    byte.forward()
    if byte.ripeDown() then
      byte.digDown()
      byte.placeDown()
    end
  end
  for i = 1, 5 do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end
mine.say("Harvest done!")
