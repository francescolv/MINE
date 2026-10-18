[English](README.md) · **Italiano**

# Lezione 16 — Messaggi segreti: il cifrario di Cesare

| | |
|---|---|
| **Durata** | 1 ora (50–60 minuti) |
| **Età** | 12–16 anni |
| **Concetti** | testi e caratteri, codici dei caratteri, resto della divisione (`%`), funzioni, forza bruta; sicurezza informatica |
| **Prerequisiti** | [lezione 5](../05-functions/README.it.md) (funzioni); si fa in **Lua** o in **Python** |

## Obiettivi

Alla fine della lezione lo studente sa:

1. spiegare come funziona il **cifrario di Cesare** e decifrarlo a mano;
2. scorrere un testo **una lettera alla volta** e trasformarla con il suo codice numerico;
3. usare il **resto** (`%`) per fare il giro dell'alfabeto;
4. trovare una chiave sconosciuta **provandole tutte** (forza bruta), e capire perché questo
   cifrario oggi non è sicuro.

## Come funziona

Ogni volta che un programma parte, **MINE scrive un messaggio segreto nuovo**, in maiuscolo, con
due parole di Minecraft (uguali in italiano e in inglese: CREEPER, NETHER, PORTAL…), ogni lettera
spostata avanti nell'alfabeto di un certo numero di posti (la **chiave**). Il programma lo legge con
`mine.challenge()`, lo decifra e lo fa dire a Byte con `mine.say`. MINE controlla che sia il
messaggio giusto.

Non servono blocchi né spazio intorno a Byte. Con i blocchi la lezione non si fa: servono i codici
dei caratteri.

## Preparazione (insegnante, prima della lezione)

- Nient'altro che Byte. Per Python, l'editor Python (`/mine python`).
- Una **ruota di Cesare** di carta per ogni coppia (due cerchi con l'alfabeto, uno dentro l'altro)
  aiuta molto nell'introduzione.
- Prova tu le tre tappe con [`solutions/it/`](solutions/it/) (Lua e Python).

## Missioni: MINE controlla le tappe

```
/mine lesson start cipher @a
```

| Tappa | `mine.challenge()` dà… | Superata quando… |
|---|---|---|
| 1 · `caesar` | il messaggio segreto, chiave 3 | Byte dice il messaggio decifrato |
| 2 · `key` | il messaggio e la chiave (da 1 a 25, ogni volta diversa) | come sopra |
| 3 · `crack` | solo il messaggio, che contiene la parola BYTE | come sopra |

## Svolgimento

### Introduzione — 10 minuti, senza computer

Giulio Cesare spostava ogni lettera di 3 posti: A → D, B → E, … X → A. Alla lavagna: decifrate
insieme `FUHHSHU` (CREEPER). Come si fa con la Z? Si ricomincia dall'inizio: è il **giro
dell'alfabeto**.

### Attività 1 — Chiave 3 (20 minuti)

Al computer le lettere sono numeri: `string.byte("A")` è 65, `string.char(65)` è "A". Per spostare
una lettera indietro di 3 posti facendo il giro:

```lua
local function decifra(testo, chiave)
  local chiaro = ""
  for i = 1, #testo do
    local c = testo:sub(i, i)
    if c >= "A" and c <= "Z" then
      c = string.char((c:byte() - 65 - chiave) % 26 + 65)
    end
    chiaro = chiaro .. c
  end
  return chiaro
end

mine.say(decifra(mine.challenge(), 3))
```

Cose da spiegare: `c:byte() - 65` dà 0 per A, 25 per Z; `% 26` è il resto della divisione per 26,
che riporta -1 a 25 (il giro); `+ 65` torna al codice della lettera.

In Python è lo stesso, con `ord(c)` e `chr(n)`: [`solutions/it/cesare.py`](solutions/it/cesare.py).
Soluzione Lua: [`solutions/it/cesare.lua`](solutions/it/cesare.lua).

### Attività 2 — La chiave cambia (10 minuti)

```lua
local segreto, chiave = mine.challenge()
mine.say(decifra(segreto, chiave))
```

Grazie alla funzione con il parametro `chiave`, basta cambiare una riga.

Soluzione: [`solutions/it/chiave.lua`](solutions/it/chiave.lua) / [`.py`](solutions/it/chiave.py).

### Attività 3 — Senza chiave: la forza bruta (15 minuti)

Le chiavi possibili sono solo 25: si provano tutte, e ci si ferma quando nel messaggio decifrato
compare una parola che si sa esserci (qui BYTE).

```lua
local segreto = mine.challenge()
for chiave = 1, 25 do
  local chiaro = decifra(segreto, chiave)
  if chiaro:find("BYTE") then
    mine.say("La chiave è " .. chiave .. ": " .. chiaro)
    break
  end
end
```

Soluzione: [`solutions/it/forza.lua`](solutions/it/forza.lua) / [`.py`](solutions/it/forza.py).

### Conclusione — 10 minuti

- Quante prove servirebbero se ogni lettera avesse una chiave diversa? (Molte di più: è l'idea dei
  cifrari moderni, con chiavi lunghissime.)
- La domanda di riflessione di MINE: *Byte ha scoperto un messaggio senza la chiave, provandole
  tutte. Perché oggi il cifrario di Cesare non è sicuro? Come restano segreti i messaggi sul tuo
  telefono?*

## Problemi frequenti

| Cosa succede | Perché | Cosa fare |
|---|---|---|
| Byte dice lettere strane (simboli, minuscole) | manca `% 26`, o il `+ 65` | il giro dell'alfabeto: `(n - 65 - chiave) % 26 + 65` |
| Gli spazi diventano lettere | si trasformano anche i caratteri che non sono lettere | trasforma solo da "A" a "Z" |
| Il messaggio è spostato dalla parte sbagliata | `+ chiave` invece di `- chiave` | per decifrare si torna **indietro** |
| *"Byte deve dire il messaggio"* | il programma non chiama `mine.say` | `mine.say(...)` alla fine |

## Riferimenti

- [Lezione 5 — Funzioni](../05-functions/README.it.md); [API Lua](../../guide/lua-api.it.md) (`mine.challenge`), [guida Python](../../guide/python.it.md).
- Soluzioni in [`solutions/it/`](solutions/it/) (in inglese in [`solutions/`](solutions/)), in Lua e
  in Python, provate in automatico su messaggi nuovi ogni volta.
