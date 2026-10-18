[English](lesson-format.md) · **Italiano**

# Scrivere una lezione con missioni

Le lezioni di MINE sono **file di dati**, non codice: una lezione è un file JSON in un datapack,
quindi un insegnante può scriverne una senza toccare Java e provarla subito sul proprio server.
Le lezioni di MINE funzionano esattamente allo stesso modo: puoi leggerle in
[`src/main/resources/data/mine/mine_lesson/`](../../src/main/resources/data/mine/mine_lesson/).

## Dove va il file

Un datapack è una cartella dentro la cartella `datapacks` del mondo:

```
world/datapacks/la_mia_scuola/
├── pack.mcmeta
└── data/
    └── la_mia_scuola/
        └── mine_lesson/
            └── ponti.json
```

`pack.mcmeta` è uguale per ogni datapack su Minecraft 26.3:

```json
{
	"pack": {
		"description": "Le lezioni MINE della nostra scuola",
		"min_format": 121,
		"max_format": 121
	}
}
```

Il nome del file è l'id della lezione, quello che si scrive in `/mine lesson start ponti`: usa
solo lettere minuscole, cifre e `_`. La cartella sotto `data/` (qui `la_mia_scuola`) può avere
qualunque nome.

Dopo aver salvato il file, scrivi `/reload` sul server: `/mine lesson list` mostra la nuova
lezione. Se nel file c'è un errore, la lezione viene saltata e il **log del server** spiega
perché, in una riga che comincia con `[MINE] Skipping lesson la_mia_scuola:ponti` — per esempio,
manca un campo obbligatorio.

## Cosa contiene una lezione

```json
{
	"format": 1,
	"order": 10,
	"title": "Ponti",
	"reflection": "Quale parte del programma cambieresti per costruire un ponte più lungo?",
	"steps": [
		{
			"id": "deck",
			"goal": "Costruisci l'impalcato di un ponte lungo 8 blocchi e largo 2.",
			"not_yet": "Non ancora: l'impalcato deve essere lungo 8 blocchi e largo 2.",
			"hints": [
				"È un pavimento come nella lezione 2: 2 file da 8 blocchi.",
				"Metti almeno 16 blocchi nello slot evidenziato di Byte."
			],
			"check": { "type": "build", "length": 8, "width": 2 }
		}
	]
}
```

I nomi dei campi restano in inglese; i testi possono essere nella lingua della tua classe.

| Campo | Obbligatorio | Cos'è |
|---|---|---|
| `format` | no | il formato del file, oggi `1` (anche se manca). Un MINE più nuovo lo legge ancora; un file fatto per un MINE più nuovo del tuo viene saltato, con il motivo nel log del server |
| `order` | no | dove compare la lezione in `/mine lesson list` (le lezioni di MINE usano 1, 2, 3…; se manca vale 1000) |
| `title` | sì | il nome della lezione, mostrato quando inizia |
| `reflection` | sì | la domanda mostrata quando lo studente completa la lezione |
| `steps` | sì | le tappe, in ordine (da 1 a 32) |

Ogni tappa:

| Campo | Obbligatorio | Cos'è |
|---|---|---|
| `id` | sì | un nome breve, usato nel log del server e nelle traduzioni |
| `goal` | sì | cosa deve fare lo studente |
| `not_yet` | sì | cosa dice MINE quando un programma finisce ma la tappa non è superata |
| `hints` | no | suggerimenti mostrati uno alla volta con `/mine lesson hint` |
| `block_goal` | no | l'obiettivo come lo mostra l'[editor a blocchi](blocks.it.md), quando `goal` parla di Lua (per esempio "usando un ciclo for") |
| `block_hints` | no | i suggerimenti con le parole dei blocchi, per il pulsante **Suggerimento** dell'editor a blocchi. Mettine tanti quanti `hints`: i suggerimenti si contano insieme, quindi il suggerimento 2 è il secondo in tutte e due le versioni |
| `python_goal` | no | l'obiettivo come lo mostra l'[editor Python](python.it.md), quando `goal` parla di Lua |
| `python_hints` | no | i suggerimenti con le parole di Python, per il pulsante **Suggerimento** dell'editor Python; tanti quanti `hints` |
| `check` | sì | come MINE decide che la tappa è superata: vedi sotto |
| `give` | no | oggetti che MINE mette nello slot evidenziato di Byte quando la tappa inizia: `{"item": "minecraft:oak_planks", "count": 64}`. Se c'è anche `"max_count"`, ogni studente ne riceve un numero casuale tra `count` e `max_count` |

