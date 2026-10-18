-- Lezione 3, tappa 2, corretto: mancava l'end del ciclo interno che va avanti.
byte.up()
for fila = 1, 3 do
  for i = 1, 4 do
    byte.placeDown()
    byte.forward()
  end
  for i = 1, 4 do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end

mine.say("Pavimento finito!")
