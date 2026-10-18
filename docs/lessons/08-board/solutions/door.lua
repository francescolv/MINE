-- Lesson 8, activity 2: button A on the board opens the door for 3 seconds.
-- The program listens forever: stop it with Ctrl+T (in the lesson, MINE stops it after its test).
redstone.setOutput("front", false)
while true do
  mine.device.wait("pulsante_a") -- waits until the board writes pulsante_a
  redstone.setOutput("front", true)
  os.sleep(3)
  redstone.setOutput("front", false)
end
