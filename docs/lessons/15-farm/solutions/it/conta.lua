-- Lezione 15, tappa 3: lo stesso raccolto, contando il grano maturo.
local raccolti = 0
byte.up()
for fila = 1, 3 do
  for i = 1, 5 do
    byte.forward()
    if byte.ripeDown() then
      byte.digDown()
      byte.placeDown()
      raccolti = raccolti + 1
    end
  end
  for i = 1, 5 do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end
mine.say("Ho raccolto " .. raccolti .. " spighe")
