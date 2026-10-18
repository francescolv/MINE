[English](README.md) · **Italiano**

# Lezione 10 — Il labirinto: una mano sul muro

| | |
|---|---|
| **Durata** | 1 ora (50–60 minuti) |
| **Età** | 10–14 anni |
| **Concetti** | algoritmo, `while` e `if` insieme, condizioni con `and` e `not`, contatore |
| **Prerequisiti** | [lezione 4](../04-conditions/README.it.md) (`while`, `if`, `byte.detect`); per la tappa 3, la [lezione 5](../05-functions/README.it.md) (variabili) |

## Obiettivi

Alla fine della lezione lo studente sa:

1. descrivere un **algoritmo** a parole, prima di scriverlo: "tieni la mano destra sul muro";
2. combinare `while` e `if` in un programma che decide a ogni passo;
3. unire due condizioni con `and` e `not` ("c'è un muro **e** non è l'oro");
4. contare con una variabile quello che il programma fa.

## Come funziona

Come nella lezione 4, **MINE costruisce la sfida da solo** davanti a Byte, e ogni volta è
diversa: un labirinto di mattoni di pietra alto due blocchi, con un **blocco d'oro** nel muro
dove c'è l'uscita. Byte parte dall'ingresso. Il labirinto si allarga a destra di Byte e in
avanti, mai dietro, dove sta lo studente: largo 7 blocchi e profondo fino a 13, entra nella
corsia del [laboratorio standard](../../guide/server.it.md#3-il-laboratorio-standard).

Ogni volta che un programma finisce, riuscito o no, **MINE toglie il labirinto**, e al
programma successivo ne costruisce uno nuovo. Il percorso che ha funzionato una volta non
funzionerà la prossima: serve un algoritmo che vada bene **per tutti** i labirinti.

Conta solo camminare nei corridoi: MINE controlla che Byte non sia mai salito (niente scavalcare
i muri) e che nessun muro sia stato rotto (Byte ha un piccone!).

## Preparazione (insegnante, prima della lezione)

- Il modo più rapido: il **laboratorio standard** (`/mine lab build <studenti>`, poi
  `/mine lab assign`). Altrimenti un mondo superpiatto, studenti a **8 blocchi** l'uno dall'altro,
  con **14 blocchi liberi davanti** e **6 a destra** di ogni Byte.
- Nient'altro. A ogni tappa MINE mette **un blocco d'oro** nello slot evidenziato di Byte: serve
  a `byte.compare()` per riconoscere l'oro.
- Prova tu le tre tappe con i programmi in [`solutions/it/`](solutions/it/): 10 minuti.

## Missioni: MINE controlla le tappe

```
/mine lesson start maze @a
```

| Tappa | Ogni volta che un programma parte, MINE… | Superata quando… |
|---|---|---|
| 1 · `corridor` | costruisce un corridoio pieno di curve, senza bivi, con l'oro in fondo | Byte è proprio davanti all'oro, senza essere salito né aver rotto muri |
| 2 · `maze` | costruisce un labirinto vero, con vicoli ciechi, e l'uscita sul fondo o su un lato | come sopra |
| 3 · `steps` | come la tappa 2 | come sopra, e Byte **dice** quanti passi ha fatto: MINE li conta e confronta |

Quando qualcosa non va, MINE dice cosa: *"Byte è passato sopra i muri"*, *"ha rotto un muro"*,
*"si è fermato a 4 blocchi dall'oro"*, *"ha fatto 23 passi, ma ha detto 20"*.

**Dopo ogni tappa riuscita** Byte è all'uscita e il labirinto sparisce: lo studente scrive
`/mine byte recall` dall'inizio della corsia per riportarlo davanti a sé, ed è pronto per la tappa
dopo. Se davanti a Byte non c'è spazio, MINE lo dice.

## Con i blocchi

Tutta la lezione funziona nell'[editor a blocchi](../../guide/blocks.it.md). I blocchi che servono
sono *blocco davanti?* e *blocco davanti uguale a quello dello slot?* (in Lua `byte.detect()` e
`byte.compare()`), *ripeti fino a che*, *ripeti mentre*, *se / altrimenti*, *e*, *non* (in Logica).

| Tappa | File |
|---|---|
| 1 · Il corridoio | [corridoio.json](solutions/it/blocks/corridoio.json) |
| 2 · Il labirinto | [labirinto.json](solutions/it/blocks/labirinto.json) |
| 3 · Conta i passi | [passi.json](solutions/it/blocks/passi.json) |

```
ripeti fino a che blocco davanti uguale a quello dello slot?
    gira a destra
    ripeti mentre blocco davanti? e non blocco davanti uguale a quello dello slot?
        gira a sinistra
    se non blocco davanti uguale a quello dello slot?
        vai avanti
di' "Fuori dal labirinto!"
```

## Svolgimento

### Introduzione — 10 minuti, senza computer

Disegna un labirinto alla lavagna (o usa quello di un giornalino). Chiedi: *come uscireste da un
labirinto al buio, senza vedere niente?* Lasciali proporre. Poi racconta il trucco antico:
**tieni sempre la mano destra sul muro e cammina**. Provatelo insieme col dito sul disegno:
si esce, anche passando per i vicoli ciechi.

Scrivete insieme l'algoritmo a parole, in tre righe:

1. gira a destra;
2. finché davanti hai un muro, gira a sinistra;
3. fai un passo. E ricomincia, finché non vedi l'uscita.

### Attività 1 — Il corridoio (15 minuti)

**Compito:** portare Byte in fondo a un corridoio pieno di curve, davanti all'oro.

Prima idea, la più naturale: "vai avanti finché puoi; all'angolo guarda a destra; se è chiuso,
gira dall'altra parte".

```lua
while not byte.compare() do
  if not byte.detect() then
    byte.forward()
  else
    byte.turnRight()
    if byte.detect() and not byte.compare() then
      byte.turnLeft()
      byte.turnLeft()
    end
  end
end
mine.say("Oro!")
```

Cose da spiegare:

- `byte.compare()` è vero quando il blocco davanti è uguale a quello nello slot evidenziato:
  l'oro. Il ciclo si ferma quando Byte ha l'oro davanti.
- `and` unisce due condizioni: vere **tutte e due**. Perché serve `not byte.compare()`? (Se
  l'oro è a destra, è anche lui un "muro": senza, Byte si girerebbe dall'altra parte.)

Fai avviare il programma due o tre volte: ogni volta un corridoio diverso, e funziona sempre.

Soluzione: [`solutions/it/corridoio.lua`](solutions/it/corridoio.lua).

### Attività 2 — Il labirinto vero (15 minuti)

**Compito:** uscire da un labirinto con i vicoli ciechi.

Prima di scrivere, fai provare il programma del corridoio: in un vicolo cieco Byte gira avanti e
indietro, o si ferma nel posto sbagliato. Serve l'algoritmo della mano sul muro:

```lua
while not byte.compare() do
  byte.turnRight()
  while byte.detect() and not byte.compare() do
    byte.turnLeft()
  end
  if not byte.compare() then
    byte.forward()
  end
end
mine.say("Fuori dal labirinto!")
```

Domande per la discussione:

- Perché Byte gira **prima** a destra, anche quando potrebbe andare dritto? (Per "tenere la mano"
  sul muro di destra: se a destra c'è un passaggio, lo prende.)
- Che succede in un vicolo cieco? (Gira a sinistra tre volte e torna indietro: il vicolo è
  esplorato e lasciato.)
- Funzionerebbe anche con la mano sinistra? (Sì: basta scambiare destra e sinistra. Provatelo!)

Soluzione: [`solutions/it/labirinto.lua`](solutions/it/labirinto.lua).

### Attività 3 — Conta i passi (10 minuti)

**Compito:** lo stesso algoritmo, e all'uscita Byte dice quanti passi ha fatto. MINE li conta
anche lui e li confronta: niente numeri inventati!

```lua
local passi = 0
-- ... lo stesso ciclo, e dopo byte.forward():
    passi = passi + 1
-- ... alla fine:
mine.say("Fuori dal labirinto in " .. passi .. " passi!")
```

Soluzione completa: [`solutions/it/passi.lua`](solutions/it/passi.lua).

Domanda: la mano destra e la mano sinistra fanno lo stesso numero di passi? Fate la prova con lo
stesso labirinto disegnato alla lavagna.

### Sfida extra — Per chi finisce prima

- Scrivi la versione con la **mano sinistra**.
- Conta anche le **svolte** con una seconda variabile.
- Alla lavagna: disegna un labirinto in cui la mano sul muro **non** trova l'uscita. (Serve
  un'"isola": un'uscita al centro, attaccata a muri che non toccano il bordo. I labirinti di MINE
  non ne hanno.)

### Conclusione — 5 minuti

- Cos'è un **algoritmo**? (Una ricetta precisa che funziona sempre, non solo una volta.)
- La domanda di riflessione che MINE mostra alla fine: *Il tuo programma ha fatto uscire Byte da
  labirinti che non aveva mai visto. Perché tenere una mano sul muro funziona sempre? Quando non
  funzionerebbe?*

## Problemi frequenti

| Cosa succede | Perché | Cosa fare |
|---|---|---|
| MINE dice che servono blocchi liberi davanti a Byte | Byte è ancora all'uscita del labirinto di prima, o girato dalla parte sbagliata | `/mine byte recall` dall'inizio della corsia |
| Byte si ferma subito, senza muoversi | lo slot evidenziato è vuoto: `byte.compare()` con lo slot vuoto davanti all'aria è vero | il blocco d'oro nello slot **evidenziato**; `/mine lesson restart` lo ridà |
| Byte gira su sé stesso all'infinito | manca `not byte.compare()` nel ciclo interno, o manca il passo avanti | tieni premuti Ctrl+T per fermarlo, poi confronta con l'algoritmo alla lavagna |
| *"Byte è passato sopra i muri"* | il programma usa `byte.up()` | nel labirinto solo avanti e svolte |
| *"Byte ha rotto un muro"* | il programma usa `byte.dig()` | come sopra |
| Il conto dei passi è sbagliato | il `passi = passi + 1` è fuori dall'`if`, o conta anche le svolte | si conta solo dopo `byte.forward()` |

## Per l'insegnante: cosa osservare

- Sanno **dire a parole** l'algoritmo prima di scriverlo?
- Avviano il programma **più volte**, su labirinti diversi, prima di dire che funziona?
- Capiscono **perché** serve `and not byte.compare()`?

## Riferimenti

- [Lezione 4 — Le condizioni](../04-conditions/README.it.md), [lezione 5 — Variabili e funzioni](../05-functions/README.it.md)
- [API Lua di MINE](../../guide/lua-api.it.md); in gioco, `help byte` nel terminale di Byte.
- Le soluzioni sono in [`solutions/it/`](solutions/it/) (in inglese in [`solutions/`](solutions/))
  e vengono provate in automatico a ogni modifica di MINE, su labirinti costruiti a caso.
