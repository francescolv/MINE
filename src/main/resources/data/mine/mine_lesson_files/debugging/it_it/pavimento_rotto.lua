-- Pavimento 4 x 3: Lua non lo avvia!
byte.up()
for fila = 1, 3 do
  for i = 1, 4 do
    byte.placeDown()
    byte.forward()

  for i = 1, 4 do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end

mine.say("Pavimento finito!")
