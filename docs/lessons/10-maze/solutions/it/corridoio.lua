-- Lezione 10, tappa 1: lungo il corridoio fino all'oro (un blocco d'oro è nello slot evidenziato).
-- Avanti finché la strada è libera; all'angolo guarda a destra, e se è un muro, dall'altra parte.
while not byte.compare() do
  if not byte.detect() then
    byte.forward()
  else
    byte.turnRight()
    if byte.detect() and not byte.compare() then
      byte.turnLeft()
      byte.turnLeft()
    end
  end
end
mine.say("Oro!")
