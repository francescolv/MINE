-- Lezione 2, attività 3: un cubo pieno 3 x 3 x 3.
-- Volume = 3 x 3 x 3 = 27: metti almeno 27 blocchi nello slot evidenziato di Byte.
for strato = 1, 3 do
  byte.up()
  -- un pavimento 3 x 3, come nell'attività 2
  for fila = 1, 3 do
    for i = 1, 3 do
      byte.placeDown()
      byte.forward()
    end
    for i = 1, 3 do
      byte.back()
    end
    byte.turnRight()
    byte.forward()
    byte.turnLeft()
  end
  -- torna alla prima fila, pronto per lo strato successivo
  byte.turnLeft()
  for i = 1, 3 do
    byte.forward()
  end
  byte.turnRight()
end

mine.say("Cubo finito: 27 blocchi!")
