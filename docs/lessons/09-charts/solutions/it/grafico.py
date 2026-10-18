# Lezione 9, attività 2: 5 numeri dalla scheda in una lista, poi il loro grafico a barre.
from mine import byte
import mine

numeri = []
for i in range(5):
    chiave, valore = mine.device.wait("dato")  # aspetta il prossimo numero della scheda
    numeri.append(valore)
print("numeri:", numeri)

for n in numeri:
    for h in range(n):
        byte.place()
        byte.up()
    for h in range(n):
        byte.down()
    byte.turnRight()
    byte.forward()
    byte.turnLeft()

# Di nuovo dove Byte era partito: il prossimo grafico ha spazio.
byte.turnLeft()
for i in range(5):
    byte.forward()
byte.turnRight()
