-- Lesson 10, step 2: out of any maze, with a hand on the wall to the right.
-- Turn right; while there's a wall (not the gold), turn left; then one step.
while not byte.compare() do
  byte.turnRight()
  while byte.detect() and not byte.compare() do
    byte.turnLeft()
  end
  if not byte.compare() then
    byte.forward()
  end
end
mine.say("Out of the maze!")
