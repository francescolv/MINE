-- Lezione 10, tappa 3: la stessa strada, contando i passi in una variabile.
local passi = 0
while not byte.compare() do
  byte.turnRight()
  while byte.detect() and not byte.compare() do
    byte.turnLeft()
  end
  if not byte.compare() then
    byte.forward()
    passi = passi + 1
  end
end
mine.say("Fuori dal labirinto in " .. passi .. " passi!")
