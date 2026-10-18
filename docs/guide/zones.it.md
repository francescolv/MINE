[English](zones.md) · **Italiano**

# Zone

Una **zona** è una parte di un mondo che l'insegnante delimita: un villaggio, una piazza, la scena
di una lezione. MINE tiene una **fotografia** dei suoi blocchi, così dopo la lezione l'insegnante la
rimette com'era, in un secondo, con gli studenti ancora in gioco.

## Segnare una zona

```
/mine zone corners          due blocchi angolo (terracotta smaltata magenta) nell'inventario
```

Mettine uno a un angolo dell'area e l'altro all'**angolo opposto**, sul terreno: segnano l'area,
come su una mappa. La zona scende anche di **10 blocchi nel terreno** (le buche scavate tornano
piene) e sale di **30 blocchi** (quello che ci si costruisce sopra). Ogni angolo mostra una colonna
di luce, visibile da lontano (solo da te), finché la zona non è creata; dopo il secondo angolo, il
contorno della zona si vede per qualche secondo, con le sue misure. Poi:

```
/mine zone create villaggio         crea la zona, toglie i due blocchi angolo e fa la sua fotografia
/mine zone create villaggio 3 50    invece: 3 blocchi nel terreno e 50 in su
```

Una zona può avere fino a un milione di blocchi (per esempio 100 × 100 × 100). Nomi: lettere
minuscole, cifre e `_` (fino a 24).

## Rimetterla com'era

```
/mine zone restore villaggio  la zona torna come nella sua fotografia
/mine zone save villaggio     una nuova fotografia: d'ora in poi torna com'è adesso
```

Il ripristino rimette ogni blocco, compreso il contenuto dei bauli, e toglie gli oggetti a terra.
Non tocca:

- gli **Byte**: quelli nella zona restano dove sono, con i loro programmi, e nessuno viene
  ricopiato dove ce n'era uno al momento della fotografia;
- **NPC, lavagne** e le altre entità (animali, supporti per armature…).

## Zone protette

```
/mine zone protect villaggio on     gli studenti non possono cambiarne i blocchi
/mine zone protect villaggio off    di nuovo sì
```

In una zona protetta gli studenti non possono rompere né mettere blocchi, versare acqua o lava,
accendere il fuoco, arare o scortecciare, e **nemmeno i loro Byte** (lì `byte.dig()` non scava).
Anche esplosioni e fuoco non la toccano. **Porte, leve, pulsanti e bauli continuano a funzionare**:
non cambiano i blocchi della zona, e servono per le lezioni di redstone. Gli insegnanti, e i loro
Byte, la cambiano come sempre.

Chi ci prova legge, sopra la barra degli oggetti, che la zona è protetta.

> Non coperti: acqua o lava che entrano scorrendo da fuori della zona, e pistoni che spingono blocchi
> dentro. Per sicurezza, fai la zona un po' più grande di quello che protegge.

## Altri comandi

| Comando | Cosa fa |
|---|---|
| `/mine zone list` | le zone, il mondo di ognuna, le sue misure, e se è protetta |
| `/mine zone show <nome>` | mostra il contorno della zona per 15 secondi |
| `/mine zone remove <nome>` | dimentica la zona e la sua fotografia (i blocchi restano come sono) |

Sono tutti per gli insegnanti, anche dai [blocchi comandi](command-blocks.it.md) (per esempio un
pulsante che rimette a posto la zona). Le zone appartengono al loro mondo: cancellare un
[mondo di classe](worlds.it.md) dimentica anche le sue zone.

