[English](README.md) · **Italiano**

# Lezione 11 — Caccia al tesoro: le coordinate

| | |
|---|---|
| **Durata** | 1 ora (50–60 minuti) |
| **Età** | 10–14 anni |
| **Concetti** | coordinate (avanti, destra), variabili, `for` con una variabile, funzione con parametri |
| **Prerequisiti** | [lezione 2](../02-loops/README.it.md) (cicli); per la tappa 3, la [lezione 5](../05-functions/README.it.md) (funzioni) |

## Obiettivi

Alla fine della lezione lo studente sa:

1. dire dove si trova un punto con **due numeri**, contando da un punto di partenza;
2. tenere numeri in **variabili** e usarli per decidere quanti passi fare;
3. usare le variabili come **memoria**, per ritrovare la strada di casa;
4. scrivere una **funzione con parametri** che porta Byte in qualunque punto.

## Come funziona

Ogni volta che un programma parte, **MINE seppellisce un tesoro**, un blocco d'oro nel terreno,
in un posto nuovo vicino a Byte: da 1 a 6 blocchi **avanti** e da 0 a 4 **a destra**. Il programma
non lo vede: lo trova con i numeri di `mine.challenge()`.

```lua
local avanti, destra = mine.challenge()   -- per esempio 4 e 2
```

Con i blocchi, *il numero 1 della sfida* è quanto avanti, *il numero 2 della sfida* quanto a destra
(in Lua `mine.challenge(1)` e `mine.challenge(2)`). Quando il programma finisce, MINE rimette il
terreno com'era.

Se Byte sbaglia, MINE lo dice **in coordinate**: *"il tesoro era 4 avanti e 2 a destra; Byte si è
fermato 4 avanti e 0 a destra"*. È il momento migliore per parlarne alla lavagna.

## Preparazione (insegnante, prima della lezione)

- Il **laboratorio standard** (`/mine lab build <studenti>`, poi `/mine lab assign`), o un mondo
  superpiatto con **7 blocchi liberi davanti** e **5 a destra** di ogni Byte, su terreno pieno.
- Nient'altro: Byte ha già il piccone per scavare.
- Prova tu le tre tappe con [`solutions/it/`](solutions/it/).

## Missioni: MINE controlla le tappe

```
/mine lesson start treasure @a
```

| Tappa | `mine.challenge()` dà… | Superata quando… |
|---|---|---|
| 1 · `map` | avanti e destra del tesoro | Byte è proprio sopra il tesoro |
| 2 · `dig` | come sopra | il tesoro è scavato (`byte.digDown()`) e Byte è tornato dove è partito |
| 3 · `three` | 6 numeri: avanti e destra di tre tesori | tutti e tre scavati, e Byte a casa |

Dopo ogni tappa riuscita, `/mine byte recall` dall'inizio della corsia riporta Byte al suo posto.

## Con i blocchi

| Tappa | File |
|---|---|
| 1 · La mappa | [mappa.json](solutions/it/blocks/mappa.json) |
| 2 · Scava e torna | [scava.json](solutions/it/blocks/scava.json) |
| 3 · Tre tesori | [tre.json](solutions/it/blocks/tre.json) |

Servono *il numero … della sfida* (Byte), *imposta … a* e la variabile (Variabili), *ripeti* con
dentro la variabile al posto del numero; per la tappa 3 una funzione con due ingressi (Funzioni,
l'ingranaggio per aggiungerli).

## Svolgimento

### Introduzione — 10 minuti, senza computer

Battaglia navale alla lavagna, o in cortile: "il tesoro è **4 passi avanti e 2 a destra**". Uno
studente fa Byte e cammina. Poi: *e per tornare indietro?* (2 passi indietro, girarsi, 4 passi
indietro: gli stessi numeri, al contrario).

### Attività 1 — La mappa (15 minuti)

```lua
local avanti, destra = mine.challenge()
mine.say("Il tesoro è " .. avanti .. " avanti e " .. destra .. " a destra")
for i = 1, avanti do
  byte.forward()
end
byte.turnRight()
for i = 1, destra do
  byte.forward()
end
```

Cose da spiegare:

- una **variabile** è una scatola con un nome: `avanti` contiene un numero, diverso ogni volta;
- `for i = 1, avanti do` ripete tante volte quanto vale `avanti`;
- e se `destra` è 0? Il ciclo non gira neanche una volta: va bene così.

Soluzione: [`solutions/it/mappa.lua`](solutions/it/mappa.lua).

### Attività 2 — Scava e torna a casa (10 minuti)

Le variabili **ricordano**: dopo aver scavato con `byte.digDown()`, la strada di casa è fatta
degli stessi numeri, all'indietro (`byte.back()`), nell'ordine opposto.

Soluzione: [`solutions/it/scava.lua`](solutions/it/scava.lua).

### Attività 3 — Tre tesori (15 minuti)

Ora i tesori sono tre, e tra un tesoro e l'altro non si può ripartire da casa: Byte deve sapere
**dov'è**. Due variabili, `a` e `d`, tengono la sua posizione; una funzione lo porta ovunque:

```lua
local a, d = 0, 0

local function vaiA(versoA, versoD)
  while a < versoA do byte.forward() a = a + 1 end
  while a > versoA do byte.back() a = a - 1 end
  byte.turnRight()
  while d < versoD do byte.forward() d = d + 1 end
  while d > versoD do byte.back() d = d - 1 end
  byte.turnLeft()
end

for i = 1, 3 do
  vaiA(mine.challenge(2 * i - 1), mine.challenge(2 * i))
  byte.digDown()
end
vaiA(0, 0)
```

Domande: perché `vaiA(0, 0)` riporta Byte a casa? Cosa succede se il secondo tesoro è più
indietro del primo? (`a > versoA`: Byte va indietro.)

Soluzione: [`solutions/it/tre.lua`](solutions/it/tre.lua).

### Conclusione — 5 minuti

- Dove si usano coordinate così? (Mappe, GPS, battaglia navale, i fogli di calcolo: B3!)
- La domanda di riflessione di MINE: *Byte ha trovato tesori che non vedeva, solo con dei numeri.
  Dove si usano coordinate così nella vita vera? Perché a Byte servivano le variabili per tornare a
  casa?*

## Problemi frequenti

| Cosa succede | Perché | Cosa fare |
|---|---|---|
| `mine.challenge()` dà `nil` | il programma è partito fuori dalla lezione, o lo studente non è a una tappa della caccia | `/mine lesson status`; avvia il programma dal terminale di Byte o con `/mine byte run` |
| Byte si ferma di lato al tesoro | ha girato prima di andare avanti, o a sinistra invece che a destra | prima avanti, poi `byte.turnRight()` |
| Byte non torna a casa | i passi all'indietro sono nell'ordine sbagliato | prima i passi a destra, poi girarsi, poi quelli in avanti |
| MINE dice che serve spazio libero | Byte è ancora dove è finito il programma di prima | `/mine byte recall` dall'inizio della corsia |

## Riferimenti

- [Lezione 5 — Variabili e funzioni](../05-functions/README.it.md); [API Lua](../../guide/lua-api.it.md) (`mine.challenge`).
- Soluzioni in [`solutions/it/`](solutions/it/) (in inglese in [`solutions/`](solutions/)), provate
  in automatico su tesori sepolti a caso.
