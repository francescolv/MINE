# Lezione 12, tappa 1: sei temperature dal Pico della serra, e la loro media.
import mine

temperature = []
for i in range(6):
    chiave, valore = mine.device.wait("temperatura")
    temperature.append(valore)
print("temperature:", temperature)

media = sum(temperature) / len(temperature)
mine.say("Temperatura media: " + str(round(media, 1)))
