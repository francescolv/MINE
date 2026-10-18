[English](README.md) · **Italiano**

# Server di MINE

Il server della classe per MINE: Minecraft Java con Fabric, CC: Tweaked e MINE, preparato per un
laboratorio scolastico. Questa cartella lo installa su un PC **Linux** (Fedora, Ubuntu, Debian…)
come servizio che parte con il PC, con una console e i backup ogni notte.

La guida completa, con i PC del laboratorio, il codice classe e il laboratorio:
https://github.com/francescolv/MINE/blob/main/docs/guide/server.it.md

## Cosa c'è in questa cartella

| | |
|---|---|
| `install.sh` | l'installatore (rilancialo per aggiornare) |
| `mine-*.jar` | MINE |
| `mods.list` | le altre mod (Fabric API, CC: Tweaked): l'installatore le scarica e controlla che siano esattamente questi file |
| `server.properties.defaults` | le impostazioni per una classe, scritte la prima volta |
| `mine-cmd`, `mine-backup`, `systemd/` | la console, i backup e il servizio |

## 1. Cosa serve

- Un PC Linux acceso durante le lezioni. Per circa 25 studenti: 4 core, 8 GB di RAM, 10 GB di disco.
- **Java 25** o più recente:

  ```bash
  sudo dnf install java-25-openjdk-headless      # Fedora
  sudo apt install openjdk-25-jre-headless       # Ubuntu, Debian
  ```

- `curl` e una connessione a Internet (per Fabric e le mod).
- Il tuo **nome in Minecraft Java**: diventi l'insegnante.

## 2. Installa

In questa cartella:

```bash
sudo ./install.sh --teacher IlTuoNomeMinecraft
```

Rispondi `y` quando chiede di accettare la [EULA di Minecraft](https://aka.ms/MinecraftEULA).
L'installatore mette il server in `/opt/mine-server`, lo avvia, apre le porte nel firewall
(firewalld o ufw) e ti rende operatore. Alla fine scrive «MINE server installed … and running».

Opzioni (`./install.sh --help`):

| | |
|---|---|
| `--teacher NOME` | un insegnante: operatore e nella whitelist (ripetilo per più insegnanti) |
| `--memory 6G` | memoria di Java (predefinita `4G`, circa 25 studenti) |
| `--port 25566` | porta del server (predefinita `25565`) |
| `--dir /srv/mine` | dove va il server (predefinita `/opt/mine-server`) |
| `--accept-eula` | accetti la EULA senza che te lo chieda (per gli script) |
| `--no-service` | solo i file, senza root: lo avvii a mano con `java -Xmx4G -jar fabric-server-launch.jar nogui` |

## 3. Configura

Le impostazioni sono in `/opt/mine-server/server.properties`; dopo una modifica:
`sudo systemctl restart mine-server`. L'installatore le scrive solo la prima volta, quindi un
aggiornamento tiene le tue. All'inizio sono:

- un **mondo piatto in Creativa**, pacifico, senza mostri né PvP; MINE lo tiene a mezzogiorno con
  il cielo sereno (`/gamerule advance_time true` per cambiarlo);
- **whitelist attiva**: entra solo la classe;
- `online-mode=true`: servono veri account Minecraft. Non disattivarlo mai con una classe vera:
  chiunque potrebbe entrare con il nome dell'insegnante;
- blocchi comandi attivi (li piazzano e li scrivono solo gli insegnanti).

Porte, aperte dall'installatore:

| | |
|---|---|
| 25565/tcp | il gioco |
| 25580/tcp | l'editor a blocchi, aperto nel browser degli studenti |
| 4445/udp | l'annuncio in LAN: il server compare nella lista *Multigiocatore* |

Altri insegnanti: `sudo mine-cmd op IlLoroNome` e `sudo mine-cmd whitelist add IlLoroNome`.

## 4. Gli studenti

Sui PC del laboratorio: [Prism Launcher](https://prismlauncher.org) e il modpack `MINE.mrpack`
della **stessa versione** di questo server
(https://github.com/francescolv/MINE/releases/latest/download/MINE.mrpack).

Nel gioco scrivi `/mine class code`: mostra i quattro simboli al proiettore. Gli studenti entrano
nel server, cliccano i simboli e sono nella classe per sempre. `/mine lab build 25` costruisce un
laboratorio con una corsia per ognuno di 25 studenti.

## Ogni giorno

| | |
|---|---|
| Sta andando? | `systemctl status mine-server` |
| Cosa sta dicendo? | `journalctl -u mine-server -f` |
| Un comando nella sua console | `sudo mine-cmd whitelist add Alex` |
| Ferma / avvia | `sudo systemctl stop mine-server` / `start` |
| Backup adesso | `sudo mine-backup` (ogni notte alle 2:00 in `/opt/mine-server/backups`, tenuti 14 giorni) |

Copia la cartella `backups` anche su un altro disco: un backup sullo stesso disco non sopravvive al disco.

## Aggiornare

Scompatta il nuovo `MINE-server.zip` e rilancia `sudo ./install.sh` al suo interno: MINE e le mod
vengono sostituiti, il mondo e le impostazioni restano. Poi importa il nuovo `MINE.mrpack` sui PC
del laboratorio: server e PC devono avere la stessa versione.

## Rimuoverlo

```bash
sudo systemctl disable --now mine-server mine-backup.timer
sudo rm /etc/systemd/system/mine-server.* /etc/systemd/system/mine-backup.* /usr/local/bin/mine-cmd /usr/local/bin/mine-backup
sudo systemctl daemon-reload
```

Il mondo resta in `/opt/mine-server`: cancellalo solo quando sei sicuro che non ti serve.
