[English](blocks.md) · **Italiano**

# Programmare Byte con i blocchi

Gli studenti che non sono ancora pronti per il Lua possono programmare il loro Byte con i
**blocchi**, come in Scratch o nel MakeCode di Minecraft Education: uniscono i blocchi nel browser,
vedono il Lua che quei blocchi creano e mandano il programma al loro Byte. Le lezioni controllano
quello che succede nel mondo, quindi funzionano allo stesso modo con i blocchi e con il Lua.

L'editor è una pagina web servita dal server della classe ([Blockly](https://github.com/RaspberryPiFoundation/blockly),
incluso in MINE: non serve internet). Funziona in qualunque browser dei PC del laboratorio.

## Aprirlo

Nel gioco premi **T** per aprire la chat e scrivi:

```
/mine blocks
```

L'editor si apre nel browser, con il nome dello studente in alto. Il collegamento è personale:
manda i programmi **a Byte di quello studente**, e smette di funzionare quando esce dal gioco
(la volta dopo si riscrive `/mine blocks`).

> `/mine blocks` va nella **chat** di Minecraft, non nel terminale di Byte: il terminale esegue
> i programmi, come quelli fatti con i blocchi.

## I blocchi

| Gruppo | Blocchi |
|---|---|
| **Byte** | vai avanti/indietro/su/giù, gira a sinistra/destra, piazza un blocco, scava, blocco davanti/sopra/sotto?, blocco … uguale a quello dello slot?, usa lo slot, blocchi nello slot, di', il numero della sfida |
| **Redstone** | segnale redstone [lato] a vero/falso, segnale da [lato]?, aspetta un cambio di redstone |
| **Scheda** | aspetta [chiave] dalla scheda, valore della scheda [chiave], manda alla scheda [chiave] [valore], scheda collegata? (vedi [schede](devices.it.md)) |
| **Cicli** | ripeti N volte, ripeti mentre/finché, conta da … a …, aspetta N secondi |
| **Logica** | se … allora … altrimenti, confronta, e/o, non, vero/falso |
| **Matematica** | numeri, + − × ÷, numero a caso |
| **Testo** | testo, unisci |
| **Variabili** e **Funzioni** | create da te (quelle di Blockly) |

A destra c'è **il Lua dei tuoi blocchi**, aggiornato a ogni modifica: è lo stesso Lua delle
lezioni, un ponte dai blocchi al testo (il pulsante **Lua** lo nasconde).

## Invia ed Esegui

- **Esegui ▶**: torna in Minecraft. Il programma parte appena ci sei, dopo **3, 2, 1**, così vedi
  muoversi il tuo Byte. Se non torni entro un minuto, non parte.
- **Invia a Byte**: il programma viene scritto su Byte con il nome in alto (es. `blocchi`,
  `quadrato`); nel suo terminale scrivi quel nome per eseguirlo.
- Nel gioco, **R** esegue di nuovo l'ultimo programma eseguito da Byte, dai blocchi o scritto nel
  suo terminale (il tasto si cambia in Controlli, sotto MINE). Così puoi scrivere un programma nel
  terminale, eseguirlo una volta, chiudere il terminale con **Esc** e premere **R** per guardare
  Byte al lavoro.

Il gioco non va in pausa mentre il browser è davanti. Se Byte è spento (per esempio dopo un
riavvio del server), MINE lo accende.

## I programmi

Ogni programma inviato resta sul server, con il suo nome. **I miei programmi** li riapre; **+ Nuovo
programma** riparte da zero. Usare un nome già usato sostituisce quel programma.

Gli **insegnanti** vedono anche **Programmi degli studenti…**: il programma di uno studente si apre
da guardare (per aiutarlo, o per mostrarlo alla classe); **Copia nei miei** ne fa una copia da
modificare ed eseguire sul proprio Byte.

## Lezioni

Tutte le lezioni si possono fare interamente con i blocchi: ogni guida ha una parte "Con i blocchi"
([lezione 1](../lessons/01-first-steps/README.it.md#con-i-blocchi), [2](../lessons/02-loops/README.it.md#con-i-blocchi),
[3](../lessons/03-debugging/README.it.md#con-i-blocchi), [4](../lessons/04-conditions/README.it.md#con-i-blocchi),
[5](../lessons/05-functions/README.it.md#con-i-blocchi), [6](../lessons/06-redstone/README.it.md#con-i-blocchi),
[7](../lessons/07-sensors/README.it.md#con-i-blocchi))
con i programmi, e le soluzioni si aprono con **Apri file** (**Salva file** salva i blocchi sul PC,
per esempio per condividere un programma). Una lezione può anche dare agli studenti dei programmi a
blocchi, che compaiono in **I miei programmi** quando inizia: la lezione 3 dà i programmi rotti da
correggere.

### La lezione nell'editor

Durante una lezione, una barra verde sotto i pulsanti mostra la lezione, la tappa e il suo
obiettivo, con le parole dei blocchi. **Suggerimento** mostra i suggerimenti della tappa uno alla
volta, scritti anch'essi per i blocchi (contano come quelli di `/mine lesson hint`). Quando lo
studente torna al browser dal gioco, la barra mostra la nuova tappa, e **I miei programmi** i
programmi che una lezione ha appena dato.

## Consigli

- Con uno schermo piccolo, metti il browser e Minecraft uno accanto all'altro: si vede Byte che
  si muove mentre i blocchi sono sullo schermo.
- Un passo di lezione non ancora fatto lo dice dopo ogni programma, anche con i blocchi ("Non
  ancora: …").

## Per il tecnico

L'editor usa la **porta 25580** del server: l'installatore la apre nel firewall. Funziona solo su
un server di classe (il pacchetto server di MINE), non in un mondo in giocatore singolo. Se la
pagina non si apre, controlla che i PC del laboratorio raggiungano il server su quella porta (es.
`http://192.168.1.20:25580` in un browser mostra l'editor, che chiede `/mine blocks`).
