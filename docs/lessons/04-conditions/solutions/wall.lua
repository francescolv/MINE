-- Lesson 4, step 1: over the wall.
-- MINE builds the wall at a different distance every time.
while not byte.detect() do
  byte.forward()
end
-- touching the wall: up, over it, and down behind it
byte.up()
byte.forward()
byte.forward()
byte.down()

mine.say("Over the wall!")
