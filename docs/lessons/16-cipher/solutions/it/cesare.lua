-- Lezione 16, tappa 1: il cifrario di Cesare. Ogni lettera è stata spostata di 3 posti avanti: riportala indietro.
local function decifra(testo, chiave)
  local chiaro = ""
  for i = 1, #testo do
    local c = testo:sub(i, i)
    if c >= "A" and c <= "Z" then
      -- A è 65: da 0 a 25, indietro di chiave, il giro dell'alfabeto con %, e di nuovo una lettera
      c = string.char((c:byte() - 65 - chiave) % 26 + 65)
    end
    chiaro = chiaro .. c
  end
  return chiaro
end

local segreto = mine.challenge()
mine.say(decifra(segreto, 3))
