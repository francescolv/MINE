-- Lesson 16, step 1: Caesar's cipher. Every letter was moved 3 places forward: move it back.
local function decode(text, key)
  local plain = ""
  for i = 1, #text do
    local c = text:sub(i, i)
    if c >= "A" and c <= "Z" then
      -- A is 65: from 0 to 25, back by key, round the alphabet with %, and to a letter again
      c = string.char((c:byte() - 65 - key) % 26 + 65)
    end
    plain = plain .. c
  end
  return plain
end

local secret = mine.challenge()
mine.say(decode(secret, 3))
