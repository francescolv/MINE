-- Lesson 7, activity 3: each press of the button moves the walkway out, or back in.
-- The button is on only for a moment: the variable "out" remembers where the walkway is.
local out = false
redstone.setOutput("front", out)
while true do
  os.pullEvent("redstone")
  if redstone.getInput("right") then
    out = not out
    redstone.setOutput("front", out)
  end
end
