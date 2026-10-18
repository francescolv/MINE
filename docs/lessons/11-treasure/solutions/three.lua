-- Lesson 11, step 3: three treasures. Byte remembers where it is in two variables, and a
-- function takes it anywhere: forward f and right r of the start.
local f, r = 0, 0

local function goTo(toF, toR)
  while f < toF do byte.forward() f = f + 1 end
  while f > toF do byte.back() f = f - 1 end
  byte.turnRight()
  while r < toR do byte.forward() r = r + 1 end
  while r > toR do byte.back() r = r - 1 end
  byte.turnLeft()
end

for i = 1, 3 do
  goTo(mine.challenge(2 * i - 1), mine.challenge(2 * i))
  byte.digDown()
end
goTo(0, 0)
mine.say("Three treasures, and back home!")
