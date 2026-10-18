-- Lezione 4, tappa 1: oltre il muro.
-- MINE costruisce il muro ogni volta a una distanza diversa.
while not byte.detect() do
  byte.forward()
end
-- a contatto con il muro: sali, scavalcalo e scendi dietro
byte.up()
byte.forward()
byte.forward()
byte.down()

mine.say("Muro superato!")
