-- Lezione 16, tappa 3: niente chiave, ma nel messaggio c'è BYTE. Prova tutte le 25 chiavi.
local function decifra(testo, chiave)
  local chiaro = ""
  for i = 1, #testo do
    local c = testo:sub(i, i)
    if c >= "A" and c <= "Z" then
      c = string.char((c:byte() - 65 - chiave) % 26 + 65)
    end
    chiaro = chiaro .. c
  end
  return chiaro
end

local segreto = mine.challenge()
for chiave = 1, 25 do
  local chiaro = decifra(segreto, chiave)
  if chiaro:find("BYTE") then
    mine.say("La chiave è " .. chiave .. ": " .. chiaro)
    break
  end
end
