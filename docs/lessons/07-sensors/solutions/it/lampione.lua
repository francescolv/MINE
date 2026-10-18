-- Lezione 7, attività 2: un lampione notturno.
-- Il sensore sopra manda un segnale quando c'è luce: la lampada dietro fa il contrario.
while true do
  redstone.setOutput("back", not redstone.getInput("top"))
  os.pullEvent("redstone")
end
