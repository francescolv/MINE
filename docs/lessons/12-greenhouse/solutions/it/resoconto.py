# Lezione 12, tappa 3: ogni 5 temperature, la loro media torna al Pico (media).
import mine

gruppo = []
while True:
    chiave, temperatura = mine.device.wait("temperatura")
    gruppo.append(temperatura)
    if len(gruppo) == 5:
        media = round(sum(gruppo) / len(gruppo), 1)
        mine.device.send("media", media)
        print("media di", gruppo, "=", media)
        gruppo = []
