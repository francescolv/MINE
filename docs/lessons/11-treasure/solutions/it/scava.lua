-- Lezione 11, tappa 2: fino al tesoro, scavalo, e a casa per la stessa strada, all'indietro.
local avanti, destra = mine.challenge()
for i = 1, avanti do
  byte.forward()
end
byte.turnRight()
for i = 1, destra do
  byte.forward()
end
byte.digDown()
for i = 1, destra do
  byte.back()
end
byte.turnLeft()
for i = 1, avanti do
  byte.back()
end
mine.say("Tesoro trovato, e sono a casa!")
