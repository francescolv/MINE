-- Lesson 10, step 1: along the corridor to the gold (a gold block is in the highlighted slot).
-- Forward while the way is free; at a corner, look right, and if it's a wall, the other way.
while not byte.compare() do
  if not byte.detect() then
    byte.forward()
  else
    byte.turnRight()
    if byte.detect() and not byte.compare() then
      byte.turnLeft()
      byte.turnLeft()
    end
  end
end
mine.say("Gold!")
