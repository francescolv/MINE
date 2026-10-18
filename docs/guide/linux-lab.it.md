[English](linux-lab.md) · **Italiano**

# Preparare un laboratorio Linux per MINE

Note pratiche per insegnanti e tecnici. Si allungherà man mano che proviamo MINE in classi vere.

## Touchpad: il clic destro rompe i blocchi invece di aprirli

Su Fedora, Ubuntu e altre distribuzioni con GNOME, il touchpad di default usa il
**clic con le dita**: conta *quante* dita premono, non *dove*. Premere nell'angolo in
basso a destra con un dito è quindi un clic **sinistro**, e in modalità Creativa rompe
subito il blocco (per esempio Byte) invece di aprirlo.

Soluzioni:

- **Senza cambiare nulla:** clic o tocco **con due dita** = tasto destro.
- **Clic destro nell'angolo in basso a destra, come su Windows:**
  Impostazioni → Mouse e touchpad → Touchpad → metodo di clic "Aree", oppure da terminale:

  ```bash
  gsettings set org.gnome.desktop.peripherals.touchpad click-method 'areas'
  ```

  (per tornare indietro: `'fingers'`). L'impostazione vale per l'utente corrente: nei
  laboratori con account condivisi conviene impostarla nel profilo di default.

- **Il più semplice:** in laboratorio, un mouse USB.

## Il server della scuola non compare in Multigiocatore

Il server MINE si annuncia sulla rete come un mondo *Apri in LAN*, sulla porta **4445/udp**. Se il
firewall di un PC blocca quella porta, il server non compare nella sua lista Multigiocatore
(entrare con **Aggiungi server** e l'indirizzo funziona comunque). Succede su Fedora con la zona
`public`, per esempio Fedora Server o installazioni personalizzate; Fedora Workstation la lascia
passare. Controlla con `sudo firewall-cmd --get-active-zones`, poi apri la porta per sempre:

```bash
sudo firewall-cmd --permanent --add-port=4445/udp && sudo firewall-cmd --reload
```

Con ufw (spento di default su Ubuntu): `sudo ufw allow 4445/udp`. L'installatore del server lo fa
già sul PC del server.

Su un **PC Windows** la porta la blocca Windows Defender Firewall: alla prima partita chiede se
consentire Java, e se la rete è impostata come *Pubblica* il permesso dato per le reti private non
basta. Imposta la rete della scuola come *Privata* (Impostazioni → Rete e Internet → la rete →
Tipo di profilo di rete), oppure in *Consenti app attraverso Windows Firewall* spunta **Java(TM)
Platform SE binary** (`javaw.exe`) anche per *Pubblica*.

## Le schede (micro:bit, Arduino): il gruppo dialout

Per leggere una [scheda](devices.it.md) collegata con il cavo USB, l'utente con cui gli studenti
entrano nel PC deve poter aprire la porta seriale: su Fedora, Ubuntu e Debian deve essere nel
gruppo `dialout` (su Arch e alcune altre `uucp`), come per l'IDE di Arduino. Una volta per PC:

```bash
sudo usermod -aG dialout studente
```

Poi l'utente esce e rientra nella sessione. Vale anche per Prism Launcher installato da Flatpak.
Senza, la finestra `/mine device` dice «Non riesco ad aprire /dev/ttyACM0».

## Byte: carburante

CC: Tweaked di default richiede carburante per muovere le byte. Nelle lezioni di coding è solo
un intralcio, quindi MINE lo disattiva al primo avvio del server. Se vuoi riattivarlo (ad esempio
per un'attività in sopravvivenza), cambialo in `config/computercraft-server.toml`: MINE non lo
toccherà più. Disattivato, appare così:

```toml
[turtle]
	need_fuel = false
```
