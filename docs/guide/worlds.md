**English** · [Italiano](worlds.it.md)

# Class worlds

Like Minecraft Education's lesson worlds: each class gets **its own world** on the class server,
made by the teacher in a few seconds while everybody is playing. The 2B doesn't see what the 3A
built, and a new class can start from a clean copy of the same map.

The server's own world stays the **main world** (`main`): the teachers' room, where everybody
arrives the first time.

## Making a world

```
/mine world create 2b flat          a flat world, ready for the lab (/mine lab build)
/mine world create 2b map egypt     a copy of the map "egypt"
```

A class world is ready at once, and it is made for a lesson: **always midday, no rain, no
monsters**. Names are short: small letters, digits and `_` (up to 16).

## Maps

A map is any **Minecraft Java world**: downloaded from a map site, or built by you in single
player. Put its folder (the one with `level.dat` and `region` inside) in the `mine/maps` folder of
the server, with a simple name:

```
/opt/mine-server/mine/maps/egypt/level.dat
/opt/mine-server/mine/maps/egypt/region/...
```

`/mine world maps` lists the maps the server sees. A class world made from a map is a **copy**:
the map never changes, so the next class gets it as new. Students arrive where the map's
players arrived; outside the map there is nothing (void).

> Only the map's normal world is copied, not its Nether or End. Maps made with older versions of
> Minecraft are updated when they are opened, like in single player.

## Going there

```
/mine world go 2b              you go to the world 2b
/mine world go 2b @a           the whole class comes too
/mine world go main            back to the main world
/mine world spawn              students who come to this world arrive where you stand
/mine world                    which world am I in? (everyone)
```

Whoever goes to a class world comes back there if they fall or die. The worlds are there again
after a server restart, and so are the students in them.

## A lab in each world

Each world has its own [standard lab](server.md#3-the-standard-lab): build it once in the class
world, and the `/mine lab` commands are about the lab of the world you're in.

```
/mine world go 2b
/mine lab build 24             the 2B's lab, where you stand
/mine lab assign               the students who are in the 2B's world go to their lanes
/mine lab reset                clears the 2B's lab, and only that one
```

Lanes are kept per world: a student has their lane in the 2B's lab. Deleting a world deletes its
lab too.

## A code for each world

Each world has its own **class code**, as in Minecraft Education: a student who types the 2B's
code arrives straight in the 2B's world (on a lab lane, if that world has the lab).

```
/mine class code               the code of the world you're in (opens it if needed)
/mine class code 2b            the code of the world 2b, from anywhere
/mine class close 2b           closes the 2b code; /mine class close closes them all
/mine class admit Alex 2b      lets in a student who is waiting, into the world 2b
```

Several codes can be open at once (the 2B's and the 3A's, at the same time): they are always
different. The code is only asked to students who aren't in the class yet: the others come back
where they left, which is their world.

## Class rules

Each class world has its own rules, which the teacher changes **during the lesson**, standing in
that world. They change at once, only there: the 2B's free time doesn't bother the 3A.

```
/mine world rules              what is allowed here
/mine world rules lesson       nothing that gets in the way
/mine world rules free         free time: everything allowed
/mine world rules night on     one at a time: monsters, night, fire, explosions, pvp — on or off
```

| Rule | On | Off |
|---|---|---|
| `monsters` | hostile mobs (normal difficulty) | none: peaceful, the ones there disappear |
| `night` | day and night | always midday |
| `fire` | fire spreads | fire doesn't spread |
| `explosions` | TNT explodes, creepers break blocks | TNT does nothing, creepers break nothing |
| `pvp` | players can hurt each other | they can't |

A new world has monsters and night off, and the rest on, as in Minecraft. Everybody in the world
reads in chat what the teacher has changed. There is never rain: class worlds have the main
world's weather, and a class server keeps it clear.

## Deleting a world

```
/mine world remove 2b            asks first
/mine world remove 2b confirm    deletes it, for good
```

Everything in it is gone; whoever is in it goes back to the main world. To start a class over
from a map: delete its world and make it again from the same map.

## Commands

| Command | What it does |
|---|---|
| `/mine world list` | the class worlds, and how many players are in each |
| `/mine world maps` | the maps in `mine/maps`, and where that folder is |
| `/mine world create <name> flat` / `map <map>` | makes a class world |
| `/mine world go <name> [players]` | takes you, or the players, there (`main`: the main world) |
| `/mine world spawn` | where students arrive in the world you're in |
| `/mine world rules [lesson\|free\|<rule> on\|off]` | the rules of the world you're in |
| `/mine world remove <name> confirm` | deletes a class world |

All of them are for teachers, except `/mine world`.

## Zones

Parts of a world can be put back as they were after a lesson, or protected so that students can't
break them: see [Zones](zones.md).
