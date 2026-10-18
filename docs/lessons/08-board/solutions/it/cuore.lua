-- Lezione 8, attività 4: la leva alla sinistra di Byte accende il cuore sulla scheda.
-- Il programma ascolta per sempre: fermalo con Ctrl+T (nella lezione lo ferma MINE dopo la prova).
while true do
  os.pullEvent("redstone") -- aspetta che un segnale di redstone cambi
  if redstone.getInput("left") then
    mine.device.send("cuore", 1)
  else
    mine.device.send("cuore", 0)
  end
end
