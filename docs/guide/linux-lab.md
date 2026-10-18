**English** · [Italiano](linux-lab.it.md)

# Setting up a Linux lab for MINE

Practical notes for teachers and technicians. This page will grow as we try MINE in real
classrooms.

## Touchpad: right-click breaks blocks instead of opening them

On Fedora, Ubuntu and other GNOME-based distributions, the touchpad uses **finger click** by
default: what counts is *how many* fingers press, not *where*. Pressing the bottom-right corner
with one finger is therefore a **left** click, and in Creative mode it immediately breaks the
block (Byte, for example) instead of opening it.

Solutions:

- **Without changing anything:** a **two-finger** click or tap = right button.
- **Right-click in the bottom-right corner, like on Windows:**
  Settings → Mouse & Touchpad → Touchpad → click method "Areas", or from a terminal:

  ```bash
  gsettings set org.gnome.desktop.peripherals.touchpad click-method 'areas'
  ```

  (to go back: `'fingers'`). The setting applies to the current user: in labs with shared
  accounts, set it in the default profile.

- **The simplest:** a USB mouse in the lab.

## The school server doesn't appear in Multiplayer

The MINE server announces itself on the network like an *Open to LAN* world, on port **4445/udp**.
If a PC's firewall blocks that port, the server doesn't appear in its Multiplayer list (joining
with **Add Server** and the address still works). It happens on Fedora with the `public` zone,
e.g. Fedora Server or custom installations; Fedora Workstation lets it through. Check with
`sudo firewall-cmd --get-active-zones`, then open the port for good:

```bash
sudo firewall-cmd --permanent --add-port=4445/udp && sudo firewall-cmd --reload
```

With ufw (off by default on Ubuntu): `sudo ufw allow 4445/udp`. The server's installer already
does this on the server PC.

On a **Windows PC** Windows Defender Firewall blocks it: the first time the game starts it asks
whether to allow Java, and if the network is set as *Public* the permission given for private
networks isn't enough. Set the school network as *Private* (Settings → Network & Internet → the
network → Network profile type), or in *Allow an app through Windows Firewall* tick **Java(TM)
Platform SE binary** (`javaw.exe`) for *Public* too.

## Boards (micro:bit, Arduino): the dialout group

To read a [board](devices.md) plugged in with its USB cable, the user the students log in with
must be able to open the serial port: on Fedora, Ubuntu and Debian they must be in the `dialout`
group (`uucp` on Arch and a few others), as for the Arduino IDE. Once per PC:

```bash
sudo usermod -aG dialout student
```

Then the user logs out and in again. This also applies to Prism Launcher installed from Flatpak.
Without it, the `/mine device` window says "Can't open /dev/ttyACM0".

## Byte: fuel

By default CC: Tweaked requires fuel to move turtles. For coding lessons that only gets in the way,
so MINE turns it off the first time the server starts. If you want fuel back (for example for a
survival activity), set it in `config/computercraft-server.toml`: MINE won't change it again.
Turned off, it looks like this:

```toml
[turtle]
	need_fuel = false
```
