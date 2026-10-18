**English** · [Italiano](zones.it.md)

# Zones

A **zone** is a part of a world the teacher marks out: a village, a square, the stage of a
lesson. MINE keeps a **snapshot** of its blocks, so that after the lesson the teacher puts it
back as it was, in a second, with the students still in the game.

## Marking a zone

```
/mine zone corners          two corner blocks (magenta glazed terracotta) in your inventory
```

Place one at a corner of the area and the other at the **opposite corner**, on the ground: they
mark the area, as on a map. The zone also goes **10 blocks into the ground** (holes dug in it come
back filled) and **30 blocks up** (what is built on it). Each corner shows a column of light, seen
from far away (only by you), until the zone is made; after the second one, the zone's outline shows
for a few seconds, with its size. Then:

```
/mine zone create village         makes the zone, takes away the two corner blocks and takes its snapshot
/mine zone create village 3 50    3 blocks into the ground and 50 up, instead
```

A zone can have up to a million blocks (e.g. 100 × 100 × 100). Names: small letters, digits and
`_` (up to 24).

## Putting it back

```
/mine zone restore village  the zone goes back to its snapshot
/mine zone save village     a new snapshot: from now on it comes back as it is now
```

The restore puts back every block, chests' contents included, and takes away dropped items.
It leaves alone:

- the **Bytes**: the ones in the zone stay where they are, with their programs, and none is
  copied back where one was when the snapshot was taken;
- **NPCs, boards** and other entities (animals, armour stands…).

## Protected zones

```
/mine zone protect village on     students can't change its blocks
/mine zone protect village off    they can again
```

In a protected zone students can't break or place blocks, pour water or lava, light fire, till
or strip, and **neither can their Bytes** (a `byte.dig()` there doesn't dig). Explosions and fire
don't touch it either. **Doors, levers, buttons and chests keep working**: they don't change the
zone's blocks, and redstone lessons need them. Teachers, and their Bytes, change it as usual.

A student who tries is told, above the hotbar, that the zone is protected.

> Not covered: water or lava flowing in from outside the zone, and pistons pushing blocks into
> it. To be safe, keep the zone a little larger than what it protects.

## Other commands

| Command | What it does |
|---|---|
| `/mine zone list` | the zones, the world each is in, its size, and whether it's protected |
| `/mine zone show <name>` | shows the zone's outline for 15 seconds |
| `/mine zone remove <name>` | forgets the zone and its snapshot (the blocks stay as they are) |

All of them are for teachers, also from [command blocks](command-blocks.md) (e.g. a button that
puts the zone back). Zones belong to their world: deleting a [class world](worlds.md)
forgets its zones too.

