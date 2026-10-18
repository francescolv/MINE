[English](npc.md) · **Italiano**

# NPC per la didattica

Gli NPC sono personaggi che parlano ai tuoi studenti, come gli NPC di Minecraft Education: una
guida che spiega l'attività, un personaggio di una storia, il custode di un museo… Quello che
dicono lo scrivi **nel gioco**, in un libro e penna.

## Creare un NPC

Mettiti dove lo vuoi, rivolto da quella parte, e scrivi:

```
/mine npc create Ada
```

Ada compare due blocchi davanti a te, rivolta verso di te. Gli NPC non si muovono, non si
possono ferire e non commerciano: cliccandoli col tasto destro si legge il loro dialogo. Per
rinominarne uno usa un cartellino, come per qualsiasi personaggio di Minecraft.

Possono creare NPC gli insegnanti (operatori, livello di permesso 2) e i **costruttori**: vedi
[Studenti che costruiscono](#studenti-che-costruiscono). In un mondo in giocatore singolo con i
trucchi attivi chi gioca è operatore, quindi può creare NPC anche lui.

## Scrivere il dialogo

**Clicca l'NPC col tasto destro**: vedi il suo dialogo, con **[✎ Modifica]** accanto al nome.
Cliccalo e si apre la finestra di MINE con il dialogo com'è: una pagina per schermata, con i
pulsanti per sfogliare, aggiungere e togliere pagine. Scrivi e premi **Fatto**: *"Ora Ada dirà le 3
pagine del tuo libro."*

### Con un libro e penna

Lo stesso funziona con un libro, anche in un gioco senza MINE:

1. Prendi un **libro e penna** dall'inventario della modalità creativa (tasto `E`). Tienilo in mano
   e fai **tasto destro in aria**, non sull'NPC, per aprirlo e scriverci dentro. **Ogni pagina è
   una schermata** del dialogo: tieni le pagine brevi, poche righe ciascuna.
2. Clicca l'NPC col tasto destro tenendo il libro. MINE risponde: *"Ora Ada dirà le 3 pagine del
   tuo libro."* Il libro resta a te.

Per modificarlo dopo, **accovacciati e clicca l'NPC col tasto destro a mani vuote**: ricevi un
libro con il suo dialogo. Modificalo e ridaglielo nello stesso modo.

### Pulsanti

Un pulsante si scrive come un link: `[testo](destinazione)`.

- `[Inizia la lezione](/mine lesson start loops)` — un **comando**, eseguito dallo studente che
  clicca, con i suoi permessi. Gli studenti possono usare i comandi `/mine lesson` e
  `/mine byte`, quindi un NPC può avviare una lezione, dare un suggerimento o richiamare Byte.
- `[Scopri di più](https://example.org)` — una **pagina web**, che Minecraft chiede prima di aprire.

Per esempio, una pagina così:

```
Benvenuti in laboratorio! Oggi costruiamo con i cicli.

[Cominciamo](/mine lesson start loops)
```

### Quiz

Una pagina diventa una **domanda a scelta multipla** quando contiene risposte scritte come una
lista di spunta: `[ ]` per una risposta sbagliata, `[x]` per una giusta (possono essercene più di una).

```
Quanti blocchi mette?
for i = 1, 4 do byte.place() end

[ ] 3
[x] 4
[ ] 5
```

Gli studenti vedono le risposte come pulsanti, `A) 3`, `B) 4`, `C) 5`. Dopo il clic il libro si
riapre sulla stessa pagina, con l'esito sotto la domanda: *"✔ Giusto!"* oppure *"✘ Non proprio:
riprova."*. Gli studenti vanno avanti, o tornano indietro, con le frecce del libro; le domande a
cui hanno risposto giusto mantengono la ✔, anche quando riparlano con l'NPC. Una pagina senza nessuna `[x]` è un **sondaggio**: ogni risposta
va bene (*"Quale parte ti è piaciuta di più?"*).

Un dialogo può alternare pagine: spiegazioni, domande, un'ultima pagina con il pulsante per
iniziare la lezione.

Una pagina può contenere **più domande**, una dopo l'altra: dopo le risposte di una domanda, la
riga successiva inizia la domanda seguente. Ogni esito compare sotto la sua domanda. La pagina di un libro però è piccola: se le risposte non ci stanno, usa una pagina
per domanda.

### Pagine che seguono la lezione

Un NPC può seguire la lezione di ogni studente: un suggerimento per la tappa a cui è arrivato, i
complimenti quando finisce. Scrivi nelle **prime righe della pagina** per chi è:

| prima riga | la pagina è per gli studenti… |
|---|---|
| `@lesson loops` | che stanno facendo la lezione `loops` |
| `@lesson loops 2` | alla tappa 2 di `loops` (o un intervallo: `@lesson loops 2-3`) |
| `@done loops` | che hanno finito `loops`, anche se sono passati a un'altra lezione |
| `@lesson none` | che in questo momento non stanno facendo nessuna lezione |

Più righe vogliono dire "una qualsiasi di queste". Le pagine senza queste righe sono per tutti.
Il nome della lezione è quello di `/mine lesson start loops` (`/mine lesson list` le mostra
tutte). Ad esempio, una guida per la lezione sui cicli:

```
Ciao! Sono Ada, ti aiuto con i cicli.    ← pagina 1, per tutti
```
```
@lesson loops 1
Conta i blocchi di una fila: è il
numero del for.                          ← pagina 2, solo alla tappa 1
```
```
@lesson loops 2
Un quadrato sono quattro file: un
ciclo dentro un ciclo!                   ← pagina 3, solo alla tappa 2
```
```
@done loops
Bravo! Pronto per un quiz?
[ ] ...                                  ← pagina 4, dopo la lezione
```

Ogni studente vede solo le sue pagine, e riparlando con l'NPC più tardi vede quelle per il punto
a cui è arrivato. Tu (e il costruttore che ha creato l'NPC) vedi **tutte** le pagine, ognuna con
in alto, in grigio, per chi è: per vederlo come uno studente usa `/mine view student`. Se MINE
non capisce una riga, o la lezione o la tappa non esistono, te lo dice quando ridai il libro, e
la pagina lo mostra con ⚠.

## Cosa vedono gli studenti

Gli studenti cliccano l'NPC col tasto destro: il suo dialogo si apre in una **finestra a forma
di libro**, con il nome dell'NPC in cima, le frecce del libro per sfogliare le pagine, e i
pulsanti da cliccare col mouse. Nel loro inventario non viene aggiunto niente, e non devono
installare niente: un NPC è un abitante del villaggio di Minecraft, e il dialogo sta sul server. Solo una cosa richiede MINE nel gioco dello studente (il modpack
di MINE): dopo aver risposto a una domanda, il libro si riapre **sulla stessa pagina**. Senza,
il libro si riapre partendo da quella pagina, e le pagine precedenti non si possono rileggere.

## Risultati dei quiz

Mettiti accanto all'NPC e scrivi `/mine npc results`. Per ogni domanda vedi quanti studenti
hanno risposto giusto al primo tentativo e, per ogni studente, la sua **prima** risposta (quello
che sapeva) e quanti tentativi gli sono serviti per arrivare a quella giusta:

```
Quanti blocchi mette?
   9 su 12 giuste al primo tentativo
   Alex: ✔ B) 4
   Sam: ✘ A) 3 — giusta dopo 2 tentativi
   Kim: ✘ C) 5 — non ancora giusta
```

Per i sondaggi vedi l'ultima scelta di ogni studente. Gli insegnanti possono leggere i risultati
anche da lontano con il nome dell'NPC, `/mine npc results Ada`, anche dopo che l'NPC è stato
rimosso. I costruttori leggono i risultati dei propri NPC.

Le risposte sono salvate nel mondo. Se **riscrivi una domanda**, conta come una domanda nuova: le
risposte al vecchio testo restano lì. Per riusare un NPC con un'altra classe, cancella le sue
risposte con `/mine npc clear`, accanto a lui.

Gli studenti rispondono cliccando nel libro, vicino all'NPC: non possono rispondere da qualunque
punto scrivendo il comando.

### Scaricare le risposte

`/mine npc export` salva **tutte le risposte ai quiz**, di tutti gli NPC, in un file CSV da aprire
con un foglio di calcolo (LibreOffice Calc, Excel, Fogli Google) per ordinarle, filtrarle e farne
grafici. C'è una riga per ogni studente e domanda:

| colonna | |
|---|---|
| `npc`, `page`, `question` | quale domanda (pagina del dialogo dell'NPC) |
| `type` | `quiz`, oppure `poll` (sondaggio) |
| `student` | il nome dello studente |
| `first_answer`, `first_answer_text` | la sua prima risposta (`A`, `B`…) e il suo testo; per i sondaggi, l'ultima |
| `first_right` | `true` se la prima risposta era giusta |
| `solved`, `tries` | se alla fine ha risposto giusto, e dopo quanti tentativi |
| `time` | quando ha risposto la prima volta |

Il file viene salvato nella cartella del mondo, in `mine/exports`. Se nel tuo gioco c'è MINE (il
modpack di MINE), viene anche **scaricato sul tuo computer**, nella cartella Scaricati: il gioco
apre una finestra con i pulsanti **Apri il file** e **Apri la cartella** (in chat resta il nome
del file, per ritrovarlo dopo). Il file separa le colonne con la virgola: se il
foglio di calcolo lo chiede all'apertura, scegli *virgola* e *UTF-8*.

## NPC e lavagne nei datapack

Le lezioni possono arrivare con i loro personaggi e le loro lavagne, pronti da mettere nel mondo:
MINE ha una guida e una lavagna per la lezione sui cicli.

```
/mine npc place mine:loops_guide
/mine board place mine:loops
```

La guida, Ada, segue la lezione: propone di iniziarla, a ogni tappa dà un consiglio e un
pulsante per il suggerimento, e alla fine fa una domanda e un sondaggio. Ogni studente la legge
**nella propria lingua**. Resta collegata al suo file: quando il file cambia (dopo `/reload`),
dice il nuovo dialogo. Se le dai un tuo libro, dice il tuo (MINE te lo segnala), e puoi partire
dal suo: accovacciati e cliccala col tasto destro a mano vuota.

Le lavagne dai datapack vengono scritte nella lingua di chi le mette, e poi sono come tutte le
altre.

### Scriverli

Sono file JSON in un datapack, accanto alle lezioni: `data/<namespace>/mine_npc/<id>.json` e
`data/<namespace>/mine_board/<id>.json`. Pagine e righe si scrivono come nel libro, con `\n` per
andare a capo; una lista per lingua (o una sola lista per tutte le lingue):

```json
{
	"format": 1,
	"name": "Ada",
	"pages": {
		"en_us": ["Hi! I'm Ada.", "@lesson loops 1\nA tip for step 1."],
		"it_it": ["Ciao! Sono Ada.", "@lesson loops 1\nUn consiglio per la tappa 1."]
	}
}
```

```json
{
	"format": 1,
	"size": 2,
	"text": {
		"en_us": ["# Loops", "for i = 1, 10 do", "end"],
		"it_it": ["# I cicli", "for i = 1, 10 do", "end"]
	}
}
```

Gli studenti la cui lingua non c'è leggono l'inglese, o la lingua che il file ha. Tieni le
stesse pagine in tutte le lingue: le risposte alle domande sono salvate per pagina. Dopo aver
modificato un file scrivi `/reload`; un file con un errore viene saltato, con il motivo nel log
del server. `"format": 1` è facoltativo, come nelle [lezioni](lesson-format.it.md#cosa-contiene-una-lezione). Quelli di MINE sono in [`src/main/resources/data/mine/`](../../src/main/resources/data/mine/).

## Rimuovere un NPC

Mettiti vicino e scrivi `/mine npc remove`: rimuove l'NPC più vicino entro 8 blocchi (per un
costruttore, il più vicino tra i suoi).

## Studenti che costruiscono

Anche gli studenti possono voler costruire con gli NPC: un museo con le guide, una storia con i
personaggi, un percorso a quiz per i compagni. Renderli operatori darebbe loro tutti i poteri del
gioco, quindi MINE ha un ruolo intermedio, come il *World Builder* di Minecraft Education:

```
/mine builder add Alex Sam
/mine builder list
/mine builder remove Sam
```

| | Studente | Costruttore | Insegnante |
|---|---|---|---|
| legge gli NPC | ✓ | ✓ | ✓ |
| crea NPC | | ✓ | ✓ |
| scrive, riprende e rimuove gli NPC | | solo i suoi | tutti |
| legge e cancella i risultati dei quiz | | solo dei suoi NPC | di tutti |
| `/mine builder`, comandi delle lezioni per la classe | | | ✓ |

Il ruolo resta salvato nel mondo. Quando lo togli, lo studente può ancora leggere i suoi NPC ma
non modificarli più; tu puoi sempre modificare o rimuovere qualsiasi NPC.

## Vedere quello che vedono gli studenti

Per provare i tuoi NPC e le lezioni come uno studente, scrivi `/mine view student`: MINE ti tratta
come uno studente qualsiasi (gli NPC si possono solo leggere, niente comandi da insegnante o da
costruttore) finché non scrivi `/mine view teacher`, o il server si riavvia. I poteri da operatore
di Minecraft, come `/gamemode`, restano: per vedere il gioco esattamente come uno studente, usa un
account che non sia operatore.

## Lavagne

Per il testo grande nel mondo vedi [Lavagne](boards.it.md).