## Controlli

MINE non legge mai il codice dello studente: quando un suo programma finisce senza errori, guarda
cosa ha fatto davvero Byte. Un controllo è un `type` più i suoi parametri.

| Tipo | Parametri | Superato quando… |
|---|---|---|
| `said` | — | il programma ha fatto parlare Byte con `mine.say` |
| `roundtrip` | `distance` | Byte si è allontanato di almeno `distance` blocchi ed è tornato al punto di partenza, rivolto come prima |
| `tower` | `height` | c'è una colonna di `height` blocchi proprio davanti al punto di partenza di Byte, e Byte è tornato all'altezza di partenza |
| `build` | `length`, `width` (se manca 1), `height` (se manca 1) | il programma ha costruito un parallelepipedo pieno di blocchi nuovi vicino alla partenza: una fila, un pavimento, un muro, un cubo… `length` e `width` sono orizzontali, in qualunque direzione |
| `over_the_wall` | `min_distance`, `max_distance` | *Ogni volta che parte un programma, MINE costruisce un muro di mattoni sulla strada di Byte, a una distanza casuale.* Byte l'ha scavalcato ed è atterrato subito dietro, all'altezza di partenza, senza romperlo |
| `gold_row` | `length`, `min_gold`, `max_gold` | *Ogni volta che parte un programma, MINE posa davanti a Byte una fila di pietra, con dell'oro in posti casuali.* Tutto l'oro è stato scavato e tutta la pietra è ancora al suo posto |
| `count_to_wall` | `min_distance`, `max_distance` | *MINE costruisce un muro di mattoni a distanza casuale, come per `over_the_wall`.* Byte si è fermato proprio davanti e ha detto con `mine.say` quanti passi ha fatto: quel numero e nessun altro |
| `square_of_challenge` | `min_side`, `max_side` | ogni volta che parte un programma MINE sceglie un lato, che il programma legge con `mine.challenge()`; superato quando ha costruito un pavimento quadrato pieno di quella misura |
| `pyramid` | `base` | una piramide a gradoni vicino alla partenza: un quadrato pieno `base` × `base` con sopra quadrati di 2 blocchi più piccoli, centrati, fino alla cima |
| `redstone_lamp` | — | *MINE costruisce la scena di redstone intorno a Byte (lampada dietro, porta di ferro davanti, una leva per lato), una volta per tutta la lezione.* Quando il programma finisce la lampada dietro Byte è accesa |
| `lever_door` | — | *Dal vivo, nella stessa scena: mentre il programma gira MINE accende e spegne la leva di sinistra, poi ferma il programma.* La porta ha seguito la leva ogni volta |
| `lever_and` | — | *Dal vivo, nella stessa scena: MINE prova le due leve in tutti e quattro i modi, poi ferma il programma.* La lampada era accesa solo con tutte e due le leve accese |
| `tower_of_all` | `min_blocks` (se manca 3) | davanti al punto di partenza di Byte c'è una torre fatta con **tutti** i blocchi che aveva nello slot evidenziato, e Byte è tornato a terra |
| `plate_door`, `night_lamp`, `button_bridge` | — | *Lezione 7, dal vivo: MINE costruisce la sua parte di stazione intorno a Byte (piastra e porta di ferro, sensore di luce e lampada, pulsante e pistone con passerella), sale sulla piastra, fa scendere la notte, preme il pulsante.* La porta si richiude da sola dopo 3 s; la lampada è accesa solo al buio; ogni pressione sposta la passerella |
| `board_door`, `board_lamp`, `board_heart` | — | *Lezione 8, dal vivo: MINE fa la parte della scheda dello studente (pulsante A, la luce) e tira una leva.* La porta si apre per 3 s; la lampada è accesa quando `luce` è sotto 50; Byte dice alla scheda `cuore:1`/`cuore:0` |
| `chart` | `key`, `count`, `min`, `max`, `height` (facoltativo) | *MINE fa la parte della scheda: `count` numeri nuovi tra `min` e `max` sulla chiave `key`, mentre il programma gira.* Una colonna per numero davanti a dove è partito Byte e alla sua destra, alta come il numero (o `numero * height // max`), e Byte di nuovo dove era partito |
| `chart_max` | `key`, `count`, `min`, `max` | *Come `chart`.* Byte ha detto il numero più grande e la sua posizione, contando da 1 |
| `maze` | `width`, `depth`, facoltativi `corridor`, `say_steps` | *Ogni volta che parte un programma, MINE costruisce davanti a Byte un labirinto di mattoni di pietra (`width` celle alla sua destra, `depth` in avanti; `corridor`: un solo corridoio senza bivi), con un blocco d'oro nel muro dove c'è l'uscita, e lo toglie quando il programma finisce.* Byte è proprio davanti all'oro, non è mai salito e non ha rotto muri; con `say_steps` ha anche detto quanti passi ha fatto |
| `treasure` | `count`, facoltativi `dig`, `home` | *MINE seppellisce `count` blocchi d'oro nel terreno vicino a Byte (1–6 avanti, 0–4 a destra); `mine.challenge()` dà avanti e destra di ognuno; il terreno torna com'era quando il programma finisce.* Senza `dig`, Byte è sopra il tesoro; con `dig`, sono tutti scavati, e con `home` Byte è tornato dove è partito |
| `farm` | `width`, `length`, facoltativi `harvest`, `count` | *MINE scava davanti a Byte un campo di terra arata (`width` file alla sua destra, lunghe `length`), con l'acqua a sinistra; con `harvest` è seminato, con grano maturo in posti a caso.* Senza `harvest`, tutto seminato; con `harvest`, il grano maturo raccolto e riseminato e quello giovane lasciato; con `count`, Byte ha anche detto quanto |
| `cipher` | `shift`, facoltativo `tell_key` | *Ogni volta un messaggio segreto nuovo, cifrato alla Cesare con chiave `shift` (0: a caso); `mine.challenge()` lo dà, e con `tell_key` anche la chiave; senza chiave il messaggio contiene BYTE.* Byte ha detto il messaggio decifrato |
| `greenhouse_average` | `count` | *MINE manda `count` temperature (`temperatura`).* L'ultimo numero detto da Byte è la loro media, a mezzo grado |
| `greenhouse_alarm` | `threshold` | *Dal vivo: MINE manda temperature.* Dopo ognuna la scheda ha ricevuto `allarme:1` solo sopra `threshold`, altrimenti `allarme:0` |
| `greenhouse_report` | `every` | *Dal vivo: MINE manda due gruppi di `every` temperature.* Dopo ogni gruppo la scheda ha ricevuto la loro media come `media` |
| `fan` | `on`, `off`, facoltativo `button` | *Dal vivo, con una lampada (la ventola) dietro Byte: MINE manda temperature e, con `button`, pressioni di `pulsante_a`.* La ventola è accesa da `on`, spenta a `off` o meno, invariata in mezzo; `pulsante_a` accende e spegne un modo manuale in cui è sempre accesa |
| `crossing` | facoltativi `reopen`, `count` | *Dal vivo, con una porta (la sbarra) davanti a Byte, una lampada dietro e una leva a sinistra: MINE manda `treno:1` e muove la leva.* All'inizio aperto e spento, chiuso e acceso dopo un treno; con `reopen` la leva lo riapre; con `count` si riapre quando sono passati tutti i treni, e la scheda riceve `led` |

