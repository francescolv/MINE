[English](README.md) · **Italiano**

# Lezione 13 — Python: il controllo automatico (micro:bit)

| | |
|---|---|
| **Durata** | 1–2 ore |
| **Età** | 14–18 anni |
| **Concetti** | **Python**: soglia, **isteresi**, variabili di stato, eventi da più sorgenti; i sistemi di controllo |
| **Prerequisiti** | la [lezione 8](../08-board/README.it.md) (la scheda e Byte) e la [lezione 9](../09-charts/README.it.md) (Python); un micro:bit è consigliato, non necessario |

## Obiettivi

Alla fine della lezione lo studente sa:

1. accendere un attuatore con una **soglia**, a partire da una misura;
2. spiegare perché una soglia sola fa "sfarfallare" l'attuatore, e risolverlo con l'**isteresi**;
3. usare una **variabile di stato** che ricorda cosa è successo prima;
4. gestire **due sorgenti** di eventi (la temperatura, un pulsante) nello stesso ciclo.

## Come funziona

La lampada **dietro Byte** è la **ventola** della serra: Byte la accende con
`redstone.setOutput("back", True)`. Il micro:bit misura la temperatura (`temperatura`) e ha un
pulsante (`pulsante_a`), che arrivano a Byte come nella [lezione 8](../08-board/README.it.md).

Per controllare le tappe **MINE fa la parte del micro:bit**: manda temperature e pressioni del
pulsante, e dopo ognuna guarda se la ventola è accesa o spenta come deve.

## Preparazione (insegnante, prima della lezione)

- L'editor Python (`/mine python`), vedi la [guida Python](../../guide/python.it.md).
- Spazio intorno a Byte: MINE mette la lampada dietro di lui (il laboratorio standard va bene).
- Se avete i micro:bit: collegati con `/mine device`, con questo programma (MicroPython):

```python
# MINE: il micro:bit della serra. Ogni secondo la temperatura; il pulsante A quando lo premi.
from microbit import *

print("programma:serra")
while True:
    if button_a.was_pressed():
        print("pulsante_a:1")
    print("temperatura:%d" % temperature())
    sleep(1000)
```

Il sensore del micro:bit misura la temperatura del chip: tenendolo in mano sale di qualche grado.
Per provarlo in classe si possono abbassare le soglie (per esempio 26 e 24).

## Missioni: MINE controlla le tappe

```
/mine lesson start control @a
```

| Tappa | Superata quando… |
|---|---|
| 1 · `threshold` | la ventola è accesa da 28 gradi in su, spenta sotto |
| 2 · `hysteresis` | si accende a 28 o più, si spegne a 24 o meno, e **in mezzo resta com'era** |
| 3 · `manual` | in più, il pulsante A accende e spegne il **modo manuale**, in cui la ventola è sempre accesa |

Le prove sono dal vivo: il programma resta in ascolto (`while True:`). Se qualcosa non va, MINE
racconta cosa ha mandato e cosa ha visto: *"dopo 22°, 26°, 29°, 26° la ventola doveva essere
accesa, ed era spenta"*.

## Svolgimento

### Introduzione — 10 minuti

Un **sistema di controllo**: un sensore misura, un programma decide, un attuatore agisce. Esempi:
il termostato, il frigorifero, i lampioni che si accendono al buio. Disegna alla lavagna
temperatura e ventola nel tempo, con una sola soglia a 28: quando la temperatura oscilla tra 27 e
28, la ventola si accende e spegne di continuo. Come si evita?

### Attività 1 — La soglia (10 minuti)

```python
from mine import redstone
import mine

while True:
    chiave, temperatura = mine.device.wait("temperatura")
    redstone.setOutput("back", temperatura >= 28)
```

Il confronto `temperatura >= 28` è già `True` o `False`: non serve un `if`.

Soluzione: [`solutions/it/soglia.py`](solutions/it/soglia.py).

### Attività 2 — L'isteresi (20 minuti)

Due soglie: si accende a 28, si spegne a 24; **in mezzo non cambia niente**. Il programma deve
ricordare se la ventola è accesa: una **variabile di stato**.

```python
accesa = False
while True:
    chiave, temperatura = mine.device.wait("temperatura")
    if temperatura >= 28:
        accesa = True
    elif temperatura <= 24:
        accesa = False
    redstone.setOutput("back", accesa)
```

Fate il grafico di prima con le due soglie: quante volte si accende ora?

Soluzione: [`solutions/it/isteresi.py`](solutions/it/isteresi.py).

### Attività 3 — Il pulsante del giardiniere (20 minuti)

`mine.device.wait()` **senza chiave** aspetta qualunque cosa arrivi dalla scheda: la chiave dice
cosa.

```python
accesa = False
manuale = False
while True:
    chiave, valore = mine.device.wait()
    if chiave == "pulsante_a":
        manuale = not manuale
    elif chiave == "temperatura":
        if valore >= 28:
            accesa = True
        elif valore <= 24:
            accesa = False
    redstone.setOutput("back", accesa or manuale)
```

Ora gli stati sono quattro (automatico o manuale, acceso o spento): disegnateli.

Soluzione: [`solutions/it/manuale.py`](solutions/it/manuale.py).

### Conclusione — 5 minuti

La domanda di riflessione di MINE: *La tua serra ha acceso e spento la ventola da sola, senza
farla sfarfallare. Dove altro una macchina decide da sola con una soglia? Perché serve uno spazio
tra acceso e spento?*

## Problemi frequenti

| Cosa succede | Perché | Cosa fare |
|---|---|---|
| La ventola si spegne tra 24 e 28 | il programma non ricorda lo stato: ricalcola tutto dalla temperatura | una variabile `accesa` fuori dal ciclo, cambiata solo alle soglie |
| Il pulsante non fa niente | il programma aspetta solo `"temperatura"` | `mine.device.wait()` senza chiave, poi guarda `chiave` |
| MINE dice che serve spazio | dietro Byte c'è qualcosa | `/mine byte recall` in un punto libero |

## Riferimenti

- [Lezione 8 — La scheda](../08-board/README.it.md), [le schede](../../guide/devices.it.md), [guida Python](../../guide/python.it.md).
- Soluzioni in [`solutions/it/`](solutions/it/) (in inglese in [`solutions/`](solutions/)), provate
  in automatico in MicroPython.
