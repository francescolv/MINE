**English** · [Italiano](blocks.it.md)

# Programming Byte with blocks

Students who aren't ready for Lua yet can program their Byte with **blocks**, as in Scratch or
Minecraft Education's MakeCode: they put blocks together in their browser, see the Lua those
blocks make, and send the program to their Byte. The lessons check what happens in the world, so
they work the same with blocks and with Lua.

The editor is a web page served by the class server ([Blockly](https://github.com/RaspberryPiFoundation/blockly),
included in MINE: no internet needed). It works in any browser of the lab PCs.

## Opening it

In the game, press **T** to open the chat and type:

```
/mine blocks
```

The editor opens in the browser, with the student's name at the top. The link is personal: it
sends programs to **that student's Byte**, and it stops working when they leave the game (type
`/mine blocks` again next time).

> `/mine blocks` goes in Minecraft's **chat**, not in Byte's terminal: the terminal runs
> programs, such as the ones made with blocks.

## The blocks

| Group | Blocks |
|---|---|
| **Byte** | move forward/back/up/down, turn left/right, place a block, dig, block in front/above/below?, block … same as the slot's?, use slot, blocks in the slot, say, the challenge's number |
| **Redstone** | redstone signal [side] set to true/false, signal from [side]?, wait for a redstone change |
| **Board** | wait for [key] from the board, board value [key], send to the board [key] [value], board connected? (see [boards](devices.md)) |
| **Loops** | repeat N times, repeat while/until, count from … to …, wait N seconds |
| **Logic** | if … then … else, compare, and/or, not, true/false |
| **Math** | numbers, + − × ÷, random number |
| **Text** | text, join |
| **Variables** and **Functions** | make your own (Blockly's) |

On the right, the **Lua your blocks make**, updated at every change: it's the same Lua of the
lessons, a bridge from blocks to text (the **Lua** button hides it).

## Send and Run

- **Run ▶**: go back to Minecraft. The program starts as soon as you are there, after
  **3, 2, 1**, so you see your Byte move. If you don't come back within a minute, it doesn't start.
- **Send to Byte**: the program is written on Byte with the name at the top (e.g.
  `blocks`, `square`); in its terminal, type that name to run it.
- In the game, **R** runs again the last program Byte ran, from the blocks or typed in its
  terminal (the key can be changed in Controls, under MINE). So you can write a program in the
  terminal, run it once, close the terminal with **Esc** and press **R** to watch Byte work.

The game doesn't pause while the browser is in front. If Byte is off (e.g. the server
restarted), MINE turns it on.

## Programs

Every program sent is kept on the server, with its name. **My programs** opens them again; **+ New
program** starts from scratch. Using a name already used replaces that program.

**Teachers** also see **Students' programs…**: a student's program opens to be looked at (to help
them, or to show it to the class); **Copy to my programs** makes a copy to change and run on the
teacher's own Byte.

## Lessons

Every lesson can be done entirely with blocks: each guide has a "With blocks" part
([lesson 1](../lessons/01-first-steps/README.md#with-blocks), [2](../lessons/02-loops/README.md#with-blocks),
[3](../lessons/03-debugging/README.md#with-blocks), [4](../lessons/04-conditions/README.md#with-blocks),
[5](../lessons/05-functions/README.md#with-blocks), [6](../lessons/06-redstone/README.md#with-blocks),
[7](../lessons/07-sensors/README.md#with-blocks))
with the programs, and the solutions open with **Open file** (**Save file** saves the blocks on the
PC, e.g. to share a program). A lesson can also give students block programs, which appear in
**My programs** when it starts: lesson 3 gives the broken programs to fix.

### The lesson in the editor

During a lesson, a green bar under the buttons shows the lesson, the step and its goal, in the
blocks' words. **Hint** shows the step's hints one at a time, also written for blocks (they count
as the hints of `/mine lesson hint`). When the student comes back to the browser from the game, the
bar shows the new step, and **My programs** the programs a lesson has just given.

## Tips

- With a small screen, put the browser and Minecraft side by side: Byte can be seen moving
  while the blocks are on screen.
- A lesson step that isn't done yet says so after every program, with blocks too ("Not yet: …").

## For the technician

The editor uses **port 25580** of the server: the installer opens it in the firewall. It only runs
on a class server (MINE's server package), not in a single-player world. If the page doesn't open,
check that the lab PCs reach the server on that port (e.g. `http://192.168.1.20:25580` in a
browser shows the editor, asking for `/mine blocks`).
