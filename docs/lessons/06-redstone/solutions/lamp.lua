-- Lesson 6, activity 1: the lamp on top of Byte.
-- redstone.setOutput sends a signal from one side of Byte: "back" is the lamp, behind Byte.
redstone.setOutput("back", true)
mine.say("Light on!")