I controlli in *corsivo* costruiscono una sfida davanti a Byte, solo dove c'è spazio libero
(altrimenti dicono quanto spazio serve). La sfida compare appena la tappa inizia; se un programma
non la supera, MINE la toglie e ne costruisce una nuova quando parte il programma successivo,
così lo studente può riprovare nello stesso punto. Siccome la sfida cambia ogni volta, un
programma passa solo se guarda il mondo invece di contare su numeri fissi.

## Cosa prepara MINE

Una lezione non ha bisogno di niente dalle precedenti. Quando parte, MINE crea Byte a ogni
studente che non ce l'ha (davanti a lui, se c'è spazio). Ogni Byte senza attrezzi riceve un
piccone di diamante, e gli oggetti `give` di ogni tappa arrivano nel suo slot evidenziato: quello
che c'era viene spostato in un altro slot, mai buttato. Gli oggetti si ricevono una volta per
tappa; `/mine lesson restart` li dà di nuovo. Byte riceve tutto questo quando la sua shell
aspetta un comando — subito se è così, altrimenti appena viene acceso.

Alcuni controlli spiegano *perché* una tappa non è ancora superata, invece di ripetere
`not_yet`: `tower` e `build` si accorgono quando Byte ha finito i blocchi, e `build` misura
quello che è stato costruito ("15 blocchi, lunghi 5, larghi 3 e alti 1").

