# Lezione 12, tappa 2: l'allarme. Sopra i 30 gradi si accende il LED del Pico (allarme:1).
import mine

while True:
    chiave, temperatura = mine.device.wait("temperatura")
    if temperatura > 30:
        mine.device.send("allarme", 1)
    else:
        mine.device.send("allarme", 0)
