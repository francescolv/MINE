**English** · [Italiano](server.it.md)

# Installing MINE: the school server and the lab PCs

MINE has two parts:

- a **server**, one for the class: a Linux PC (or virtual machine) that keeps the world, the
  lessons, the portfolios;
- the **MINE modpack** on every PC in the lab: Minecraft Java with Fabric, CC: Tweaked and MINE.

## Download

| | |
|---|---|
| The server | [**MINE-server.zip**](https://github.com/francescolv/MINE/releases/latest/download/MINE-server.zip) |
| The lab PCs | [**MINE.mrpack**](https://github.com/francescolv/MINE/releases/latest/download/MINE.mrpack) |

The links always give the latest version. Earlier versions and what's new in each are in the
[Releases](https://github.com/francescolv/MINE/releases). Developers can build them from this
repository: `./gradlew modpack serverPack`.

## What you need

- A **Linux PC for the server**, always on during lessons. For a class of about 25 students:
  4 cores, 8 GB of RAM (4 for Minecraft), 10 GB of disk. Fedora, Ubuntu, Debian are fine.
- **Java 25** or newer on the server: `sudo dnf install java-25-openjdk-headless` (Fedora) or
  `sudo apt install openjdk-25-jre-headless` (Ubuntu 22.04 and newer, Debian 13).
- **A Minecraft Java account for every student** (and the teacher). MINE doesn't need Microsoft
  365 or Minecraft Education, but it is Minecraft Java: each player needs a licence.
- The lab PCs and the server **on the same network**.

## 1. The server

On the server, in a terminal:

```bash
curl -LO https://github.com/francescolv/MINE/releases/latest/download/MINE-server.zip
unzip MINE-server.zip
cd MINE-server
sudo ./install.sh --teacher YourMinecraftName
```

(Or download [MINE-server.zip](https://github.com/francescolv/MINE/releases/latest/download/MINE-server.zip)
with the browser and start from `unzip`.) Replace `YourMinecraftName` with your name in
Minecraft; when the installer asks you to accept the EULA, answer `y`. The `README.md` in the
folder sums up installing and configuring, for when this guide isn't at hand.

The installer:

- downloads Fabric's server and the mods MINE is tested with, checking that they are exactly the
  expected files;
- writes **settings for a class**: a flat world in Creative, no monsters, peaceful, and the
  **whitelist** on (only your students can join); MINE then keeps it at midday with a clear sky
  (to change it: `/gamerule advance_time true`, `/gamerule advance_weather true`);
- asks you to accept the [Minecraft EULA](https://aka.ms/MinecraftEULA): without it the server
  can't start;
- installs the server as a **service** (`mine-server`): it starts with the PC and restarts by
  itself if it crashes;
- opens the port (25565) in the firewall (firewalld or ufw), 4445/udp so a game on the server
  PC too sees the server in its list, and 25580 for the [block editor](blocks.md);
- makes you (`--teacher`) an **operator**, i.e. a teacher for MINE. In the game a teacher has a gold
  **[Teacher]** before their name, which students see too, and on joining reads in chat the
  commands that run the class.

Other options: `--memory 6G` for more students, `--port 25566`, `--dir /srv/mine` (default
`/opt/mine-server`). See `./install.sh --help`.

### Adding the students

Only students in the class (the server's **whitelist**) can join. The simplest way to add them is
the **class code**, as in Minecraft Education: in the game, type

```
/mine class code
```

A window shows four symbols (e.g. *Cake · Carrot · Gold Ingot · Emerald*): show it on the
projector. A student who isn't in the class yet joins the server, MINE asks for the code, they
click the four symbols in order and they are in: on the whitelist for good. If there is a
[lab](#3-the-standard-lab) they get a lane, and arrive at its entrance, in front of Ada. Next time
they join straight away.

- The code stays open **2 hours**; `/mine class code` shows it again, `/mine class code new`
  makes a new one, `/mine class close` closes it now (only students already in the class can join).
- After 10 wrong codes a student must be let in by you.
- When a student who isn't in the class tries to join, you get a message in the chat with a
  **[Let in]** button. `/mine class waiting` lists who tried, `/mine class admit Alex` lets one in
  (they join again and are in).
- With [class worlds](worlds.md), each world has its own code: the code a student types decides
  where they arrive. `/mine class code` is the code of the world you're in.

You don't need to know the students' Minecraft names in advance: with the code, or with
*[Let in]*, the names come by themselves. To add someone by hand:

```bash
sudo mine-cmd whitelist add Alex
```

`mine-cmd` sends any command to the server, as if you typed it in its console, and shows the
answer: `sudo mine-cmd mine class code`, `sudo mine-cmd say The lesson starts in 5 minutes`,
`sudo mine-cmd op OtherTeacher`. From inside the game, teachers can also use `/whitelist add <name>`.

### Every day

| | |
|---|---|
| Is it running? | `systemctl status mine-server` |
| What is it saying? | `journalctl -u mine-server -f` |
| Stop / start | `sudo systemctl stop mine-server` / `start` |
| Settings | `/opt/mine-server/server.properties`, then `sudo systemctl restart mine-server` |

### Backups

Every night at 2:00 the world is copied into `/opt/mine-server/backups` (kept 14 days). The world
also holds the lessons' progress, the answers, the NPCs and the **portfolios' photos**. To make a
copy now: `sudo mine-backup`.

To go back to a copy:

```bash
sudo systemctl stop mine-server
cd /opt/mine-server
sudo mv world world-old
sudo tar -xzf backups/world-2026-10-07_0200.tar.gz
sudo chown -R mine:mine world
sudo systemctl start mine-server
```

Copy the `backups` folder somewhere else too (another disk, the school's NAS): a backup on the
same disk doesn't survive the disk.

### Updating MINE

Download and unzip `MINE-server.zip` again (the same commands as in step 1) and run
`sudo ./install.sh` again: MINE and the mods are replaced, the world and your settings are kept.
On the lab PCs import the new `MINE.mrpack`: the server and the PCs need the same version. A PC
with a different one can't join: the student sees which MINE modpack to import.

### Removing it

```bash
sudo systemctl disable --now mine-server mine-backup.timer
sudo rm /etc/systemd/system/mine-server.* /etc/systemd/system/mine-backup.* /usr/local/bin/mine-cmd /usr/local/bin/mine-backup
sudo systemctl daemon-reload
```

The world stays in `/opt/mine-server`: delete it only when you are sure you don't need it.

### Without the service

To try MINE on your own PC, without root: `./install.sh --dir ~/mine-server --no-service`, then
`cd ~/mine-server && java -Xmx4G -jar fabric-server-launch.jar nogui`. Type commands (e.g.
`op YourName`) in that window.

### Trying it with several students, without licences

To test MINE before a lesson (the class code, the lab, several students at once) you can let in
players without a Minecraft licence, **only on a test server**:

1. In `/opt/mine-server/server.properties` set `online-mode=false`, then
   `sudo systemctl restart mine-server`.
2. In Prism Launcher add **offline accounts** with any names (Prism asks for a Microsoft account
   added first): each one is a pretend student.
3. When you are done, set `online-mode=true` again and restart.

> **Never with a real class.** With `online-mode=false` the server doesn't check who players
> are: anyone can join with the teacher's name and get the teacher's powers. The whitelist and the
> class code go by name, so they don't protect against this. That's why the installer always sets
> `online-mode=true`.

## 2. The lab PCs

We suggest [Prism Launcher](https://prismlauncher.org), free and open source, which handles
Microsoft accounts and modpacks.

1. Install it. On Linux: `flatpak install flathub org.prismlauncher.PrismLauncher` (or from the
   software centre).
2. **Add Instance → Import** and paste the link
   `https://github.com/francescolv/MINE/releases/latest/download/MINE.mrpack` (or choose the
   [MINE.mrpack](https://github.com/francescolv/MINE/releases/latest/download/MINE.mrpack) file
   you downloaded): Prism installs Minecraft 26.3, Fabric, Fabric API, CC: Tweaked and MINE.
3. **Accounts**: each student signs in with their Microsoft account.
4. Start the instance and open **Multiplayer**: the school server appears by itself at the bottom
   of the list, under *Scanning for games on your local network* (that line is always there, it
   isn't an error). Double-click it, or select it and **Add** it to the list to keep it.
5. The first time, MINE asks for the **class code** (see [Adding the students](#adding-the-students)).

If the server doesn't appear (some networks block the announcement, or the PC's firewall does:
see [Setting up a Linux lab](linux-lab.md#the-school-server-doesnt-appear-in-multiplayer)),
use **Add Server** with the server's address (e.g. `192.168.1.20`; `hostname -I` on the server
shows it).

The [Modrinth App](https://modrinth.com/app) imports the same `.mrpack` file. In labs where PCs
are restored at every start, prepare the instance once in the image.

See also [Setting up a Linux lab](linux-lab.md) (touchpads and other tips).

## Windows

The **lab PCs can be Windows**: Prism Launcher and the Modrinth App import the same `.mrpack`,
and everything in MINE works the same (downloads go to the user's *Downloads* folder).

The **server is Linux only, by choice**: MINE wants to help schools move their PCs to Linux, not
keep Windows going, so there is no Windows server package. Any PC that can run Linux will do,
even an old one, or a virtual machine. To just try MINE on a Windows PC, open a single-player
world with the MINE modpack (cheats on, for the teacher's commands) and use *Open to LAN*: students
join from the school network while the teacher's game is open.

## 3. The standard lab

The lessons need students side by side, facing the same way, with room in front of their Bytes.
MINE builds this lab for you, the same every time, in the flat world of the server:

```
/mine lab build 25        a lane for each of 25 students
/mine lab assign          every student online on their lane
/mine lab reset           after the lesson: builds and Bytes removed, the lab as new
```

Stand where the lab should start, **facing the way students will face**, and type
`/mine lab build` with the number of students. In front of you and to your right appear:

- one **lane** per student, 8 blocks apart, with a green **start square** and the lane's
  **number** at its end; rows of ten lanes;
- a **board** with the lab's rules at the end of each row, like a classroom blackboard;
- **Ada**, the guide, at the entrance: hints, Byte back, the camera and the portfolio, one
  click away.

`/mine lab assign` puts every student online (not the teachers) on a lane, facing the right way,
and makes it their spawn point. Each student keeps their lane from one lesson to the next; new
students get the free ones. To assign some students only: `/mine lab assign Alex Sam`.

`/mine lab reset` clears everything built in the lab, Bytes included (they come back when a
lesson starts, or with `/mine byte`): the next class finds it as new. Building the lab again
replaces it and gives the lanes again.

## 4. Class worlds

Each class can have its own world, flat or a copy of a downloaded map, made in a few seconds:
`/mine world create 2b flat`, then `/mine world go 2b @a`. See [Class worlds](worlds.md).

## 5. Command blocks

They are on: teachers build interactive worlds with them (a button that starts the lesson, a plate
that takes students to another world). See [Command blocks](command-blocks.md).
