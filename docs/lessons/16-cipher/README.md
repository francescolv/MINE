**English** · [Italiano](README.it.md)

# Lesson 16 — Secret messages: Caesar's cipher

| | |
|---|---|
| **Length** | 1 hour (50–60 minutes) |
| **Age** | 12–16 |
| **Concepts** | text and characters, character codes, remainder (`%`), functions, brute force; cybersecurity |
| **Prerequisites** | [lesson 5](../05-functions/README.md) (functions); done in **Lua** or in **Python** |

## Objectives

By the end of the lesson students can:

1. explain how **Caesar's cipher** works and decode it by hand;
2. go through a text **one letter at a time** and transform it with its number code;
3. use the **remainder** (`%`) to go round the alphabet;
4. find an unknown key by **trying them all** (brute force), and understand why this cipher isn't
   safe today.

## How it works

Every time a program starts, **MINE writes a new secret message**, in capitals, with two Minecraft
words (the same in English and Italian: CREEPER, NETHER, PORTAL…), each letter moved forward in the
alphabet by some number of places (the **key**). The program reads it with `mine.challenge()`,
decodes it and makes Byte say it with `mine.say`. MINE checks it's the right message.

No blocks are needed, nor room around Byte. The lesson isn't done with blocks: it takes character
codes.

## Preparation (teacher, before the lesson)

- Nothing but Byte. For Python, the Python editor (`/mine python`).
- A paper **Caesar wheel** for each pair (two circles with the alphabet, one inside the other)
  helps a lot in the introduction.
- Try the three steps yourself with [`solutions/`](solutions/) (Lua and Python).

## Missions: MINE checks the steps

```
/mine lesson start cipher @a
```

| Step | `mine.challenge()` gives… | Done when… |
|---|---|---|
| 1 · `caesar` | the secret message, key 3 | Byte says the decoded message |
| 2 · `key` | the message and the key (1 to 25, different every time) | as above |
| 3 · `crack` | only the message, which contains the word BYTE | as above |

## Lesson plan

### Introduction — 10 minutes, no computers

Julius Caesar moved every letter 3 places: A → D, B → E, … X → A. On the board: decode `FUHHSHU`
(CREEPER) together. What about Z? Start again from the beginning: it's going **round the alphabet**.

### Activity 1 — Key 3 (20 minutes)

In the computer letters are numbers: `string.byte("A")` is 65, `string.char(65)` is "A". To move a
letter back 3 places, going round:

```lua
local function decode(text, key)
  local plain = ""
  for i = 1, #text do
    local c = text:sub(i, i)
    if c >= "A" and c <= "Z" then
      c = string.char((c:byte() - 65 - key) % 26 + 65)
    end
    plain = plain .. c
  end
  return plain
end

mine.say(decode(mine.challenge(), 3))
```

Things to explain: `c:byte() - 65` gives 0 for A, 25 for Z; `% 26` is the remainder of dividing by
26, which takes -1 back to 25 (the way round); `+ 65` goes back to the letter's code.

In Python it's the same, with `ord(c)` and `chr(n)`: [`solutions/caesar.py`](solutions/caesar.py).
Lua solution: [`solutions/caesar.lua`](solutions/caesar.lua).

### Activity 2 — The key changes (10 minutes)

```lua
local secret, key = mine.challenge()
mine.say(decode(secret, key))
```

Thanks to the function with the parameter `key`, one line changes.

Solution: [`solutions/key.lua`](solutions/key.lua) / [`.py`](solutions/key.py).

### Activity 3 — No key: brute force (15 minutes)

There are only 25 possible keys: try them all, and stop when a word you know is there (here BYTE)
appears in the decoded message.

```lua
local secret = mine.challenge()
for key = 1, 25 do
  local plain = decode(secret, key)
  if plain:find("BYTE") then
    mine.say("The key is " .. key .. ": " .. plain)
    break
  end
end
```

Solution: [`solutions/crack.lua`](solutions/crack.lua) / [`.py`](solutions/crack.py).

### Wrap-up — 10 minutes

- How many tries would it take if every letter had a different key? (Many more: that's the idea of
  modern ciphers, with very long keys.)
- MINE's reflection question: *Byte cracked a message without the key, trying them all. Why is
  Caesar's cipher not safe today? How do the messages on your phone stay secret?*

## Common problems

| What happens | Why | What to do |
|---|---|---|
| Byte says strange characters (symbols, small letters) | the `% 26` or the `+ 65` is missing | round the alphabet: `(n - 65 - key) % 26 + 65` |
| Spaces turn into letters | characters that aren't letters get changed too | change only "A" to "Z" |
| The message is moved the wrong way | `+ key` instead of `- key` | decoding goes **back** |
| *"Byte has to say the decoded message"* | the program doesn't call `mine.say` | `mine.say(...)` at the end |

## References

- [Lesson 5 — Functions](../05-functions/README.md); [Lua API](../../guide/lua-api.md) (`mine.challenge`), [Python guide](../../guide/python.md).
- Solutions in [`solutions/`](solutions/) (in Italian in [`solutions/it/`](solutions/it/)), in Lua
  and in Python, tested automatically on new messages every time.