Ti serve un controllo che non esiste ancora? Apri una issue e descrivi l'attività: i nuovi
controlli si aggiungono quando una lezione ne ha bisogno.

## Programmi per gli studenti

Una lezione può dare agli studenti dei programmi, copiati sul loro Byte quando la lezione
inizia — per esempio i programmi rotti della [lezione 3](../lessons/03-debugging/README.it.md).
Mettili accanto alla lezione, una cartella per lingua:

```
data/la_mia_scuola/mine_lesson_files/ponti/en_us/half_bridge.lua
data/la_mia_scuola/mine_lesson_files/ponti/it_it/mezzo_ponte.lua
```

La cartella dopo `mine_lesson_files` è l'id della lezione. Ogni studente riceve i programmi nella
lingua del suo gioco, altrimenti quelli in inglese (`en_us`), altrimenti quelli che ci sono.
I nomi dei file seguono la stessa regola degli id delle lezioni, più `.lua`.

Byte li prende appena è in attesa di un comando (o la prossima volta che viene acceso), e il
terminale li elenca. Vengono copiati **di nuovo ogni volta che la lezione inizia**, sovrascrivendo
i file con lo stesso nome: `/mine lesson restart` dà allo studente una copia nuova.

I programmi fatti con i **blocchi** vanno nelle stesse cartelle, come file `.json` salvati con
**Salva file** dell'editor a blocchi (nomi fino a 24 caratteri). Non vanno su Byte: compaiono
in **I miei programmi** dello studente nell'[editor a blocchi](blocks.it.md), di nuovo ogni volta
che la lezione inizia.

## Lingue

Gli studenti vedono i testi scritti nel file della lezione. Per tradurre una lezione in più
lingue, aggiungi i suoi testi ai file di lingua di un resource pack con queste chiavi
(le lezioni di MINE lo fanno in [`assets/mine/lang/`](../../src/main/resources/assets/mine/lang/)):

```
mine.lesson.<lezione>.title
mine.lesson.<lezione>.reflection
mine.lesson.<lezione>.<tappa>.goal
mine.lesson.<lezione>.<tappa>.not_yet
mine.lesson.<lezione>.<tappa>.hint.1, .hint.2, ...
mine.lesson.<lezione>.<tappa>.block_goal
mine.lesson.<lezione>.<tappa>.block_hint.1, .block_hint.2, ...
mine.lesson.<lezione>.<tappa>.python_goal
mine.lesson.<lezione>.<tappa>.python_hint.1, .python_hint.2, ...
```

Quando una chiave non ha traduzione, si usa il testo del file della lezione. Le righe che MINE
scrive nel terminale di Byte e nell'editor a blocchi sono tradotte sul server, che conosce solo i file di lingua di
MINE: lì le lezioni di altri datapack usano sempre il testo del file della lezione.

## Condividere la tua lezione

Una lezione funziona meglio con una guida per l'insegnante e soluzioni provate, come quelle in
[`docs/lessons`](../lessons/README.it.md). Se vuoi che la tua lezione entri in MINE, leggi
[CONTRIBUTING.md](../../CONTRIBUTING.md) — cerchiamo soprattutto insegnanti.
