-- Lezione 6, attività 2: la lampada lampeggia 5 volte.
for i = 1, 5 do
  redstone.setOutput("back", true)
  os.sleep(0.5)
  redstone.setOutput("back", false)
  os.sleep(0.5)
end
