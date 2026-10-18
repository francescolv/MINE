-- Lezione 4, tappa 3: una torre con tutti i blocchi.
-- Funziona con qualunque numero di blocchi nello slot evidenziato.
while byte.place() do
  byte.up()
end
-- blocchi finiti: scendi finché Byte tocca terra
while byte.down() do
end

mine.say("Torre finita!")
