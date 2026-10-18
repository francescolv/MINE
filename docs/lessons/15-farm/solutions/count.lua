-- Lesson 15, step 3: the same harvest, counting the ripe wheat.
local harvested = 0
byte.up()
for row = 1, 3 do
  for i = 1, 5 do
    byte.forward()
    if byte.ripeDown() then
      byte.digDown()
      byte.placeDown()
      harvested = harvested + 1
    end
  end
  for i = 1, 5 do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end
mine.say("I harvested " .. harvested .. " wheat")
