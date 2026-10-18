-- Lezione 5, tappa 2: un quadrato grande quanto chiede MINE.
local lato = mine.challenge()
mine.say("Lato: " .. lato)

byte.up()
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
