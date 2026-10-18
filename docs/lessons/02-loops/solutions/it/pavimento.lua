-- Lezione 2, attività 2: un pavimento lungo 5 blocchi e largo 4.
-- Area = 5 x 4 = 20: metti almeno 20 blocchi nello slot evidenziato di Byte.
byte.up()
for fila = 1, 4 do
  -- una fila di 5 blocchi
  for i = 1, 5 do
    byte.placeDown()
    byte.forward()
  end
  -- torna all'inizio della fila, poi un passo a destra
  for i = 1, 5 do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end

mine.say("Pavimento finito: 20 blocchi!")
