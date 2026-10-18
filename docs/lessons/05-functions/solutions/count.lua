-- Lesson 5, step 1: count the steps to the wall.
local steps = 0
while not byte.detect() do
  byte.forward()
  steps = steps + 1
end

mine.say("I took " .. steps .. " steps")
