[English](README.md) · **Italiano**

# Lezione 3 — Debugging: ripara i programmi di Byte

| | |
|---|---|
| **Durata** | 1 ora (50–60 minuti) |
| **Età** | 10–14 anni (anche prima superiore) |
| **Concetti** | bug, messaggio di errore, numero di riga; errori di sintassi, di esecuzione, di logica |
| **Prerequisiti** | [lezione 2](../02-loops/README.it.md): cicli `for` e cicli dentro cicli |

## Obiettivi

Alla fine della lezione lo studente sa:

1. leggere un messaggio di errore e trovare la riga a cui si riferisce;
2. distinguere i tre tipi di bug: Lua **non fa partire** il programma (sintassi), il programma
   **si ferma a metà** (esecuzione), il programma **funziona ma fa la cosa sbagliata** (logica);
3. correggere un bug cambiando una cosa alla volta e riprovando;
4. vedere un errore come un'informazione, non come un fallimento.

## Preparazione (insegnante, prima della lezione)

- Lo stesso ambiente della lezione 2: un mondo **superpiatto in modalità creativa**, studenti uno
  accanto all'altro, tutti rivolti dalla stessa parte, a circa **8 blocchi di distanza**, con
  almeno **10 blocchi liberi davanti** a ogni Byte.
