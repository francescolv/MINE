[English](README.md) · **Italiano**

# Lezione 4 — Le condizioni: Byte decide

| | |
|---|---|
| **Durata** | 1 ora (50–60 minuti) |
| **Età** | 10–14 anni (anche prima superiore) |
| **Concetti** | vero e falso, `if`, `while`, sensori (`byte.detect`, `byte.compareDown`), istruzioni che rispondono `false` |
| **Prerequisiti** | [lezione 2](../02-loops/README.it.md) (cicli `for`); la [lezione 3](../03-debugging/README.it.md) aiuta |

## Obiettivi

Alla fine della lezione lo studente sa:

1. spiegare che alcune istruzioni **rispondono** `true` (vero) o `false` (falso), e usare quella risposta;
2. usare `if ... then ... end` per fare qualcosa **solo quando** una condizione è vera;
3. usare `while ... do ... end` per ripetere **finché** una condizione è vera, quando non si può
   sapere prima quante volte;
4. scrivere un programma che funziona in situazioni che non ha mai visto.

## Perché MINE cambia la sfida ogni volta

Le istruzioni della turtle perdonano molto: `byte.forward()` contro un muro, o
`byte.placeDown()` sul terreno, rispondono semplicemente `false` e il programma va avanti.
Così un programma senza nessuna condizione potrebbe spesso "passare" per caso, per esempio
facendo 20 passi e fermandosi contro il muro.

Per questo, in questa lezione, **MINE costruisce la sfida da solo**, davanti a Byte, e ogni
volta è diversa: il muro è a una distanza diversa, l'oro è in posti diversi. La sfida compare
appena inizia la tappa, e il primo programma affronta quella. Se un programma non riesce, MINE
toglie la sfida e ne costruisce una nuova e diversa quando parte il programma successivo (anche
se Byte è stato spostato); quando riesce, la sfida resta. Il numero "5" che ha funzionato
l'ultima volta non funzionerà la prossima: Byte deve **guardare** e **decidere**.

## Preparazione (insegnante, prima della lezione)

- Un mondo **superpiatto in modalità creativa**, studenti uno accanto all'altro, tutti rivolti
  dalla stessa parte, a circa **8 blocchi di distanza**, con almeno **10 blocchi liberi davanti**
  a ogni Byte.
