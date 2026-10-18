-- Lesson 2, activity 1: a row of 10 blocks.
-- Before running it, put at least 10 blocks in Byte's highlighted slot.
byte.up()
for i = 1, 10 do
  byte.placeDown()
  byte.forward()
end

mine.say("Row done!")
