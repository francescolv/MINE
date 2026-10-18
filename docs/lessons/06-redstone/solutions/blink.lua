-- Lesson 6, activity 2: the lamp blinks 5 times.
for i = 1, 5 do
  redstone.setOutput("back", true)
  os.sleep(0.5)
  redstone.setOutput("back", false)
  os.sleep(0.5)
end
