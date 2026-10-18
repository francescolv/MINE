-- Una fila di 6 blocchi, ma Byte
-- si ferma con un errore: correggilo!
byte.up()
for i = 1, 6 do
  byte.placedown()
  byte.forward()
end

mine.say("Fila finita!")
