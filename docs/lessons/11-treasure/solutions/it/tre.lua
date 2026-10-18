-- Lezione 11, tappa 3: tre tesori. Byte ricorda dov'è in due variabili, e una funzione lo
-- porta ovunque: a avanti e d a destra della partenza.
local a, d = 0, 0

local function vaiA(versoA, versoD)
  while a < versoA do byte.forward() a = a + 1 end
  while a > versoA do byte.back() a = a - 1 end
  byte.turnRight()
  while d < versoD do byte.forward() d = d + 1 end
  while d > versoD do byte.back() d = d - 1 end
  byte.turnLeft()
end

for i = 1, 3 do
  vaiA(mine.challenge(2 * i - 1), mine.challenge(2 * i))
  byte.digDown()
end
vaiA(0, 0)
mine.say("Tre tesori, e sono a casa!")
