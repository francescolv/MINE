[English](server.md) · **Italiano**

# Installare MINE: il server della scuola e i PC del laboratorio

MINE ha due parti:

- un **server**, uno per la classe: un PC Linux (o una macchina virtuale) che tiene il mondo, le
  lezioni, i portfolio;
- il **modpack di MINE** su ogni PC del laboratorio: Minecraft Java con Fabric, CC: Tweaked e MINE.

## Scarica

| | |
|---|---|
| Il server | [**MINE-server.zip**](https://github.com/francescolv/MINE/releases/latest/download/MINE-server.zip) |
| I PC del laboratorio | [**MINE.mrpack**](https://github.com/francescolv/MINE/releases/latest/download/MINE.mrpack) |

I link danno sempre l'ultima versione. Le versioni precedenti e le novità di ognuna sono tra le
[Releases](https://github.com/francescolv/MINE/releases). Chi sviluppa può costruirli da questo
repository: `./gradlew modpack serverPack`.

## Cosa serve

- Un **PC Linux per il server**, acceso durante le lezioni. Per una classe di circa 25 studenti:
  4 core, 8 GB di RAM (4 per Minecraft), 10 GB di disco. Vanno bene Fedora, Ubuntu, Debian.
- **Java 25** o più recente sul server: `sudo dnf install java-25-openjdk-headless` (Fedora) o
  `sudo apt install openjdk-25-jre-headless` (Ubuntu 22.04 e successive, Debian 13).
- **Un account Minecraft Java per ogni studente** (e per l'insegnante). MINE non richiede
  Microsoft 365 né Minecraft Education, ma è Minecraft Java: ogni giocatore ha bisogno di una licenza.
- I PC del laboratorio e il server **sulla stessa rete**.

## 1. Il server

Sul server, in un terminale:

```bash
curl -LO https://github.com/francescolv/MINE/releases/latest/download/MINE-server.zip
unzip MINE-server.zip
cd MINE-server
sudo ./install.sh --teacher IlTuoNomeMinecraft
```

(Oppure scarica [MINE-server.zip](https://github.com/francescolv/MINE/releases/latest/download/MINE-server.zip)
con il browser e parti da `unzip`.) Al posto di `IlTuoNomeMinecraft` scrivi il tuo nome in
Minecraft; quando l'installatore chiede di accettare l'EULA, rispondi `y`. Il `README.it.md` nella
cartella riassume installazione e configurazione, per quando questa guida non è a portata di mano.

L'installatore:

- scarica il server di Fabric e le mod con cui MINE è testato, controllando che siano esattamente
  i file attesi;
- scrive **impostazioni per una classe**: mondo piatto in Creativa, niente mostri, pacifico, e la
  **whitelist** attiva (entrano solo i tuoi studenti); poi MINE lo tiene a mezzogiorno con il cielo
  sereno (per cambiarlo: `/gamerule advance_time true`, `/gamerule advance_weather true`);
- ti chiede di accettare l'[EULA di Minecraft](https://aka.ms/MinecraftEULA): senza, il server
  non può partire;
- installa il server come **servizio** (`mine-server`): parte con il PC e si riavvia da solo se
  si blocca;
- apre la porta (25565) nel firewall (firewalld o ufw), la 25580 per
  l'[editor a blocchi](blocks.it.md) e la 4445/udp perché anche un gioco sul PC del server veda il
  server nella sua lista;
- ti rende (`--teacher`) **operatore**, cioè insegnante per MINE. Nel gioco un insegnante ha
  **[Insegnante]** in oro prima del nome, che vedono anche gli studenti, ed entrando legge in chat i
  comandi per guidare la classe.

Altre opzioni: `--memory 6G` per più studenti, `--port 25566`, `--dir /srv/mine` (predefinita
`/opt/mine-server`). Vedi `./install.sh --help`.

### Aggiungere gli studenti

Entrano solo gli studenti della classe (la **whitelist** del server). Il modo più semplice per
aggiungerli è il **codice classe**, come in Minecraft Education: nel gioco scrivi

```
/mine class code
```

Una finestra mostra quattro simboli (es. *Torta · Carota · Lingotto d'oro · Smeraldo*): mostrala
al proiettore. Uno studente che non è ancora nella classe si collega al server, MINE gli chiede il
codice, clicca i quattro simboli nell'ordine ed è dentro: nella whitelist per sempre. Se c'è un
[laboratorio](#3-il-laboratorio-standard) riceve una corsia, e arriva all'ingresso, davanti ad Ada.
La volta dopo entra subito.

- Il codice resta aperto **2 ore**; `/mine class code` lo mostra di nuovo, `/mine class code new`
  ne crea uno nuovo, `/mine class close` lo chiude subito (entra solo chi è già nella classe).
- Dopo 10 codici sbagliati lo studente deve essere fatto entrare da te.
- Quando uno studente che non è nella classe prova a entrare, ricevi un messaggio in chat con il
  pulsante **[Fai entrare]**. `/mine class waiting` elenca chi ha provato, `/mine class admit Alex`
  ne fa entrare uno (si ricollega ed è dentro).
- Con i [mondi di classe](worlds.it.md), ogni mondo ha il suo codice: il codice che lo studente
  inserisce decide dove arriva. `/mine class code` è il codice del mondo in cui sei.

Non serve conoscere prima i nomi Minecraft degli studenti: con il codice, o con *[Fai entrare]*, i
nomi arrivano da soli. Per aggiungere qualcuno a mano:

```bash
sudo mine-cmd whitelist add Alex
```

`mine-cmd` manda qualunque comando al server, come se lo scrivessi nella sua console, e mostra la
risposta: `sudo mine-cmd mine class code`, `sudo mine-cmd say La lezione inizia tra 5 minuti`,
`sudo mine-cmd op AltroInsegnante`. Dentro il gioco gli insegnanti possono usare anche
`/whitelist add <nome>`.

### Tutti i giorni

| | |
|---|---|
| È acceso? | `systemctl status mine-server` |
| Cosa sta dicendo? | `journalctl -u mine-server -f` |
| Fermare / avviare | `sudo systemctl stop mine-server` / `start` |
| Impostazioni | `/opt/mine-server/server.properties`, poi `sudo systemctl restart mine-server` |

### Backup

Ogni notte alle 2:00 il mondo viene copiato in `/opt/mine-server/backups` (tenuti 14 giorni). Il
mondo contiene anche i progressi delle lezioni, le risposte, gli NPC e le **foto dei portfolio**.
Per fare una copia subito: `sudo mine-backup`.

Per tornare a una copia:

```bash
sudo systemctl stop mine-server
cd /opt/mine-server
sudo mv world world-vecchio
sudo tar -xzf backups/world-2026-10-07_0200.tar.gz
sudo chown -R mine:mine world
sudo systemctl start mine-server
```

Copia la cartella `backups` anche altrove (un altro disco, il NAS della scuola): un backup sullo
stesso disco non sopravvive al disco.

### Aggiornare MINE

Scarica ed estrai di nuovo `MINE-server.zip` (gli stessi comandi del punto 1) e lancia di nuovo
`sudo ./install.sh`: MINE e le mod vengono sostituiti, il mondo e le tue impostazioni restano.
Sui PC del laboratorio importa il nuovo `MINE.mrpack`: server e PC devono avere la stessa versione.
Un PC con una versione diversa non entra: lo studente vede quale modpack MINE importare.

### Toglierlo

```bash
sudo systemctl disable --now mine-server mine-backup.timer
sudo rm /etc/systemd/system/mine-server.* /etc/systemd/system/mine-backup.* /usr/local/bin/mine-cmd /usr/local/bin/mine-backup
sudo systemctl daemon-reload
```

Il mondo resta in `/opt/mine-server`: cancellalo solo quando sei sicuro che non serve più.

### Senza il servizio

Per provare MINE sul tuo PC, senza root: `./install.sh --dir ~/mine-server --no-service`, poi
`cd ~/mine-server && java -Xmx4G -jar fabric-server-launch.jar nogui`. I comandi (es.
`op IlTuoNome`) si scrivono in quella finestra.

### Provare con più studenti, senza licenze

Per provare MINE prima di una lezione (il codice classe, il laboratorio, più studenti insieme)
puoi far entrare giocatori senza licenza Minecraft, **solo su un server di prova**:

1. In `/opt/mine-server/server.properties` metti `online-mode=false`, poi
   `sudo systemctl restart mine-server`.
2. In Prism Launcher aggiungi **account offline** con nomi a piacere (Prism chiede di aver prima
   aggiunto un account Microsoft): ognuno è uno studente finto.
3. Finita la prova, rimetti `online-mode=true` e riavvia.

> **Mai con una classe vera.** Con `online-mode=false` il server non controlla chi sono i
> giocatori: chiunque può entrare con il nome dell'insegnante e avere i suoi poteri. La whitelist e
> il codice classe si basano sul nome, quindi non proteggono da questo. Per questo l'installatore
> mette sempre `online-mode=true`.

## 2. I PC del laboratorio

Consigliamo [Prism Launcher](https://prismlauncher.org), libero e open source, che gestisce gli
account Microsoft e i modpack.

1. Installalo. Su Linux: `flatpak install flathub org.prismlauncher.PrismLauncher` (o dal
   centro software).
2. **Aggiungi istanza → Importa** e incolla il link
   `https://github.com/francescolv/MINE/releases/latest/download/MINE.mrpack` (o scegli il file
   [MINE.mrpack](https://github.com/francescolv/MINE/releases/latest/download/MINE.mrpack) che hai
   scaricato): Prism installa Minecraft 26.3, Fabric, Fabric API, CC: Tweaked e MINE.
3. **Account**: ogni studente accede con il proprio account Microsoft.
4. Avvia l'istanza e apri **Multigiocatore**: il server della scuola compare da solo in fondo
   alla lista, sotto *Ricerca di partite nella rete locale...* (quella riga c'è sempre, non è un
   errore). Doppio clic per entrare, oppure selezionalo e **Aggiungi** per tenerlo nella lista.
5. La prima volta MINE chiede il **codice classe** (vedi [Aggiungere gli studenti](#aggiungere-gli-studenti)).

Se il server non compare (alcune reti bloccano l'annuncio, o lo blocca il firewall del PC: vedi
[Preparare un laboratorio Linux](linux-lab.it.md#il-server-della-scuola-non-compare-in-multigiocatore)), usa
**Aggiungi server** con l'indirizzo del server (es. `192.168.1.20`; sul server `hostname -I` lo
mostra).

Anche la [Modrinth App](https://modrinth.com/app) importa lo stesso file `.mrpack`. Nei laboratori
dove i PC si ripristinano a ogni avvio, prepara l'istanza una volta nell'immagine.

Vedi anche [Preparare un laboratorio Linux](linux-lab.it.md) (touchpad e altri consigli).

## Windows

I **PC del laboratorio possono essere Windows**: Prism Launcher e Modrinth App importano lo stesso
`.mrpack`, e in MINE tutto funziona allo stesso modo (i download vanno nella cartella *Download*
dell'utente).

Il **server è solo Linux, per scelta**: MINE vuole aiutare le scuole a passare i loro PC a Linux,
non a tenere in vita Windows, quindi non c'è un pacchetto server per Windows. Va bene qualsiasi PC
su cui gira Linux, anche vecchio, o una macchina virtuale. Per provare MINE su un PC Windows, apri
un mondo in giocatore singolo con il modpack di MINE (trucchi attivi, per i comandi da insegnante)
e usa *Apri in LAN*: gli studenti si collegano dalla rete della scuola finché il gioco
dell'insegnante è aperto.

## 3. Il laboratorio standard

Le lezioni chiedono studenti uno accanto all'altro, rivolti dalla stessa parte, con spazio
davanti ai loro Byte. MINE costruisce questo laboratorio per te, uguale ogni volta, nel mondo
piatto del server:

```
/mine lab build 25        una corsia per ognuno di 25 studenti
/mine lab assign          ogni studente collegato nella sua corsia
/mine lab reset           dopo la lezione: costruzioni e Byte tolti, il laboratorio come nuovo
```

Mettiti dove deve iniziare il laboratorio, **rivolto come saranno rivolti gli studenti**, e scrivi
`/mine lab build` con il numero di studenti. Davanti a te e alla tua destra compaiono:

- una **corsia** per studente, a 8 blocchi di distanza, con un **quadrato verde** di partenza e
  il **numero** della corsia in fondo; file da dieci corsie;
- una **lavagna** con le regole del laboratorio in fondo a ogni fila, come in classe;
- **Ada**, la guida, all'ingresso: suggerimenti, Byte da richiamare, la fotocamera e il
  portfolio, a un clic.

`/mine lab assign` mette ogni studente collegato (non gli insegnanti) in una corsia, rivolto
nella direzione giusta, e la rende il suo punto di rinascita. Ogni studente tiene la sua corsia
da una lezione all'altra; i nuovi prendono quelle libere. Per assegnare solo alcuni studenti:
`/mine lab assign Alex Sam`.

`/mine lab reset` toglie tutto quello che è stato costruito nel laboratorio, Byte compresi
(tornano quando parte una lezione, o con `/mine byte`): la classe successiva lo trova come nuovo.
Costruire di nuovo il laboratorio lo sostituisce e riassegna le corsie.

## 4. Mondi di classe

Ogni classe può avere il suo mondo, piatto o copia di una mappa scaricata, creato in pochi secondi:
`/mine world create 2b flat`, poi `/mine world go 2b @a`. Vedi [Mondi di classe](worlds.it.md).

## 5. Blocchi comandi

Sono attivi: gli insegnanti ci costruiscono mondi interattivi (un pulsante che avvia la lezione, una
piastra che porta gli studenti in un altro mondo). Vedi [Blocchi comandi](command-blocks.it.md).
