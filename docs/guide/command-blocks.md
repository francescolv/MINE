**English** · [Italiano](command-blocks.it.md)

# Command blocks

With **command blocks** the teacher builds interactive worlds: a button that starts the lesson, a
pressure plate that takes students to another world, a lever for free time, a sign that gives the
materials. Students don't write them: they **press** buttons, levers and plates, and the command
blocks do the rest.

## Turning them on

On a server installed with MINE's package they are already on. On a server installed before,
turn them on once:

```bash
sudo sed -i 's/^enable-command-block=.*/enable-command-block=true/' /opt/mine-server/server.properties
sudo systemctl restart mine-server
```

In single player or with *Open to LAN*, commands must be allowed (*Allow Commands*: on).

## Placing one

Only a **teacher** (operator), in **Creative**, can place a command block and write in it; students
can't open them, nor break them in a [protected zone](zones.md). To get one:

```
/give @s command_block
```

Right-click it to write the command (with or without the `/`). The buttons at the bottom:

| Button | Choices |
|---|---|
| **Impulse / Chain / Repeat** | *Impulse*: once, each time it gets power. *Chain*: after the block pointing at it. *Repeat*: every tick, while it has power |
| **Conditional / Unconditional** | in a chain, *Conditional* runs only if the block before it succeeded |
| **Needs Redstone / Always Active** | *Needs Redstone*: a button, lever or plate starts it. *Always Active*: it runs by itself |

Put a button on the command block, or next to it, or connect a plate with redstone dust.

## Who the command is for

| Selector | Who |
|---|---|
| `@p` | the nearest player: the student who pressed the button |
| `@a` | everyone |
| `@a[distance=..10]` | everyone within 10 blocks |

## Recipes

| What | Command | With |
|---|---|---|
| Start the lesson for the student | `mine lesson start loops @p` | a button |
| Start it for the whole class | `mine lesson start loops @a` | a button near the teacher's desk |
| A portal to another class world | `mine world go 3a @p` | a pressure plate |
| Back to the main world | `mine world go main @p` | a pressure plate |
| Free time / lesson time | `mine world rules free` / `mine world rules lesson` | two buttons, out of the students' reach |
| Put the village back as it was | `mine zone restore village` | a button for the teacher |
| The materials of the activity | `give @p oak_planks 64` | a button |
| A camera for the portfolio | `mine camera @p` | a button |
| A message for the student | `tellraw @p "Well done! Now go to the next room."` | a pressure plate |
| Move a few steps | `tp @p ~ ~ ~5` | a pressure plate |

`/mine world rules`, `/mine class code` and `/mine lab` from a command block are about the world
the command block is in, as when the teacher types them there.

## Careful

- A command block has the **teacher's powers**: whoever presses its button runs its command. Put
  the buttons that only the teacher should press (free time, restoring a zone) where students
  can't reach them.
- Put the command blocks in a [protected zone](zones.md): students can't break them, but they can
  still press the buttons, which is what they're for.
- Never make students operators to let them use command blocks: an operator can do anything on the
  server.
- In class worlds, command blocks don't write in the chat what they did. In the main world:
  `/gamerule command_block_output false`.
- Nothing happens? Check that command blocks are on (above), that the block is on *Needs Redstone*
  with a button that powers it, and the command's spelling: in the block's window, *Previous Output*
  shows the error.
