# Lezione 14, tappa 1: all'inizio aperto; un treno dall'Arduino (treno) chiude il passaggio a livello.
from mine import redstone
import mine


def passaggio(chiuso):
    redstone.setOutput("front", not chiuso)  # la sbarra: la porta è aperta finché riceve redstone
    redstone.setOutput("back", chiuso)  # la luce


passaggio(False)
while True:
    mine.device.wait("treno")
    passaggio(True)
