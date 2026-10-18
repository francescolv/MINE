# Lezione 16, tappa 2, in Python: ora MINE dà anche la chiave.
import mine


def decifra(testo, chiave):
    chiaro = ""
    for c in testo:
        if "A" <= c <= "Z":
            c = chr((ord(c) - 65 - chiave) % 26 + 65)
        chiaro = chiaro + c
    return chiaro


segreto, chiave = mine.challenge()
mine.say(decifra(segreto, chiave))
