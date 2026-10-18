**English** · [Italiano](README.it.md)

# Lesson 5 — Variables and functions

| | |
|---|---|
| **Duration** | 1 hour (50–60 minutes) |
| **Age** | 10–14 years old (also fine for the first year of secondary school) |
| **Concepts** | variable, counter, value from outside (`mine.challenge()`), function, parameter |
| **Prerequisites** | [lesson 2](../02-loops/README.md) (loops) and [lesson 4](../04-conditions/README.md) (`while`) |

## Goals

By the end of the lesson students can:

1. use a **variable** to remember a number and change it (`steps = steps + 1`);
2. write a program that works with a number it only finds out **while running**;
3. give a name to a piece of program with `function`, and reuse it with different
   **parameters**;
4. explain why a function saves lines and mistakes.

## Like lesson 4, MINE changes the challenge

The wall is at a different distance every time, and the square MINE asks for changes size every
time: a number written by hand in the program won't work twice. The program has to **count**,
or **read** the number: that's what variables are for.

The size of the square comes from a new instruction: `mine.challenge()` gives the program what
the step asks this time (here, the side of the square). It is chosen when the program starts.

## Preparation (teacher, before the lesson)

- A **superflat world in Creative mode**, students side by side, all facing the same way, about
  **8 blocks apart**, with at least **10 free blocks in front** of each Byte.
