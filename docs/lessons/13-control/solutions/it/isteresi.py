# Lezione 13, tappa 2: si accende a 28 o più, si spegne a 24 o meno, e in mezzo resta com'era.
from mine import redstone
import mine

accesa = False
while True:
    chiave, temperatura = mine.device.wait("temperatura")
    if temperatura >= 28:
        accesa = True
    elif temperatura <= 24:
        accesa = False
    redstone.setOutput("back", accesa)
