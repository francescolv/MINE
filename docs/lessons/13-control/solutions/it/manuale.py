# Lezione 13, tappa 3: il pulsante A accende e spegne il modo manuale; in manuale la ventola è accesa.
from mine import redstone
import mine

accesa = False
manuale = False
while True:
    chiave, valore = mine.device.wait()  # qualunque chiave: la temperatura o il pulsante
    if chiave == "pulsante_a":
        manuale = not manuale
    elif chiave == "temperatura":
        if valore >= 28:
            accesa = True
        elif valore <= 24:
            accesa = False
    redstone.setOutput("back", accesa or manuale)
