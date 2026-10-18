[English](README.md) · **Italiano**

# Lezione 6 — Redstone: Byte ascolta e accende

| | |
|---|---|
| **Durata** | 1 ora (50–60 minuti) |
| **Età** | 10–14 anni (anche prima superiore) |
| **Concetti** | ingresso e uscita, segnale acceso/spento, aspettare un evento, porte logiche E / O |
| **Prerequisiti** | [lezione 2](../02-loops/README.it.md) (cicli) e [lezione 4](../04-conditions/README.it.md) (condizioni, `while`) |

## Obiettivi

Alla fine della lezione lo studente sa:

1. distinguere un **ingresso** (leva, pulsante, sensore) da un'**uscita** (lampada, porta, pistone);
2. far **accendere** a Byte un'uscita con `redstone.setOutput`;
3. far **ascoltare** a Byte un ingresso con `redstone.getInput`, e aspettare che cambi con
   `os.pullEvent("redstone")`;
4. costruire una **porta logica E** (AND): un'uscita accesa solo quando due ingressi lo sono.

È anche la preparazione alle schede esterne (micro:bit, Arduino): lì la leva diventerà un
pulsante vero.

## La scena di MINE

Quando la lezione parte, MINE costruisce intorno a ogni Byte la stessa scena, che resta per tutta
la lezione:

- una **lampada di redstone dietro** Byte (`"back"`);
- una **porta di ferro davanti** (`"front"`);
- a **sinistra** e a **destra** una colonnina di pietra con una **leva** sul lato esterno: la leva
  alimenta la pietra, e Byte sente il segnale da quel lato (`"left"`, `"right"`).

La lampada è dietro e non sopra di proposito: una lampada accesa da Byte alimenta anche i
blocchi accanto, e sopra Byte toccherebbe la porta.

Gli studenti possono tirare le leve con il tasto destro, per provare. Nelle tappe 2 e 3 le leve le
muove **MINE**, mentre il programma gira (vedi sotto).

## Preparazione (insegnante, prima della lezione)

- Un mondo **superpiatto in modalità creativa**, studenti uno accanto all'altro, con lo spazio per
  la scena: un blocco libero dietro Byte, due davanti (la porta, con terreno solido sotto), due a
  sinistra e due a destra.
