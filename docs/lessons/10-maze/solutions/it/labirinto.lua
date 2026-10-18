-- Lezione 10, tappa 2: fuori da qualunque labirinto, con una mano sul muro a destra.
-- Gira a destra; finché c'è un muro (non l'oro), gira a sinistra; poi un passo.
while not byte.compare() do
  byte.turnRight()
  while byte.detect() and not byte.compare() do
    byte.turnLeft()
  end
  if not byte.compare() then
    byte.forward()
  end
end
mine.say("Fuori dal labirinto!")
