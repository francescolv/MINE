-- Lezione 8, attività 3: la lampada è accesa quando la scheda è al buio (luce sotto 50).
-- Il programma ascolta per sempre: fermalo con Ctrl+T (nella lezione lo ferma MINE dopo la prova).
while true do
  redstone.setOutput("back", mine.device.read("luce") < 50)
  os.sleep(0.5) -- guarda di nuovo tra mezzo secondo
end
