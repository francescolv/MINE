-- Lezione 8, attività 2: il pulsante A della scheda apre la porta per 3 secondi.
-- Il programma ascolta per sempre: fermalo con Ctrl+T (nella lezione lo ferma MINE dopo la prova).
redstone.setOutput("front", false)
while true do
  mine.device.wait("pulsante_a") -- aspetta che la scheda scriva pulsante_a
  redstone.setOutput("front", true)
  os.sleep(3)
  redstone.setOutput("front", false)
end
