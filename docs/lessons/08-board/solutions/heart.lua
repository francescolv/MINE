-- Lesson 8, activity 4: the lever on Byte's left lights the heart on the board.
-- The program listens forever: stop it with Ctrl+T (in the lesson, MINE stops it after its test).
while true do
  os.pullEvent("redstone") -- waits until a redstone signal changes
  if redstone.getInput("left") then
    mine.device.send("cuore", 1)
  else
    mine.device.send("cuore", 0)
  end
end
