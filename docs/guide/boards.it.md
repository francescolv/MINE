[English](boards.md) · **Italiano**

# Lavagne

Le lavagne mostrano testo grande nel mondo, come le lavagne di Minecraft Education: il titolo di
un'attività, le istruzioni, le regole del laboratorio, un pezzo di codice da ricordare. Come gli
NPC, le scrivi **nel gioco**, in un libro e penna. Una lavagna è fatta con i display di testo di
Minecraft.

## Creare una lavagna

Mettiti dove la vuoi, rivolto da quella parte, e scrivi:

```
/mine board create
```

Due blocchi davanti a te compare una lavagna vuota, rivolta verso di te. La sua dimensione è 2;
puoi sceglierne un'altra, da 0.5 a 8: `/mine board create 4`.

## Scriverla

**Clicca la lavagna col tasto destro** a mano vuota, in qualunque punto del testo e fino a 10
blocchi di distanza: si apre la finestra di MINE con il testo com'è. Scrivilo e premi **Fatto**.
Le righe che iniziano con `# ` sono **titoli** (gialli, in grassetto). Le righe lunghe vanno a capo
da sole, a circa 25 lettere.

```
# Regole del laboratorio
Costruisci solo nella tua area.
Chiedi prima di usare la TNT.
Divertiti!
```

Funziona allo stesso modo con un **libro e penna**: scrivi il testo
nel libro (tienilo in mano e fai tasto destro in aria per aprirlo), poi clicca col tasto destro il
**centro** della lavagna tenendolo. Per avere il testo della lavagna in un libro,
**accovacciati e cliccala col tasto destro a mano vuota**.

## Dimensione e rimozione

Mettiti accanto e scrivi `/mine board size 3` per cambiarne la dimensione, `/mine board remove`
per rimuoverla: entrambi agiscono sulla lavagna più vicina entro 8 blocchi.

Le lavagne le creano insegnanti e costruttori; i costruttori modificano e rimuovono solo le
proprie, come per gli NPC (vedi [Studenti che costruiscono](npc.it.md#studenti-che-costruiscono)).

## Lavagne dai datapack

Le lezioni possono arrivare con le loro lavagne pronte: `/mine board place mine:loops` mette la
lavagna della lezione sui cicli, nella tua lingua. Vedi
[NPC e lavagne nei datapack](npc.it.md#npc-e-lavagne-nei-datapack).
