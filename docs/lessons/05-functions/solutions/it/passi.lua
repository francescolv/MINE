-- Lezione 5, tappa 1: conta i passi fino al muro.
local passi = 0
while not byte.detect() do
  byte.forward()
  passi = passi + 1
end

mine.say("Ho fatto " .. passi .. " passi")
