[English](lua-api.md) · **Italiano**

# API Lua di MINE

Ogni Byte (e in generale ogni turtle, su un server con MINE) ha a disposizione l'API `mine`,
in aggiunta alle API di [CC: Tweaked](https://tweaked.cc/) come `turtle`, `fs` e `term`.

Nel terminale di Byte, `help mine` mostra questo riepilogo in gioco, in italiano se il gioco è in italiano.

## `mine.say(messaggio)`

Byte scrive il messaggio in chat, firmandosi con la sua etichetta:

```
[Studente's Byte] Ciao!
```

Per ora l'etichetta di Byte è in inglese ("Studente's Byte"); diventerà configurabile
(è nella [roadmap](../../ROADMAP.md)).

- Lo leggono solo i giocatori entro **32 blocchi** da Byte, così in un laboratorio con
  trenta Byte la chat resta leggibile.
- Accetta anche numeri e altri valori: `mine.say(3 + 4)` scrive `7`.
- I messaggi più lunghi di 256 caratteri vengono tagliati.
- Ogni chiamata richiede un tick del server (1/20 di secondo).

## `mine.owner()`

Il nome dello studente a cui appartiene Byte, cioè chi l'ha creato con `/mine byte`.
Restituisce `nil` se la turtle non ha un proprietario.

```lua
mine.say("Ciao! Sono Byte di " .. mine.owner())
```

## `mine.challenge()`

Quello che la tappa della lezione chiede **questa volta**, scelto da MINE quando il programma
parte: per esempio il lato del quadrato nella [lezione 5](../lessons/05-functions/README.it.md).
Siccome cambia a ogni avvio, il programma deve leggerlo invece di contare su un numero fisso.
Non restituisce niente (`nil`) quando la tappa non chiede nulla, o fuori da una lezione.

```lua
local lato = mine.challenge()
mine.say("MINE vuole un quadrato " .. lato .. " x " .. lato)
```

A volte i numeri sono più d'uno, per esempio avanti e destra di un tesoro nella
[lezione 11](../lessons/11-treasure/README.it.md): `local avanti, destra = mine.challenge()`.
`mine.challenge(n)` dà solo l'`n`-esimo (da 1), come il blocco *il numero … della sfida*.

## `byte.ripeDown()`

`true` quando il blocco sotto Byte è un raccolto che ha finito di crescere (grano, carote,
patate, barbabietole…), come nella [lezione 15](../lessons/15-farm/README.it.md); con i blocchi,
*raccolto maturo sotto?*.

## `mine.version()`

La versione di MINE installata sul server, ad esempio `"0.1.0"`.

## `mine.device`: la scheda

La [scheda](devices.it.md) (micro:bit, Arduino…) che lo studente ha collegato al suo PC con
`/mine device`. La scheda scrive righe `chiave:valore`; i valori sono numeri interi o testi.

| Funzione | Cosa fa |
|---|---|
| `mine.device.read(chiave)` | L'ultimo valore arrivato per `chiave`, o `0` se la scheda non l'ha mai scritta |
| `mine.device.wait([chiave])` | Aspetta che la scheda scriva `chiave` (o una chiave qualunque, senza argomento) e restituisce chiave e valore |
| `mine.device.send(chiave, valore)` | Scrive `chiave:valore` sulla scheda; `false` se non c'è una scheda collegata |
| `mine.device.connected()` | `true` se lo studente ha una scheda collegata |

Ogni riga arriva anche come evento `mine_device`, con chiave e valore, per i programmi che
aspettano più cose insieme:

```lua
local _, chiave, valore = os.pullEvent("mine_device")
```

```lua
mine.device.wait("pulsante_a")
mine.device.send("cuore", 1)
byte.forward()
if mine.device.read("luce") < 50 then
  mine.say("È buio")
end
```

## Avviare i programmi dalla chat

`/mine byte run <programma>` avvia un programma di Byte come se lo studente l'avesse
scritto nel suo terminale (anche con argomenti: `/mine byte run torre 5`). Funziona quando
Byte è acceso e aspetta un comando al prompt `>`; alla fine MINE scrive in chat allo
studente se il programma è finito o si è fermato per un errore.

Il tasto **R** (in Controlli, sotto MINE) esegue di nuovo l'ultimo programma eseguito da Byte,
con i suoi argomenti, che sia stato scritto nel terminale, avviato dalla chat o mandato dai
[blocchi](blocks.it.md): gli studenti scrivono e provano un programma nel terminale, lo chiudono
con **Esc** e premono **R** per guardare Byte da fuori.

Lo realizza `rom/autorun/mine.lua`, che si appoggia alle funzioni interne `mine._idle` e
`mine._report`: non vanno usate nei programmi degli studenti. Lo stesso file copia su Byte i
programmi che una lezione dà allo studente (`mine._prepare`, vedi
[Scrivere una lezione](lesson-format.it.md#programmi-per-gli-studenti)). Quando Byte si accende,
sostituisce anche l'intestazione di CraftOS e il suo consiglio del giorno con il benvenuto di
MINE — versione, nome di Byte, lezione e tappa dello studente — nella lingua dello studente
(`mine._welcome`).

Lo stesso file avvisa il server quando un programma dello studente inizia e finisce
(`mine._started` e `mine._finished`), così le missioni delle lezioni possono controllare cosa ha
fatto Byte. Chiamarle a mano non serve a niente: il server guarda solo il mondo (dov'è
Byte, verso dove guarda, quali blocchi ci sono), mai quello che dichiara Lua.

## Il piccone di Byte

Ogni Byte viene creato con un **piccone di diamante**, così può scavare come l'Agent di
Minecraft Education: `byte.dig()`, `byte.digUp()` e `byte.digDown()` rompono il blocco
davanti, sopra o sotto, e lo mettono nell'inventario di Byte. Lo ricevono anche i Byte
creati con versioni più vecchie di MINE, appena aspettano un comando, se non hanno altri attrezzi.

## Cresce con le lezioni

L'API cresce insieme alle lezioni: ogni funzione nuova nasce da un'attività che ne ha
bisogno. Proposte e richieste nelle issue del progetto.

L'API **cresce soltanto**: le funzioni si aggiungono, non si rinominano né si tolgono, e
continuano a fare quello che dice questa pagina, così i programmi degli studenti e le lezioni
funzionano con ogni nuovo MINE.
