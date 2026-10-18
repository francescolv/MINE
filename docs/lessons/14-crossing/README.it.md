[English](README.md) · **Italiano**

# Lezione 14 — Python: il passaggio a livello (Arduino)

| | |
|---|---|
| **Durata** | 1–2 ore |
| **Età** | 14–18 anni |
| **Concetti** | **Python**: **macchina a stati**, eventi da due sorgenti (`os.pullEvent`), funzioni, un contatore come stato |
| **Prerequisiti** | la [lezione 8](../08-board/README.it.md) (la scheda) e la [lezione 13](../13-control/README.it.md) (stato); un Arduino è consigliato, non necessario |

## Obiettivi

Alla fine della lezione lo studente sa:

1. descrivere un sistema come una **macchina a stati**: stati, eventi, transizioni;
2. aspettare **eventi da più sorgenti** (la scheda, la redstone del mondo) nello stesso ciclo;
3. raccogliere in una **funzione** tutto quello che cambia con lo stato;
4. usare un **contatore** come stato, quando "chiuso" dipende da quanti treni ci sono.

## Come funziona

Intorno a Byte MINE costruisce un passaggio a livello:

- la **porta di ferro davanti** a Byte è la **sbarra**: è aperta finché Byte le manda redstone
  (`redstone.setOutput("front", True)`);
- la **lampada dietro** è la **luce** lampeggiante (`redstone.setOutput("back", ...)`);
- la **leva alla sua sinistra** è il **sensore sul binario**: si accende quando il treno è passato
  (`redstone.getInput("left")`).

L'**Arduino** ha un pulsante: quando lo premi arriva `treno:1` (un treno in arrivo); e un LED, che
si accende con `led:1`. Per controllare le tappe **MINE fa l'Arduino** e muove la leva.

## Preparazione (insegnante, prima della lezione)

- L'editor Python (`/mine python`), vedi la [guida Python](../../guide/python.it.md).
- Spazio intorno a Byte, con terreno pieno davanti e a sinistra (il laboratorio standard va bene).
- Se avete gli Arduino: un pulsante tra il pin 2 e GND, il LED sulla scheda (pin 13), collegati
  con `/mine device`, con questo programma:

```cpp
// MINE: l'Arduino del passaggio a livello. Il pulsante sul pin 2 dice che arriva un treno
// (treno:1); il LED sul pin 13 segue led:1 / led:0.
const int PULSANTE = 2;
const int LED = 13;
String riga;

void setup() {
  Serial.begin(115200);
  pinMode(PULSANTE, INPUT_PULLUP);
  pinMode(LED, OUTPUT);
  Serial.println("programma:passaggio_a_livello");
}

void loop() {
  static int prima = HIGH;
  int ora = digitalRead(PULSANTE);
  if (prima == HIGH && ora == LOW) {   // appena premuto
    Serial.println("treno:1");
  }
  prima = ora;
  while (Serial.available()) {         // da Byte, es. "led:1"
    char c = Serial.read();
    if (c == '\n') {
      if (riga.startsWith("led:")) digitalWrite(LED, riga.substring(4).toInt() > 0 ? HIGH : LOW);
      riga = "";
    } else if (c != '\r') {
      riga += c;
    }
  }
  delay(20);
}
```

## Missioni: MINE controlla le tappe

```
/mine lesson start crossing @a
```

| Tappa | MINE… | Superata quando… |
|---|---|---|
| 1 · `close` | manda `treno:1` | all'inizio la sbarra è aperta e la luce spenta; dopo il treno, sbarra chiusa e luce accesa |
| 2 · `reopen` | manda un treno, poi accende la leva (il treno è passato); due volte | il treno chiude, la leva riapre |
| 3 · `trains` | manda **due** treni, poi due passaggi; poi un altro treno | si riapre solo quando sono passati tutti; l'Arduino riceve `led:1` finché è chiuso, `led:0` quando è aperto |

Le prove sono dal vivo; se qualcosa non va MINE racconta la storia: *"dopo un treno, un treno, un
treno passato la sbarra doveva essere chiusa e la luce accesa"*.

## Svolgimento

### Introduzione — 10 minuti, senza computer

Disegnate alla lavagna gli **stati** (aperto, chiuso) come cerchi e gli **eventi** (arriva un
treno, il treno è passato) come frecce. È una **macchina a stati**: un ascensore, un distributore,
un semaforo funzionano così. Cosa succede se arriva un treno quando è già chiuso?

### Attività 1 — Chiudi (15 minuti)

```python
from mine import redstone
import mine


def passaggio(chiuso):
    redstone.setOutput("front", not chiuso)  # la sbarra
    redstone.setOutput("back", chiuso)  # la luce


passaggio(False)
while True:
    mine.device.wait("treno")
    passaggio(True)
```

La funzione `passaggio` tiene insieme tutto quello che cambia con lo stato: non si dimentica la
luce.

Soluzione: [`solutions/it/chiudi.py`](solutions/it/chiudi.py).

### Attività 2 — Riapri: due tipi di eventi (20 minuti)

`mine.device.wait` aspetta **solo** la scheda: mentre aspetta, la leva non la sente. Serve
`os.pullEvent()`, che aspetta **qualunque** evento e dice quale:

```python
evento = os.pullEvent()   # ("mine_device", chiave, valore) oppure ("redstone",)
if evento[0] == "mine_device" and evento[1] == "treno":
    passaggio(True)
elif evento[0] == "redstone" and redstone.getInput("left"):
    passaggio(False)
```

Perché `redstone.getInput("left")`? (L'evento `redstone` arriva anche quando la leva si spegne:
quello non è un treno passato.)

Soluzione: [`solutions/it/riapri.py`](solutions/it/riapri.py).

### Attività 3 — Più treni (20 minuti)

Con due binari, un secondo treno può arrivare prima che il primo sia passato: se la sbarra si
riaprisse al primo passaggio, sarebbe un disastro. Lo stato non è più "aperto o chiuso" ma **quanti
treni** ci sono: `treni > 0` vuol dire chiuso. E l'Arduino accende il suo LED finché è chiuso.

Soluzione: [`solutions/it/treni.py`](solutions/it/treni.py).

### Conclusione — 5 minuti

La domanda di riflessione di MINE: *Il tuo passaggio a livello era sempre in uno stato, e gli eventi
lo facevano passare da uno all'altro. Quali macchine intorno a te funzionano così? Quali sono i loro
stati e i loro eventi?*

## Problemi frequenti

| Cosa succede | Perché | Cosa fare |
|---|---|---|
| *"quando il programma parte, la sbarra deve essere aperta"* | lo stato iniziale non è impostato | `passaggio(False)` prima del ciclo |
| La leva non fa niente | il programma aspetta solo la scheda (`mine.device.wait`) | `os.pullEvent()` per tutti gli eventi |
| Si riapre quando la leva si spegne | manca il controllo `redstone.getInput("left")` | riapri solo quando la leva è accesa |
| Con due treni si riapre troppo presto | lo stato è un sì/no invece di un contatore | `treni` + 1 e − 1 |

## Riferimenti

- [Lezione 8 — La scheda](../08-board/README.it.md), [le schede](../../guide/devices.it.md), [guida Python](../../guide/python.it.md).
- Soluzioni in [`solutions/it/`](solutions/it/) (in inglese in [`solutions/`](solutions/)), provate
  in automatico in MicroPython.
