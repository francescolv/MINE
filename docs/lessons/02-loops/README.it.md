[English](README.md) · **Italiano**

# Lezione 2 — I cicli: file, pavimenti e cubi

| | |
|---|---|
| **Durata** | 1 ora (50–60 minuti) |
| **Età** | 10–14 anni (anche prima superiore) |
| **Concetti** | ciclo (`for`), contatore, cicli annidati; area e volume |
| **Prerequisiti** | [lezione 1](../01-first-steps/README.it.md): scrivere, salvare e avviare un programma |

## Obiettivi

Alla fine della lezione lo studente sa:

1. spiegare perché un **ciclo** è meglio che scrivere tante volte la stessa riga;
2. usare `for i = 1, n do ... end` per ripetere delle istruzioni un numero preciso di volte;
3. mettere un ciclo **dentro** un altro per coprire una superficie, e un terzo per riempire un volume;
4. collegare il programma alla matematica: un pavimento di 5 × 4 blocchi ha **area** 20, un cubo
   di 3 × 3 × 3 ha **volume** 27 — e i cicli si moltiplicano proprio così.

## Preparazione (insegnante, prima della lezione)

- Lo stesso ambiente della lezione 1: un mondo **superpiatto in modalità creativa**, con MINE e
  CC: Tweaked.
- Le costruzioni occupano spazio. Metti gli studenti in fila uno accanto all'altro, **tutti
  rivolti dalla stessa parte**, a circa **8 blocchi di distanza**, con almeno **12 blocchi liberi
  davanti** a ogni Byte. Pavimenti e cubi crescono verso la destra di Byte.
