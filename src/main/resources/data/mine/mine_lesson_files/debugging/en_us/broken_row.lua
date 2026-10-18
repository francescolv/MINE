-- A row of 6 blocks, but Byte
-- stops with an error. Fix it!
byte.up()
for i = 1, 6 do
  byte.placedown()
  byte.forward()
end

mine.say("Row done!")
