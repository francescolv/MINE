[English](worlds.md) · **Italiano**

# Mondi di classe

Come i mondi delle lezioni di Minecraft Education: ogni classe ha **il suo mondo** sul server di
classe, creato dall'insegnante in pochi secondi mentre tutti giocano. La 2B non vede cosa ha
costruito la 3A, e una classe nuova può partire da una copia pulita della stessa mappa.

Il mondo del server resta il **mondo principale** (`main`): la sala insegnanti, dove tutti
arrivano la prima volta.

## Creare un mondo

```
/mine world create 2b flat          un mondo piatto, pronto per il laboratorio (/mine lab build)
/mine world create 2b map egitto    una copia della mappa "egitto"
```

Un mondo di classe è pronto subito, ed è fatto per una lezione: **sempre mezzogiorno, niente
pioggia, niente mostri**. I nomi sono brevi: lettere minuscole, cifre e `_` (fino a 16).

## Mappe

Una mappa è un qualunque **mondo di Minecraft Java**: scaricato da un sito di mappe, o costruito
da te in giocatore singolo. Metti la sua cartella (quella con dentro `level.dat` e `region`) nella
cartella `mine/maps` del server, con un nome semplice:

```
/opt/mine-server/mine/maps/egitto/level.dat
/opt/mine-server/mine/maps/egitto/region/...
```

`/mine world maps` elenca le mappe che il server vede. Un mondo di classe creato da una mappa è
una **copia**: la mappa non cambia mai, quindi la classe successiva la riceve nuova. Gli studenti
arrivano dove arrivavano i giocatori della mappa; fuori dalla mappa non c'è niente (vuoto).

> Si copia solo il mondo normale della mappa, non il suo Nether o End. Le mappe fatte con versioni
> vecchie di Minecraft vengono aggiornate quando si aprono, come in giocatore singolo.

## Andarci

```
/mine world go 2b              vai nel mondo 2b
/mine world go 2b @a           viene anche tutta la classe
/mine world go main            torni nel mondo principale
/mine world spawn              chi viene in questo mondo arriva dove sei tu
/mine world                    in che mondo sono? (tutti)
```

Chi va in un mondo di classe ci ritorna se cade o muore. Dopo un riavvio del server i mondi ci sono
ancora, e anche gli studenti che erano dentro.

## Un laboratorio in ogni mondo

Ogni mondo ha il suo [laboratorio standard](server.it.md#3-il-laboratorio-standard): costruiscilo
una volta nel mondo della classe, e i comandi `/mine lab` riguardano il laboratorio del mondo in cui
sei.

```
/mine world go 2b
/mine lab build 24             il laboratorio della 2B, dove sei tu
/mine lab assign               gli studenti che sono nel mondo della 2B vanno nelle loro corsie
/mine lab reset                svuota il laboratorio della 2B, e solo quello
```

Le corsie sono per mondo: uno studente ha la sua corsia nel laboratorio della 2B. Cancellare un
mondo cancella anche il suo laboratorio.

## Un codice per ogni mondo

Ogni mondo ha il suo **codice di classe**, come in Minecraft Education: uno studente che inserisce
il codice della 2B arriva direttamente nel mondo della 2B (su una corsia del laboratorio, se quel
mondo ha il laboratorio).

```
/mine class code               il codice del mondo in cui sei (lo apre se serve)
/mine class code 2b            il codice del mondo 2b, da qualunque posto
/mine class close 2b           chiude il codice della 2b; /mine class close li chiude tutti
/mine class admit Alex 2b      fa entrare uno studente in attesa, nel mondo 2b
```

Si possono aprire più codici insieme (quello della 2B e quello della 3A, nello stesso momento):
sono sempre diversi. Il codice viene chiesto solo a chi non è ancora nella classe: gli altri
tornano dove erano usciti, cioè nel loro mondo.

## Regole di classe

Ogni mondo di classe ha le sue regole, che l'insegnante cambia **durante la lezione**, stando in
quel mondo. Cambiano subito, e solo lì: la ricreazione della 2B non disturba la 3A.

```
/mine world rules              cosa è permesso qui
/mine world rules lesson       niente che disturbi
/mine world rules free         ricreazione: tutto permesso
/mine world rules night on     una alla volta: monsters, night, fire, explosions, pvp — on o off
```

| Regola | On | Off |
|---|---|---|
| `monsters` (mostri) | mob ostili (difficoltà normale) | nessuno: pacifico, quelli che ci sono spariscono |
| `night` (notte) | giorno e notte | sempre mezzogiorno |
| `fire` (fuoco) | il fuoco si propaga | il fuoco non si propaga |
| `explosions` (esplosioni) | il TNT esplode, i creeper rompono i blocchi | il TNT non fa niente, i creeper non rompono niente |
| `pvp` (combattimenti) | i giocatori possono colpirsi | non possono |

Un mondo nuovo ha mostri e notte spenti, e il resto acceso, come in Minecraft. Tutti quelli che
sono nel mondo leggono in chat cosa ha cambiato l'insegnante. Non piove mai: i mondi di classe
hanno il meteo del mondo principale, e un server di classe lo tiene sereno.

## Cancellare un mondo

```
/mine world remove 2b            prima chiede
/mine world remove 2b confirm    lo cancella, per sempre
```

Tutto quello che c'è dentro sparisce; chi è dentro torna nel mondo principale. Per far ripartire
una classe da una mappa: cancella il suo mondo e ricrealo dalla stessa mappa.

## Comandi

| Comando | Cosa fa |
|---|---|
| `/mine world list` | i mondi di classe, e quanti giocatori ci sono in ognuno |
| `/mine world maps` | le mappe in `mine/maps`, e dov'è quella cartella |
| `/mine world create <nome> flat` / `map <mappa>` | crea un mondo di classe |
| `/mine world go <nome> [giocatori]` | porta te, o i giocatori, lì (`main`: il mondo principale) |
| `/mine world spawn` | dove arrivano gli studenti nel mondo in cui sei |
| `/mine world rules [lesson\|free\|<regola> on\|off]` | le regole del mondo in cui sei |
| `/mine world remove <nome> confirm` | cancella un mondo di classe |

Sono tutti per gli insegnanti, tranne `/mine world`.

## Zone

Parti di un mondo si possono rimettere com'erano dopo una lezione, o proteggere perché gli studenti
non le rompano: vedi [Zone](zones.it.md).