- The quickest way: the **standard lab** (`/mine lab build <students>`, then `/mine lab assign`)
  has exactly these lanes ([guide](../../guide/server.md#3-the-standard-lab)); `/mine lab reset`
  clears it after the lesson.
- Nothing else: when the lesson starts MINE creates a Byte for any student who doesn't have
  one, builds the first wall, and gives **64 planks** at steps 2 and 3.
- Try the three steps yourself, with the programs in [`solutions/`](solutions/): 10 minutes.

## Missions: MINE checks the steps

At the start of the lesson, type:

```
/mine lesson start functions @a
```

| Step | MINE… | Done when… |
|---|---|---|
| 1 · `count` | builds a wall 3 to 8 blocks ahead (a new one after every failed attempt) | Byte stopped right in front of the wall and **said** how many steps it took — that number and no other |
| 2 · `square` | chooses the side of a square, 2 to 5, each time a program starts: `mine.challenge()` | the program built a solid square floor of that size |
| 3 · `pyramid` | — | there is a step pyramid: a 5 × 5 square, a 3 × 3 square centred on it, one block on top (35 blocks) |

Saying every number from 1 to 9 "to be sure" doesn't pass step 1: MINE wants only the right one.
When something isn't right, MINE says so, for example *"this time Byte took 5 steps to
reach the wall"* or *"this time mine.challenge() asked for a square 4 × 4, and your Byte built
12 blocks, 4 long and 3 wide"*.

The other commands are the same as in lesson 1 (see the
[lesson 1 guide](../01-first-steps/README.md#missions-mine-checks-the-steps)).

## With blocks

The whole lesson works in the [block editor](../../guide/blocks.md) too (`/mine blocks`, then
**Run ▶**). Variables and functions are Blockly's own: **Variables** → *Create variable...*, and
**Functions** → *to do something*, whose ⚙ adds parameters.

| Activity | With blocks | File |
|---|---|---|
| 1 · Counting the steps | see below | [count.json](solutions/blocks/count.json) |
| 2 · The square MINE asks for | `set side to the challenge's number`, then the floor of lesson 2 with `side` in every `repeat` | [square.json](solutions/blocks/square.json) |
| 3 · The pyramid | see below | [pyramid.json](solutions/blocks/pyramid.json) |

```
set steps to 0                              to square with: side
repeat while not block in front?                repeat side times
    move forward                                    repeat side times
    change steps by 1                                   place a block below
say create text with "I took "                          move forward
                     steps                          repeat side times
                     " steps"                           move back
                                                    turn right
move up                                             move forward
square with side 5                                  turn left
nextLayer                                       turn left
square with side 3                              repeat side times
nextLayer                                           move forward
square with side 1                              turn right
say "Pyramid done: 25 + 9 + 1 = 35 blocks!"
                                            to nextLayer
                                                move up, move forward, turn right,
                                                move forward, turn left
```

With blocks, a function is a block of its own that appears in the **Functions** group as soon as
it's made: students literally see that they have added a new instruction to Byte. The
Italian solutions are in [`solutions/it/blocks`](solutions/it/blocks).

## Lesson plan

### Introduction — 5 minutes, no computers

Ask a student to walk to the classroom wall while the class counts the steps out loud. To
count, everyone keeps **one number in their head** and adds 1 at every step. That number is a
**variable**: a box with a name, holding a value that can change.

Then: "you know how to make a sandwich. If I say *make three sandwiches*, I don't explain the
recipe three times". A recipe with a name is a **function**; "with ham" or "with cheese" is a
**parameter**.

### Activity 1 — Counting the steps (15 minutes)

**Task:** Byte walks up to the wall (MINE has already built it) and says how many steps it
took.

```lua
local steps = 0
while not byte.detect() do
  byte.forward()
  steps = steps + 1
end
mine.say("I took " .. steps .. " steps")
```

Things to explain:

- `local steps = 0` creates the variable and puts 0 in it. `local` means "it belongs to this
  program".
- `steps = steps + 1` reads like an order, not like maths: "take the value of steps, add 1, put
  the result back into steps".
- `..` joins text and numbers, as in lesson 1.

Ask them to **predict** the number before running the program, by looking at the wall. Then run
it twice: the wall moves, the program doesn't change, and the number is still right.

Solution: [`solutions/count.lua`](solutions/count.lua).

### Activity 2 — The square MINE asks for (15 minutes)

**Task:** MINE chooses the side of a square every time; build it.

```lua
local side = mine.challenge()
mine.say("Side: " .. side)
```

The rest is the floor of lesson 2, with `side` in place of the numbers:
`for row = 1, side do ... end` and `for i = 1, side do ... end`.

Discussion questions:

- How many blocks does a square with side 3 take? And with side 5? (The area: side × side.)
- Why can't we write `4` in the program? (Because next time MINE might ask for 2.)

Solution: [`solutions/square.lua`](solutions/square.lua).

### Activity 3 — The pyramid, with a function (15 minutes)

**Task:** a step pyramid: 5 × 5, then 3 × 3 on top, then one block.

Writing the square program three times would be long, and easy to get wrong. Instead, give it a
name:

```lua
function square(side)
  -- the square program, using side
end
```

Now `square(5)`, `square(3)` and `square(1)` build the three layers. Inside the function,
`side` is 5 the first time, 3 the second, 1 the third: it's the **parameter**.

Two details make it work:

- the function must bring Byte **back to the corner where it started**, so that the next
  layer knows where it is (after the rows: turn left, forward `side` times, turn right);
- between two layers Byte goes up one block and moves one block forward and one to the
  right, so the smaller square is centred. That can be a function too: `nextLayer()`.

Solution: [`solutions/pyramid.lua`](solutions/pyramid.lua).

### Extra challenge — For early finishers

- A 7 × 7 pyramid: how many blocks does it take? (49 + 25 + 9 + 1 = 84: more than one stack!)
- A function `tower(height)` and a row of towers 1, 2, 3, 4, 5 blocks tall: a staircase.

### Wrap-up — 5 minutes

- What is a variable? Give an example from today.
- Where did the value of `side` come from in activity 2? And in activity 3?
- The reflection question MINE shows at the end: *In the pyramid, the function square(side) did
  three different jobs. How many lines did it save you? What else could you build with it?*

## Common problems

| What happens | Why | What to do |
|---|---|---|
| `attempt to perform arithmetic on a nil value (global 'steps')` | the variable was never created, or its name is spelled differently | `local steps = 0` before the loop, and the same name everywhere |
| Byte says "0 steps" | `steps = steps + 1` is outside the loop | it goes inside, after `byte.forward()` |
| MINE says Byte took 5 steps, but it said 6 | the counter goes up also when Byte can't move, or starts from 1 | start from 0 and count only real steps |
| `attempt to concatenate a nil value` with `mine.challenge()` | the program ran outside step 2, where MINE doesn't ask anything | check with `/mine lesson status` that the student is on step 2 |
| The square is the right size only sometimes | a number is written by hand instead of `side` | every `4` becomes `side` |
| `attempt to call a nil value (global 'square')` | the function is used before it is written | `function square(side) ... end` goes at the top of the program |
| The layers of the pyramid are shifted | the function doesn't bring Byte back to its corner, or the move between layers is missing | see the two details in activity 3 |
| MINE needs free blocks in front of Byte | something is in the way of the wall | `/mine byte recall` from an open spot |

## For the teacher: what to look for

- **Can they read `steps = steps + 1` aloud** and explain what happens?
- **Do they predict the number** of steps or blocks before running the program?
- **Do they test the function** on a single square before building the whole pyramid?

## References

- [Lesson 4 — Conditions](../04-conditions/README.md)
- [MINE Lua API](../../guide/lua-api.md): `mine.challenge()`, `mine.say()`.
- The solutions are in [`solutions/`](solutions/) (Italian versions in [`solutions/it/`](solutions/it/))
  and are tested automatically every time MINE changes.
