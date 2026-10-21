[English](devices.md) · **Italiano**

# Le schede: micro:bit, Arduino e Byte

Uno studente collega una **scheda** (micro:bit, Arduino, Raspberry Pi Pico…) al suo PC con il
cavo USB, e la scheda e il suo Byte si parlano: un pulsante del micro:bit fa camminare Byte,
Byte accende un cuore sul display del micro:bit.

I programmi sono **due**, uno per macchina, e ognuno si scrive e si carica dal suo editor:

| | Si scrive con | Si carica con | Gira su |
|---|---|---|---|
| Programma della **scheda** | [MakeCode](https://makecode.microbit.org) (blocchi, JavaScript o Python), MicroPython, l'IDE di Arduino | MakeCode: **Scarica**, poi il file `.hex` sull'unità **MICROBIT**; Arduino: **Carica** | la scheda |
| Programma dell'**Byte** | l'[editor a blocchi](blocks.it.md) di MINE (`/mine blocks`) o il Lua | **Esegui ▶** o **Invia a Byte** | Byte, in Minecraft |

Ognuno resta dov'è: la scheda tiene il suo programma anche scollegata e lo fa ripartire ogni volta
che riceve corrente; Byte tiene il suo sul disco (si rilancia con **R**). Si ricarica solo
quello che cambia.

> I blocchi di Byte («vai avanti», «gira») **non** sono in MakeCode: la scheda non muove
> Byte, gli **dice** qualcosa («hanno premuto A») e il programma di Byte decide cosa fare.
> Il «MakeCode per Minecraft» con i blocchi di Byte è un altro editor, quello di Minecraft
> Education: con MINE si usa il MakeCode del micro:bit.

## Come si parlano: righe `chiave:valore`

La scheda e MINE si scambiano **una riga di testo per messaggio**, sulla porta seriale USB a
**115200 baud**:

```
pulsante_a:1
luce:120
programma:semaforo
```

| | |
|---|---|
| Chiave | lettere minuscole, cifre e `_`, fino a 32 caratteri (le maiuscole diventano minuscole) |
| Valore | un numero intero da −32768 a 32767 (oltre viene limitato) oppure un testo |
| Riga | al massimo 256 caratteri; le righe che non sono `chiave:valore` vengono ignorate |
| Frequenza | al massimo 40 righe al secondo per studente, le altre si perdono: conviene scrivere **solo quando il valore cambia** |

È lo stesso formato di [SerialCraft](https://github.com/LeonardoAliaga/SerialCraft/blob/HEAD/docs/en/protocol.md),
con in più i numeri negativi e i testi: gli sketch scritti per SerialCraft funzionano anche con
MINE.

Ogni scheda parla solo con Byte **dello studente che l'ha collegata**: 25 schede in un
laboratorio non si disturbano.

## Collegare la scheda

1. Collega la scheda al PC con il cavo USB (per il micro:bit: un cavo **dati**, non solo di
   ricarica).
2. Nel gioco, apri la chat (**T**) e scrivi `/mine device`.
3. Nella finestra clicca la tua scheda (il micro:bit compare come `/dev/ttyACM0` su Linux, `COM3`
   o simile su Windows). Se non c'è, collegala e clicca **Cerca di nuovo**.

La finestra mostra in tempo reale quello che la scheda scrive (→) e quello che Byte le manda
(←), e sotto come Byte legge l'ultimo valore arrivato. Chiudila con **Fatto**: la scheda resta
collegata finché lo studente non clicca **Scollega** o esce dal gioco.

Quando si collega, MINE **fa ripartire** la scheda (il micro:bit con un segnale sulla seriale;
Arduino Uno e Nano ripartono da soli quando si apre la porta), così il suo programma comincia da
capo.

### Che programma c'è sulla scheda?

Caricare un programma nuovo sulla scheda non si vede dalla seriale: per farlo sapere a MINE, il
programma della scheda **scrive il suo nome quando parte**, con la chiave `programma` (o
`program`):

```
programma:semaforo
```

La finestra mostra allora «Sulla scheda: semaforo (partito alle 09:36:11)»; a finestra chiusa,
«Sulla tua scheda gira semaforo» compare sopra la barra. Ogni volta che si carica il programma il
nome e l'orario si aggiornano: è il modo per essere sicuri che sulla scheda ci sia la versione
nuova.

## Il programma della scheda

### micro:bit con MakeCode

Su [makecode.microbit.org](https://makecode.microbit.org): **Nuovo progetto**. I blocchi che
servono:

| Blocco | Categoria |
|---|---|
| **all'avvio** (c'è già nell'area di lavoro) | Di base |
| **scrivi riga seriale** «…» | Avanzate → Seriale |
| **quando si preme il pulsante** A | Input |
| **quando la seriale riceve** «nuova riga», con **leggi seriale fino a** «nuova riga» | Avanzate → Seriale |
| **mostra icona** | Di base |

Un **semaforo**: A lo fa diventare verde, B rosso.

```
all'avvio
  scrivi riga seriale "programma:semaforo"
  mostra icona ✗
  scrivi riga seriale "semaforo:0"

quando si preme il pulsante A
  mostra icona ✓
  scrivi riga seriale "semaforo:1"

quando si preme il pulsante B
  mostra icona ✗
  scrivi riga seriale "semaforo:0"
```

Poi **Scarica** e trascina il file `.hex` sull'unità **MICROBIT** (si accende la luce gialla sul
retro mentre lo carica). Lo stesso in JavaScript, nella scheda JavaScript di MakeCode:

```js
serial.writeLine("programma:semaforo")
basic.showIcon(IconNames.No)
serial.writeLine("semaforo:0")
input.onButtonPressed(Button.A, function () {
    basic.showIcon(IconNames.Yes)
    serial.writeLine("semaforo:1")
})
input.onButtonPressed(Button.B, function () {
    basic.showIcon(IconNames.No)
    serial.writeLine("semaforo:0")
})
```

Per ricevere quello che manda Byte (per esempio `cuore:1`):

```js
serial.onDataReceived(serial.delimiters(Delimiters.NewLine), function () {
    let riga = serial.readUntil(serial.delimiters(Delimiters.NewLine))
    if (riga == "cuore:1") {
        basic.showIcon(IconNames.Heart)
    } else if (riga == "cuore:0") {
        basic.clearScreen()
    }
})
```

> Per caricare il programma usa il file `.hex` sull'unità MICROBIT. Il caricamento diretto dal
> browser (**Collega dispositivo**, WebUSB) non è ancora stato provato insieme a MINE.

### micro:bit con MicroPython

```python
from microbit import *

print("programma:pulsanti")
while True:
    if button_a.was_pressed():
        print("pulsante_a:1")
    if button_b.was_pressed():
        print("pulsante_b:1")
    sleep(50)
```

### Raspberry Pi Pico con MicroPython

Provato con un Pico e MicroPython 1.15 e 1.29. Ogni secondo manda la temperatura del suo chip; `led:1` /
`led:0` accendono e spengono il LED sulla scheda. Salvalo sul Pico come `mine_prova.py` (Thonny:
*File → Salva come → Raspberry Pi Pico*), e come `main.py` una sola riga, `import mine_prova`, così
parte da solo ogni volta che il Pico si collega.

```python
# MINE: a Raspberry Pi Pico that talks with Byte, one line key:value at a time.
# Every second it sends its temperature (temperatura:NN); led:1 / led:0 switch its LED.
import machine
import select
import sys
import time

led = machine.Pin(25, machine.Pin.OUT)
sensor = machine.ADC(4)  # the temperature sensor inside the RP2040
poll = select.poll()
poll.register(sys.stdin, select.POLLIN)

print("programma:pico_prova")
last = time.ticks_ms()
line = ""
while True:
    if time.ticks_diff(time.ticks_ms(), last) >= 1000:
        last = time.ticks_ms()
        volts = sensor.read_u16() * 3.3 / 65535
        print("temperatura:%d" % round(27 - (volts - 0.706) / 0.001721))
    if poll.poll(20):
        char = sys.stdin.read(1)
        if char in "\r\n":
            if line.startswith("led:"):
                try:
                    led.value(1 if int(line[4:]) > 0 else 0)
                except ValueError:
                    pass
            line = ""
        else:
            line += char
```

- Il Pico non riparte quando MINE apre la sua porta (il micro:bit sì): la sua riga `programma:`,
  scritta all'accensione, può non comparire in `/mine device`. Tutto il resto funziona uguale.
- La temperatura del chip è approssimata (sbaglia di qualche grado): va bene per vederla salire
  quando ci soffi sopra, non come termometro.
- Chiudi il collegamento di Thonny al Pico (o quello di `/mine device`) prima di usare l'altro: la
  porta la può usare un programma alla volta.

### Raspberry Pi Pico 2 W in Wi-Fi

Un Pico 2 W (o un Pico W) parla con il server MINE da solo, senza cavo verso il PC: basta
alimentarlo, anche con un power bank. Provato con un Pico 2 W e MicroPython 1.29. Le righe sono le
stesse `chiave:valore` del cavo, e arrivano solo al Byte dello studente del codice.

1. Installa MicroPython per il **Pico 2 W** (tieni premuto BOOTSEL mentre lo colleghi e copia il
   file `.uf2` nel disco RP2350).
2. Scarica `mine_wifi.py` dal server, `http://<indirizzo del server>:25580/python/pico/mine_wifi.py`,
   e salvalo sul Pico (Thonny: *File → Salva come → Raspberry Pi Pico*).
3. Salva sul Pico un file `mine.txt`:

   ```
   wifi=nome della rete
   password=la sua password
   code=il codice dello studente (nel gioco: /mine python)
   lang=it
   ```

   Il server lo trova da solo sulla rete della scuola (come la lista Multigiocatore); se non ci
   riesce, aggiungi `server=192.168.1.20`.
4. Salva il programma come `mine_prova_wifi.py` e, come `main.py`, la riga `import mine_prova_wifi`.

```python
# MINE: a Raspberry Pi Pico 2 W that talks with Byte over Wi-Fi, no cable to the PC.
# Every second it sends its temperature (temperatura:NN); led:1 / led:0 switch its LED.
import time

import mine_wifi as mine
from machine import ADC, Pin

led = Pin("LED", Pin.OUT)
sensor = ADC(4)  # the temperature sensor inside the chip

mine.connect()
mine.send("programma", "pico_wifi_prova")
while True:
    volts = sensor.read_u16() * 3.3 / 65535
    mine.send("temperatura", round(27 - (volts - 0.706) / 0.001721))
    for key, value in mine.receive():
        if key == "led":
            led.value(1 if value else 0)
    time.sleep(1)
```

- Mentre entra nel Wi-Fi il LED lampeggia; poi è del programma. I messaggi (`MINE: …`) si vedono
  in Thonny quando il Pico è collegato al PC.
- La scheda parla con Byte solo mentre lo studente è nel gioco: prima aspetta, da sola.
- `mine.receive()` va chiamato spesso, almeno ogni pochi secondi: è lì che la scheda chiede al
  server le righe per lei. Se tace per 10 secondi, MINE la considera scollegata (e la ricollega
  alla richiesta successiva). Le righe inviate molto vicine (a meno di 0,15 secondi) partono
  insieme, con il `mine.send()` o il `mine.receive()` successivo.
- `/mine device` dice in chat che la scheda è collegata via Wi-Fi e quale programma ci gira.
- Prima di comprarne tanti, prova con uno: alcune reti Wi-Fi delle scuole non lasciano parlare tra
  loro i dispositivi (isolamento dei client). Se la scheda non trova il server neanche con
  `server=`, chiedi al tecnico di rete.

### Arduino

Un pulsante sul pin 2, un LED sul pin 9:

```cpp
void setup() {
  Serial.begin(115200);
  pinMode(2, INPUT_PULLUP);
  pinMode(9, OUTPUT);
  Serial.println("programma:pulsante");
}

void loop() {
  static int last = -1;
  int pressed = digitalRead(2) == LOW ? 1 : 0;
  if (pressed != last) {           // solo quando cambia
    Serial.println(String("pulsante:") + pressed);
    last = pressed;
  }
  if (Serial.available()) {        // da Byte, es. "led:255"
    String line = Serial.readStringUntil('\n');
    if (line.startsWith("led:")) analogWrite(9, line.substring(4).toInt());
  }
}
```

## Il programma di Byte

### Con i blocchi

Nell'[editor a blocchi](blocks.it.md), la categoria **Scheda**:

| Blocco | Cosa fa |
|---|---|
| **aspetta** «pulsante_a» **dalla scheda** | Byte si ferma finché la scheda non scrive quella chiave |
| **valore della scheda** «luce» | l'ultimo valore arrivato per quella chiave (0 finché la scheda non l'ha mai scritto) |
| **manda alla scheda** «cuore» 1 | scrive `cuore:1` sulla scheda |
| **scheda collegata?** | vero se lo studente ha collegato una scheda con `/mine device` |

Con il semaforo di sopra, Byte cammina solo con il verde:

```
ripeti mentre vero
  se  valore della scheda «semaforo» = 1
    vai avanti
  aspetta 0.5 secondi
```

> **aspetta 0.5 secondi** (in Cicli) non va tolto: un ciclo che controlla senza mai fermarsi
> blocca Byte con l'errore «Too long without yielding». I blocchi **aspetta … dalla scheda**,
> **vai** e **gira** aspettano già da soli.

Premo A, Byte fa un passo e il micro:bit mostra un cuore per un secondo:

```
ripeti mentre vero
  aspetta «pulsante_a» dalla scheda
  manda alla scheda «cuore» 1
  vai avanti
  aspetta 1 secondi
  manda alla scheda «cuore» 0
```

### In Lua

Le stesse cose con `mine.device` (vedi l'[API Lua](lua-api.it.md#minedevice-la-scheda)):

```lua
while true do
  local chiave, valore = mine.device.wait()   -- la prossima riga, qualunque chiave
  if chiave == "pulsante_a" then
    mine.device.send("cuore", 1)
    byte.forward()
    mine.device.send("cuore", 0)
  end
end
```

## Senza codice: i blocchi della scheda

Due blocchi, nella sezione **Pietrarossa** dell'inventario in Creativa, collegano la scheda e la
redstone senza nessun programma di Byte:

| Blocco | Cosa fa |
|---|---|
| **Ricevitore della scheda** (verde, un cuore) | dà redstone quando la scheda scrive la sua chiave: `pulsante_a:1` lo accende, `pulsante_a:0` lo spegne. I numeri da 1 a 15 sono la forza della redstone (oltre 15, 15); vanno bene anche `on`/`off` |
| **Trasmettitore per la scheda** (blu, una freccia) | quando gli arriva redstone, scrive `chiave:potenza` sulla scheda: `led:15` con una leva accanto, `led:0` quando la leva torna giù |

Quando lo piazzi, una finestra chiede la **chiave** (la stessa parola che usa il programma della
scheda). Il blocco è della **tua** scheda: la scheda di un altro studente non lo tocca. Clic destro
per vedere o cambiare la chiave; gli altri lo vedono ma non possono cambiarlo.

- Una scheda che dice solo «premuto» (*quando si preme il pulsante A* di MakeCode che scrive
  `pulsante_a:1`): imposta il ricevitore **Come un pulsante**, e resta acceso un secondo ogni volta.
- Una luce da 0 a 255 vale 15 per qualsiasi valore sopra 15: fai mandare `luce:1`/`luce:0` alla
  scheda (con un *se*), oppure lascia decidere Byte (`mine.device.read("luce") > 100`).
- Quando la scheda si collega, dopo un paio di secondi (le schede ripartono) i trasmettitori le
  dicono come sono.
- Gli insegnanti possono dare un blocco a uno studente: nella sua finestra, **Scheda di** con il nome
  dello studente (deve essere collegato). Così un mondo dell'insegnante può avere una porta per il
  micro:bit di ogni studente.

Esempi: una porta che si apre con il pulsante A (ricevitore *Come un pulsante* sotto la porta); una
leva che accende il cuore del micro:bit (trasmettitore accanto alla leva, e sulla scheda *se `led` è
15, mostra un cuore*); un sensore di luce diurna accanto a un trasmettitore, così la scheda sa quando
nel gioco è notte.

## Durante una lezione

La [lezione 8](../lessons/08-board/README.it.md) è la lezione con la scheda: una porta, un lampione
e un cuore, con il programma per il micro:bit pronto da copiare.

Se lo studente ha una lezione in corso, MINE mette alla prova **ogni** programma che parte per la
tappa della lezione, e lo ferma quando ha visto abbastanza («Non ancora…»): anche un programma
della scheda, che non c'entra con la tappa. Per provare le schede fuori dalle lezioni, il docente
azzera i progressi dello studente con `/mine lesson reset <studente>`.

## Se non funziona

| Cosa succede | Cosa fare |
|---|---|
| La finestra dice «Nessuna scheda» | Il cavo è solo di ricarica, o la scheda non è collegata bene: prova un altro cavo o un'altra porta USB, poi **Cerca di nuovo**. |
| «Non riesco ad aprire /dev/ttyACM0» (Linux) | L'utente del PC deve essere nel gruppo `dialout` (su alcune distribuzioni `uucp`): `sudo usermod -aG dialout <utente>`, poi esci e rientra nella sessione. Vale anche per Prism Launcher da Flatpak. |
| «Non riesco ad aprire COM3» (Windows) | Un altro programma usa la porta (l'IDE di Arduino con il monitor seriale aperto, MakeCode collegato con WebUSB): chiudilo. |
| La scheda è collegata ma non arriva niente | Il programma della scheda non scrive sulla seriale, o a un'altra velocità: deve essere **115200 baud** (in MakeCode è già così). |
| Le righe arrivano grigie, «ignorata» | Non sono `chiave:valore`: manca i due punti, o la chiave ha spazi o lettere accentate. |
| Byte non reagisce | Il programma di Byte non sta girando (rilancialo con **R**), o la chiave è scritta diversa nei due programmi (`pulsante_a` e `pulsanteA` sono chiavi diverse). |
| «Non ancora…» e il programma si ferma | È in corso una lezione: vedi sopra. |
