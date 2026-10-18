[English](portfolio.md) · **Italiano**

# Fotocamera, portfolio e galleria di classe

Come la Fotocamera e il Portfolio di Minecraft Education: gli studenti scattano **foto** di
quello che costruiscono, scrivono **note** su cosa hanno fatto e imparato, e l'insegnante
raccoglie tutto. L'insegnante crea anche **gallerie di classe**, dove gli studenti condividono
con tutti le loro foto migliori.

La fotocamera e la finestra del portfolio hanno bisogno di **MINE nel gioco dello studente** (il
modpack di MINE): solo il gioco può fotografare quello che lo studente vede.

## La fotocamera

```
/mine camera                 una fotocamera per te
/mine camera @a              insegnante: una fotocamera per tutti
```

Clicca col tasto destro tenendo la fotocamera: il gioco fotografa quello che vedi, senza la barra
degli oggetti e la mano, e la salva nel tuo portfolio, con il luogo, la data e la tappa della
lezione a cui eri. Una copia resta anche sul tuo computer, nella cartella `screenshots/mine` del
gioco.

## Il portfolio

`/mine portfolio` apre il tuo portfolio in una finestra: una foto o una nota alla volta, con le
frecce (o i tasti ← →) per sfogliarle.

- **Didascalia**: una frase breve sotto la foto.
- **Nuova nota**: una pagina di testo: cosa hai fatto, cosa hai imparato, cosa cambieresti.
- **Condividi**: mette una copia della foto nella galleria di classe (se l'insegnante ne ha creata una).
- **Elimina**: la toglie, dopo averlo chiesto.

Ogni studente vede solo il proprio portfolio. L'insegnante vede quelli di tutti:
`/mine portfolio Alex`, e può eliminare quello che non va bene.

## Gallerie di classe

L'insegnante crea una galleria per un'attività, ad esempio la lezione sui cicli della 3B:

```
/mine gallery create cicli_3B
```

Da quel momento gli studenti trovano **Condividi** nel loro portfolio: le loro foto vanno in
`cicli_3B`. Tutti possono guardarla, con `/mine gallery`. In una galleria l'insegnante può anche
aggiungere note (un'introduzione, la consegna) ed eliminare qualunque elemento; gli studenti
possono eliminare quello che hanno condiviso.

| | |
|---|---|
| `/mine gallery` | tutti: apre la galleria corrente |
| `/mine gallery open <nome>` | tutti: ne apre un'altra |
| `/mine gallery list` | tutti: tutte le gallerie |
| `/mine gallery create <nome>` | insegnante: una nuova galleria, dove gli studenti condividono da ora |
| `/mine gallery use <nome>` | insegnante: da ora gli studenti condividono con questa galleria |
| `/mine gallery remove <nome>` | insegnante: rimuove la galleria e le sue foto |

I nomi usano lettere, cifre, `-` e `_`.

## Esportare

`/mine portfolio export` crea uno **zip di pagine web** da aprire con qualunque browser, senza
internet: un indice, una pagina per ogni studente e una per ogni galleria. La pagina di ogni
studente ha le sue foto e note, le sue risposte alle domande di riflessione delle lezioni e ai
quiz degli NPC. Le pagine si stampano bene, da conservare o da mostrare alle famiglie.

- L'insegnante riceve i portfolio di tutti e tutte le gallerie.
- Uno studente riceve il proprio portfolio.

Lo zip viene scaricato sul tuo computer (cartella Scaricati), con una finestra per aprirlo, e
viene salvato anche sul server, nella cartella del mondo, in `mine/exports`.

## Dove stanno le cose

Le foto sono sul server, nella cartella del mondo: `mine/portfolio/<studente>/` e
`mine/portfolio/gallery-<nome>/`; il resto è salvato nel mondo. Facendo il backup della cartella
del mondo, fai il backup anche dei portfolio.

Limiti: le foto vengono rimpicciolite (al massimo 1280 × 720, circa 100–300 KB l'una); un
portfolio o una galleria contengono fino a 300 elementi; una foto ogni 2 secondi.
