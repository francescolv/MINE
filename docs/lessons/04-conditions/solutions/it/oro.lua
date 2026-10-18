-- Lezione 4, tappa 2: scava solo l'oro.
-- Prima metti un blocco d'oro nello slot evidenziato:
-- byte.compareDown() confronta con quello il blocco sotto.
byte.up()
for i = 1, 8 do
  byte.forward()
  if byte.compareDown() then
    byte.digDown()
  end
end

mine.say("Tutto l'oro è mio!")
