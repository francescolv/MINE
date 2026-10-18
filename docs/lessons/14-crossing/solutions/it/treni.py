# Lezione 14, tappa 3: una macchina a stati. Byte conta i treni tra le sbarre: si riapre solo
# quando sono passati tutti. Il LED dell'Arduino è acceso finché è chiuso.
from mine import redstone
import mine
import os

treni = 0


def mostra():
    chiuso = treni > 0
    redstone.setOutput("front", not chiuso)
    redstone.setOutput("back", chiuso)
    mine.device.send("led", 1 if chiuso else 0)


mostra()
while True:
    evento = os.pullEvent()
    if evento[0] == "mine_device" and evento[1] == "treno":
        treni = treni + 1
        mostra()
    elif evento[0] == "redstone" and redstone.getInput("left") and treni > 0:
        treni = treni - 1
        mostra()
