-- Lesson 16, step 3: no key, but the message has BYTE in it. Try all 25 keys.
local function decode(text, key)
  local plain = ""
  for i = 1, #text do
    local c = text:sub(i, i)
    if c >= "A" and c <= "Z" then
      c = string.char((c:byte() - 65 - key) % 26 + 65)
    end
    plain = plain .. c
  end
  return plain
end

local secret = mine.challenge()
for key = 1, 25 do
  local plain = decode(secret, key)
  if plain:find("BYTE") then
    mine.say("The key is " .. key .. ": " .. plain)
    break
  end
end