- Il modo più rapido: il **laboratorio standard** (`/mine lab build <studenti>`, poi
  `/mine lab assign`) ha proprio queste corsie ([guida](../../guide/server.it.md#3-il-laboratorio-standard));
  `/mine lab reset` lo libera dopo la lezione.
- Nient'altro da preparare: quando la lezione parte MINE crea Byte a chi non ce l'ha, e a
  ogni tappa mette **64 assi** nello slot evidenziato di Byte (quello che c'era viene spostato
  in un altro slot).
- Prova tu stesso i tre programmi, da [`solutions/it/`](solutions/it/): 10 minuti.
- Facoltativo: metti la guida e la lavagna della lezione dove gli studenti le vedono,
  `/mine npc place mine:loops_guide` e `/mine board place mine:loops`. Ada propone di iniziare la
  lezione, dà un consiglio a ogni tappa e fa una domanda alla fine: `/mine npc results` accanto a
  lei mostra le risposte ([guida](../../guide/npc.it.md#npc-e-lavagne-nei-datapack)).

## Missioni: MINE controlla le tappe

All'inizio dell'attività 2 (dopo il riscaldamento) scrivi:

```
/mine lesson start loops @a
```

| Tappa | Superata quando… |
|---|---|
| 1 · `row` | il programma ha costruito una fila dritta di 10 blocchi, alta un blocco |
| 2 · `floor` | il programma ha costruito un pavimento pieno lungo 5 blocchi e largo 4 |
| 3 · `cube` | il programma ha costruito un cubo pieno 3 × 3 × 3 |

MINE guarda i **blocchi nuovi** messi dal programma vicino al punto di partenza di Byte: i
blocchi che c'erano già non contano, e la forma può essere orientata in qualunque direzione. Non
legge il codice, quindi in teoria uno studente potrebbe costruire la fila con dieci righe invece
che con un ciclo — l'obiettivo chiede un ciclo, e questo lo si controlla girando tra i banchi.

Quando una costruzione ha la misura sbagliata, MINE la misura, per esempio: *"Non ancora: il tuo
Byte ha costruito 15 blocchi, lunghi 5, larghi 3 e alti 1. Confrontalo con l'obiettivo: cosa è
diverso?"* È già un piccolo problema di area: lascia che gli studenti capiscano cosa manca prima
di aiutarli.

I comandi sono gli stessi della lezione 1: `/mine lesson status @a`, `/mine lesson hint`,
`/mine lesson restart`, `/mine lesson reset <studenti>` (vedi la
[guida della lezione 1](../01-first-steps/README.it.md#missioni-mine-controlla-le-tappe)).

## Con i blocchi

Tutta la lezione funziona anche nell'[editor a blocchi](../../guide/blocks.it.md) di MINE
(`/mine blocks`, poi **Esegui ▶**): le missioni controllano cosa è stato costruito, non come. È una
buona lezione per confrontare blocchi e Lua: il blocco `ripeti 10 volte` diventa
`for count = 1, 10 do`, e l'editor lo mostra accanto ai blocchi.

| Attività | Con i blocchi | File |
|---|---|---|
| 1 · Il contatore parla | `conta con i da 1 a 5 per 1` con dentro `di'` e la variabile `i` | |
| 2 · Fila | vedi sotto | [fila.json](solutions/it/blocks/fila.json) |
| 3 · Pavimento | vedi sotto | [pavimento.json](solutions/it/blocks/pavimento.json) |
| 4 · Cubo | il pavimento dell'attività 3 dentro `ripeti 3 volte`, con prima `vai su` | [cubo.json](solutions/it/blocks/cubo.json) |

```
vai su                              vai su
ripeti 10 volte                     ripeti 4 volte
    piazza un blocco sotto              ripeti 5 volte
    vai avanti                              piazza un blocco sotto
di' "Fila finita!"                          vai avanti
                                        ripeti 5 volte
                                            vai indietro
                                        gira a destra
                                        vai avanti
                                        gira a sinistra
                                    di' "Pavimento finito: 20 blocchi!"
```

Un ciclo dentro un ciclo con i blocchi si vede bene: un blocco contiene l'altro. Chiedi agli
studenti quale blocco ripete le file e quale i blocchi di una fila. Le soluzioni in inglese sono in
[`solutions/blocks`](solutions/blocks).

## Svolgimento

### Introduzione — 5 minuti, senza computer

Riprendi la sfida extra della lezione 1: una torre alta 10 blocchi. Scrivi il programma alla
lavagna, riga dopo riga, e fermati a metà: *"Dobbiamo davvero scriverlo tutto?"*

Chiedi come lo direbbero a un amico: "**dieci volte**: metti un blocco e sali". Quella frase è un
ciclo. In Lua:

```lua
for i = 1, 10 do
  byte.place()
  byte.up()
end
```

Fai notare che:

- le righe tra `do` e `end` vengono **ripetute**: sono il *corpo* del ciclo;
- `i` è un **contatore**: vale 1 la prima volta, 2 la seconda… fino a 10;
- cambiare 10 in 100 non costa niente. Scrivere 100 righe a mano sì.

### Attività 1 — Il contatore parla (5 minuti)

Per vedere il ciclo al lavoro prima di costruire: `edit conta`, poi

```lua
for i = 1, 5 do
  mine.say(i)
end
```

Avvialo con `conta`. Poi chiedi di **prevedere** prima di provare: cosa dice con `for i = 1, 3`?
E con `for i = 2, 10, 2`? (Conta di due in due: 2, 4, 6, 8, 10.)

### Attività 2 — Una fila di 10 blocchi (10 minuti)

**Compito:** con un ciclo, Byte costruisce una fila dritta di 10 blocchi.

Il trucco nuovo: `byte.placeDown()` mette un blocco **sotto** Byte. Se prima Byte sale di
un blocco, può "dipingere" il terreno mentre va avanti:

```lua
byte.up()
for i = 1, 10 do
  byte.placeDown()
  byte.forward()
end
```

I blocchi ci sono già: MINE li ha messi nello slot evidenziato di Byte.

Domande per la discussione:

- Cosa succede se scambi `placeDown()` e `forward()` dentro il ciclo? (Il primo blocco finisce un
  passo più avanti: la fila ha sempre 10 blocchi, comincia solo più in là.)
- E se dimentichi `byte.up()`? (`placeDown()` non riesce: sotto Byte c'è già il terreno.
  Restituisce `false` senza errori, e Byte cammina e basta.)

Soluzione: [`solutions/it/fila.lua`](solutions/it/fila.lua).

### Attività 3 — Il pavimento, e la sua area (15 minuti)

**Compito:** un pavimento pieno, lungo 5 blocchi e largo 4.

**Prima, senza computer:** quanti blocchi servono? Disegnalo su un foglio a quadretti. La
risposta è l'**area**: 5 × 4 = 20. È anche il numero di blocchi che userà Byte.

Un pavimento è fatto di 4 file da 5 blocchi: **un ciclo dentro un ciclo**. Il ciclo esterno conta
le file; quello interno costruisce una fila. Tra una fila e l'altra Byte deve tornare
all'inizio della fila e spostarsi di un passo di lato:

```lua
byte.up()
for fila = 1, 4 do
  for i = 1, 5 do
    byte.placeDown()
    byte.forward()
  end
  for i = 1, 5 do
    byte.back()
  end
  byte.turnRight()
  byte.forward()
  byte.turnLeft()
end
```

Cose da spiegare:

- l'**indentazione** (gli spazi all'inizio delle righe) non cambia quello che fa Lua, ma mostra
  quali righe appartengono a quale ciclo. Chiedi agli studenti di rientrare le righe così.
- i contatori hanno nomi diversi (`fila`, `i`) per non confonderli.
- quante volte viene eseguito `byte.placeDown()`? 4 volte il ciclo interno, 5 volte ciascuno:
  4 × 5 = 20. **I cicli si moltiplicano, proprio come l'area.**

Soluzione: [`solutions/it/pavimento.lua`](solutions/it/pavimento.lua).

### Attività 4 — Il cubo, e il suo volume (15 minuti)

**Compito:** un cubo pieno, lungo 3 blocchi, largo 3 e alto 3.

Di nuovo, prima si prevede: quanti blocchi? 3 × 3 × 3 = 27: il **volume**. Un cubo è fatto di tre
pavimenti 3 × 3 uno sopra l'altro, quindi il pavimento va dentro **un terzo ciclo**, un giro per
strato. All'inizio di ogni strato Byte sale di un blocco; alla fine torna alla prima fila
dello strato.

Suggerimento, solo se serve: "costruisci prima un pavimento 3 × 3. Quando funziona, mettilo
dentro un ciclo che lo ripete 3 volte."

Soluzione: [`solutions/it/cubo.lua`](solutions/it/cubo.lua).

### Sfida extra — Per chi finisce prima

- Costruisci un cubo 4 × 4 × 4. Quali numeri cambi? Quanti blocchi serviranno? (64: una pila
  intera!)
- Una scala: ogni gradino è un blocco più in alto e un blocco più avanti.
- Un cubo **vuoto**, come una stanza. (Servono idee delle prossime lezioni: lasciali provare e
  parlate del perché è più difficile.)

### Conclusione — 5 minuti

- Cos'è un ciclo? (un modo per ripetere istruzioni senza riscriverle)
- Perché il pavimento richiede 20 blocchi e il cubo 27? (area e volume: i cicli si moltiplicano)
- La domanda di riflessione che MINE mostra alla fine: *Quante righe servirebbero per il cubo
  senza i cicli? Cosa cambieresti nel programma per costruire un cubo largo 10 blocchi?*

## Problemi frequenti

| Cosa succede | Perché | Cosa fare |
|---|---|---|
| `'do' expected` o `'end' expected` | manca un `do` o un `end`: ogni `for` li vuole entrambi | conta i `for` e gli `end`: devono essere tanti quanti |
| `'=' expected near ...` | una virgola o uno spazio nel posto sbagliato: `for i 1, 10` | la forma esatta è `for i = 1, 10 do` |
| Byte cammina ma non mette blocchi | non è salito prima: sotto c'è il terreno | `byte.up()` prima del ciclo |
| La fila si ferma prima o ha dei buchi | Byte ha finito i blocchi, o qualcosa l'ha bloccato | se lo slot è vuoto MINE lo dice; altrimenti cerca un ostacolo davanti |
| Il pavimento è tutto su una riga, o in diagonale | lo spostamento laterale tra le file manca o è nel posto sbagliato | gira-avanti-gira va **dopo** il ciclo interno, ma **dentro** quello esterno |
| Il pavimento cresce a sinistra invece che a destra | `turnLeft` e `turnRight` scambiati | va bene così: MINE accetta il pavimento in qualunque direzione |
| MINE dice "lunghi 5, larghi 3" o simili | il ciclo esterno gira un numero sbagliato di volte | confronta i numeri del messaggio con l'obiettivo |
| Il cubo è fatto di 3 pavimenti uno accanto all'altro | Byte non sale tra uno strato e l'altro | `byte.up()` all'inizio di ogni strato |
| Gli strati del cubo sono spostati | finito uno strato Byte non torna alla prima fila | gira a sinistra, avanti 3, gira a destra alla fine di ogni strato |
| Byte sbatte contro la costruzione precedente | non c'è abbastanza spazio | `/mine byte recall` da un punto libero |

## Per l'insegnante: cosa osservare

- **Prevedono il numero di blocchi** prima di avviare il programma? Lì c'è la matematica.
- **Leggono le misure di MINE** ("lunghi 5, larghi 3") e correggono il numero giusto?
- **Costruiscono il cubo un passo alla volta**, partendo da un pavimento che funziona, invece di
  scrivere tutto in una volta?

## Riferimenti

- [Lezione 1 — Primi passi con Byte](../01-first-steps/README.it.md)
- [API Lua di MINE](../../guide/lua-api.it.md); in gioco, `help byte` nel terminale di Byte.
- Le soluzioni sono in [`solutions/it/`](solutions/it/) (in inglese in [`solutions/`](solutions/))
  e vengono provate in automatico a ogni modifica di MINE.
