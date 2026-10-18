-- Lesson 8, activity 3: the lamp is on when the board is in the dark (light below 50).
-- The program listens forever: stop it with Ctrl+T (in the lesson, MINE stops it after its test).
while true do
  redstone.setOutput("back", mine.device.read("luce") < 50)
  os.sleep(0.5) -- look again in half a second
end
