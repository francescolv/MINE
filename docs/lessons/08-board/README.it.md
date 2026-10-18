[English](README.md) · **Italiano**

# Lezione 8 — La scheda: dalla scrivania al mondo

| | |
|---|---|
| **Durata** | 1 ora (50–60 minuti) |
| **Età** | 10–14 anni |
| **Concetti** | ingresso e uscita tra due computer, messaggi `chiave:valore`, il numero di un sensore e una soglia (`< 50`), `if … else` |
| **Prerequisiti** | [lezione 7](../07-sensors/README.it.md) (attese, timer, `not`); un **micro:bit** (o un Arduino) per studente o per coppia, con il suo cavo USB |

## Obiettivi

Alla fine della lezione gli studenti sanno:

1. far cambiare il mondo del gioco a un **pulsante vero**: il pulsante A della scheda apre una porta;
2. usare il **numero di un sensore** con una **soglia**: la luce della scheda sotto 50 vuol dire «buio»;
3. mandare un messaggio **nell'altro verso**: una leva nel gioco accende un cuore sulla scheda;
4. spiegare che la scheda e Byte sono **due computer con due programmi**, che si parlano con
   messaggi brevi (`pulsante_a:1`, `luce:120`, `cuore:1`).

Sono le idee dietro i telecomandi, la casa domotica e i sensori di una stazione meteo.

## La stazione di MINE

A ogni passo MINE costruisce la sua parte intorno a Byte, e toglie quella del passo prima:

| Passo | Intorno a Byte |
|---|---|
| 1 | una **porta di ferro davanti** (`"front"`) |
| 2 | una **lampada di redstone dietro** (`"back"`) |
| 3 | una **leva** a terra a **sinistra** (`"left"`) |

## Preparazione (insegnante, prima della lezione)

- Come nelle lezioni 6 e 7: un mondo piatto, spazio intorno a ogni Byte e terreno solido intorno.
  Va bene il **laboratorio standard** (`/mine lab build <studenti>`, `/mine lab assign`).
