-- Lesson 5, step 3: a step pyramid made with a function.

-- Builds a solid square under Byte and brings it
-- back to the corner where it started.
function square(side)
  for row = 1, side do
    for i = 1, side do
      byte.placeDown()
      byte.forward()
    end
    for i = 1, side do
      byte.back()
    end
    byte.turnRight()
    byte.forward()
    byte.turnLeft()
  end
  byte.turnLeft()
  for i = 1, side do
    byte.forward()
  end
  byte.turnRight()
end

-- Up one layer, and one block inwards.
function nextLayer()
  byte.up()
  byte.forward()
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end

byte.up()
square(5)
nextLayer()
square(3)
nextLayer()
square(1)

mine.say("Pyramid done: 25 + 9 + 1 = 35 blocks!")
