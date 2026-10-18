-- Quadrato 4x4: nessun errore. Vero?
byte.up()
for fila = 1, 4 do
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

mine.say("Quadrato finito!")
