-- Lezione 2, attività 1: una fila di 10 blocchi.
-- Prima di avviarlo, metti almeno 10 blocchi nello slot evidenziato di Byte.
byte.up()
for i = 1, 10 do
  byte.placeDown()
  byte.forward()
end

mine.say("Fila finita!")
