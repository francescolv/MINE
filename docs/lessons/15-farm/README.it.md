[English](README.md) · **Italiano**

# Lezione 15 — La fattoria: file e campi

| | |
|---|---|
| **Durata** | 1 ora (50–60 minuti) |
| **Età** | 10–14 anni |
| **Concetti** | cicli annidati (un ciclo dentro un ciclo), condizioni, contatore; area di un rettangolo |
| **Prerequisiti** | [lezione 2](../02-loops/README.it.md) (cicli) e [lezione 4](../04-conditions/README.it.md) (condizioni) |

## Obiettivi

Alla fine della lezione lo studente sa:

1. percorrere un **campo** (righe e colonne) con un **ciclo dentro un ciclo**;
2. fare qualcosa **solo dove serve**, con una condizione (`byte.ripeDown()`);
3. **contare** con una variabile quello che il programma ha fatto;
4. collegare il numero di giri dei cicli all'**area**: 3 file × 5 = 15 blocchi.

## Come funziona

Ogni volta che un programma parte, **MINE scava un campo** davanti a Byte: **3 file da 5** blocchi
di terra arata, con un canale d'acqua a sinistra che la tiene bagnata. Byte **vola un blocco sopra
le piante**, come le turtle che coltivano: prima `byte.up()`, poi semina con `byte.placeDown()` e
raccoglie con `byte.digDown()`. Quando il programma finisce MINE rimette il terreno com'era.

A ogni tappa MINE mette **64 semi di grano** nello slot evidenziato di Byte. Dalla tappa 2 il
grano è già cresciuto: in parte **maturo** (dorato), in parte **giovane** (verde), ogni volta in
posti diversi. `byte.ripeDown()` (con i blocchi *raccolto maturo sotto?*) dice se il grano sotto
Byte è maturo.

## Preparazione (insegnante, prima della lezione)

- Il **laboratorio standard**, o un mondo superpiatto con **6 blocchi liberi davanti**, **3 a
  destra** e **1 a sinistra** di ogni Byte, su terreno pieno.
- Prova tu le tre tappe con [`solutions/it/`](solutions/it/).

## Missioni: MINE controlla le tappe

```
/mine lesson start farm @a
```

| Tappa | Il campo… | Superata quando… |
|---|---|---|
| 1 · `sow` | è vuoto | ogni blocco di terra arata ha i semi |
| 2 · `harvest` | ha grano maturo e giovane | tutto il grano maturo è raccolto **e riseminato**, quello giovane è ancora al suo posto |
| 3 · `count` | come la tappa 2 | come sopra, e Byte dice quante spighe ha raccolto |

Dopo ogni tappa riuscita, `/mine byte recall` dall'inizio della corsia riporta Byte al suo posto.

## Con i blocchi

| Tappa | File |
|---|---|
| 1 · Semina | [semina.json](solutions/it/blocks/semina.json) |
| 2 · Raccolta | [raccolta.json](solutions/it/blocks/raccolta.json) |
| 3 · Conta | [conta.json](solutions/it/blocks/conta.json) |

```
vai su
ripeti 3 volte
    ripeti 5 volte
        vai avanti
        se raccolto maturo sotto?
            scava sotto
            piazza un blocco sotto
    ripeti 5 volte
        vai indietro
    gira a destra
    vai avanti
    gira a sinistra
```

## Svolgimento

### Introduzione — 5 minuti, senza computer

Un campo a quadretti alla lavagna, 3 file da 5. Come lo semina un trattore? Una fila, si torna
indietro, si passa alla fila dopo. Quante volte si semina? (3 × 5 = 15: l'area!)

### Attività 1 — Semina (15 minuti)

```lua
byte.up()
for fila = 1, 3 do
  for i = 1, 5 do
    byte.forward()
    byte.placeDown()
  end
  for i = 1, 5 do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end
```

Il ciclo di dentro fa **una fila**; quello di fuori lo ripete per **ogni fila**. Quante volte
viene eseguito `byte.placeDown()`?

Soluzione: [`solutions/it/semina.lua`](solutions/it/semina.lua).

### Attività 2 — Raccogli solo il maturo (15 minuti)

Lo stesso percorso; in ogni blocco una domanda: è maturo?

```lua
    byte.forward()
    if byte.ripeDown() then
      byte.digDown()
      byte.placeDown()
    end
```

Il grano giovane resta; dove si raccoglie si risemina subito (con i semi raccolti, che tornano
nello slot).

Soluzione: [`solutions/it/raccolta.lua`](solutions/it/raccolta.lua).

### Attività 3 — Conta il raccolto (10 minuti)

Una variabile `raccolti` parte da 0 e cresce di 1 dentro l'`if`. Alla fine Byte la dice. MINE
conta anche lui: niente numeri inventati!

Soluzione: [`solutions/it/conta.lua`](solutions/it/conta.lua).

### Sfida extra

- Il campo a **serpentina**: senza tornare indietro a ogni fila (una fila avanti, la successiva
  all'indietro). Serve sapere se la fila è pari o dispari: `fila % 2 == 0`.
- Quante spighe **giovani** c'erano? (15 meno quelle raccolte.)

### Conclusione — 5 minuti

La domanda di riflessione di MINE: *Byte ha seminato e raccolto un campo intero con poche righe,
un ciclo dentro un ciclo. Dove usano davvero gli agricoltori macchine che seguono un programma?*

## Problemi frequenti

| Cosa succede | Perché | Cosa fare |
|---|---|---|
| Byte non riesce ad avanzare sul campo | non è salito: il grano gli sbarra la strada | `byte.up()` all'inizio |
| *"N blocchi su 15 hanno i semi"* | il ritorno all'inizio della fila o il passaggio alla fila dopo sono sbagliati | prova a mano una fila sola, poi aggiungi il ciclo di fuori |
| Byte taglia anche il grano giovane | manca l'`if`, o la condizione è al contrario | `if byte.ripeDown() then` |
| Non risemina | lo slot evidenziato è vuoto o ha altro | i semi nello slot evidenziato; `/mine lesson restart` li ridà |
| MINE dice che serve spazio | Byte è ancora sul campo di prima | `/mine byte recall` dall'inizio della corsia |

## Riferimenti

- [Lezione 2 — I cicli](../02-loops/README.it.md), [lezione 4 — Le condizioni](../04-conditions/README.it.md); [API Lua](../../guide/lua-api.it.md) (`byte.ripeDown`).
- Soluzioni in [`solutions/it/`](solutions/it/) (in inglese in [`solutions/`](solutions/)), provate
  in automatico su campi con grano maturo in posti a caso.
