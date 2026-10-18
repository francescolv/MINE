-- Lezione 11, tappa 1: MINE dice dov'è il tesoro, contando da Byte: avanti, poi a destra.
local avanti, destra = mine.challenge()
mine.say("Il tesoro è " .. avanti .. " avanti e " .. destra .. " a destra")
for i = 1, avanti do
  byte.forward()
end
byte.turnRight()
for i = 1, destra do
  byte.forward()
end
mine.say("Qui c'è la X!")
