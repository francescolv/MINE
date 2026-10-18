-- Lezione 7, attività 3: ogni pressione del pulsante fa uscire la passerella, o la fa rientrare.
-- Il pulsante resta acceso solo un momento: la variabile "fuori" ricorda dov'è la passerella.
local fuori = false
redstone.setOutput("front", fuori)
while true do
  os.pullEvent("redstone")
  if redstone.getInput("right") then
    fuori = not fuori
    redstone.setOutput("front", fuori)
  end
end
