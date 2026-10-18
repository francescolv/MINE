-- Lezione 5, tappa 3: una piramide a gradoni fatta con una funzione.

-- Costruisce un quadrato pieno sotto Byte e lo riporta
-- all'angolo da cui è partito.
function quadrato(lato)
  for fila = 1, lato do
    for i = 1, lato do
      byte.placeDown()
      byte.forward()
    end
    for i = 1, lato do
      byte.back()
    end
    byte.turnRight()
    byte.forward()
    byte.turnLeft()
  end
  byte.turnLeft()
  for i = 1, lato do
    byte.forward()
  end
  byte.turnRight()
end

-- Su di uno strato, e un blocco verso l'interno.
function stratoSuccessivo()
  byte.up()
  byte.forward()
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end

byte.up()
quadrato(5)
stratoSuccessivo()
quadrato(3)
stratoSuccessivo()
quadrato(1)

mine.say("Piramide finita: 25 + 9 + 1 = 35 blocchi!")
