[English](README.md) · **Italiano**

# Lezione 5 — Variabili e funzioni

| | |
|---|---|
| **Durata** | 1 ora (50–60 minuti) |
| **Età** | 10–14 anni (anche prima superiore) |
| **Concetti** | variabile, contatore, valore che arriva da fuori (`mine.challenge()`), funzione, parametro |
| **Prerequisiti** | [lezione 2](../02-loops/README.it.md) (cicli) e [lezione 4](../04-conditions/README.it.md) (`while`) |

## Obiettivi

Alla fine della lezione lo studente sa:

1. usare una **variabile** per ricordare un numero e cambiarlo (`passi = passi + 1`);
2. scrivere un programma che funziona con un numero che scopre solo **mentre gira**;
3. dare un nome a un pezzo di programma con `function`, e riusarlo con **parametri** diversi;
4. spiegare perché una funzione fa risparmiare righe ed errori.

## Come nella lezione 4, MINE cambia la sfida

Il muro è ogni volta a una distanza diversa, e il quadrato che MINE chiede cambia misura ogni
volta: un numero scritto a mano nel programma non funziona due volte. Il programma deve
**contare**, o **leggere** il numero: a questo servono le variabili.

La misura del quadrato arriva da un'istruzione nuova: `mine.challenge()` dà al programma quello
che la tappa chiede questa volta (qui, il lato del quadrato). Viene scelta quando il programma
parte.

## Preparazione (insegnante, prima della lezione)

- Un mondo **superpiatto in modalità creativa**, studenti uno accanto all'altro, tutti rivolti
  dalla stessa parte, a circa **8 blocchi di distanza**, con almeno **10 blocchi liberi davanti**
  a ogni Byte.
