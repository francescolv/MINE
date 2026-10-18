-- Lesson 3, step 1, fixed: placedown → placeDown (capital letters matter).
byte.up()
for i = 1, 6 do
  byte.placeDown()
  byte.forward()
end

mine.say("Row done!")
