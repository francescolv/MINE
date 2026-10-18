-- Lesson 4, step 3: a tower with all the blocks.
-- It works with any number of blocks in the highlighted slot.
while byte.place() do
  byte.up()
end
-- no more blocks: back down until Byte touches the ground
while byte.down() do
end

mine.say("Tower done!")
