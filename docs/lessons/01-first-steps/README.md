**English** · [Italiano](README.it.md)

# Lesson 1 — First steps with Byte

| | |
|---|---|
| **Duration** | 1 hour (50–60 minutes) |
| **Age** | 10–14 years old (also fine for the first year of secondary school) |
| **Concepts** | instruction, sequence, program, direction |
| **Prerequisites** | being able to move around in Minecraft; no programming experience |

## Goals

By the end of the lesson students can:

1. give Byte an instruction and see what happens;
2. write, save and run a program;
3. explain that a program is a **sequence** of instructions carried out **in order**, one at a time;
4. predict where Byte will end up after a sequence of moves.

## Preparation (teacher, before the lesson)

- A server with MINE and CC: Tweaked (see the [README](../../../README.md#requirements)).
- A **superflat world in Creative mode**, so nobody runs out of blocks.
  For now the lesson has no dedicated world: any wide, flat area works, with at least
  5 blocks of free space between students.
- Try activities 2–4 yourself: it takes 10 minutes and you will spot any keyboard or
  permission problems straight away.
- If you use laptops with touchpads, read the [lab setup guide](../../guide/linux-lab.md):
  a touchpad right-click can break Byte instead of opening it.

## Missions: MINE checks the steps

Activities 2–4 are also **missions** that the server checks by itself, so you can see at a
glance who is stuck. At the start of the lesson, type:

```
/mine lesson start first_steps @a
```

`@a` is every student online: teachers (the operators) are left out. To try the lesson yourself,
`/mine lesson start first_steps` without `@a`.

Every student gets the goal of the first step in chat; students who don't have a Byte yet
get one in front of them (in activity 1 `/mine byte` replaces it, which is fine). Each time one of their programs ends
without errors, MINE looks at what their Byte really did and, if the step is done, moves them
to the next one with a title and a sound:

| Step | Done when… |
|---|---|
| 1 · `hello` | the program made Byte speak with `mine.say` |
| 2 · `roundtrip` | Byte went at least 3 blocks away and came back to where it started, facing the same way |
| 3 · `tower` | there is a 3-block tower right in front of where Byte started, and Byte is back on the ground |

MINE doesn't read the code: any program that does the job counts, whatever its name or the
message it says. Commands typed one at a time in `lua` don't count — saving a program is the
point of activity 2.

| Command | Who | What it does |
|---|---|---|
| `/mine lesson status @a` | teacher | where every student is, e.g. "Alex — First steps with Byte: step 2 of 3" |
| `/mine lesson status` | student | their own step and its goal |
| `/mine lesson hint` | student | the next hint for their step (two per step, then "ask your teacher") |
| `/mine lesson restart` | student | starts the lesson over from step 1 |
| `/mine lesson reset <students>` | teacher | forgets their progress (not their answers) |
| `/mine lesson answer <text>` | student | at the end: their answer to the reflection question |
| `/mine lesson answers <lesson>` | teacher | every answer to that lesson's question, also from students who have left |

"Teacher" means an operator (permission level 2): `/op <name>` on the server. Progress is saved
with the world, so it survives a server restart. If a student breaks their Byte and creates a
new one, they keep their progress.

At the end each student sees "Mission complete!" and the reflection question: *Why is it useful
to save instructions in a program instead of typing them one at a time?* They can answer it in
the chat with `/mine lesson answer` (the message is clickable). `/mine lesson status @a` shows
each answer under the student's line, and `/mine lesson answers first_steps` lists them all, so
you can read a few aloud in the wrap-up. Answers stay saved in the world for every lesson, also
after a reset or when the class moves on to the next lesson.

## With blocks

Students who don't write Lua yet can do the whole lesson in MINE's
[block editor](../../guide/blocks.md): they type `/mine blocks` in the chat, put the blocks
together in the browser and press **Run ▶**. The missions look at the world, so they count the
same. Next to the blocks, the editor shows the Lua they make: the same Lua as below, which makes
the move to text easier later.

| Activity | With blocks | File |
|---|---|---|
| 1 · The first command | one block on its own, e.g. `move forward`, then **Run ▶** | |
| 2 · `hello` | `say "Hi! I'm your Byte."` · `say "I'm ready for instructions."` | [hello.json](solutions/blocks/hello.json) |
| 3 · There and back | see below | [roundtrip.json](solutions/blocks/roundtrip.json) |
| 4 · The tower | see below | [tower.json](solutions/blocks/tower.json) |

```
repeat 3 times                      place a block in front
    move forward                    move up
repeat 2 times                      place a block in front
    turn left                       move up
repeat 3 times                      place a block in front
    move forward                    repeat 2 times
repeat 2 times                          move down
    turn left                       say "Tower done!"
say "I'm back!"
```

The program's name is the one at the top of the editor (`blocks` at first): students give each
activity its own (`hello`, `roundtrip`, `tower`) so they find them again under **My programs**.
The files open with **Open file**, e.g. to show a solution on the projector. The Italian
solutions are in [`solutions/it/blocks`](solutions/it/blocks).

## Lesson plan

### Introduction — 5 minutes, no computers

Ask a student to give a classmate directions to walk from the door to the teacher's desk.
Write the instructions on the board as they go: "3 steps forward", "turn right"…

Point out that:

- each instruction is **simple** and **precise** ("go over there" doesn't work);
- **order** matters: "turn, then forward" leads somewhere different from "forward, then turn";
- a list of instructions in order is a **program**. The computer follows it to the letter.

### Activity 1 — The first command (10 minutes)

1. Each student types `/mine byte` in the chat: their Byte appears in front of them.
2. Right-click Byte: its screen, the **terminal**, opens.
3. Type `lua` and press Enter. Now Byte runs every line we type.
4. Try these, one at a time:

   ```lua
   byte.forward()
   byte.turnLeft()
   byte.up()
   byte.down()
   ```

5. Each instruction answers `true` (done) or `false` (I couldn't do it). Ask:
   *when does it answer `false`?* (when there is a block in front, or below).
6. To leave `lua` mode, type `exit()`: the prompt goes back to `>` instead of `lua>`.

> **If Byte wanders too far:** `/mine byte recall` brings it back in front of the student,
> with all its programs.

### Activity 2 — The first program: `hello` (10 minutes)

Typing instructions one at a time is tedious: a program saves them so we can reuse them.

1. Check that the prompt is `>` and **not** `lua>` (otherwise type `exit()` first).
   Then type `edit hello`: the screen clears and `Press Ctrl to access menu` appears at the bottom.
2. Type:

   ```lua
   mine.say("Hi! I'm " .. mine.owner() .. "'s Byte.")
   mine.say("I'm ready for instructions.")
   ```

3. Press and release **Ctrl** (just once). No window opens: the **bottom line** changes and
   becomes `[Save]  Exit`. The item in square brackets is the selected one: press Enter to save.
   Then **Ctrl** again, the **→** arrow to `Exit`, and Enter.
4. Still in Byte's terminal, type `hello` and press Enter: Byte writes its messages in
   the terminal and in the chat. To see the chat, close the terminal with Esc.

> **Watch out:** typing `hello` in the Minecraft chat (T key) starts nothing: it's just a
> message. From the chat, a program is started with `/mine byte run hello`.

Things to explain:

- `mine.say(...)` makes Byte write a message in the chat. Players nearby can read it.
- `mine.owner()` is the name of whoever owns Byte: everyone sees their own name.
- Quotes `"..."` wrap a piece of **text**; `..` joins two pieces of text together.
- The two messages **always** arrive in that order: it's a sequence.

To vary it: have everyone write a three-line introduction with a message of their choice.

### Activity 3 — There and back (10 minutes)

**Task:** Byte takes 3 steps forward, goes back to where it started and faces the
original direction again. At the end it says "I'm back!".

Before writing the program, ask students to **predict** the sequence on paper (or on the
board). Then `edit roundtrip`, write it, save it and run it with `roundtrip`.

Discussion questions:

- How many times do you need to turn to go back? (twice to the left, or twice to the right)
- If we remove the last two turns, does Byte still get back? (yes, but it faces the wrong way)

To watch Byte while it moves, it's best to start it **from the chat**: close its screen
with Esc and type `/mine byte run roundtrip`. When it's done, MINE says so in the chat.

Solution: [`solutions/roundtrip.lua`](solutions/roundtrip.lua).

### Activity 4 — The tower (15 minutes)

**Task:** Byte builds a 3-block-tall tower in front of itself, then comes back down to
the ground.

Before starting, Byte needs blocks. Right-click it: the **4×4 grid on the right** is its
inventory (the one on the left belongs to the student). Drag at least 3 blocks into the
**top-left slot of the 4×4 grid**, the highlighted one, **all in that same slot**:
`byte.place()` only uses the highlighted slot, and doesn't move on to the others when it's empty.

The new instructions:

| Instruction | What it does |
|---|---|
| `byte.place()` | places a block in front of Byte |
| `byte.placeUp()` / `byte.placeDown()` | places a block above / below |

Hint, only if needed: "place a block, go up, place a block…".

Solution: [`solutions/tower.lua`](solutions/tower.lua).

A question for discussion, if it happens to someone: why did Byte say "Tower done!" even
though there was no tower? (When `byte.place()` can't place the block it doesn't raise an
error: it returns `false`, and the program goes on. We'll use that in [lesson 4](../04-conditions/README.md).)

### Extra challenge — For early finishers

Build a tower **10 blocks tall**. How many lines do you need? What happens if you write
one line too few?

Let them find out on their own: it's boring and it's easy to miscount. That's exactly the
problem we will solve in lesson 2 with **loops**.

### Wrap-up — 5 minutes

Go back to the board from the introduction and ask:

- What is a program? (a sequence of instructions)
- Why does order matter? (give an example with the tower: going up before placing the block)
- What was the most annoying thing to write? (link to lesson 2)

## Common problems

| What happens | Why | What to do |
|---|---|---|
| `attempt to call field ... (a nil value)` or `attempt to index global ...` | misspelled name: `byte.foward()`, `Turtle.forward()` | capitalisation matters: everything lowercase except `turnLeft`/`turnRight` |
| Pressing Ctrl "does nothing" | the menu is on the bottom line, not in a window; a second Ctrl closes it again | press Ctrl once and look at the bottom: `[Save]  Exit` |
| `Save` is missing from the menu | the file is read-only (for example a file in `rom/`) | open your own program with `edit name` |
| `edit hello` gives an error and Ctrl opens no menu | you are still in `lua` mode (`lua>` prompt), where `edit` doesn't exist | `exit()`, then `edit hello` |
| After typing `hello` in the chat, Byte does nothing | the Minecraft chat doesn't start programs | type it in Byte's terminal, or `/mine byte run hello` in the chat |
| `/mine byte run` says Byte is busy | a program is still running, or the editor or `lua` is open | in Byte's terminal: hold Ctrl+T for a second, or leave the editor / `exit()` |
| `No such program` | the program wasn't saved, or its name is different | `ls` lists the saved programs |
| Byte doesn't move and answers `false` | there is a block in front of it | move Byte to free space with `/mine byte recall` |
| The tower doesn't appear: Byte goes up and down but places no blocks | Byte has no blocks in the selected slot: they often stayed in the student's inventory | drag them into the highlighted slot of the 4×4 grid on the right |
| The tower stops at the first block | the blocks are spread over several slots (one per slot): `byte.place()` only uses the highlighted one | put them all in the highlighted slot |
| Right-click breaks Byte | touchpad: tapping the bottom-right corner is a left click | see the [lab setup guide](../../guide/linux-lab.md) |
| Byte has disappeared | broken by mistake | `/mine byte` creates a new one (the old one's programs are lost) |

## For the teacher: what to look for

- **Do they predict before trying?** A student who writes the sequence on paper before running
  it is already thinking like a programmer.
- **Do they fix things on their own?** When Byte does something unexpected, can they find
  the wrong line?
- **Do they use the saved program** instead of retyping everything in interactive `lua`?

## References

- In game: `help mine` and `help byte` in Byte's terminal.
- [MINE Lua API](../../guide/lua-api.md)
- The solutions are in [`solutions/`](solutions/) (Italian versions in [`solutions/it/`](solutions/it/))
  and are tested automatically every time MINE changes: if a lesson stops working, we notice.
