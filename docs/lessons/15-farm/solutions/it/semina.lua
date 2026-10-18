-- Lezione 15, tappa 1: semina il campo, 3 file da 5. Byte vola un blocco sopra.
byte.up()
for fila = 1, 3 do
  for i = 1, 5 do
    byte.forward()
    byte.placeDown()
  end
  -- indietro all'inizio della fila, poi alla fila dopo
  for i = 1, 5 do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end
mine.say("Tutto seminato!")
