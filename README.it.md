# MINE — *Mine Is Not Education*

[English](README.md) · **Italiano**

[![build](https://github.com/francescolv/MINE/actions/workflows/build.yml/badge.svg)](https://github.com/francescolv/MINE/actions/workflows/build.yml)
[![License: Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

Mod Fabric per Minecraft Java Edition che porta a scuola le funzionalità didattiche
utili di Minecraft Education — **anche su Linux**, dove Minecraft Education non esiste.

🌐 Pagina del progetto: **https://francescolv.github.io/MINE/it/**

📦 Per le scuole: [server](https://github.com/francescolv/MINE/releases/latest/download/MINE-server.zip) · [modpack per i PC del laboratorio](https://github.com/francescolv/MINE/releases/latest/download/MINE.mrpack) · [guida all'installazione](docs/guide/server.it.md)

## Perché

Minecraft Education gira su Windows, macOS, iPad e Chromebook, ma non su Linux, e
richiede licenze Microsoft 365 Education. Molte scuole usano (o vorrebbero usare) Linux
nei laboratori. MINE parte da Minecraft Java — che su Linux funziona benissimo — e ci
aggiunge gli strumenti per fare lezione, in modo open source e sotto il controllo della scuola.

Cosa serve per giocare: una copia di **Minecraft Java Edition** (acquistabile con un
normale account Microsoft/Mojang). Cosa **non** serve: licenze Microsoft 365, client
Education, tenant scolastici.

## Cosa NON è

Per evitare fraintendimenti / issue fuori scope:

- ❌ Nessun modulo Chemistry (banco chimico, elementi, reazioni)
- ❌ Nessun Classroom Mode (dashboard insegnante esterna, teletrasporto/freeze di massa)
- ❌ Non è un fork di Minecraft Education, non ne usa asset o codice
- ❌ Non è un prodotto ufficiale Minecraft, né approvato o associato a Mojang o Microsoft

## Cosa c'è dentro

| Modulo | Equivalente Education | Cosa ha MINE |
|---|---|---|
| **Code Builder / Agent** | Code Builder, Agent | **Byte**, un robot per ogni studente (una turtle di [CC: Tweaked](https://github.com/cc-tweaked/CC-Tweaked)), programmato in Lua, a [blocchi](docs/guide/blocks.it.md) nel browser o in [Python](docs/guide/python.it.md); [16 lezioni](docs/lessons/README.it.md) con missioni che il server controlla da solo |
| **NPC didattici** | NPC, lavagne | NPC con dialoghi e quiz scritti nel gioco, pagine che seguono la lezione, lavagne con testo grande, anche dai datapack |
| **Portfolio / Camera** | Camera, Portfolio, Book & Quill | fotocamera, finestra del portfolio, gallerie di classe, esportazione in pagine web |
| **Mondi per classe** | World library, lesson worlds | [un mondo per classe](docs/guide/worlds.it.md), piatto o copia di una mappa ([Fantasy](https://github.com/NucleoidMC/fantasy)), regole di classe, un codice di simboli per entrare; [zone](docs/guide/zones.it.md) da rimettere com'erano, o protette |
| **Physical computing** | — | [micro:bit, Arduino e Raspberry Pi Pico](docs/guide/devices.it.md) che parlano con Byte e con il mondo (con il cavo, o il Pico 2 W in Wi-Fi), blocchi che trasformano una scheda in redstone |

Cosa manca ancora, per chi vuole dare una mano: [ROADMAP.md](ROADMAP.md).

## Requisiti

| Software | Versione |
|---|---|
| Minecraft Java | 26.3 |
| Fabric Loader | 0.19.5+ |
| Fabric API | 0.162.0+26.3 |
| CC: Tweaked (consigliato) | 1.120.3 |
| JDK (solo per sviluppare) | 25 |

## Build

Serve un **JDK 25** completo (con `javac`). Su Fedora: `sudo dnf install java-25-openjdk-devel`.

```bash
# Build del mod
./gradlew build

# Ambiente di test (client con MINE + CC: Tweaked caricati)
./gradlew runClient
```

Il file compilato lo trovi in `build/libs/mine-<versione>.jar`. Per le scuole:
`./gradlew modpack serverPack` crea il modpack (`.mrpack`) e il pacchetto per il server in
`build/distributions/` ([guida](docs/guide/server.it.md)).

```bash
# Test automatici in gioco (server headless, gli stessi della CI)
./gradlew runGametest
```

## Comandi

Ogni studente ha **Byte**, un robot da programmare in Lua o a blocchi (Minecraft Education lo
chiama Agent). Un byte è un blocco di 8 bit: Byte lavora con i blocchi del mondo.

| Comando | Cosa fa |
|---|---|
| `/mine byte` | Crea il tuo Byte (una turtle CC: Tweaked) davanti a te. Se ne avevi già uno, lo sostituisce. |
| `/mine byte recall` | Riporta il tuo Byte davanti a te, con programmi e inventario. |
| `/mine blocks` | Apre l'editor a blocchi nel browser: programma il tuo Byte con i blocchi, guarda il Lua che creano, eseguilo ([guida](docs/guide/blocks.it.md)). |
| *Python* | Nell'editor a blocchi, **Python** apre l'editor Python: Python vero (MicroPython) con gli stessi nomi di Lua, eseguito sul tuo Byte; oppure [Thonny](docs/guide/python.it.md#thonny) sul PC ([guida](docs/guide/python.it.md)). |
| `/mine device` | Collega la scheda attaccata al tuo PC (micro:bit, Arduino): parla con il tuo Byte con righe `chiave:valore` ([guida](docs/guide/devices.it.md)). |
| `/mine byte run <programma>` | Avvia un programma del tuo Byte dalla chat, così lo guardi lavorare. Quando finisce (o si ferma per un errore) te lo dice in chat. |
| `/mine lesson list` | Le lezioni con missioni che MINE controlla da solo. |
| `/mine lesson start <lezione>` | Avvia una lezione: MINE ti dà un obiettivo alla volta e lo controlla quando i tuoi programmi finiscono. L'insegnante può indicare gli studenti, es. `/mine lesson start first_steps @a`. |
| `/mine lesson status` | La tua tappa e il suo obiettivo. Insegnante: `/mine lesson status @a` mostra tutta la classe. |
| `/mine lesson hint` | Il prossimo suggerimento per la tua tappa. |
| `/mine lesson restart` | Ricomincia la tua lezione. |
| `/mine lesson reset <studenti>` | Solo insegnante: cancella i progressi degli studenti (non le risposte). |
| `/mine lesson answer <testo>` | Alla fine di una lezione: la tua risposta alla domanda di riflessione. |
| `/mine lesson answers <lezione>` | Solo insegnante: le risposte di tutti alla domanda di quella lezione, anche di chi non è collegato. |
| `/mine builder add\|remove <studenti>` | Solo insegnante: permette agli studenti di costruire con gli strumenti di MINE (NPC, lavagne) senza essere operatori; `/mine builder list` mostra chi. |
| `/mine view student\|teacher` | Solo insegnante: prova MINE come lo vedono gli studenti (NPC solo da leggere, niente comandi da insegnante), poi torna indietro. |
| `/mine npc create <nome>` | Insegnanti e costruttori: un NPC davanti a te. Scrivi il suo dialogo in un libro e penna e cliccalo col tasto destro tenendo il libro ([guida](docs/guide/npc.it.md)). |
| `/mine npc remove` | Insegnanti e costruttori: rimuove l'NPC più vicino (i costruttori: tra i propri). |
| `/mine npc place <id>` | Insegnanti e costruttori: un NPC da un datapack, es. `mine:loops_guide`, la guida della lezione sui cicli. |
| `/mine npc results [nome]` | Insegnanti e costruttori: le risposte ai quiz dell'NPC più vicino (insegnanti: di qualunque NPC, per nome). `/mine npc clear` le cancella. |
| `/mine npc export` | Solo insegnante: tutte le risposte ai quiz in un file CSV, scaricato sul tuo computer. |
| `/mine board create [dimensione]` | Insegnanti e costruttori: una lavagna con testo grande davanti a te; scrivila con un libro e penna ([guida](docs/guide/boards.it.md)). Anche `place <id>`, `size <dimensione>` e `remove`. |
| `/mine lab build <corsie>` | Solo insegnante: il laboratorio standard dove sei, una corsia per studente; `assign` mette gli studenti nelle corsie, `reset` lo libera dopo la lezione ([guida](docs/guide/server.it.md#3-il-laboratorio-standard)). |
| `/mine world` | Insegnanti: un mondo per classe, piatto o copia di una mappa: `create`, `go`, `rules lesson\|free`, `list`, `remove` ([guida](docs/guide/worlds.it.md)). |
| `/mine zone` | Insegnanti: zone segnate con due blocchi angolo, da rimettere com'erano (`restore`) o da proteggere (`protect`) ([guida](docs/guide/zones.it.md)). |
| Blocchi comandi | Attivi sul server di classe, per gli insegnanti: pulsanti che avviano le lezioni, piastre verso altri mondi ([guida](docs/guide/command-blocks.it.md)). |
| `/mine camera [studenti]` | Una fotocamera: clicca col tasto destro per scattare una foto per il tuo portfolio (insegnanti: la danno agli studenti) ([guida](docs/guide/portfolio.it.md)). |
| `/mine portfolio [studente]` | Il tuo portfolio, in una finestra: foto, didascalie, note, condivisione (insegnanti: quello di uno studente). `/mine portfolio export`: pagine web in uno zip. |
| `/mine gallery` | La galleria di classe. Insegnanti: `create\|use\|remove <nome>`; tutti: `open <nome>`, `list`. |

"Insegnante" vuol dire operatore (livello di permesso 2).

Dentro Byte, oltre alle API di CC: Tweaked, c'è l'API `mine`: `mine.say(...)` per
parlare in chat, `mine.challenge()` per quello che chiede la lezione, `mine.device` per la scheda.
Dettagli in
[docs/guide/lua-api.it.md](docs/guide/lua-api.it.md), oppure `help mine` nel terminale di Byte.

## Lezioni

Lezioni pronte per un'ora di laboratorio, con guida per l'insegnante e soluzioni
provate in automatico: [docs/lessons](docs/lessons/README.it.md).

1. [Primi passi con Byte](docs/lessons/01-first-steps/README.it.md): istruzioni e sequenze
2. [I cicli: file, pavimenti e cubi](docs/lessons/02-loops/README.it.md): cicli `for`, area e volume
3. [Debugging: ripara i programmi di Byte](docs/lessons/03-debugging/README.it.md): leggere gli errori, correggere i bug
4. [Le condizioni: Byte decide](docs/lessons/04-conditions/README.it.md): `if`, `while` e i sensori di Byte
5. [Variabili e funzioni](docs/lessons/05-functions/README.it.md): contare, valori da fuori, una piramide con una funzione
6. [Redstone](docs/lessons/06-redstone/README.it.md): una lampada, una porta con la leva, una porta logica E
7. [Sensori](docs/lessons/07-sensors/README.it.md): aspettare, timer, memoria
8. [La scheda](docs/lessons/08-board/README.it.md): un micro:bit o un Arduino e Byte
9. [Python: i dati della scheda, in grafico](docs/lessons/09-charts/README.it.md) (14–18 anni)
10. [Il labirinto](docs/lessons/10-maze/README.it.md): un algoritmo, la mano sul muro
11. [Caccia al tesoro](docs/lessons/11-treasure/README.it.md): coordinate e variabili
12. [Python: la serra connessa](docs/lessons/12-greenhouse/README.it.md) (Pico 2 W, 14–18 anni)
13. [Python: il controllo automatico](docs/lessons/13-control/README.it.md) (micro:bit, 14–18 anni)
14. [Python: il passaggio a livello](docs/lessons/14-crossing/README.it.md) (Arduino, 14–18 anni)
15. [La fattoria](docs/lessons/15-farm/README.it.md): cicli annidati e condizioni
16. [Messaggi segreti](docs/lessons/16-cipher/README.it.md): il cifrario di Cesare

Gli insegnanti possono scrivere lezioni con missioni come file JSON in un datapack:
[docs/guide/lesson-format.it.md](docs/guide/lesson-format.it.md).

## Usare MINE in laboratorio

**Installare**: il server della scuola (un comando: servizio, whitelist, backup notturni) e il
modpack di MINE per i PC del laboratorio (Prism Launcher): [docs/guide/server.it.md](docs/guide/server.it.md).

Consigli pratici per aule Linux (touchpad, configurazione Byte...):
[docs/guide/linux-lab.it.md](docs/guide/linux-lab.it.md).

## Contribuire

Il codice e la documentazione tecnica sono in inglese; le guide per insegnanti e studenti
sono anche in italiano. Cerchiamo sviluppatori Java/Fabric, programmatori Lua e Python,
**insegnanti** che scrivano lezioni e traduttori. Cosa resta da fare è in [ROADMAP.md](ROADMAP.md);
come cominciare, in [CONTRIBUTING.md](CONTRIBUTING.md).

## Licenza

Apache License 2.0 — vedi [LICENSE](LICENSE).

Il jar di MINE contiene [Fantasy](https://github.com/NucleoidMC/fantasy) (LGPL-3.0), non modificata,
come libreria separata.

"Minecraft" è un marchio di Mojang Synergies AB. MINE non è affiliato a Mojang né a Microsoft.
