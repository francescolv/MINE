-- Lesson 7, activity 2: a night light.
-- The sensor on top sends a signal when it's light: the lamp behind is the opposite.
while true do
  redstone.setOutput("back", not redstone.getInput("top"))
  os.pullEvent("redstone")
end