- Il modo più rapido: il **laboratorio standard** (`/mine lab build <studenti>`, poi
  `/mine lab assign`) ha proprio queste corsie ([guida](../../guide/server.it.md#3-il-laboratorio-standard));
  `/mine lab reset` lo libera dopo la lezione.
- Nient'altro: quando la lezione parte MINE crea Byte a chi non ce l'ha, costruisce il primo
  muro, e dà **64 assi** alle tappe 2 e 3.
- Prova tu stesso le tre tappe, con i programmi in [`solutions/it/`](solutions/it/): 10 minuti.

## Missioni: MINE controlla le tappe

All'inizio della lezione scrivi:

```
/mine lesson start functions @a
```

| Tappa | MINE… | Superata quando… |
|---|---|---|
| 1 · `count` | costruisce un muro da 3 a 8 blocchi più avanti (uno nuovo dopo ogni tentativo fallito) | Byte si è fermato proprio davanti al muro e ha **detto** quanti passi ha fatto — quel numero e nessun altro |
| 2 · `square` | sceglie il lato di un quadrato, da 2 a 5, ogni volta che parte un programma: `mine.challenge()` | il programma ha costruito un pavimento quadrato pieno di quella misura |
| 3 · `pyramid` | — | c'è una piramide a gradoni: un quadrato 5 × 5, un quadrato 3 × 3 centrato sopra, un blocco in cima (35 blocchi) |

Dire tutti i numeri da 1 a 9 "per sicurezza" non fa superare la tappa 1: MINE vuole solo quello
giusto. Quando qualcosa non va, MINE lo dice, per esempio *"questa volta Byte ha fatto 5
passi per arrivare al muro"* o *"questa volta mine.challenge() chiedeva un quadrato 4 × 4, e il
tuo Byte ha costruito 12 blocchi, lunghi 4 e larghi 3"*.

Gli altri comandi sono gli stessi della lezione 1 (vedi la
[guida della lezione 1](../01-first-steps/README.it.md#missioni-mine-controlla-le-tappe)).

## Con i blocchi

Tutta la lezione funziona anche nell'[editor a blocchi](../../guide/blocks.it.md) (`/mine blocks`,
poi **Esegui ▶**). Variabili e funzioni sono quelle di Blockly: **Variabili** → *Crea variabile...*,
e **Funzioni** → *per fai qualcosa*, il cui ⚙ aggiunge i parametri.

| Attività | Con i blocchi | File |
|---|---|---|
| 1 · Contare i passi | vedi sotto | [passi.json](solutions/it/blocks/passi.json) |
| 2 · Il quadrato che chiede MINE | `imposta lato a il numero della sfida`, poi il pavimento della lezione 2 con `lato` in ogni `ripeti` | [quadrato.json](solutions/it/blocks/quadrato.json) |
| 3 · La piramide | vedi sotto | [piramide.json](solutions/it/blocks/piramide.json) |

```
imposta passi a 0                           per quadrato con: lato
ripeti mentre non blocco davanti?               ripeti lato volte
    vai avanti                                      ripeti lato volte
    cambia passi di 1                                   piazza un blocco sotto
di' crea testo con "Ho fatto "                          vai avanti
                   passi                            ripeti lato volte
                   " passi"                             vai indietro
                                                    gira a destra
vai su                                              vai avanti
quadrato con lato 5                                 gira a sinistra
stratoSuccessivo                                gira a sinistra
quadrato con lato 3                             ripeti lato volte
stratoSuccessivo                                    vai avanti
quadrato con lato 1                             gira a destra
di' "Piramide finita: 25 + 9 + 1 = 35 blocchi!"
                                            per stratoSuccessivo
                                                vai su, vai avanti, gira a destra,
                                                vai avanti, gira a sinistra
```

Con i blocchi una funzione è un blocco nuovo, che compare nel gruppo **Funzioni** appena creata:
gli studenti vedono davvero di aver aggiunto un'istruzione a Byte. Le soluzioni in inglese sono
in [`solutions/blocks`](solutions/blocks).

## Svolgimento

### Introduzione — 5 minuti, senza computer

Chiedi a uno studente di camminare fino al muro dell'aula mentre la classe conta i passi ad alta
voce. Per contare, ognuno tiene **un numero in testa** e aggiunge 1 a ogni passo. Quel numero è
una **variabile**: una scatola con un nome, che contiene un valore che può cambiare.

Poi: "sapete fare un panino. Se dico *fate tre panini*, non spiego la ricetta tre volte". Una
ricetta con un nome è una **funzione**; "col prosciutto" o "col formaggio" è un **parametro**.

### Attività 1 — Contare i passi (15 minuti)

**Compito:** Byte cammina fino al muro (MINE l'ha già costruito) e dice quanti passi ha fatto.

```lua
local passi = 0
while not byte.detect() do
  byte.forward()
  passi = passi + 1
end
mine.say("Ho fatto " .. passi .. " passi")
```

Cose da spiegare:

- `local passi = 0` crea la variabile e ci mette 0. `local` vuol dire "appartiene a questo
  programma".
- `passi = passi + 1` si legge come un ordine, non come matematica: "prendi il valore di passi,
  aggiungi 1, rimetti il risultato in passi".
- `..` unisce testo e numeri, come nella lezione 1.

Chiedi di **prevedere** il numero prima di avviare il programma, guardando il muro. Poi avvialo
due volte: il muro si sposta, il programma non cambia, e il numero è sempre giusto.

Soluzione: [`solutions/it/passi.lua`](solutions/it/passi.lua).

### Attività 2 — Il quadrato che chiede MINE (15 minuti)

**Compito:** MINE sceglie ogni volta il lato di un quadrato; costruiscilo.

```lua
local lato = mine.challenge()
mine.say("Lato: " .. lato)
```

Il resto è il pavimento della lezione 2, con `lato` al posto dei numeri:
`for fila = 1, lato do ... end` e `for i = 1, lato do ... end`.

Domande per la discussione:

- Quanti blocchi servono per un quadrato di lato 3? E di lato 5? (L'area: lato × lato.)
- Perché non possiamo scrivere `4` nel programma? (Perché la prossima volta MINE potrebbe chiedere 2.)

Soluzione: [`solutions/it/quadrato.lua`](solutions/it/quadrato.lua).

### Attività 3 — La piramide, con una funzione (15 minuti)

**Compito:** una piramide a gradoni: 5 × 5, poi sopra 3 × 3, poi un blocco.

Scrivere tre volte il programma del quadrato sarebbe lungo, e facile da sbagliare. Invece, gli
diamo un nome:

```lua
function quadrato(lato)
  -- il programma del quadrato, che usa lato
end
```

Ora `quadrato(5)`, `quadrato(3)` e `quadrato(1)` costruiscono i tre strati. Dentro la funzione,
`lato` vale 5 la prima volta, 3 la seconda, 1 la terza: è il **parametro**.

Due dettagli la fanno funzionare:

- la funzione deve riportare Byte **all'angolo da cui è partito**, così lo strato successivo
  sa dov'è (dopo le file: gira a sinistra, avanti `lato` volte, gira a destra);
- tra uno strato e l'altro Byte sale di un blocco e si sposta di un blocco avanti e uno a
  destra, così il quadrato più piccolo è centrato. Anche questa può essere una funzione:
  `stratoSuccessivo()`.

Soluzione: [`solutions/it/piramide.lua`](solutions/it/piramide.lua).

### Sfida extra — Per chi finisce prima

- Una piramide 7 × 7: quanti blocchi servono? (49 + 25 + 9 + 1 = 84: più di una pila!)
- Una funzione `torre(altezza)` e una fila di torri alte 1, 2, 3, 4, 5 blocchi: una scala.

### Conclusione — 5 minuti

- Cos'è una variabile? Fai un esempio di oggi.
- Da dove arrivava il valore di `lato` nell'attività 2? E nell'attività 3?
- La domanda di riflessione che MINE mostra alla fine: *Nella piramide, la funzione
  quadrato(lato) ha fatto tre lavori diversi. Quante righe ti ha fatto risparmiare? Cos'altro
  potresti costruirci?*

## Problemi frequenti

| Cosa succede | Perché | Cosa fare |
|---|---|---|
| `attempt to perform arithmetic on a nil value (global 'passi')` | la variabile non è mai stata creata, o il nome è scritto in modo diverso | `local passi = 0` prima del ciclo, e lo stesso nome dappertutto |
| Byte dice "0 passi" | `passi = passi + 1` è fuori dal ciclo | va dentro, dopo `byte.forward()` |
| MINE dice che Byte ha fatto 5 passi, ma lui ne ha detti 6 | il contatore parte da 1, o aumenta anche quando Byte non si muove | parti da 0 e conta solo i passi veri |
| `attempt to concatenate a nil value` con `mine.challenge()` | il programma è partito fuori dalla tappa 2, dove MINE non chiede niente | controlla con `/mine lesson status` che lo studente sia alla tappa 2 |
| Il quadrato ha la misura giusta solo a volte | c'è un numero scritto a mano invece di `lato` | ogni `4` diventa `lato` |
| `attempt to call a nil value (global 'quadrato')` | la funzione è usata prima di essere scritta | `function quadrato(lato) ... end` va in cima al programma |
| Gli strati della piramide sono spostati | la funzione non riporta Byte al suo angolo, o manca lo spostamento tra gli strati | vedi i due dettagli dell'attività 3 |
| MINE dice che servono blocchi liberi davanti a Byte | c'è qualcosa sulla strada del muro | `/mine byte recall` da un punto libero |

## Per l'insegnante: cosa osservare

- **Sanno leggere ad alta voce `passi = passi + 1`** e spiegare cosa succede?
- **Prevedono il numero** di passi o di blocchi prima di avviare il programma?
- **Provano la funzione** su un solo quadrato prima di costruire tutta la piramide?

## Riferimenti

- [Lezione 4 — Le condizioni](../04-conditions/README.it.md)
- [API Lua di MINE](../../guide/lua-api.it.md): `mine.challenge()`, `mine.say()`.
- Le soluzioni sono in [`solutions/it/`](solutions/it/) (in inglese in [`solutions/`](solutions/))
  e vengono provate in automatico a ogni modifica di MINE.
