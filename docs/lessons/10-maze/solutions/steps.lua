-- Lesson 10, step 3: the same way out, counting the steps in a variable.
local steps = 0
while not byte.compare() do
  byte.turnRight()
  while byte.detect() and not byte.compare() do
    byte.turnLeft()
  end
  if not byte.compare() then
    byte.forward()
    steps = steps + 1
  end
end
mine.say("Out of the maze in " .. steps .. " steps!")
