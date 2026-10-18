-- Lesson 6, activity 4: the lamp is on only when both levers are on (AND).
while true do
  local both = redstone.getInput("left") and redstone.getInput("right")
  redstone.setOutput("back", both)
  os.pullEvent("redstone")
end
