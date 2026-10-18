[English](command-blocks.md) · **Italiano**

# Blocchi comandi

Con i **blocchi comandi** l'insegnante costruisce mondi interattivi: un pulsante che avvia la
lezione, una piastra a pressione che porta gli studenti in un altro mondo, una leva per la
ricreazione, un cartello che dà i materiali. Gli studenti non li scrivono: **premono** pulsanti,
leve e piastre, e i blocchi comandi fanno il resto.

## Attivarli

Su un server installato con il pacchetto di MINE sono già attivi. Su un server installato prima,
attivali una volta:

```bash
sudo sed -i 's/^enable-command-block=.*/enable-command-block=true/' /opt/mine-server/server.properties
sudo systemctl restart mine-server
```

In giocatore singolo o in *Rete locale*, i comandi devono essere permessi (*Comandi*: sì).

## Metterne uno

Solo un **insegnante** (operatore), in **Creativa**, può mettere un blocco comandi e scriverci dentro;
gli studenti non possono aprirli, né romperli in una [zona protetta](zones.it.md). Per averne uno:

```
/give @s command_block
```

Clic destro per scrivere il comando (con o senza la `/`). I pulsanti in basso:

| Pulsante | Scelte |
|---|---|
| **A impulso / A catena / A ripetizione** | *A impulso*: una volta, ogni volta che riceve corrente. *A catena*: dopo il blocco che lo punta. *A ripetizione*: a ogni tick, finché ha corrente |
| **Condizionale / Non condizionale** | in una catena, *Condizionale* parte solo se il blocco prima è riuscito |
| **Serve redstone / Sempre attivo** | *Serve redstone*: lo avvia un pulsante, una leva o una piastra. *Sempre attivo*: parte da solo |

Metti un pulsante sul blocco comandi, o accanto, oppure collega una piastra con la polvere di
redstone.

## Per chi è il comando

| Selettore | Chi |
|---|---|
| `@p` | il giocatore più vicino: lo studente che ha premuto il pulsante |
| `@a` | tutti |
| `@a[distance=..10]` | tutti entro 10 blocchi |

## Ricette

| Cosa | Comando | Con |
|---|---|---|
| Avviare la lezione per lo studente | `mine lesson start loops @p` | un pulsante |
| Avviarla per tutta la classe | `mine lesson start loops @a` | un pulsante vicino alla cattedra |
| Un portale verso un altro mondo di classe | `mine world go 3a @p` | una piastra a pressione |
| Tornare al mondo principale | `mine world go main @p` | una piastra a pressione |
| Ricreazione / si fa lezione | `mine world rules free` / `mine world rules lesson` | due pulsanti, fuori dalla portata degli studenti |
| Rimettere il villaggio com'era | `mine zone restore villaggio` | un pulsante per l'insegnante |
| I materiali dell'attività | `give @p oak_planks 64` | un pulsante |
| Una fotocamera per il portfolio | `mine camera @p` | un pulsante |
| Un messaggio per lo studente | `tellraw @p "Bravo! Ora vai nella stanza dopo."` | una piastra a pressione |
| Spostarsi di qualche passo | `tp @p ~ ~ ~5` | una piastra a pressione |

`/mine world rules`, `/mine class code` e `/mine lab` da un blocco comandi riguardano il mondo in cui
si trova il blocco, come quando l'insegnante li scrive lì.

## Attenzione

- Un blocco comandi ha i **poteri dell'insegnante**: chi preme il suo pulsante esegue il suo
  comando. Metti i pulsanti che deve premere solo l'insegnante (ricreazione, ripristino di una
  zona) dove gli studenti non arrivano.
- Metti i blocchi comandi in una [zona protetta](zones.it.md): gli studenti non possono romperli, ma
  possono ancora premere i pulsanti, che è quello a cui servono.
- Non rendere mai operatori gli studenti per fargli usare i blocchi comandi: un operatore può fare
  qualunque cosa sul server.
- Nei mondi di classe i blocchi comandi non scrivono in chat cosa hanno fatto. Nel mondo principale:
  `/gamerule command_block_output false`.
- Non succede niente? Controlla che i blocchi comandi siano attivi (sopra), che il blocco sia su
  *Serve redstone* con un pulsante che gli dà corrente, e come è scritto il comando: nella finestra del
  blocco, *Esito precedente* mostra l'errore.
