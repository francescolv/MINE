[English](README.md) · **Italiano**

# Lezione 9 — Python: i dati della scheda, in grafico

| | |
|---|---|
| **Durata** | 1 ora (50–60 minuti) |
| **Età** | 14–18 anni (e ragazzi più giovani già bravi) |
| **Concetti** | Python: liste (`append`, `max`, `index`), `for` su una lista, funzioni, divisione intera (`//`), riportare una misura in scala |
| **Prerequisiti** | un po' di Python, o le lezioni 1–5 in Lua o a blocchi; la [lezione 8](../08-board/README.it.md) per la scheda; un **micro:bit** (o un Arduino, o un Raspberry Pi Pico) per studente o per coppia |
| **Linguaggio** | **Python**, nell'[editor Python](../../guide/python.it.md) di MINE (o in [Thonny](../../guide/python.it.md#thonny)) |

## Obiettivi

Alla fine della lezione gli studenti sanno:

1. raccogliere le misure di una scheda in una **lista**;
2. trasformare una lista in un **grafico a barre** costruito da Byte, colonna per colonna;
3. trovare il valore **più grande** e la sua **posizione** (`max`, `index`, contando da 0);
4. **riportare in scala** una misura allo spazio che c'è (`valore * 8 // 255`).

Sono i primi passi della scienza dei dati: misurare, conservare, guardare, riassumere.

## Il programma della scheda

Ogni pressione di **A** lancia un dado (da 1 a 6) e lo manda come `dato`; ogni pressione di **B**
manda la luce come `luce` (da 0 a 255). Il JavaScript di MakeCode:

```javascript
serial.writeLine("programma:lezione9")
input.onButtonPressed(Button.A, function () {
    let n = randint(1, 6)
    basic.showNumber(n)
    serial.writeLine("dato:" + n)
})
input.onButtonPressed(Button.B, function () {
    let luce = input.lightLevel()
    basic.showNumber(luce)
    serial.writeLine("luce:" + luce)
})
```

Con un **Raspberry Pi Pico** e MicroPython le stesse righe si scrivono con `print("dato:" + str(n))`
(vedi [Le schede](../../guide/devices.it.md)). Niente schede? MINE fa la parte della scheda quando
prova i programmi.

## Missioni: MINE verifica i passi

```
/mine lesson start charts @a
```

| Passo | Fatto quando… |
|---|---|
| 1 · `bar_chart` | **MINE manda 5 numeri** (da 1 a 6) come `dato`; Byte costruisce una colonna per numero, alta come il numero, la prima davanti a dove è partito e le altre una dopo l'altra alla sua destra, poi torna dove era partito |
| 2 · `largest` | MINE manda 5 numeri (da 1 a 9); Byte **dice** il più grande e la sua posizione, da 1 (es. "il più grande è 8, il numero 2") |
| 3 · `light_chart` | MINE manda 5 letture della luce (da 0 a 255) come `luce`; colonne alte `luce * 8 // 255`, e Byte di nuovo dove era partito |

MINE sceglie **numeri nuovi ogni volta**: un programma che costruisce sempre lo stesso grafico non
passa. MINE dà 64 assi di legno alle tappe 1 e 3, e toglie il grafico del tentativo prima.

## Piano della lezione

### Introduzione — 5 minuti

Una lista di numeri dice poco; un grafico dice molto. Mostra un grafico a barre di qualcosa di vero
(la temperatura della settimana, le altezze della classe). Cosa c'è sugli assi? Cosa dice la barra
più alta?

### Attività 1 — I numeri in una lista (10 minuti)

`/mine lesson start charts @a`, `/mine device`, poi nell'editor Python:

```python
import mine

numeri = []
for i in range(5):
    chiave, valore = mine.device.wait("dato")
    numeri.append(valore)
print(numeri)
```

Premi A cinque volte: la lista compare a destra dell'editor. `mine.device.wait` restituisce **due**
valori, la chiave e il valore: `chiave, valore = ...` li prende tutti e due.

### Attività 2 — Il grafico a barre (15 minuti)

```python
for n in numeri:
    for h in range(n):
        byte.place()
        byte.up()
    for h in range(n):
        byte.down()
    byte.turnRight()
    byte.forward()
    byte.turnLeft()

byte.turnLeft()
for i in range(5):
    byte.forward()
byte.turnRight()
```

- `for n in numeri` scorre la lista, un numero alla volta.
- Le ultime quattro righe riportano Byte indietro: nel laboratorio le corsie sono a 8 blocchi, e il
  prossimo grafico ha bisogno di spazio.

Soluzione: [`solutions/it/grafico.py`](solutions/it/grafico.py).

### Attività 3 — Il più grande (10 minuti)

```python
top = max(numeri)
mine.say("il più grande è " + str(top) + ", il numero " + str(numeri.index(top) + 1))
```

Perché `+ 1`? Le liste contano da 0, le persone da 1. Chiedi cos'è `numeri[0]`, e `numeri[5]` (un
errore: non c'è un sesto numero). Soluzione: [`solutions/it/massimo.py`](solutions/it/massimo.py).

### Attività 4 — La luce in scala (15 minuti)

255 blocchi non ci starebbero: `valore * 8 // 255` trasforma 0–255 in 0–8. `//` divide e butta via i
decimali (`7 // 2` fa 3). Una funzione tiene in ordine:

```python
def colonna(h):
    for i in range(h):
        byte.place()
        byte.up()
    for i in range(h):
        byte.down()
```

Copri la scheda, premi B, scoprila, premi di nuovo B: il grafico mostra la luce. Soluzione:
[`solutions/it/luce.py`](solutions/it/luce.py).

### Sfida in più

- Byte dice anche la **media** (`sum(numeri) / len(numeri)`), e mette un blocco d'oro in cima alla
  colonna più alta.
- Dieci lanci del dado: quanti 6? Un grafico di quante volte è uscito ogni numero.

### Conclusione — 5 minuti

La domanda di riflessione che MINE mostra alla fine: *Il tuo programma ha trasformato dei numeri in
un grafico che puoi girare intorno. Dove hai visto grafici fatti con delle misure? Cosa aiutavano a
vedere?*

## Problemi frequenti

| Cosa succede | Perché | Cosa fare |
|---|---|---|
| MINE: «il programma è finito prima che la scheda avesse mandato i suoi 5 numeri» | il programma non aspetta ogni numero | `mine.device.wait("dato")` nel ciclo |
| `TypeError` su `range`, o `('dato', 3)` nella lista stampata | `valore = mine.device.wait(...)` prende chiave e valore insieme | `chiave, valore = mine.device.wait("dato")` |
| MINE: «il grafico è giusto! Ora riporta Byte» | Byte resta all'ultima colonna | `turnLeft`, 5 × `forward`, `turnRight` |
| MINE: «la colonna 3 è alta 2» | le colonne sono nel posto sbagliato, o Byte non è tornato giù | ogni colonna davanti a Byte, poi di nuovo giù prima di spostarsi a destra |
| Le colonne sono troppo alte nella tappa 3 | `/` al posto di `//`, o manca la scala | `valore * 8 // 255` |

## Riferimenti

- [L'editor Python di MINE e Thonny](../../guide/python.it.md)
- [Le schede: micro:bit, Arduino e Byte](../../guide/devices.it.md)
- Le soluzioni sono in [`solutions/`](solutions/) (in italiano in [`solutions/it/`](solutions/it/)), e
  vengono provate automaticamente, in MicroPython, ogni volta che MINE cambia.
