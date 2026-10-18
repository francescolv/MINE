# Lezione 9, attività 4: 5 letture della luce (da 0 a 255), in colonne alte al massimo 8.
from mine import byte
import mine


def colonna(h):
    """Una colonna alta h blocchi davanti a Byte; poi Byte torna a terra."""
    for i in range(h):
        byte.place()
        byte.up()
    for i in range(h):
        byte.down()


def prossima_colonna():
    byte.turnRight()
    byte.forward()
    byte.turnLeft()


letture = []
for i in range(5):
    chiave, valore = mine.device.wait("luce")
    letture.append(valore)

for valore in letture:
    colonna(valore * 8 // 255)  # // divide e butta via i decimali
    prossima_colonna()

# Di nuovo dove Byte era partito.
byte.turnLeft()
for i in range(5):
    byte.forward()
byte.turnRight()