- Il modo più rapido: il **laboratorio standard** (`/mine lab build <studenti>`, poi
  `/mine lab assign`), le cui corsie sono larghe 8 blocchi
  ([guida](../../guide/server.it.md#3-il-laboratorio-standard)).
- Nient'altro: MINE crea Byte a chi non ce l'ha e costruisce la scena.

## Missioni: MINE controlla le tappe

All'inizio dell'attività 1:

```
/mine lesson start redstone @a
```

| Tappa | Fatta quando… |
|---|---|
| 1 · `lamp` | quando il programma finisce, la lampada dietro Byte è accesa |
| 2 · `door` | **mentre il programma gira, MINE tira la leva di sinistra**, spegne e riaccende: la porta deve aprirsi e chiudersi ogni volta con lei |
| 3 · `and` | **MINE prova le due leve in tutti e quattro i modi**: la lampada deve accendersi solo con tutte e due accese |

Le tappe 2 e 3 sono un **controllo dal vivo**. Il programma degli studenti ascolta per sempre
(`while true do ... end`), e MINE prova ogni caso: un programma che accende sempre la lampada, o che
guarda una leva sola, non passa per fortuna. Finita la prova, MINE **ferma da solo il programma**
e dice com'è andata, nel terminale e in chat. Se il programma finisce da solo
prima della prova, MINE spiega che deve restare in ascolto.

Gli altri comandi sono gli stessi delle altre lezioni: `/mine lesson status @a`, `/mine lesson hint`,
`/mine lesson restart`, `/mine lesson reset <studenti>` (vedi la
[guida della lezione 1](../01-first-steps/README.it.md#missioni-mine-controlla-le-tappe)).

## Con i blocchi

Tutta la lezione si fa anche nell'[editor a blocchi](../../guide/blocks.it.md) (`/mine blocks`, poi
**Esegui ▶**), con la categoria **Redstone**: *segnale redstone [lato] a …*, *segnale da [lato]?*,
*aspetta un cambio di redstone*; *aspetta N secondi* è in **Cicli**.

| Attività | Con i blocchi | File |
|---|---|---|
| 1 · La lampada | `segnale redstone dietro a vero` | [lampada.json](solutions/it/blocks/lampada.json) |
| 3 · La porta | vedi sotto | [porta.json](solutions/it/blocks/porta.json) |
| 4 · E | vedi sotto | [e.json](solutions/it/blocks/e.json) |

```
ripeti mentre vero                          ripeti mentre vero
    segnale redstone davanti a                  segnale redstone dietro a
        segnale da sinistra?                        segnale da sinistra? e segnale da destra?
    aspetta un cambio di redstone               aspetta un cambio di redstone
```

## Svolgimento

### Introduzione — 5 minuti, senza computer

Ingressi e uscite intorno a noi: il campanello (pulsante → suono), il lampione (sensore di luce →
luce), il cancello automatico (telecomando → motore). Chi decide cosa fa l'uscita? A volte niente
(il filo del campanello), a volte un programma (il cancello).

In Minecraft gli **ingressi** di redstone sono leve, pulsanti, piastre a pressione, sensori di
luce; le **uscite** sono lampade, porte di ferro, pistoni. Byte può essere entrambe le cose: un
programma in mezzo.

### Attività 1 — Byte accende la lampada (10 minuti)

```lua
redstone.setOutput("back", true)
```

Il primo valore è il **lato** di Byte: `"back"` (dietro), `"front"` (davanti), `"left"`,
`"right"`, `"top"` (sopra), `"bottom"` (sotto). Il secondo: `true` acceso, `false` spento. Il
segnale resta acceso anche quando il programma finisce: per spegnerlo serve `false`.

Soluzione: [`solutions/it/lampada.lua`](solutions/it/lampada.lua).

### Attività 2 — Lampeggia! (10 minuti)

Un ripasso dei cicli, senza missione: la lampada si accende e si spegne 5 volte.

```lua
for i = 1, 5 do
  redstone.setOutput("back", true)
  os.sleep(0.5)
  redstone.setOutput("back", false)
  os.sleep(0.5)
end
```

`os.sleep(0.5)` aspetta mezzo secondo. Domande: come lo facciamo più veloce? E se togliamo il
secondo `os.sleep`? (La lampada si spegne e si riaccende subito: sembra sempre accesa.)

Soluzione: [`solutions/it/lampeggia.lua`](solutions/it/lampeggia.lua).

### Attività 3 — La porta che ascolta (15 minuti)

**Compito:** la porta segue la leva di sinistra.

```lua
while true do
  redstone.setOutput("front", redstone.getInput("left"))
  os.pullEvent("redstone") -- aspetta che un segnale cambi
end
```

`redstone.getInput("left")` risponde `true` quando la leva di sinistra è accesa. Il valore va dritto
alla porta. `os.pullEvent("redstone")` **aspetta** che un segnale cambi, invece di controllare di
continuo: il programma non consuma niente mentre aspetta.

Prima che MINE faccia la sua prova, lasciate che gli studenti tirino la leva da soli. Il programma
non finisce mai: per fermarlo si tengono premuti **Ctrl+T** nel terminale **per un secondo** (non
basta toccarli): compare *Terminated*, poi il prompt `>`.

> **Guardare la scena:** il terminale copre la lampada e la porta. Un programma che ascolta continua
> a girare anche chiudendo il terminale con **Esc**: tirate la leva e guardate. Dopo averlo fermato
> (Ctrl+T), **R** fa ripartire l'ultimo programma di Byte senza aprire il terminale.

Soluzione: [`solutions/it/porta.lua`](solutions/it/porta.lua).

### Attività 4 — Due leve: E e O (15 minuti)

**Compito:** la lampada si accende solo quando la leva di sinistra **e** quella di destra sono
accese.

```lua
while true do
  local tutte_e_due = redstone.getInput("left") and redstone.getInput("right")
  redstone.setOutput("back", tutte_e_due)
  os.pullEvent("redstone")
end
```

Poi la variante con `or` (basta una leva), da provare a mano. Discussione: una cassaforte con due
chiavi è E o O? E il campanello di casa, che suona dalla porta e dal cancello?

Soluzione: [`solutions/it/e.lua`](solutions/it/e.lua).

### Sfida extra — Per chi finisce prima

- **Porta automatica:** mettete una piastra a pressione sopra la colonnina di sinistra: chi ci sale
  sopra alimenta la pietra come la leva, e la porta si apre.
- **Contatore:** Byte conta quante volte la leva è stata accesa e lo dice con `mine.say`.
- **Porta O:** la lampada accesa con almeno una delle due leve.

### Conclusione — 5 minuti

- Cos'è un ingresso, e cos'è un'uscita? Dove li avete visti fuori da Minecraft?
- Perché nelle tappe 2 e 3 il programma non finisce mai?
- La domanda di riflessione che MINE mostra alla fine: *Dove trovi nella vita reale un ingresso e
  un'uscita come le leve e la lampada di Byte? Pensa a un dispositivo che per funzionare ha
  bisogno di due cose insieme (E).*

## Problemi frequenti

| Cosa succede | Perché | Cosa fare |
|---|---|---|
| MINE dice che serve spazio intorno a Byte | c'è qualcosa dietro, davanti o ai lati, o manca il terreno sotto la porta | `/mine byte recall` da un punto libero, su terreno piano |
| `bad argument #1 (unknown option black)` | il nome del lato è scritto male (qui `"black"` invece di `"back"`) | i lati sono in inglese: `"back"`, `"front"`, `"left"`, `"right"`, `"top"`, `"bottom"` |
| La lampada non si accende | il lato è sbagliato (`"top"` invece di `"back"`) | la lampada è **dietro**: `"back"` |
| La lampada resta accesa anche dopo il programma | è normale: il segnale resta finché non lo spegne un `false` | `redstone.setOutput("back", false)` |
| La porta si apre una volta e poi non segue più la leva | manca il `while true`: il programma finisce subito | `while true do ... end` |
| MINE dice che il programma è finito prima della prova | come sopra | il programma deve restare in ascolto |
| Ctrl+T non ferma il programma | è stato solo toccato | aprire il terminale, cliccarci dentro e **tenere premuti** Ctrl+T per un secondo, finché compare *Terminated* |
| Lo studente ha rotto Byte o un pezzo della scena | in creativa basta un clic | `/mine byte` da un punto libero: il nuovo Byte ha gli stessi programmi del vecchio, e la scena si ricostruisce intorno a lui quando parte il programma |
| Con il programma E la porta non si apre | `e` comanda solo la lampada (`"back"`): la porta è il compito di `porta` | fermare `e` con Ctrl+T, poi eseguire `porta` |
| La lampada si accende con una leva sola | è una porta O (`or`), non E | `and` |

## Per l'insegnante: cosa osservare

- **Sanno dire** quale lato di Byte è un ingresso e quale un'uscita, in ogni attività?
- **Provano a mano** la leva prima che MINE faccia la sua prova?
- **Spiegano a parole** la differenza tra E e O con un esempio di vita reale?

## Riferimenti

- [Lezione 4 — Le condizioni](../04-conditions/README.it.md)
- [L'API `redstone` di CC: Tweaked](https://tweaked.cc/module/redstone.html); in gioco,
  `help redstone` nel terminale di Byte.
- Le soluzioni sono in [`solutions/it/`](solutions/it/) (in inglese in [`solutions/`](solutions/)),
  anche a blocchi, e vengono provate in automatico a ogni modifica di MINE, con le leve mosse da MINE.
