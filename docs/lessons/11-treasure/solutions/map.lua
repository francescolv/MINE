-- Lesson 11, step 1: MINE says where the treasure is, counted from Byte: forward, then right.
local forward, right = mine.challenge()
mine.say("The treasure is " .. forward .. " forward and " .. right .. " to the right")
for i = 1, forward do
  byte.forward()
end
byte.turnRight()
for i = 1, right do
  byte.forward()
end
mine.say("X marks the spot!")