- Il modo più rapido: il **laboratorio standard** (`/mine lab build <studenti>`, poi
  `/mine lab assign`) ha proprio queste corsie ([guida](../../guide/server.it.md#3-il-laboratorio-standard));
  `/mine lab reset` lo libera dopo la lezione.
- Nient'altro da preparare. Quando la lezione parte MINE crea Byte a chi non ce l'ha; ogni
  Byte riceve un **piccone di diamante** per scavare l'oro (si vede sul fianco), anche quelli
  creati con versioni più vecchie di MINE, senza perdere i programmi. A ogni tappa MINE mette
  nello slot evidenziato di Byte quello che serve: **un blocco d'oro** per la tappa 2, **da 3
  a 12 assi** per la tappa 3, un numero diverso per ogni studente.
- Prova tu stesso le tre tappe, con i programmi in [`solutions/it/`](solutions/it/): 10 minuti.

## Missioni: MINE controlla le tappe

All'inizio della lezione scrivi:

```
/mine lesson start conditions @a
```

| Tappa | Ogni volta che un programma parte, MINE… | Superata quando… |
|---|---|---|
| 1 · `wall` | costruisce un muro di mattoni sulla strada di Byte, da 2 a 6 blocchi più avanti | Byte lo scavalca e atterra subito dietro, di nuovo a terra, senza romperlo |
| 2 · `gold` | posa una fila di 8 blocchi davanti a Byte: pietra, con da 2 a 5 blocchi d'oro in posti a caso | tutto l'oro è stato scavato e tutta la pietra è ancora al suo posto |
| 3 · `tower` | — | davanti a Byte c'è una torre fatta con **tutti** i blocchi del suo slot evidenziato (almeno 3), e Byte è tornato a terra |

Quando qualcosa non va, MINE dice cos'è successo, per esempio *"questa volta il muro era a 4
blocchi"*, *"nella fila ci sono ancora 2 blocchi d'oro, e sono stati scavati 2 blocchi di
pietra"* o *"Byte aveva 7 blocchi, e la torre è alta 5"*. Se davanti a Byte non c'è
abbastanza spazio libero, MINE non costruisce niente e lo dice: `/mine byte recall` da un
punto libero.

Gli altri comandi sono gli stessi della lezione 1 (vedi la
[guida della lezione 1](../01-first-steps/README.it.md#missioni-mine-controlla-le-tappe)).

## Con i blocchi

Tutta la lezione funziona anche nell'[editor a blocchi](../../guide/blocks.it.md) (`/mine blocks`,
poi **Esegui ▶**). Per lei ci sono due blocchi **Byte**: *blocco [sotto] uguale a quello dello
slot?* (in Lua `byte.compareDown()`) e *blocchi nello slot* (`byte.getItemCount()`).

| Attività | Con i blocchi | File |
|---|---|---|
| 1 · Domande sì o no | `di'` con dentro `blocco davanti?`: dice `true` o `false` | |
| 2 · Oltre il muro | vedi sotto | [muro.json](solutions/it/blocks/muro.json) |
| 3 · La fila d'oro | vedi sotto | [oro.json](solutions/it/blocks/oro.json) |
| 4 · Una torre con tutti i blocchi | vedi sotto | [torre.json](solutions/it/blocks/torre.json) |

```
ripeti mentre non blocco davanti?       vai su
    vai avanti                          ripeti 8 volte
vai su                                      vai avanti
vai avanti                                  se blocco sotto uguale a quello dello slot?
vai avanti                                      scava sotto
vai giù                                 di' "Tutto l'oro è mio!"
di' "Muro superato!"

ripeti mentre blocchi nello slot > 0
    piazza un blocco davanti
    vai su
ripeti fino a che blocco sotto?
    vai giù
di' "Torre finita!"
```

La torre è scritta in modo diverso dal Lua: in Lua `byte.place()` mette un blocco e insieme
risponde se ci è riuscito, un trucco che i blocchi non hanno. Byte guarda invece quanti blocchi
restano nel suo slot. Una bella domanda: quale versione si legge meglio? Le soluzioni in inglese
sono in [`solutions/blocks`](solutions/blocks).

## Svolgimento

### Introduzione — 5 minuti, senza computer

Giocate al "robot nel corridoio": uno studente fa il robot, a occhi chiusi, e può fare un solo
passo alla volta. La classe gli dà **un solo** programma che deve funzionare da qualunque punto
di partenza: "**finché** non hai niente davanti, fai un passo". Si ferma al muro, ovunque sia.

Poi scrivi alla lavagna le due domande che un programma può fare:

- **se** qualcosa è vero, allora fai questo (una volta);
- **finché** qualcosa è vero, continua a fare questo (ancora e ancora).

### Attività 1 — Domande con risposta sì o no (5 minuti)

Nel prompt `lua`, gli studenti provano:

```lua
byte.detect()
```

Risponde `false`. Poi mettono un blocco davanti a Byte e riprovano: `true`. Byte ha dei
**sensori**: `byte.detect()` guarda davanti, `byte.detectDown()` guarda sotto.

Ricorda la lezione 1: anche `byte.forward()` e `byte.place()` rispondono `true` o `false`.
Ognuna di queste risposte si può usare in una condizione.

### Attività 2 — Oltre il muro (15 minuti)

**Compito:** Byte cammina fino al muro, ovunque sia, lo scavalca e atterra subito dietro.

Il muro c'è già quando la tappa inizia. Fai avviare due volte un programma che cammina e basta
(`for i = 1, 5 do byte.forward() end`), e fai notare che dopo un tentativo fallito MINE
costruisce il muro da un'altra parte.

```lua
while not byte.detect() do
  byte.forward()
end
byte.up()
byte.forward()
byte.forward()
byte.down()
```

Cose da spiegare:

- `while ... do ... end` ripete **finché** la condizione è vera, e la controlla **prima** di ogni
  giro: quando Byte tocca il muro, `byte.detect()` diventa `true`, quindi
  `not byte.detect()` diventa `false` e il ciclo finisce.
- `not` trasforma `true` in `false` e viceversa.
- La differenza con `for`: con `for` sai quante volte; con `while` no, e lasci decidere al mondo.

Soluzione: [`solutions/it/muro.lua`](solutions/it/muro.lua).

### Attività 3 — La fila d'oro (15 minuti)

**Compito:** MINE posa una fila di 8 blocchi: pietra, con un po' d'oro. Scava solo l'oro.

`byte.compareDown()` risponde `true` quando il blocco sotto Byte è uguale a quello nel suo
slot evidenziato: MINE ci ha messo **un blocco d'oro**, così Byte sa distinguere l'oro dalla
pietra. `byte.digDown()` scava il blocco sotto.

```lua
byte.up()
for i = 1, 8 do
  byte.forward()
  if byte.compareDown() then
    byte.digDown()
  end
end
```

Domande per la discussione:

- Cosa succede senza l'`if`? (Byte scava tutto, pietra compresa: MINE dice quanti blocchi di
  pietra sono stati scavati.)
- Qui usiamo insieme un `for` e un `if`: perché `for` e non `while`? (La fila è sempre lunga 8:
  sappiamo quante volte.)

Soluzione: [`solutions/it/oro.lua`](solutions/it/oro.lua).

### Attività 4 — Una torre con tutti i blocchi (10 minuti)

**Compito:** una torre con **tutti** i blocchi dello slot evidenziato — MINE ne ha dati a ogni
studente un numero diverso, da 3 a 12, e il programma deve funzionare con qualunque numero — poi
di nuovo a terra.

Nella lezione 1 avevamo notato che `byte.place()` risponde `false` quando non riesce a mettere
un blocco. Ora lo usiamo: quando i blocchi finiscono, `byte.place()` è `false` e il ciclo si
ferma. Allo stesso modo, `byte.down()` è `false` quando Byte tocca terra.

```lua
while byte.place() do
  byte.up()
end
while byte.down() do
end
```

Il secondo ciclo ha il corpo vuoto: tutto il lavoro lo fa la condizione! Fai avviare lo stesso
programma con un numero diverso di blocchi: funziona sempre.

Soluzione: [`solutions/it/torre.lua`](solutions/it/torre.lua).

### Sfida extra — Per chi finisce prima

- Modifica il programma del muro perché **dica** anche quanti passi ha fatto per arrivarci.
  (Serve una variabile per contare: è la lezione 5!)
- Nella fila d'oro, fai dire a Byte "oro!" o "pietra!" per ogni blocco su cui passa:
  `if ... then ... else ... end`.

### Conclusione — 5 minuti

- Che differenza c'è tra `if` e `while`?
- Quando usiamo `for`, e quando `while`?
- La domanda di riflessione che MINE mostra alla fine: *MINE ha cambiato la sfida ogni volta, ma
  i tuoi programmi hanno funzionato lo stesso. Come faceva Byte a sapere cosa fare?*

## Problemi frequenti

| Cosa succede | Perché | Cosa fare |
|---|---|---|
| MINE dice che servono blocchi liberi davanti a Byte | c'è qualcosa in mezzo: una costruzione precedente, un altro Byte, una collina | `/mine byte recall` da un punto libero |
| Davanti a Byte non c'è il muro | MINE lo costruisce solo nella tappa `wall`, e solo dove c'è spazio | controlla con `/mine lesson status` che lo studente sia a quella tappa; `/mine byte recall` da un punto libero, poi avvia il programma: il muro compare |
| Byte va contro il muro e si ferma, senza scavalcarlo | il programma non ha un `while`: `byte.forward()` contro il muro risponde solo `false` | `while not byte.detect() do ... end` |
| Byte scavalca troppo presto o troppo tardi | la scalata è dentro il ciclo, o la condizione del ciclo è sbagliata | la scalata va **dopo** l'`end` del `while` |
| `byte.digDown()` non fa niente | Byte non ha ancora il piccone: MINE glielo dà quando Byte aspetta un comando | apri il terminale di Byte, in modo che mostri il prompt `>` |
| Byte scava anche la pietra | manca l'`if`, o lo studente ha tolto l'oro dallo slot evidenziato | il blocco d'oro torna nello slot **evidenziato** |
| Byte non scava niente | lo slot evidenziato è vuoto o contiene altro | il blocco d'oro va nello slot evidenziato; `/mine lesson restart` ridà tutto |
| La torre è più bassa del numero di blocchi | alcuni blocchi sono in altri slot: `byte.place()` usa solo quello evidenziato | tutti i blocchi nello slot evidenziato |
| Il programma non finisce mai | un `while` la cui condizione non cambia mai, es. `while byte.detect() do end` davanti a un muro | tieni premuti Ctrl+T nel terminale per un secondo per fermarlo, poi correggi la condizione |

## Per l'insegnante: cosa osservare

- **Avviano il programma più di una volta** per controllare che funzioni con una sfida diversa?
- **Sanno dire a parole** cosa chiede la condizione ("finché non c'è niente davanti")?
- **Scelgono tra `for` e `while`** per un motivo?

## Riferimenti

- [Lezione 3 — Debugging](../03-debugging/README.it.md)
- [API Lua di MINE](../../guide/lua-api.it.md); in gioco, `help byte` nel terminale di Byte.
- Le soluzioni sono in [`solutions/it/`](solutions/it/) (in inglese in [`solutions/`](solutions/))
  e vengono provate in automatico a ogni modifica di MINE, contro sfide costruite a caso.
