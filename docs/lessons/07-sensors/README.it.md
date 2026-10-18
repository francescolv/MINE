[English](README.md) · **Italiano**

# Lezione 7 — Sensori: Byte aspetta e ricorda

| | |
|---|---|
| **Durata** | 1 ora (50–60 minuti) |
| **Età** | 10–14 anni |
| **Concetti** | sensori, timer (aspettare), il contrario di un segnale (`not`), memoria (una variabile che cambia) |
| **Prerequisiti** | [lezione 6](../06-redstone/README.it.md) (ingresso, uscita, `os.pullEvent("redstone")`) e [lezione 5](../05-functions/README.it.md) (variabili) |

## Obiettivi

Alla fine della lezione gli studenti sanno:

1. reagire a un **sensore**: una piastra a pressione su cui sale qualcuno, un sensore di luce;
2. usare un **timer**: fare qualcosa, **aspettare**, poi annullarla (`os.sleep`);
3. usare il **contrario** di un segnale (`not`): una luce accesa quando è buio;
4. **ricordare** qualcosa tra due eventi con una variabile: un pulsante che accende e spegne.

Sono le idee dietro le porte automatiche, i lampioni e le luci delle scale.

## La postazione di MINE

A ogni tappa MINE costruisce la sua parte di una piccola postazione intorno a Byte, e toglie
quella della tappa prima:

| Tappa | Intorno a Byte |
|---|---|
| 1 | una **piastra a pressione** di legno a terra, a **sinistra** (`"left"`), una **porta di ferro davanti** (`"front"`) |
| 2 | un **sensore di luce sopra** Byte (`"top"`), una **lampada di redstone dietro** (`"back"`) |
| 3 | un **pulsante** di legno a terra, a **destra** (`"right"`), davanti un **pistone appiccicoso** con una **passerella gialla** (`"front"`) |

Piastra e pulsante sono a terra accanto a Byte: gli studenti camminano sulla piastra e premono il
pulsante da soli, per provare.

## Preparazione (insegnante, prima della lezione)

- Come nella lezione 6: un mondo piatto, spazio intorno a ogni Byte (un blocco libero per lato,
  tre davanti, uno dietro e uno sopra) e terreno solido intorno. Il **laboratorio standard** va
  bene (`/mine lab build <studenti>`, `/mine lab assign`).
- Il sensore di luce deve vedere il cielo: non fare la lezione sotto un tetto.
- Nient'altro: MINE crea i Byte che mancano e costruisce la postazione.

## Missioni: MINE controlla le tappe

```
/mine lesson start sensors @a
```

| Tappa | Superata quando… |
|---|---|
| 1 · `plate_door` | **MINE sale sulla piastra** per un secondo, due volte: ogni volta la porta si apre, **resta aperta** dopo che la piastra è libera, e **si richiude da sola** circa 3 secondi dopo |
| 2 · `night_lamp` | **MINE fa scendere la notte sul sensore** (lo inverte) e poi di nuovo il giorno: la lampada deve essere accesa proprio quando il sensore dice che è buio |
| 3 · `button_bridge` | **MINE preme il pulsante** tre volte: ogni pressione fa uscire la passerella se è dentro, la fa rientrare se è fuori, e lei resta lì quando il pulsante si rialza |

Tutte e tre sono **verifiche dal vivo**, come nella lezione 6: il programma ascolta per sempre,
MINE lo prova, lo ferma e dice com'è andata. Una porta che segue la piastra, una lampada accesa di
giorno o una passerella che segue il pulsante non passano, e MINE dice perché.

## Con i blocchi

Tutta la lezione funziona anche nell'[editor a blocchi](../../guide/blocks.it.md), con i blocchi
**Redstone**, **aspetta N secondi** (Cicli), **non** (Logica) e una variabile.

| Attività | File |
|---|---|
| 2 · La porta con il timer | [porta.json](solutions/it/blocks/porta.json) |
| 3 · Il lampione | [lampione.json](solutions/it/blocks/lampione.json) |
| 4 · La passerella | [ponte.json](solutions/it/blocks/ponte.json) |

```
segnale redstone davanti a falso            imposta fuori a falso
ripeti mentre vero                          segnale redstone davanti a fuori
    aspetta un cambio di redstone           ripeti mentre vero
    se segnale da sinistra?                     aspetta un cambio di redstone
        segnale redstone davanti a vero         se segnale da destra?
        aspetta 3 secondi                           imposta fuori a non fuori
        segnale redstone davanti a falso            segnale redstone davanti a fuori

ripeti mentre vero
    segnale redstone dietro a non segnale da sopra?
    aspetta un cambio di redstone
```

Le soluzioni in inglese sono in [`solutions/blocks`](solutions/blocks).

## Svolgimento

### Introduzione — 5 minuti, senza computer

Chiedi dove le macchine **sentono** qualcosa e reagiscono: la porta automatica del supermercato, il
lampione che si accende al tramonto, la luce delle scale che si spegne da sola, il pulsante
dell'ascensore. Cosa sentono (una persona, il buio, una pressione)? Cosa fanno? E come sanno quando
fermarsi?

### Attività 1 — La piastra (5 minuti)

