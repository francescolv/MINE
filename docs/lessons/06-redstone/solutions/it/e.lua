-- Lezione 6, attività 4: la lampada è accesa solo quando tutte e due le leve sono accese (E).
while true do
  local tutte_e_due = redstone.getInput("left") and redstone.getInput("right")
  redstone.setOutput("back", tutte_e_due)
  os.pullEvent("redstone")
end
