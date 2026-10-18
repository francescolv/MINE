# Lezione 14, tappa 2: due tipi di eventi. Un treno chiude il passaggio; quando è passato (la leva
# alla sinistra di Byte), si riapre.
from mine import redstone
import mine
import os


def passaggio(chiuso):
    redstone.setOutput("front", not chiuso)
    redstone.setOutput("back", chiuso)


passaggio(False)
while True:
    evento = os.pullEvent()  # quello che succede prima: ("mine_device", chiave, valore) o ("redstone",)
    if evento[0] == "mine_device" and evento[1] == "treno":
        passaggio(True)
    elif evento[0] == "redstone" and redstone.getInput("left"):
        passaggio(False)
