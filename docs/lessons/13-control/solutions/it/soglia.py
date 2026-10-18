# Lezione 13, tappa 1: la ventola (la lampada dietro Byte) è accesa da 28 gradi in su.
from mine import redstone
import mine

while True:
    chiave, temperatura = mine.device.wait("temperatura")
    redstone.setOutput("back", temperatura >= 28)
