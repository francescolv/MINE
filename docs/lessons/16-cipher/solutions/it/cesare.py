# Lezione 16, tappa 1, in Python: ogni lettera è stata spostata di 3 posti avanti: riportala indietro.
import mine


def decifra(testo, chiave):
    chiaro = ""
    for c in testo:
        if "A" <= c <= "Z":
            c = chr((ord(c) - 65 - chiave) % 26 + 65)  # A è 65
        chiaro = chiaro + c
    return chiaro


segreto = mine.challenge()
mine.say(decifra(segreto, 3))
