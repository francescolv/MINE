[English](README.md) · **Italiano**

# Lezione 1 — Primi passi con Byte

| | |
|---|---|
| **Durata** | 1 ora (50–60 minuti) |
| **Età** | 10–14 anni (anche prima superiore) |
| **Concetti** | istruzione, sequenza, programma, direzione |
| **Prerequisiti** | saper muoversi in Minecraft; nessuna esperienza di programmazione |

## Obiettivi

Alla fine della lezione lo studente sa:

1. dare un'istruzione a Byte e vedere cosa succede;
2. scrivere, salvare e avviare un programma;
3. spiegare che un programma è una **sequenza** di istruzioni eseguite **in ordine**, una alla volta;
4. prevedere dove si troverà Byte dopo una sequenza di movimenti.

## Preparazione (insegnante, prima della lezione)

- Server con MINE e CC: Tweaked (vedi il [README](../../../README.it.md#requisiti)).
- Un mondo **superpiatto in modalità creativa**, così nessuno resta senza blocchi.
  Per ora la lezione non ha un mondo dedicato: va bene un'area ampia e piana con almeno
  5 blocchi di spazio libero tra uno studente e l'altro.
- Prova tu stesso le attività 2–4: richiedono 10 minuti e scopri subito eventuali problemi
  di tastiera o di permessi.
- Se usate portatili con touchpad, leggi la [guida per il laboratorio](../../guide/linux-lab.it.md):
  il clic destro col touchpad può rompere Byte invece di aprirlo.

## Missioni: MINE controlla le tappe

Le attività 2–4 sono anche **missioni** che il server controlla da solo, così vedi subito chi
è bloccato. All'inizio della lezione scrivi:

```
/mine lesson start first_steps @a
```

`@a` sono tutti gli studenti collegati: gli insegnanti (gli operatori) restano fuori. Per provare
la lezione da te, `/mine lesson start first_steps` senza `@a`.

Ogni studente riceve in chat l'obiettivo della prima tappa; chi non ha ancora un Byte se lo
trova davanti (nell'attività 1 `/mine byte` lo sostituisce, e va bene così). Ogni volta che un suo programma
finisce senza errori, MINE guarda cosa ha fatto davvero il suo Byte e, se la tappa è superata,
lo porta alla successiva con un titolo e un suono:

| Tappa | Superata quando… |
|---|---|
| 1 · `hello` | il programma ha fatto parlare Byte con `mine.say` |
| 2 · `roundtrip` | Byte si è allontanato di almeno 3 blocchi ed è tornato al punto di partenza, rivolto come prima |
| 3 · `tower` | c'è una torre di 3 blocchi proprio davanti al punto di partenza di Byte, e Byte è tornato a terra |

MINE non legge il codice: vale qualunque programma che faccia il lavoro, con qualunque nome e
qualunque messaggio (`saluta`, `andata` e `torre` vanno benissimo). I comandi scritti uno alla
volta in `lua` non contano: salvare un programma è proprio lo scopo dell'attività 2.

| Comando | Chi | Cosa fa |
|---|---|---|
| `/mine lesson status @a` | insegnante | a che punto è ogni studente, es. "Luca — Primi passi con Byte: tappa 2 di 3" |
| `/mine lesson status` | studente | la propria tappa e il suo obiettivo |
| `/mine lesson hint` | studente | il prossimo suggerimento per la sua tappa (due per tappa, poi "chiedi all'insegnante") |
| `/mine lesson restart` | studente | ricomincia la lezione dalla tappa 1 |
| `/mine lesson reset <studenti>` | insegnante | cancella i loro progressi (non le risposte) |
| `/mine lesson answer <testo>` | studente | alla fine: la sua risposta alla domanda di riflessione |
| `/mine lesson answers <lezione>` | insegnante | tutte le risposte alla domanda di quella lezione, anche di chi è uscito |

"Insegnante" vuol dire operatore (livello di permesso 2): `/op <nome>` sul server. I progressi
sono salvati nel mondo e sopravvivono al riavvio del server. Se uno studente rompe il suo Byte
e ne crea uno nuovo, non perde i progressi.

Alla fine ogni studente vede "Missione completata!" e la domanda di riflessione: *Perché è utile
salvare le istruzioni in un programma invece di scriverle una alla volta?* Può rispondere in chat
con `/mine lesson answer` (il messaggio è cliccabile). `/mine lesson status @a` mostra ogni
risposta sotto la riga dello studente, e `/mine lesson answers first_steps` le elenca tutte, così
nella chiusura puoi leggerne qualcuna ad alta voce. Le risposte restano salvate nel mondo per ogni
lezione, anche dopo un reset o quando la classe passa alla lezione successiva.

## Con i blocchi

Gli studenti che non scrivono ancora in Lua possono fare tutta la lezione nell'[editor a
blocchi](../../guide/blocks.it.md) di MINE: scrivono `/mine blocks` nella chat, uniscono i blocchi
nel browser e premono **Esegui ▶**. Le missioni guardano il mondo, quindi valgono allo stesso modo.
Accanto ai blocchi l'editor mostra il Lua che creano: lo stesso Lua qui sotto, che rende più facile
il passaggio al testo più avanti.

| Attività | Con i blocchi | File |
|---|---|---|
| 1 · Il primo comando | un blocco da solo, es. `vai avanti`, poi **Esegui ▶** | |
| 2 · `saluta` | `di' "Ciao! Sono il tuo Byte."` · `di' "Sono pronto per le istruzioni."` | [saluta.json](solutions/it/blocks/saluta.json) |
| 3 · Andata e ritorno | vedi sotto | [andata.json](solutions/it/blocks/andata.json) |
| 4 · La torre | vedi sotto | [torre.json](solutions/it/blocks/torre.json) |

```
ripeti 3 volte                      piazza un blocco davanti
    vai avanti                      vai su
ripeti 2 volte                      piazza un blocco davanti
    gira a sinistra                 vai su
ripeti 3 volte                      piazza un blocco davanti
    vai avanti                      ripeti 2 volte
ripeti 2 volte                          vai giù
    gira a sinistra                 di' "Torre finita!"
di' "Sono tornato!"
```

Il nome del programma è quello in alto nell'editor (`blocchi` all'inizio): gli studenti danno a
ogni attività il suo (`saluta`, `andata`, `torre`) così li ritrovano in **I miei programmi**. I file
si aprono con **Apri file**, per esempio per mostrare una soluzione al proiettore. Le soluzioni in
inglese sono in [`solutions/blocks`](solutions/blocks).

## Svolgimento

### Introduzione — 5 minuti, senza computer

Chiedi a uno studente di dare istruzioni a un compagno per andare dalla porta alla cattedra.
Scrivi alla lavagna le istruzioni man mano: "avanti 3 passi", "gira a destra"…

Fai notare che:

- ogni istruzione è **semplice** e **precisa** ("vai di là" non funziona);
- l'**ordine** conta: "gira, poi avanti" porta in un posto diverso da "avanti, poi gira";
- una lista di istruzioni in ordine è un **programma**. Il computer la segue alla lettera.

### Attività 1 — Il primo comando (10 minuti)

1. Ogni studente scrive in chat `/mine byte`: davanti a lui compare il suo Byte.
2. Clic destro su Byte: si apre il suo schermo, il **terminale**.
3. Scrivere `lua` e premere Invio. Ora Byte esegue ogni riga che scriviamo.
4. Provare, una alla volta:

   ```lua
   byte.forward()
   byte.turnLeft()
   byte.up()
   byte.down()
   ```

5. Ogni istruzione risponde `true` (fatto) o `false` (non ci sono riuscito). Chiedi:
   *quando risponde `false`?* (quando c'è un blocco davanti, o sotto).
6. Per uscire dalla modalità `lua` scrivere `exit()`: il prompt torna a essere `>` invece di `lua>`.

> **Se Byte si allontana troppo:** `/mine byte recall` lo riporta davanti allo studente,
> con tutti i suoi programmi.

### Attività 2 — Il primo programma: `saluta` (10 minuti)

Scrivere istruzioni una alla volta è scomodo: un programma le salva per riusarle.

1. Controllare che il prompt sia `>` e **non** `lua>` (altrimenti prima `exit()`).
   Poi scrivere `edit saluta`: lo schermo si svuota e in basso compare `Press Ctrl to access menu`.
2. Scrivere:

   ```lua
   mine.say("Ciao! Sono Byte di " .. mine.owner() .. ".")
   mine.say("Sono pronto a ricevere istruzioni.")
   ```

3. Premere e rilasciare **Ctrl** (una volta sola). Non si apre una finestra: cambia
   l'**ultima riga in basso**, che diventa `[Save]  Exit`. La voce tra parentesi quadre è
   quella scelta: con Invio si salva. Poi di nuovo **Ctrl**, freccia **→** su `Exit`, Invio.
4. Sempre nel terminale di Byte, scrivere `saluta` e premere Invio: Byte scrive i suoi
   messaggi nel terminale e in chat. La chat si vede chiudendo il terminale con Esc.

> **Attenzione:** scrivere `saluta` nella chat di Minecraft (tasto T) non avvia niente: è solo
> un messaggio. Dalla chat un programma si avvia con `/mine byte run saluta`.

Cose da spiegare:

- `mine.say(...)` fa scrivere a Byte un messaggio in chat. Lo leggono i giocatori vicini.
- `mine.owner()` è il nome di chi possiede Byte: ognuno vede il proprio nome.
- Le virgolette `"..."` racchiudono un **testo**; `..` unisce due testi.
- I due messaggi arrivano **sempre in quell'ordine**: è una sequenza.

Per variare: far scrivere a ognuno una presentazione di tre righe con un messaggio a scelta.

### Attività 3 — Andata e ritorno (10 minuti)

**Consegna:** Byte deve fare 3 passi avanti, tornare al punto di partenza e guardare
di nuovo nella direzione iniziale. Alla fine dice "Sono tornato!".

Prima di scrivere il programma, chiedi agli studenti di **prevedere** la sequenza su un
foglio (o alla lavagna). Poi `edit andata`, scrivere, salvare, avviare con `andata`.

Domande per la discussione:

- Quante volte bisogna girare per tornare indietro? (2 volte a sinistra, oppure 2 a destra)
- Se togliamo le ultime due svolte, Byte torna lo stesso? (sì, ma guarda dalla parte sbagliata)

Per guardare Byte mentre si muove conviene avviarlo **dalla chat**: chiudere il suo
schermo con Esc e scrivere `/mine byte run andata`. Quando ha finito, MINE lo dice in chat.

Soluzione: [`solutions/it/andata.lua`](solutions/it/andata.lua).

### Attività 4 — La torre (15 minuti)

**Consegna:** Byte costruisce davanti a sé una torre alta 3 blocchi e poi torna a terra.

Prima di cominciare Byte ha bisogno di blocchi. Aprirlo col clic destro: la griglia
**4×4 a destra** è il suo inventario (quella a sinistra è dello studente). Trascinare almeno
3 blocchi nello slot **in alto a sinistra della griglia 4×4**, quello evidenziato, **tutti nello
stesso slot**: `byte.place()` usa solo lo slot evidenziato, e quando è vuoto non passa da solo
agli altri.

Le istruzioni nuove:

| Istruzione | Cosa fa |
|---|---|
| `byte.place()` | mette un blocco davanti a Byte |
| `byte.placeUp()` / `byte.placeDown()` | mette un blocco sopra / sotto |

Suggerimento da dare solo se servono: "metti un blocco, sali, metti un blocco…".

Soluzione: [`solutions/it/torre.lua`](solutions/it/torre.lua).

Domanda per la discussione, se a qualcuno capita: perché Byte ha detto "Torre finita!" anche
se la torre non c'era? (`byte.place()` quando non riesce a mettere il blocco non dà errore:
restituisce `false`, e il programma va avanti. Lo useremo nella [lezione 4](../04-conditions/README.it.md).)

### Sfida extra — Per chi finisce prima

Costruire una torre **alta 10 blocchi**. Quante righe servono? Cosa succede se scrivi
una riga in meno?

Lascia che se ne accorgano da soli: è noioso e si sbaglia facilmente a contare. È proprio il
problema che risolveremo nella lezione 2 con i **cicli**.

### Chiusura — 5 minuti

Riprendi la lavagna dell'introduzione e chiedi:

- Cos'è un programma? (una sequenza di istruzioni)
- Perché l'ordine conta? (fare un esempio con la torre: salire prima di mettere il blocco)
- Cosa vi ha dato più fastidio scrivere? (aggancio alla lezione 2)

## Errori frequenti

| Cosa succede | Perché | Cosa fare |
|---|---|---|
| `attempt to call field ... (a nil value)` o `attempt to index global ...` | nome scritto male: `byte.foward()`, `Turtle.forward()` | le maiuscole contano: tutto minuscolo tranne `turnLeft`/`turnRight` |
| Premendo Ctrl "non succede niente" | il menu è nell'ultima riga in basso, non in una finestra; un secondo Ctrl lo richiude | premere Ctrl una volta e guardare in basso: `[Save]  Exit` |
| Nel menu manca `Save` | il file è di sola lettura (ad esempio un file in `rom/`) | aprire il proprio programma con `edit nome` |
| `edit saluta` dà un errore e Ctrl non apre nessun menu | si è ancora nella modalità `lua` (prompt `lua>`), dove `edit` non esiste | `exit()`, poi `edit saluta` |
| Scritto `saluta` nella chat, Byte non fa niente | la chat di Minecraft non avvia i programmi | scriverlo nel terminale di Byte, oppure in chat `/mine byte run saluta` |
| `/mine byte run` risponde che Byte è occupato | un programma è ancora in corso, oppure è aperto l'editor o `lua` | nel terminale di Byte: Ctrl+T tenuti premuti per un secondo, oppure uscire dall'editor / `exit()` |
| `No such program` | il programma non è stato salvato, o il nome è diverso | `ls` mostra i programmi salvati |
| Byte non si muove, risponde `false` | c'è un blocco davanti | spostare Byte con `/mine byte recall` in uno spazio libero |
| La torre non compare: Byte sale e scende ma non mette blocchi | Byte non ha blocchi nello slot selezionato: spesso sono rimasti nell'inventario dello studente | trascinarli nello slot evidenziato della griglia 4×4 a destra |
| La torre si ferma al primo blocco | i blocchi sono sparsi in più slot (uno per slot): `byte.place()` usa solo quello evidenziato | metterli tutti nello slot evidenziato |
| Il clic destro rompe Byte | touchpad: il tocco in basso a destra è un clic sinistro | vedi la [guida per il laboratorio](../../guide/linux-lab.it.md) |
| Byte è sparito | rotto per sbaglio | `/mine byte` ne crea uno nuovo (i programmi del vecchio si perdono) |

## Per l'insegnante: cosa osservare

- **Prevede prima di provare?** Lo studente che scrive la sequenza su carta prima di eseguirla
  sta già ragionando come un programmatore.
- **Corregge da solo?** Quando Byte fa una cosa diversa dal previsto, sa trovare la riga sbagliata?
- **Usa il programma salvato** invece di riscrivere tutto nel `lua` interattivo?

## Riferimenti

- In gioco: `help mine` e `help byte` nel terminale di Byte.
- [API Lua di MINE](../../guide/lua-api.it.md)
- Le soluzioni sono in [`solutions/it/`](solutions/it/) e vengono provate automaticamente a ogni
  modifica di MINE: se una lezione smette di funzionare ce ne accorgiamo.
