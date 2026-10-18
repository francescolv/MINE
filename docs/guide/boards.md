**English** · [Italiano](boards.it.md)

# Boards

Boards show large text in the world, like the boards of Minecraft Education: the title of an
activity, instructions, the rules of the lab, a piece of code to remember. Like NPCs, you write
them **in the game**, in a book and quill. Students don't need anything installed: a board is
made of Minecraft's own text displays.

## Creating a board

Stand where you want it, facing that way, and type:

```
/mine board create
```

An empty board appears two blocks in front of you, facing you. Its size is 2; you can choose
another one, from 0.5 to 8: `/mine board create 4`.

## Writing it

**Right-click the board** with an empty hand, anywhere on its text and from up to 10 blocks away:
MINE's window opens with the text as it is. Write it and press **Done**. Lines starting with `# `
are **titles** (yellow, bold). Long lines wrap by themselves, at about 25 letters.

```
# Lab rules
Build only in your area.
Ask before using TNT.
Have fun!
```

It works the same in a **book and quill**, also in a game without MINE: write the text in the
book (hold it and right-click in the air to open it), then right-click the **middle** of the board
holding it. To get the board's text in a book, **sneak and right-click it with an empty hand**.

## Size and removal

Stand next to it and type `/mine board size 3` to change its size, `/mine board remove` to
remove it: both act on the nearest board within 8 blocks.

Teachers and builders create boards; builders change and remove only their own, as for NPCs
(see [Students who build](npc.md#students-who-build)).

## Boards from datapacks

Lessons can come with their boards ready: `/mine board place mine:loops` places the board of
the loops lesson, in your language. See
[NPCs and boards in datapacks](npc.md#npcs-and-boards-in-datapacks).
