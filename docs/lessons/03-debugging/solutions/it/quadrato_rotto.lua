-- Lezione 3, tappa 3, corretto: entrambi i cicli di una fila devono ripetere 4 volte, non 3.
byte.up()
for fila = 1, 4 do
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

mine.say("Quadrato finito!")
