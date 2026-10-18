-- Lezione 7, attività 1: la porta si apre quando qualcuno sale sulla piastra,
-- e si richiude da sola 3 secondi dopo.
-- Il programma ascolta per sempre: fermalo con Ctrl+T (nella lezione lo ferma MINE dopo la prova).
redstone.setOutput("front", false)
while true do
  os.pullEvent("redstone") -- aspetta che un segnale di redstone cambi
  if redstone.getInput("left") then
    redstone.setOutput("front", true)
    os.sleep(3)
    redstone.setOutput("front", false)
  end
end
