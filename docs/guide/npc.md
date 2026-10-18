**English** · [Italiano](npc.it.md)

# Teaching NPCs

NPCs are characters that talk to your students, like the NPCs of Minecraft Education: a guide
who explains the activity, a character from a story, a museum attendant… You write what they
say **in the game**, in a book and quill.

## Creating an NPC

Stand where you want it, facing that way, and type:

```
/mine npc create Ada
```

Ada appears two blocks in front of you, facing you. NPCs don't move, can't be hurt, and don't
trade: right-clicking them shows their dialogue instead. To rename one, use a name tag, as for
any Minecraft character.

Teachers (operators, permission level 2) and **builders** can create NPCs: see
[Students who build](#students-who-build). In a single-player world with cheats on, the player
is an operator, so they can create NPCs too.

## Writing the dialogue

**Right-click the NPC**: you see its dialogue, with **[✎ Edit]** next to its name. Click it and
MINE's window opens with the dialogue as it is: one page per screen, with buttons to turn, add and
remove pages. Write and press **Done**: *"Ada will now say the 3 pages of your book."*

### With a book and quill

The same works with a book, also in a game without MINE:

1. Take a **book and quill** from the Creative inventory (`E` key). Hold it and **right-click in
   the air**, not on the NPC, to open it and write in it. **Each page is one screen** of the
   dialogue: keep pages short, a few lines each.
2. Right-click the NPC holding the book. MINE answers: *"Ada will now say the 3 pages of your
   book."* You keep your book.

To change it later, **sneak and right-click the NPC with an empty hand**: you get a book with its
dialogue. Change it and give it back the same way.

### Buttons

Write a button like a link: `[label](target)`.

- `[Start the lesson](/mine lesson start loops)` — a **command**, run by the student who clicks,
  with their own permissions. Students can run the `/mine lesson` and `/mine byte` commands, so
  an NPC can start a lesson, give a hint or bring back Byte.
- `[Read more](https://example.org)` — a **web page**, which Minecraft asks before opening.

For example, a page like this:

```
Welcome to the lab! Today we build with loops.

[Let's start](/mine lesson start loops)
```

### Quizzes

A page becomes a **multiple-choice question** when it has answers written like a checklist:
`[ ]` for a wrong answer, `[x]` for a right one (more than one may be right).

```
How many blocks does this place?
for i = 1, 4 do byte.place() end

[ ] 3
[x] 4
[ ] 5
```

Students see the answers as buttons, `A) 3`, `B) 4`, `C) 5`. After a click the book opens
again on the same page, with the result under the question: *"✔ Right!"*, or *"✘ Not quite: try
again."*. Students go on, or back, with the book's arrows; the questions they got right keep
their ✔, also when they talk to the NPC again. A page without any `[x]` is a **poll**: every answer is fine
(*"Which part did you like most?"*).

A dialogue can mix pages: explanations, questions, a last page with a button to start the lesson.

A page can hold **more questions**, one after the other: after the answers of a question, the next
line starts the next one. Each result appears under its own question. A book page is small,
though: if the answers don't fit, use one page per question.

### Pages that follow the lesson

An NPC can follow each student's lesson: a hint for the step they are at, congratulations when
they finish. Write on the **first lines of a page** who it is for:

| first line | the page is for students… |
|---|---|
| `@lesson loops` | doing the lesson `loops` |
| `@lesson loops 2` | at step 2 of `loops` (or a range: `@lesson loops 2-3`) |
| `@done loops` | who have completed `loops`, also if they have moved on to another lesson |
| `@lesson none` | not doing any lesson right now |

More lines mean "any of them". Pages without these lines are for everybody. The name of a
lesson is the one in `/mine lesson start loops` (`/mine lesson list` shows them all). For example,
a guide for the loops lesson:

```
Hi! I'm Ada, I help with loops.          ← page 1, for everybody
```
```
@lesson loops 1
Count the blocks of one row: that's
the number in the for.                   ← page 2, only at step 1
```
```
@lesson loops 2
A square is four rows: a loop inside
a loop!                                  ← page 3, only at step 2
```
```
@done loops
Well done! Ready for a quiz?
[ ] ...                                  ← page 4, after the lesson
```

Each student sees only their pages, and talking to the NPC again later shows the pages for
where they are now. You (and the builder who made the NPC) see **every** page, each with who it
is for on top, in grey: to see it as a student does, use `/mine view student`. If MINE doesn't
understand a line, or the lesson or step doesn't exist, it tells you when you give the book
back, and the page shows it with ⚠.

## What students see

Students right-click the NPC: its dialogue opens in a **book window**, with the NPC's name at
the top, the book's arrows to turn the pages, and buttons they click with the mouse. Nothing is
added to their inventory, and they don't need anything installed: an NPC is a Minecraft
villager, and the dialogue lives on the server. Only one thing needs MINE in the student's game
(the MINE modpack): after answering a question, the book opens again **on the same page**.
Without it, the book opens again starting from that page, and the pages before it can't be read.

## Quiz results

Stand next to the NPC and type `/mine npc results`. For each question you see how many students
got it right at the first try and, for each student, their **first** answer (what they knew)
and how many tries they needed to get it right:

```
How many blocks does this place?
   9 of 12 right at the first try
   Alex: ✔ B) 4
   Sam: ✘ A) 3 — right after 2 tries
   Kim: ✘ C) 5 — not right yet
```

For polls, you see each student's latest choice. Teachers can also read the results from
anywhere with the NPC's name, `/mine npc results Ada`, even after the NPC has been removed.
Builders read the results of their own NPCs.

Answers are saved in the world. If you **rewrite a question**, it counts as a new one: the
answers to the old text stay under it. To use an NPC again with another class, clear its
answers with `/mine npc clear`, next to it.

Students answer by clicking in the book, next to the NPC: they can't answer from anywhere by
typing the command.

### Downloading the answers

`/mine npc export` saves **every quiz answer**, of all NPCs, in a CSV file you can open in a
spreadsheet (LibreOffice Calc, Excel, Google Sheets) to sort, filter and make charts. There's a
row for each student and question:

| column | |
|---|---|
| `npc`, `page`, `question` | which question (page of the NPC's dialogue) |
| `type` | `quiz`, or `poll` |
| `student` | the student's name |
| `first_answer`, `first_answer_text` | their first answer (`A`, `B`…) and its text; for polls, their latest |
| `first_right` | `true` if the first answer was right |
| `solved`, `tries` | whether they got it right in the end, and after how many tries |
| `time` | when they first answered |

The file is saved in the world folder, in `mine/exports`. If your game has MINE installed (the
MINE modpack), it is also **downloaded to your computer**, in your Downloads folder: the game
opens a window with the buttons **Open file** and **Open folder** (the chat keeps the file's
name, to find it again later). The file uses commas between columns: if your spreadsheet
asks when opening it, choose *comma* and *UTF-8*.

## NPCs and boards in datapacks

Lessons can come with their own characters and boards, ready to be placed: MINE has a guide and
a board for the loops lesson.

```
/mine npc place mine:loops_guide
/mine board place mine:loops
```

The guide, Ada, follows the lesson: she offers to start it, gives a tip and a hint button at
each step, and asks a question and a poll at the end. Every student reads her **in their own
language**. She stays linked to her file: when the file changes (after `/reload`), she says the
new dialogue. If you give her a book of your own, she says yours instead (MINE tells you), and
you can start from hers: sneak and right-click her with an empty hand.

Boards from datapacks are written in the language of whoever places them, and are then like any
other board.

### Writing them

They are JSON files in a datapack, next to the lessons: `data/<namespace>/mine_npc/<id>.json`
and `data/<namespace>/mine_board/<id>.json`. Pages and lines are written as in the book, with
`\n` for a new line; one list per language (or a single list for every language):

```json
{
	"format": 1,
	"name": "Ada",
	"pages": {
		"en_us": ["Hi! I'm Ada.", "@lesson loops 1\nA tip for step 1."],
		"it_it": ["Ciao! Sono Ada.", "@lesson loops 1\nUn consiglio per la tappa 1."]
	}
}
```

```json
{
	"format": 1,
	"size": 2,
	"text": {
		"en_us": ["# Loops", "for i = 1, 10 do", "end"],
		"it_it": ["# I cicli", "for i = 1, 10 do", "end"]
	}
}
```

Students whose language isn't there read English, or whichever language the file has. Keep the
same pages in every language: answers to questions are saved by page. After changing a file,
type `/reload`; a file with a mistake is skipped, with the reason in the server log. `"format": 1`
is optional, as in [lessons](lesson-format.md#what-a-lesson-contains). MINE's own
are in [`src/main/resources/data/mine/`](../../src/main/resources/data/mine/).

## Removing an NPC

Stand next to it and type `/mine npc remove`: it removes the nearest NPC within 8 blocks (for a
builder, the nearest of their own).

## Students who build

Students may want to build with NPCs too: a museum with guides, a story with characters, a
quiz trail for their classmates. Making them operators would give them every power in the game,
so MINE has a role in between, like the *World Builder* of Minecraft Education:

```
/mine builder add Alex Sam
/mine builder list
/mine builder remove Sam
```

| | Student | Builder | Teacher |
|---|---|---|---|
| reads NPCs | ✓ | ✓ | ✓ |
| creates NPCs | | ✓ | ✓ |
| writes, takes back and removes NPCs | | only their own | all of them |
| reads and clears quiz results | | only their own NPCs | all of them |
| `/mine builder`, lesson commands for the class | | | ✓ |

The role is saved in the world. When you remove it, the student can still read their NPCs but
no longer change them; you can always change or remove any NPC.

## Seeing what students see

To try your NPCs and lessons as a student, type `/mine view student`: MINE treats you like any
student (NPCs only to read, no teacher or builder commands) until you type `/mine view teacher`,
or the server restarts. Minecraft's own operator powers, such as `/gamemode`, stay: to see the
game exactly as a student does, use an account that isn't an operator.

## Boards

For large text in the world, see [Boards](boards.md).
