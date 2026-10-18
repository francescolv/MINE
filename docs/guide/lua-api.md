**English** · [Italiano](lua-api.it.md)

# MINE Lua API

Every Byte (and, on a server running MINE, every turtle) gets the `mine` API, on top of the
[CC: Tweaked](https://tweaked.cc/) APIs such as `turtle`, `fs` and `term`.

In Byte's terminal, `help mine` shows this summary in game (in Italian when the game is in Italian).

## `mine.say(message)`

Byte writes the message in the chat, signed with its label:

```
[Student's Byte] Hi!
```

- Only players within **32 blocks** of Byte can read it, so the chat stays readable
  in a lab with thirty Bytes.
- It also accepts numbers and other values: `mine.say(3 + 4)` writes `7`.
- Messages longer than 256 characters are cut off.
- Each call takes one server tick (1/20 of a second).

## `mine.owner()`

The name of the student Byte belongs to, i.e. whoever created it with `/mine byte`.
Returns `nil` if the turtle has no owner.

```lua
mine.say("Hi! I'm " .. mine.owner() .. "'s Byte.")
```

## `mine.challenge()`

What the student's lesson step asks **this time**, chosen by MINE when the program starts: for
example the side of the square in [lesson 5](../lessons/05-functions/README.md). Because it
changes at every run, the program has to read it instead of counting on a fixed number.
Returns nothing (`nil`) when the step doesn't ask anything, or outside a lesson.

```lua
local side = mine.challenge()
mine.say("MINE wants a square " .. side .. " x " .. side)
```

Sometimes there's more than one number, for example forward and right of a treasure in
[lesson 11](../lessons/11-treasure/README.md): `local forward, right = mine.challenge()`.
`mine.challenge(n)` gives only the `n`-th (from 1), as the block *the challenge's number …* does.

## `byte.ripeDown()`

`true` when the block under Byte is a crop that has finished growing (wheat, carrots, potatoes,
beetroots…), as in [lesson 15](../lessons/15-farm/README.md); with blocks, *ripe crop below?*.

## `mine.version()`

The version of MINE installed on the server, for example `"0.1.0"`.

## `mine.device`: the board

The [board](devices.md) (micro:bit, Arduino…) the student has connected to their PC with
`/mine device`. The board writes `key:value` lines; values are whole numbers or texts.

| Function | What it does |
|---|---|
| `mine.device.read(key)` | The last value that arrived for `key`, or `0` if the board never wrote it |
| `mine.device.wait([key])` | Waits until the board writes `key` (or any key, without an argument) and returns the key and value |
| `mine.device.send(key, value)` | Writes `key:value` to the board; `false` if no board is connected |
| `mine.device.connected()` | `true` if the student has a board connected |

Each line also arrives as a `mine_device` event, with the key and value, for programs that wait
for several things at once:

```lua
local _, key, value = os.pullEvent("mine_device")
```

```lua
mine.device.wait("pulsante_a")
mine.device.send("cuore", 1)
byte.forward()
if mine.device.read("luce") < 50 then
  mine.say("It's dark")
end
```

## Running programs from the chat

`/mine byte run <program>` starts one of Byte's programs as if the student had typed it
in its terminal (arguments work too: `/mine byte run tower 5`). It works when Byte is
on and waiting for a command at the `>` prompt; when the program ends, MINE tells the student
in the chat whether it finished or stopped because of an error.

The **R** key (in Controls, under MINE) runs again the last program Byte ran, with its
arguments, whether it was typed in the terminal, started from the chat or sent from the
[blocks](blocks.md): students write and try a program in the terminal, close it with **Esc**, and
press **R** to watch Byte from outside.

This is done by `rom/autorun/mine.lua`, which relies on the internal functions `mine._idle`
and `mine._report`: students' programs should not use them. The same file copies the programs a
lesson gives the student onto Byte (`mine._prepare`, see
[Writing a lesson](lesson-format.md#programs-for-the-students)). When Byte starts, it also
replaces CraftOS's banner and tip of the day with MINE's welcome — version, Byte's name,
the student's lesson and step — in the student's language (`mine._welcome`).

The same file tells the server when one of the student's own programs starts and ends
(`mine._started` and `mine._finished`), so lesson missions can check what Byte did.
Calling them by hand gains nothing: the server only looks at the world — where Byte is,
which way it faces, which blocks are there — never at what Lua claims.

## Byte's pickaxe

Every Byte is created with a **diamond pickaxe**, so it can dig like Minecraft Education's
Agent: `byte.dig()`, `byte.digUp()` and `byte.digDown()` break the block in front, above
or below, and put it in Byte's inventory. Bytes created with older versions of MINE get
it too, as soon as they are waiting for a command, as long as they have no other tools.

## Growing with the lessons

The API grows together with the lessons: every new function comes from an activity that
needs it. Proposals and requests go in the project's issues.

The API **only grows**: functions are added, never renamed or removed, and keep doing what this
page says, so students' programs and lessons keep working with every new MINE.