`/mine lesson start sensors @a`. Gli studenti salgono sulla piastra e vedono il suo segnale arrivare
a Byte: nel terminale di Byte, `lua` e poi `redstone.getInput("left")` dice
`true` finché qualcuno ci sta sopra.

### Attività 2 — La porta con il timer (15 minuti)

**Compito:** quando qualcuno sale sulla piastra la porta si apre, e si richiude da sola 3 secondi
dopo.

```lua
redstone.setOutput("front", false)
while true do
  os.pullEvent("redstone")
  if redstone.getInput("left") then
    redstone.setOutput("front", true)
    os.sleep(3)
    redstone.setOutput("front", false)
  end
end
```

Cose da spiegare:

- `os.sleep(3)` fa **aspettare** Byte: per 3 secondi non fa nient'altro.
- Perché non `redstone.setOutput("front", redstone.getInput("left"))` come nella lezione 6?
  Provatelo: la porta si chiude appena si scende. Lo dice anche MINE.
- L'`if`: la piastra manda un segnale quando ci sali **e** quando scendi. Solo il primo apre la porta.

Soluzione: [`solutions/it/porta.lua`](solutions/it/porta.lua).

### Attività 3 — Il lampione (15 minuti)

**Compito:** la lampada dietro Byte è accesa solo quando è buio.

Il sensore di luce sopra manda un segnale **quando c'è luce**. La lampada deve fare il contrario:
`not`.

```lua
while true do
  redstone.setOutput("back", not redstone.getInput("top"))
  os.pullEvent("redstone")
end
```

Per vederlo funzionare senza aspettare la notte, **clic destro sul sensore**: diventa blu e
invertito, come se fosse notte. È anche così che lo prova MINE.

Soluzione: [`solutions/it/lampione.lua`](solutions/it/lampione.lua).

### Attività 4 — La passerella (15 minuti)

**Compito:** ogni pressione del pulsante fa uscire la passerella, o la fa rientrare.

Un pulsante resta acceso **solo un momento**: Byte non può guardarlo per sapere dov'è la
passerella. Deve **ricordarselo**, in una variabile:

```lua
local fuori = false
redstone.setOutput("front", fuori)
while true do
  os.pullEvent("redstone")
  if redstone.getInput("right") then
    fuori = not fuori
    redstone.setOutput("front", fuori)
  end
end
```

`fuori = not fuori` è il cuore di tutto: vero diventa falso, falso diventa vero. Chiedi cosa
succede senza la variabile (la passerella esce mentre il pulsante è giù, e rientra con lui).

Soluzione: [`solutions/it/ponte.lua`](solutions/it/ponte.lua).

### Sfida extra — Per chi finisce prima

- Una porta che resta aperta **finché** qualcuno è sulla piastra, e poi altri 3 secondi.
- Una passerella che esce con il pulsante e rientra da sola dopo 5 secondi.
- Un contatore: Byte dice quante volte è stato premuto il pulsante (`mine.say`).

### Conclusione — 5 minuti

- Cosa fa un sensore? Cosa fa un timer? Cosa ricorda una variabile?
- La domanda di riflessione che MINE mostra alla fine: *Il lampione funziona da solo, tutta la
  notte. Quali macchine a casa o a scuola reagiscono a un sensore, aspettano un po', o si ricordano
  cos'è successo?*

## Problemi frequenti

| Cosa succede | Perché | Cosa fare |
|---|---|---|
| MINE: "la porta si è chiusa appena la piastra è rimasta libera" | la porta segue la piastra | apri, `os.sleep(3)`, chiudi |
| MINE: "la porta è rimasta aperta" | il programma non la chiude, o aspetta troppo | `redstone.setOutput("front", false)` dopo l'attesa |
| La porta si apre due volte | manca l'`if`: anche scendere è un cambio | `if redstone.getInput("left") then` |
| MINE: "il sensore diceva luce, e la lampada era accesa" | la lampada segue il sensore invece di fare il contrario | `not redstone.getInput("top")` |
| Il sensore non cambia mai | non vede il cielo (un tetto, un blocco sopra) | uno spazio aperto; clic destro per invertirlo |
| MINE: "la passerella è tornata indietro da sola" | il pistone segue il pulsante | una variabile, cambiata solo quando il pulsante viene premuto |
| MINE: "MINE ha bisogno di spazio intorno a Byte" | blocchi o la scena della lezione 6 intorno a Byte | `/mine byte recall` da uno spazio libero |
| Il programma è finito prima della prova | manca `while true do ... end` | tienilo in ascolto |

## Per l'insegnante: cosa osservare

- **Prevedono** cosa farà un programma prima che MINE lo provi (la porta che segue la piastra, la
  passerella che segue il pulsante)?
- **Leggono il messaggio di MINE** e cambiano l'unica cosa che indica?
- Sanno dire con parole loro perché il pulsante ha bisogno di una variabile e la leva della lezione
  6 no?

## Riferimenti

- [Lezione 6 — Redstone](../06-redstone/README.it.md)
- [API Lua di MINE](../../guide/lua-api.it.md); in gioco, `help redstone` nel terminale di Byte.
- Le soluzioni sono in [`solutions/it/`](solutions/it/) (in inglese in [`solutions/`](solutions/)),
  con e senza blocchi, e vengono provate in automatico a ogni modifica di MINE.
