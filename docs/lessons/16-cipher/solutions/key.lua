-- Lesson 16, step 2: now MINE gives the key too, a different one every time.
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

local secret, key = mine.challenge()
mine.say(decode(secret, key))
