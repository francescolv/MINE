**English** · [Italiano](README.it.md)

# MINE server

The class server for MINE: Minecraft Java with Fabric, CC: Tweaked and MINE, set up for a
school lab. This folder installs it on a **Linux** PC (Fedora, Ubuntu, Debian…) as a service
that starts with the PC, with a console and nightly backups.

The full guide, with the lab PCs, the class code and the lab:
https://github.com/francescolv/MINE/blob/main/docs/guide/server.md

## What's in this folder

| | |
|---|---|
| `install.sh` | the installer (run it again to update) |
| `mine-*.jar` | MINE itself |
| `mods.list` | the other mods (Fabric API, CC: Tweaked): the installer downloads them and checks they are exactly these files |
| `server.properties.defaults` | the settings for a class, written the first time |
| `mine-cmd`, `mine-backup`, `systemd/` | the console, the backups and the service |

## 1. What you need

- A Linux PC always on during lessons. For about 25 students: 4 cores, 8 GB of RAM, 10 GB of disk.
- **Java 25** or newer:

  ```bash
  sudo dnf install java-25-openjdk-headless      # Fedora
  sudo apt install openjdk-25-jre-headless       # Ubuntu, Debian
  ```

- `curl` and an Internet connection (for Fabric and the mods).
- Your **Minecraft Java name**: you become the teacher.

## 2. Install

In this folder:

```bash
sudo ./install.sh --teacher YourMinecraftName
```

Answer `y` when it asks you to accept the [Minecraft EULA](https://aka.ms/MinecraftEULA). The
installer puts the server in `/opt/mine-server`, starts it, opens the firewall ports (firewalld
or ufw) and makes you an operator. At the end it says "MINE server installed … and running".

Options (`./install.sh --help`):

| | |
|---|---|
| `--teacher NAME` | a teacher: operator and on the whitelist (repeat it for more teachers) |
| `--memory 6G` | Java memory (default `4G`, about 25 students) |
| `--port 25566` | server port (default `25565`) |
| `--dir /srv/mine` | where the server goes (default `/opt/mine-server`) |
| `--accept-eula` | you accept the EULA without being asked (for scripts) |
| `--no-service` | only the files, no root: start it by hand with `java -Xmx4G -jar fabric-server-launch.jar nogui` |

## 3. Configure

The settings are in `/opt/mine-server/server.properties`; after a change:
`sudo systemctl restart mine-server`. The installer writes them only the first time, so an
update keeps yours. They start as:

- a **flat world in Creative**, peaceful, no monsters, no PvP; MINE keeps it at midday with a
  clear sky (`/gamerule advance_time true` to change it);
- **whitelist on**: only the class joins;
- `online-mode=true`: players need a real Minecraft account. Never turn it off with a real class:
  anyone could join with the teacher's name;
- command blocks on (only teachers place and write them).

Ports, opened by the installer:

| | |
|---|---|
| 25565/tcp | the game |
| 25580/tcp | the block editor, opened in the students' browsers |
| 4445/udp | the LAN announcement: the server appears in the *Multiplayer* list |

Other teachers: `sudo mine-cmd op TheirName` and `sudo mine-cmd whitelist add TheirName`.

## 4. The students

On the lab PCs: [Prism Launcher](https://prismlauncher.org) and the `MINE.mrpack` modpack of
**the same version** as this server
(https://github.com/francescolv/MINE/releases/latest/download/MINE.mrpack).

In the game, type `/mine class code`: show the four symbols on the projector. Students join the
server, click the symbols and they are in the class for good. `/mine lab build 25` builds a lab
with a lane for each of 25 students.

## Every day

| | |
|---|---|
| Is it running? | `systemctl status mine-server` |
| What is it saying? | `journalctl -u mine-server -f` |
| A command in its console | `sudo mine-cmd whitelist add Alex` |
| Stop / start | `sudo systemctl stop mine-server` / `start` |
| Backup now | `sudo mine-backup` (every night at 2:00 into `/opt/mine-server/backups`, kept 14 days) |

Copy the `backups` folder to another disk too: a backup on the same disk doesn't survive the disk.

## Updating

Unzip the new `MINE-server.zip` and run `sudo ./install.sh` again in it: MINE and the mods are
replaced, the world and the settings are kept. Then import the new `MINE.mrpack` on the lab PCs:
the server and the PCs need the same version.

## Removing it

```bash
sudo systemctl disable --now mine-server mine-backup.timer
sudo rm /etc/systemd/system/mine-server.* /etc/systemd/system/mine-backup.* /usr/local/bin/mine-cmd /usr/local/bin/mine-backup
sudo systemctl daemon-reload
```

The world stays in `/opt/mine-server`: delete it only when you're sure you don't need it.
