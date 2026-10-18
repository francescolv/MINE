-- Lezione 15, tappa 2: raccogli solo il grano maturo, e risemina dove hai raccolto.
byte.up()
for fila = 1, 3 do
  for i = 1, 5 do
    byte.forward()
    if byte.ripeDown() then
      byte.digDown()
      byte.placeDown()
    end
  end
  for i = 1, 5 do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end
mine.say("Raccolto fatto!")
