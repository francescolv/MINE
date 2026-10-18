[English](python.md) · **Italiano**

# Python: programmare Byte in Python

Byte esegue Lua, e con i blocchi gli studenti scrivono Lua senza vederlo. I ragazzi più grandi
possono scrivere in **Python**: Python vero (MicroPython, lo stesso che gira sul micro:bit e sul
Raspberry Pi Pico), con **gli stessi nomi** di Lua e delle lezioni: `byte.forward()`,
`redstone.getInput("left")`, `mine.say("Ciao!")`, `mine.device.wait("pulsante_a")`.

Ci sono due modi:

- **nel browser**, nell'editor Python di MINE: niente da installare;
- **in [Thonny](#thonny)** sul PC, con il Python completo, per chi vuole un vero ambiente di sviluppo.

## L'editor Python

Nel gioco scrivi `/mine blocks`: si apre l'editor a blocchi. Il pulsante **Python** in alto apre
l'editor Python, con la stessa sessione.

```python
from mine import byte
import mine

mine.say("Ciao da Python!")
for i in range(3):
    byte.up()
    byte.placeDown()
    byte.forward()
```

| | |
|---|---|
| **I miei programmi** | i programmi Python dello studente, conservati sul server (come quelli a blocchi) |
| **Salva** | conserva il programma senza eseguirlo |
| **Esegui ▶** | manda il programma a Byte; torna in Minecraft: dopo 3-2-1 parte |
| **Ferma ■** | ferma Byte e il programma |
| **Blocchi** | torna all'editor a blocchi |
| **Thonny…** | come usare Python sul PC con Thonny, il codice dello studente e **Scarica mine.py** (vedi sotto) |
| **Programmi degli studenti** | solo per gli insegnanti: apri il programma di uno studente, copialo tra i tuoi |

A destra, quello che il programma scrive con `print`, e gli errori: la riga si colora, e sotto il
messaggio di Python MINE spiega cosa vuol dire di solito («Python non conosce «x»: è scritto
giusto?»). La barra della lezione mostra l'obiettivo della tappa e i suggerimenti, con le parole di
Python quando la lezione le ha.

Nell'editor a blocchi il riquadro del codice ha **Lua | Python**: gli stessi blocchi in Python, e
**Continua in Python ▸** li trasforma in un nuovo programma Python, da continuare a mano.

## Come funziona

Il programma Python gira **nel browser**, con MicroPython. Ogni chiamata a Byte (`byte.forward()`,
`redstone.getInput("left")`…) va al server di MINE, che la fa eseguire a Byte con un piccolo
programma Lua, il ponte, che ha il nome del programma: è un programma di Byte come gli altri, quindi
**le lezioni lo verificano**, Ctrl+T lo ferma, il tasto **R** lo riesegue (solo con l'editor aperto:
il programma è nel browser). Tieni aperto l'editor mentre il programma gira.

- `time.sleep(3)` e `os.sleep(3)` aspettano 3 secondi del gioco, come in Lua.
- `os.pullEvent("redstone")` aspetta un cambio della redstone, come in Lua. Senza argomento
  aspetta qualunque evento e dà sempre una tupla: `evento[0]` è il nome (`"redstone"`,
  `"mine_device"`…), come nella [lezione 14](../lessons/14-crossing/README.it.md).
- `byte.forward()` dà `True` o `False` (senza il motivo che Lua dà in più).
- `mine.device.wait("dato")` dà due valori: `chiave, valore = mine.device.wait("dato")`.
- Python può chiamare solo le funzioni di Byte: `byte`, `redstone`, `mine`, `mine.device`, le attese.

## Thonny

[Thonny](https://thonny.org) è un editor Python per la scuola (si usa anche per il micro:bit e il
Pico): `sudo dnf install thonny` (Fedora) o `sudo apt install thonny` (Ubuntu, Debian).

1. **Una cartella per i programmi Python** sul PC, per esempio `MINE-Python` nella cartella home.
2. **`mine.py` in quella cartella**: nell'editor Python di MINE clicca **Thonny…**, poi **Scarica
   mine.py**, e spostalo da *Scaricati* alla cartella. È lo stesso file per tutti gli studenti: in un
   laboratorio con i PC che si ripristinano a ogni avvio, metti la cartella con `mine.py` nell'immagine.
3. **I programmi nella stessa cartella**: in Thonny scrivi il programma (`from mine import byte` …),
   salvalo lì (per esempio `scale.py`) e premi **F5**.
4. **La prima volta** `mine.py` trova da solo il server della scuola (MINE lo annuncia in rete, come
   per la lista di Minecraft) e chiede il **codice** dello studente: sei lettere e cifre, suo per
   sempre, che vede con `/mine python` nel gioco e nella finestra **Thonny…**. Li ricorda tutti e due
   in `mine.txt`, nella cartella. Se il server non si trova (una rete che blocca l'annuncio),
   `mine.py` ne chiede l'indirizzo.
5. **Torna in Minecraft**: dopo 3-2-1 Byte parte. La shell di Thonny mostra i `print` e gli errori.

Lo studente deve essere nel gioco mentre il programma gira (Byte ascolta mentre gioca); il
programma resta anche tra i suoi programmi Python nell'editor. Un codice sbagliato viene richiesto
di nuovo; su un PC usato da più studenti, ognuno ha la sua cartella, con il suo `mine.txt`.

## Per chi sviluppa

- Il lato browser: `src/main/resources/web/python/` (`python.html`, `python.js`, il worker
  `python-worker.js`, `mine.py`; MicroPython 1.29, MIT); il modulo per Thonny `web/python/thonny/mine.py` (i codici: `blocks/ThonnyKeys.java`, `data/mine/thonny.dat`).
- Il ponte: `web/python/bridge.lua` e `agent/PythonBridge.java`; il server dell'editor
  `blocks/BlockEditor.java` (`/api/py/run|call|end|stop|save`).
- `node tools/python-run.mjs <server> <codice> <nome> <programma.py>` esegue un programma come
  l'editor, senza browser: i GameTest lo usano per le soluzioni Python delle lezioni.