- **Le schede.** Su ogni micro:bit, il programma qui sotto (MakeCode, *Scarica*, poi copia il
  `.hex` nell'unità MICROBIT). Un solo programma per tutta la lezione: scrive il pulsante e la luce,
  e mostra il cuore quando Byte lo chiede.
- **I PC del laboratorio.** Su Linux l'utente dello studente deve essere nel gruppo `dialout` (vedi
  [Le schede](../../guide/devices.it.md#se-non-funziona)). Prova un PC prima della lezione:
  `/mine device`, scegli la scheda, premi A: nella finestra compare `pulsante_a:1`.
- Niente schede? La lezione funziona lo stesso: quando verifica i programmi, MINE fa la parte della
  scheda. Gli studenti perdono solo il piacere di vederlo con le proprie mani.

### Il programma della scheda (MakeCode, micro:bit)

```
all'avvio
  scrivi riga seriale "programma:lezione8"
quando si preme il pulsante A
  scrivi riga seriale "pulsante_a:1"
ogni 500 ms
  scrivi riga seriale unisci "luce:" (livello di luce)
quando si ricevono dati seriali (nuova riga)
  imposta riga a leggi riga seriale
  se riga comincia con "cuore:"
    se (il numero dopo "cuore:") > 0 allora mostra icona cuore
    altrimenti cancella lo schermo
```

Nel JavaScript di MakeCode:

```javascript
serial.writeLine("programma:lezione8")
input.onButtonPressed(Button.A, function () {
    serial.writeLine("pulsante_a:1")
})
loops.everyInterval(500, function () {
    serial.writeLine("luce:" + input.lightLevel())
})
serial.onDataReceived(serial.delimiters(Delimiters.NewLine), function () {
    let line = serial.readLine()
    if (line.indexOf("cuore:") == 0) {
        // cuore:1 da Byte, cuore:15 da un Trasmettitore: acceso; cuore:0: spento
        if (parseInt(line.substr(6)) > 0) {
            basic.showIcon(IconNames.Heart)
        } else {
            basic.clearScreen()
        }
    }
})
```

Con un **Arduino**: un pulsante sul pin 2 scrive `pulsante_a:1`, un sensore di luce (fotoresistenza)
su A0 scrive `luce:` con `analogRead(A0) / 4`, un LED sul pin 13 segue `cuore` (vedi
[Le schede → Arduino](../../guide/devices.it.md#arduino)).

## Missioni: MINE verifica i passi

```
/mine lesson start board @a
```

| Passo | Fatto quando… |
|---|---|
| 1 · `board_door` | **MINE preme A sulla scheda dello studente** (fa la parte di `pulsante_a:1`), due volte: ogni volta la porta si apre, è ancora aperta 2 secondi dopo, e **si richiude da sola** circa 3 secondi dopo |
| 2 · `board_lamp` | **MINE fa la parte della luce della scheda**: 200, 10, 180, 20. La lampada deve essere accesa esattamente quando la luce è sotto 50 |
| 3 · `board_heart` | **MINE tira la leva** su e giù due volte: ogni volta Byte deve dire alla scheda `cuore:1` (accesa) o `cuore:0` (spenta) |

Tutte e tre sono **verifiche dal vivo**: il programma ascolta per sempre, MINE lo prova, lo ferma e
dice com'è andata. **A MINE non serve la scheda** per la sua prova: fa la parte della scheda. La
scheda vera degli studenti funziona con lo stesso programma, nello stesso momento.

## Con i blocchi

Tutta la lezione funziona anche nell'[editor a blocchi](../../guide/blocks.it.md), con la categoria
**Scheda**, un confronto di **Logica** e **se … altrimenti**.

| Attività | File |
|---|---|
| 2 · La porta con il pulsante A | [porta.json](solutions/it/blocks/porta.json) |
| 3 · Il lampione | [lampione.json](solutions/it/blocks/lampione.json) |
| 4 · Il cuore | [cuore.json](solutions/it/blocks/cuore.json) |

```
segnale redstone davanti a falso         ripeti mentre vero
ripeti mentre vero                           aspetta un cambio di redstone
    aspetta "pulsante_a" dalla scheda        se segnale da sinistra?
    segnale redstone davanti a vero              manda alla scheda "cuore" 1
    aspetta 3 secondi                        altrimenti
    segnale redstone davanti a falso             manda alla scheda "cuore" 0

ripeti mentre vero
    segnale redstone dietro a  valore della scheda "luce" < 50
    aspetta 0.5 secondi
```

## Piano della lezione

### Introduzione — 5 minuti, senza computer

Mostra un micro:bit. Anche lui è un piccolo **computer**: ha dei pulsanti, un sensore di luce, 25 LED
e un programma dentro. Oggi parla con Byte attraverso il cavo USB, con messaggi brevi: una
parola, i due punti, un numero. `pulsante_a:1` vuol dire «è stato premuto il pulsante A». Chi lo
scrive? Chi lo legge?

### Attività 1 — Collegare la scheda (5 minuti)

`/mine lesson start board @a`, poi `/mine device` e la porta della scheda. Premi A: la finestra mostra
`pulsante_a:1`. Copri la scheda con una mano: `luce` scende. La finestra dice anche che programma c'è
sulla scheda (`lezione8`).

### Attività 2 — La porta con il pulsante A (15 minuti)

**Compito:** premendo A sulla scheda la porta si apre, e si richiude da sola 3 secondi dopo.

```lua
redstone.setOutput("front", false)
while true do
  mine.device.wait("pulsante_a")
  redstone.setOutput("front", true)
  os.sleep(3)
  redstone.setOutput("front", false)
end
```

Cose da spiegare:

- `mine.device.wait("pulsante_a")` fa **aspettare un messaggio** a Byte, come `os.pullEvent`
  aspetta la redstone nella lezione 7.
- La chiave deve essere **la stessa parola** nei due programmi: `pulsante_a` e `pulsanteA` sono diverse.

Soluzione: [`solutions/it/porta.lua`](solutions/it/porta.lua).

### Attività 3 — Il lampione (15 minuti)

**Compito:** la lampada dietro Byte è accesa solo quando la scheda è al buio: luce sotto 50.

```lua
while true do
  redstone.setOutput("back", mine.device.read("luce") < 50)
  os.sleep(0.5)
end
```

- `mine.device.read("luce")` è l'**ultimo** numero scritto dalla scheda (ne scrive uno ogni mezzo secondo).
- `< 50` trasforma un numero in **vero o falso**: è una **soglia**. Chiedi agli studenti di trovarne
  una buona per la loro classe: coprite la scheda, guardate i numeri in `/mine device`.
- Senza `os.sleep(0.5)` Byte controlla senza fermarsi mai, e CC: Tweaked lo blocca («Too long
  without yielding»).

Soluzione: [`solutions/it/lampione.lua`](solutions/it/lampione.lua).

### Attività 4 — Il cuore (15 minuti)

**Compito:** la leva alla sinistra di Byte accende il cuore sulla scheda; giù, il cuore si spegne.

```lua
while true do
  os.pullEvent("redstone")
  if redstone.getInput("left") then
    mine.device.send("cuore", 1)
  else
    mine.device.send("cuore", 0)
  end
end
```

Adesso il messaggio va **nell'altro verso**: Byte scrive, la scheda legge. L'`else` serve: senza,
il cuore non si spegne mai (e MINE lo dice).

Soluzione: [`solutions/it/cuore.lua`](solutions/it/cuore.lua).

### Senza codice: i blocchi della scheda

Per finire, o per chi finisce prima: il **Ricevitore della scheda** e il **Trasmettitore per la
scheda** nell'inventario in Creativa (Pietrarossa) fanno lo stesso senza Byte
([guida](../../guide/devices.it.md#senza-codice-i-blocchi-della-scheda)). Un Ricevitore *come un
pulsante* con la chiave `pulsante_a`, sotto una porta; una leva accanto a un Trasmettitore con la
chiave `cuore`. Che cosa sa fare Byte che i blocchi non sanno fare? (Aspettare 3 secondi,
confrontare la luce con 50, ricordare.)

### Sfida in più — Per chi finisce prima

- Il pulsante B chiude subito la porta (`pulsante_b:1` sulla scheda, un secondo `if` in Byte).
- Il lampione con tre livelli: Byte scrive `luce` in chat con `mine.say` quando cambia molto.
- Il cuore lampeggia finché la leva è accesa.

### Conclusione — 5 minuti

- Quali messaggi sono andati dalla scheda a Byte? Quali nell'altro verso?
- La domanda di riflessione che MINE mostra alla fine: *Il tuo pulsante ha aperto una porta nel
  gioco, e una leva nel gioco ha acceso la tua scheda. Dove, nella vita vera, un pulsante, un
  sensore o una luce parlano con un computer lontano?*

## Problemi frequenti

| Cosa succede | Perché | Cosa fare |
|---|---|---|
| La porta non si apre con A, ma la prova di MINE passa | la scheda non è collegata, o il suo programma non scrive `pulsante_a` | `/mine device`: premi A e cerca `pulsante_a:1` |
| MINE: «la porta è rimasta aperta» | il programma non la chiude mai | `os.sleep(3)` poi `redstone.setOutput("front", false)` |
| MINE: «la scheda ha detto luce:200, e la lampada era accesa» | il confronto è al contrario (`> 50`) | `< 50`: accesa quando è buio |
| «Too long without yielding» | un ciclo che non aspetta mai | `os.sleep(0.5)` nel ciclo |
| MINE: «Byte non ha scritto cuore:0 alla scheda» | manca l'`else` | `else mine.device.send("cuore", 0)` |
| Il cuore non si accende sulla scheda | il programma della scheda non legge `cuore`, o legge un'altra parola | la parte *quando si ricevono dati seriali* del programma della scheda |
| MINE: «a MINE serve spazio intorno a Byte» | blocchi intorno a Byte | `/mine byte recall` da un posto libero |

## Per l'insegnante: cosa osservare

- Sanno dire **quale programma** gira sulla scheda e quale su Byte?
- Scelgono una **soglia** guardando i numeri veri, non a caso?
- Tengono **la stessa chiave** nei due programmi, e se ne accorgono quando è diversa?

## Riferimenti

- [Le schede: micro:bit, Arduino e Byte](../../guide/devices.it.md)
- [Lezione 7 — Sensori](../07-sensors/README.it.md)
- [API Lua di MINE](../../guide/lua-api.it.md); in gioco, `help mine` nel terminale di Byte.
- Le soluzioni sono in [`solutions/`](solutions/) (in italiano in [`solutions/it/`](solutions/it/)),
  con e senza blocchi, e vengono provate automaticamente ogni volta che MINE cambia.
