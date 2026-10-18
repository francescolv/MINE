# Lezione 16, tappa 3, in Python: niente chiave, ma nel messaggio c'è BYTE. Prova tutte le 25 chiavi.
import mine


def decifra(testo, chiave):
    chiaro = ""
    for c in testo:
        if "A" <= c <= "Z":
            c = chr((ord(c) - 65 - chiave) % 26 + 65)
        chiaro = chiaro + c
    return chiaro


segreto = mine.challenge()
for chiave in range(1, 26):
    chiaro = decifra(segreto, chiave)
    if "BYTE" in chiaro:
        mine.say("La chiave è " + str(chiave) + ": " + chiaro)
        break
