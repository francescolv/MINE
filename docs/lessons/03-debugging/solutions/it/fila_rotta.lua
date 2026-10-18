-- Lezione 3, tappa 1, corretto: placedown → placeDown (le maiuscole contano).
byte.up()
for i = 1, 6 do
  byte.placeDown()
  byte.forward()
end

mine.say("Fila finita!")
