[English](README.md) · **Italiano**

# Lezione 12 — Python: la serra connessa (Pico 2 W)

| | |
|---|---|
| **Durata** | 1–2 ore |
| **Età** | 14–18 anni |
| **Concetti** | **Python**: liste, `sum`/`len`, media, soglia; dati da un sensore via Wi-Fi (Internet delle cose), riassumere i dati |
| **Prerequisiti** | la [lezione 9](../09-charts/README.it.md) (Python con Byte); un [Raspberry Pi Pico 2 W](../../guide/devices.it.md#raspberry-pi-pico-2-w-in-wi-fi) è consigliato, non necessario |

## Obiettivi

Alla fine della lezione lo studente sa:

1. raccogliere misure in una **lista** e calcolarne la **media**;
2. confrontare una misura con una **soglia** e mandare un comando al dispositivo;
3. **riassumere** i dati a gruppi, come fa una stazione meteo, invece di mandarli tutti;
4. spiegare il percorso dei dati: sensore → Wi-Fi → server → programma → di nuovo il dispositivo.

## Come funziona

Un **Pico 2 W** nella serra misura la temperatura e la manda al server MINE **via Wi-Fi**, senza
cavi verso il PC, con la chiave `temperatura`. Byte la riceve nel programma Python dello studente
(`mine.device.wait("temperatura")`) e risponde al Pico: `allarme:1` accende il suo LED, `media`
gli arriva come numero.

Per controllare le tappe **MINE fa la parte del Pico**: manda lui le temperature, ogni volta
diverse, e guarda cosa risponde Byte. Così la lezione si fa anche senza scheda, e con la scheda il
programma è lo stesso.

## Preparazione (insegnante, prima della lezione)

- L'editor Python (`/mine python`, o **Python** nell'editor a blocchi): vedi la
  [guida Python](../../guide/python.it.md). Va bene anche Thonny sul PC.
