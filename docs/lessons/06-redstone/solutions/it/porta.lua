-- Lezione 6, attività 3: la porta segue la leva di sinistra.
-- Il programma ascolta per sempre: fermalo con Ctrl+T (nella lezione lo ferma MINE dopo la prova).
while true do
  redstone.setOutput("front", redstone.getInput("left"))
  os.pullEvent("redstone") -- aspetta che un segnale di redstone cambi
end
