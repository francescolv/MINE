-- Lesson 4, step 2: dig up only the gold.
-- Put one gold block in the highlighted slot first:
-- byte.compareDown() compares the block below with it.
byte.up()
for i = 1, 8 do
  byte.forward()
  if byte.compareDown() then
    byte.digDown()
  end
end

mine.say("All the gold is mine!")