- Se avete i Pico 2 W: MicroPython, `mine_wifi.py` e `mine.txt` (rete, password, codice di ogni
  studente), come nella [guida delle schede](../../guide/devices.it.md#raspberry-pi-pico-2-w-in-wi-fi).
  Prova prima con un Pico che la rete della scuola lasci passare i dispositivi.
- Prova tu le tre tappe con [`solutions/it/`](solutions/it/).

### Il programma del Pico

Ogni 2 secondi manda la temperatura del suo chip; con `allarme` accende o spegne il LED; la `media`
la scrive (in Thonny). Salvalo sul Pico come `serra.py` e, come `main.py`, la riga `import serra`.

```python
# MINE: il Pico 2 W della serra. Ogni 2 secondi la sua temperatura (temperatura:NN);
# allarme:1 / allarme:0 accendono e spengono il LED; media:NN la scrive.
import time

import mine_wifi as mine
from machine import ADC, Pin

led = Pin("LED", Pin.OUT)
sensore = ADC(4)  # il sensore di temperatura dentro il chip

mine.connect()
mine.send("programma", "serra")
while True:
    volt = sensore.read_u16() * 3.3 / 65535
    mine.send("temperatura", round(27 - (volt - 0.706) / 0.001721))
    for chiave, valore in mine.receive():
        if chiave == "allarme":
            led.value(1 if valore else 0)
        elif chiave == "media":
            print("media:", valore)
    time.sleep(2)
```

Per far salire la temperatura basta stringere il Pico tra le dita o soffiarci sopra; per vedere
l'allarme in classe si può abbassare la soglia a 28.

## Missioni: MINE controlla le tappe

```
/mine lesson start greenhouse @a
```

| Tappa | MINE (il Pico)… | Superata quando… |
|---|---|---|
| 1 · `average` | manda 6 temperature tra 16 e 34 | Byte dice la loro media (va bene arrotondata) |
| 2 · `alarm` | manda temperature sotto e sopra 30, e 30 esatti | dopo ognuna il Pico ha ricevuto `allarme:1` solo **sopra** 30, altrimenti `allarme:0` |
| 3 · `report` | manda due gruppi di 5 temperature | dopo ogni gruppo il Pico ha ricevuto la loro media come `media` |

Le tappe 2 e 3 si provano **dal vivo**: il programma resta in ascolto (`while True:`) e MINE lo
ferma quando ha visto abbastanza. Se qualcosa non va, MINE dice cosa: *"la temperatura era 33, e
Byte ha detto alla scheda allarme:0"*.

## Svolgimento

### Introduzione — 10 minuti

Disegna alla lavagna il viaggio di una misura: **sensore** (nel Pico) → **Wi-Fi** → **server**
MINE → **programma** di Byte → di nuovo il **Pico** (il LED). È l'Internet delle cose: termostati,
stazioni meteo, serre vere funzionano così. Chiedi: chi decide, il Pico o il programma?

### Attività 1 — La media (15 minuti)

```python
import mine

temperature = []
for i in range(6):
    chiave, valore = mine.device.wait("temperatura")
    temperature.append(valore)

media = sum(temperature) / len(temperature)
mine.say("Temperatura media: " + str(round(media, 1)))
```

- `mine.device.wait` **aspetta** la misura successiva: il programma va al ritmo del sensore.
- `sum` e `len`: la media è la somma divisa per quante sono.

Soluzione: [`solutions/it/media.py`](solutions/it/media.py).

### Attività 2 — L'allarme (15 minuti)

```python
while True:
    chiave, temperatura = mine.device.wait("temperatura")
    if temperatura > 30:
        mine.device.send("allarme", 1)
    else:
        mine.device.send("allarme", 0)
```

Domanda: `>` o `>=`? MINE prova anche **30 esatti**, che non è "sopra i 30". Perché, in una serra
vera, manda `allarme:0` a ogni misura e non solo quando cambia? (Se un messaggio si perde, quello
dopo rimette le cose a posto.)

Soluzione: [`solutions/it/allarme.py`](solutions/it/allarme.py).

### Attività 3 — Il resoconto (20 minuti)

Mandare ogni misura costa (batteria, rete). Si manda un **riassunto**: la media ogni 5 misure.

```python
gruppo = []
while True:
    chiave, temperatura = mine.device.wait("temperatura")
    gruppo.append(temperatura)
    if len(gruppo) == 5:
        mine.device.send("media", round(sum(gruppo) / len(gruppo), 1))
        gruppo = []
```

Soluzione: [`solutions/it/resoconto.py`](solutions/it/resoconto.py).

### Sfida extra

- Manda anche la **minima** e la **massima** di ogni gruppo (`min`, `max`).
- La **tendenza**: la temperatura sale o scende rispetto al gruppo prima? Manda `tendenza:1` o
  `tendenza:-1`, e sul Pico accendi il LED solo se sale.

### Conclusione — 5 minuti

La domanda di riflessione di MINE: *La tua serra ha misurato, deciso e mandato un riassunto da
sola, via Wi-Fi. Quali dispositivi intorno a te funzionano così? Che dati mandano, e a chi?*

## Problemi frequenti

| Cosa succede | Perché | Cosa fare |
|---|---|---|
| *"il programma è finito prima che MINE potesse provarlo"* | manca il `while True:` nelle tappe 2 e 3 | il programma resta in ascolto; MINE lo ferma |
| La media è sbagliata di molto | la lista contiene anche altre chiavi, o la somma è fuori dal ciclo | aspetta solo `"temperatura"` |
| Il Pico non riceve niente | Byte manda `allarme` con un'altra chiave, o il Pico non chiama `mine.receive()` | stessa chiave dalle due parti; `mine.receive()` almeno ogni pochi secondi |
| Con il Pico vero la prova di MINE sbaglia | il Pico manda le sue temperature mentre MINE prova le sue | spegni il Pico (o staccalo) mentre MINE controlla la tappa |

## Riferimenti

- [Le schede: Pico 2 W in Wi-Fi](../../guide/devices.it.md#raspberry-pi-pico-2-w-in-wi-fi), [guida Python](../../guide/python.it.md).
- Soluzioni in [`solutions/it/`](solutions/it/) (in inglese in [`solutions/`](solutions/)), provate
  in automatico in MicroPython, come nell'editor.
