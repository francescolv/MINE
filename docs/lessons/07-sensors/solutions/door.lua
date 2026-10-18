-- Lesson 7, activity 1: the door opens when someone steps on the plate,
-- and closes by itself 3 seconds later.
-- The program listens forever: stop it with Ctrl+T (in the lesson, MINE stops it after its test).
redstone.setOutput("front", false)
while true do
  os.pullEvent("redstone") -- waits until a redstone signal changes
  if redstone.getInput("left") then
    redstone.setOutput("front", true)
    os.sleep(3)
    redstone.setOutput("front", false)
  end
end
