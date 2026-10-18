# Lezione 9, attività 3: il numero più grande, e dove si trova nella lista.
import mine

numeri = []
for i in range(5):
    chiave, valore = mine.device.wait("dato")
    numeri.append(valore)

top = max(numeri)
posizione = numeri.index(top) + 1  # le liste contano da 0, le persone da 1
mine.say("il più grande è " + str(top) + ", il numero " + str(posizione))