- Il modo più rapido: il **laboratorio standard** (`/mine lab build <studenti>`, poi
  `/mine lab assign`) ha proprio queste corsie ([guida](../../guide/server.it.md#3-il-laboratorio-standard));
  `/mine lab reset` lo libera dopo la lezione.
- Nient'altro da preparare: quando la lezione parte MINE crea Byte a chi non ce l'ha, e a
  ogni tappa mette **64 assi** nello slot evidenziato di Byte.
- Prova tu stesso la lezione: avviala con `/mine lesson start debugging` e correggi i tre
  programmi. Richiede 10 minuti. Le versioni corrette sono in [`solutions/it/`](solutions/it/).

## Missioni: MINE distribuisce i programmi rotti

All'inizio della lezione scrivi:

```
/mine lesson start debugging @a
```

MINE copia **tre programmi rotti** su Byte di ogni studente, e il terminale lo dice:
*"Nuovi programmi per la lezione: fila_rotta, pavimento_rotto, quadrato_rotto"*. Chi ha il gioco in
italiano li riceve con nomi e commenti in italiano; negli altri casi arrivano in inglese
(`broken_row`, `broken_floor`, `broken_square`). Se un Byte è spento, i programmi arrivano
quando viene acceso.

| Tappa | Programma | Il bug | Superata quando… |
|---|---|---|---|
| 1 · `typo` | `fila_rotta` | un'istruzione scritta male: **si ferma con un errore** | il programma costruisce una fila dritta di 6 blocchi |
| 2 · `missing_end` | `pavimento_rotto` | manca un `end`: Lua **non lo fa partire** | costruisce un pavimento lungo 4 e largo 3 |
| 3 · `wrong_size` | `quadrato_rotto` | nessun errore, ma costruisce **la misura sbagliata** | costruisce un quadrato 4 × 4 |

Come nella lezione 2, MINE guarda cosa costruisce Byte, non il codice. I programmi vengono
copiati di nuovo ogni volta che la lezione inizia: `/mine lesson restart` dà allo studente dei
programmi rotti nuovi, **sovrascrivendo le sue modifiche**. Anche chi crea un nuovo Byte li
riceve di nuovo.

Gli altri comandi sono gli stessi della lezione 1 (vedi la
[guida della lezione 1](../01-first-steps/README.it.md#missioni-mine-controlla-le-tappe)).

## Con i blocchi

La lezione funziona anche nell'[editor a blocchi](../../guide/blocks.it.md). Quando la lezione
inizia, MINE mette anche **tre programmi a blocchi rotti** in **I miei programmi** di ogni
studente, con gli stessi nomi (`fila_rotta`, `pavimento_rotto`, `quadrato_rotto`; in inglese
`broken_row`, `broken_floor`, `broken_square`): gli studenti ne aprono uno, premono **Esegui ▶**,
guardano, correggono i blocchi e lo eseguono di nuovo. Come quelli in Lua, vengono dati di nuovo
quando la lezione riparte, al posto delle modifiche dello studente.

Con i blocchi Lua **non può rifiutare un programma**: i blocchi si incastrano solo in modi che
hanno senso, quindi niente errori di battitura e niente `end` mancanti. È un ottimo spunto di
discussione (è il motivo per cui esistono i blocchi), ed è per questo che i bug a blocchi sono
diversi:

| Tappa | Programma | Il bug con i blocchi | Corretto |
|---|---|---|---|
| 1 | `fila_rotta` | una **scelta sbagliata**: `piazza un blocco davanti` invece di `sotto`. Byte mette un blocco in aria e resta lì | [fila_rotta.json](solutions/it/blocks/fila_rotta.json) |
| 2 | `pavimento_rotto` | un blocco **nel posto sbagliato**: `vai su` è dentro il ciclo delle file, quindi ogni fila è un blocco più in alto: una scala. MINE dice *"12 blocchi, lunghi 4, larghi 3 e alti 3"* | [pavimento_rotto.json](solutions/it/blocks/pavimento_rotto.json) |
| 3 | `quadrato_rotto` | i **numeri sbagliati**, come in Lua: ogni fila si ripete 3 volte invece di 4 | [quadrato_rotto.json](solutions/it/blocks/quadrato_rotto.json) |

Il metodo del detective resta lo stesso, con *guarda Byte* e *leggi le misure di MINE* al
posto della lettura del messaggio d'errore. Il bug 2 è la versione a blocchi dell'`end` mancante:
in tutti e due i casi un pezzo di programma è dentro il ciclo sbagliato. I programmi corretti (in
inglese in [`solutions/blocks`](solutions/blocks)) si aprono con **Apri file**.

## Svolgimento

### Introduzione — 5 minuti, senza computer

Racconta la storia del primo "bug": nel 1947 il gruppo che lavorava al computer Harvard Mark II
trovò una vera falena incastrata in uno dei suoi relè, e la attaccò con il nastro adesivo nel
registro di lavoro come il "primo vero caso di bug trovato". Gli ingegneri chiamavano già *bug*
(insetto) i guasti; da allora trovarli e correggerli si chiama **debugging**.

Poi: *"Oggi il vostro Byte ha tre programmi scritti da qualcuno che andava di fretta. Ognuno ha
un bug. Il vostro lavoro è fare i detective."*

Scrivi alla lavagna il metodo del detective — è la vera lezione di oggi:

1. **Avvia** il programma e guarda cosa succede.
2. **Leggi** il messaggio di errore, tutto.
3. **Trova** la riga.
4. **Ipotizza** cosa non va, e **cambia una cosa sola**.
5. **Riprova.**

I messaggi di errore di Lua sono in inglese: è normale, e leggerli insieme è parte della lezione.

### Attività 1 — `fila_rotta`: il programma si ferma (10 minuti)

Gli studenti scrivono `ls` per vedere i nuovi programmi, poi avviano `fila_rotta`. Byte sale
di un blocco e si ferma. Il terminale mostra:

```
/fila_rotta.lua:5: attempt to call
field 'placedown' (a nil value)
Line 5
  byte.placedown()

Did you mean: placeDown?
```

Smontate insieme il messaggio, alla lavagna:

- `fila_rotta.lua:5` — **quale programma, quale riga**. Riga 5, contando anche i commenti.
- `attempt to call field 'placedown' (a nil value)` — "tentativo di chiamare il campo
  placedown (che vale nil)": Lua ha cercato un'istruzione chiamata `placedown` e non ha trovato
  niente (`nil`).
- `Did you mean: placeDown?` — "Forse intendevi placeDown?": a volte il computer suggerisce
  persino la correzione!

Poi `edit fila_rotta`, correggi la riga 5, salva e riavvialo.

Domanda per la discussione: perché Byte è salito prima di fermarsi? (Lua esegue le righe in
ordine: `byte.up()` viene prima della riga sbagliata. È un **errore di esecuzione**: il
programma è partito, e si è fermato quando è arrivato al bug.)

### Attività 2 — `pavimento_rotto`: Lua non lo fa partire (15 minuti)

Avvia `pavimento_rotto`. Questa volta Byte **non si muove per niente**, nemmeno in alto. Il
terminale dice:

```
Unexpected end of file. Expected end or
another statement.

Line 3
for fila = 1, 3 do
Block started here.

Line 17

Expected end of block here.
```

("Fine del file inaspettata. Mi aspettavo end o un'altra istruzione. Il blocco è iniziato qui…
Mi aspettavo la fine del blocco qui.") È un **errore di sintassi**: prima di eseguire qualsiasi
cosa, Lua legge tutto il programma e controlla che sia scritto bene — come controllare che ogni
parentesi aperta sia chiusa. Ha trovato un `for` che non viene mai chiuso.

La parte interessante: Lua indica la riga 3 (il ciclo esterno), ma l'`end` che manca è del
**primo ciclo interno**, quello che va avanti. Lua non può sapere dove *volevamo* chiuderlo: sa
solo che, alla fine del file, qualcosa è ancora aperto. Chiedi agli studenti di abbinare ogni
`for` al suo `end` seguendo lo schermo con il dito. L'indentazione mostra dove dovrebbe stare
l'`end`.

Soluzione: [`solutions/it/pavimento_rotto.lua`](solutions/it/pavimento_rotto.lua).

### Attività 3 — `quadrato_rotto`: nessun errore, risultato sbagliato (15 minuti)

Avvia `quadrato_rotto`. Nessun errore: Byte costruisce, dice "Quadrato finito!"… ma MINE
risponde: *"Non ancora: il tuo Byte ha costruito 12 blocchi, lunghi 4, larghi 3 e alti 1."*

È un **errore di logica**, il tipo più difficile: il programma fa esattamente quello che c'è
scritto, ma quello che c'è scritto non è quello che volevamo. Nessun messaggio indica la riga.
Gli indizi:

- La misura di MINE: 4 file da **3**, invece di 4 file da 4.
- Quali numeri del programma valgono 3? Quali dovrebbero valere 4?

Una trappola frequente, e un'ottima discussione: chi cambia solo il **primo** `3` ottiene
*"16 blocchi, lunghi 7, larghi 4"*: il numero giusto di blocchi, ma ogni fila comincia un blocco
più avanti, come una scala. Byte va avanti di 4 ma torna indietro solo di 3. I due cicli di
una fila devono ripetere lo stesso numero di volte.

Soluzione: [`solutions/it/quadrato_rotto.lua`](solutions/it/quadrato_rotto.lua).

### Sfida extra — Per chi finisce prima

Scrivi un breve programma **con un bug dentro**, su carta o sul tuo Byte, e dallo da correggere a
un compagno. Quale tipo di bug è più difficile da trovare?

### Conclusione — 5 minuti

- Torna al metodo del detective alla lavagna: quale passo è servito di più?
- I tre tipi di bug: come si riconosce ognuno? (Lua non lo fa partire / si ferma a metà /
  funziona ma fa la cosa sbagliata.)
- La domanda di riflessione che MINE mostra alla fine: *Quale errore è stato più difficile da
  trovare: quelli con un messaggio di errore, o quello senza? Perché?*

## Problemi frequenti

| Cosa succede | Perché | Cosa fare |
|---|---|---|
| I nuovi programmi non ci sono | Byte era spento, o la lezione non è stata avviata | clic destro su Byte per accenderlo e scrivi `ls`; controlla con `/mine lesson status` |
| I programmi hanno nomi in inglese | il gioco dello studente non è in italiano | va bene così: `broken_row`, `broken_floor`, `broken_square`; oppure imposta il gioco in italiano e fai `/mine lesson restart` |
| Le correzioni di uno studente sono sparite | la lezione è stata ricominciata, e questo copia di nuovo i programmi | correggili di nuovo (la seconda volta si fa prima!) |
| La fila corretta galleggia un blocco più in alto | il tentativo rotto era già salito prima di fermarsi | conta lo stesso; oppure `/mine byte recall` prima di riavviarlo |
| `edit fila_rotta` apre un file vuoto | un errore nel nome | `ls` mostra i nomi esatti |
| Dopo una correzione compare un errore nuovo e diverso | è un passo avanti: il primo bug non c'è più, ora Lua arriva al problema successivo | leggi il nuovo messaggio: quale riga adesso? |
| MINE dice "16 blocchi, lunghi 7, larghi 4" | è stato cambiato solo uno dei due cicli della fila | entrambi i cicli di una fila devono ripetere 4 volte |
| Byte sbatte contro la costruzione precedente | non c'è abbastanza spazio | `/mine byte recall` da un punto libero |

## Per l'insegnante: cosa osservare

- **Leggono tutto il messaggio**, compreso il numero di riga, prima di cambiare qualcosa?
- **Cambiano una cosa alla volta**, o riscrivono tutto a caso?
- **Usano le misure di MINE** come indizi per l'errore di logica?

## Riferimenti

- [Lezione 2 — I cicli](../02-loops/README.it.md)
- I programmi rotti sono nei file di dati di MINE:
  [`mine_lesson_files/debugging/`](../../../src/main/resources/data/mine/mine_lesson_files/debugging/).
  Le versioni corrette sono in [`solutions/it/`](solutions/it/) (in inglese in [`solutions/`](solutions/))
  e vengono provate in automatico a ogni modifica di MINE, così come il fatto che quelle rotte
  siano davvero rotte.
