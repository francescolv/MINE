-- Lesson 6, activity 3: the door follows the left lever.
-- The program listens forever: stop it with Ctrl+T (in the lesson, MINE stops it after its test).
while true do
  redstone.setOutput("front", redstone.getInput("left"))
  os.pullEvent("redstone") -- waits until a redstone signal changes
end
