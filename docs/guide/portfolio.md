**English** · [Italiano](portfolio.it.md)

# Camera, portfolio and class gallery

Like the Camera and Portfolio of Minecraft Education: students take **photos** of what they
build, write **notes** on what they did and learnt, and the teacher collects everything. The
teacher also creates **class galleries**, where students share their best photos with everyone.

The camera and the portfolio window need **MINE in the student's game** (the MINE modpack):
only the game can take a picture of what the student sees.

## The camera

```
/mine camera                 a camera for yourself
/mine camera @a              teacher: a camera for everybody
```

Right-click with the camera: the game takes a photo of what you see, without the hotbar and the
hand, and saves it in your portfolio, with where and when you took it and the lesson step you
were at. A copy also stays on your computer, in the game's `screenshots/mine` folder.

## The portfolio

`/mine portfolio` opens your portfolio in a window: one photo or note at a time, with the arrows
(or the ← → keys) to go through them.

- **Caption**: a short sentence under the photo.
- **New note**: a page of text: what you did, what you learnt, what you'd change.
- **Share**: puts a copy of the photo in the class gallery (if the teacher has created one).
- **Delete**: removes it, after asking.

Each student sees only their own portfolio. The teacher sees everybody's:
`/mine portfolio Alex`, and can delete anything inappropriate.

## Class galleries

The teacher creates a gallery for an activity, for example the loops lesson of class 3B:

```
/mine gallery create loops_3B
```

From then on, students find **Share** in their portfolio: their photos go into `loops_3B`.
Everybody can look at it, with `/mine gallery`. In a gallery the teacher can also add notes (an
introduction, the task) and delete any entry; students can delete what they shared.

| | |
|---|---|
| `/mine gallery` | everybody: open the current gallery |
| `/mine gallery open <name>` | everybody: open another one |
| `/mine gallery list` | everybody: all the galleries |
| `/mine gallery create <name>` | teacher: a new gallery, where students share from now on |
| `/mine gallery use <name>` | teacher: students share with this gallery from now on |
| `/mine gallery remove <name>` | teacher: removes the gallery and its photos |

Names use letters, digits, `-` and `_`.

## Exporting

`/mine portfolio export` makes a **zip of web pages** to open in any browser, with no internet:
an index, a page for each student and one for each gallery. Each student's page has their photos
and notes, their answers to the lessons' reflection questions and to the NPCs' quizzes. Pages
print well, to keep or to show to families.

- The teacher gets everybody's portfolios and all the galleries.
- A student gets their own portfolio.

The zip is downloaded to your computer (Downloads folder), with a window to open it, and is
also saved on the server, in the world folder, in `mine/exports`.

## Where things are

Photos are on the server, in the world folder: `mine/portfolio/<student>/` and
`mine/portfolio/gallery-<name>/`; the rest is saved in the world. Back up the world folder and
you back up the portfolios too.

Limits: photos are made smaller (at most 1280 × 720, about 100–300 KB each); a portfolio or a
gallery holds up to 300 entries; one photo every 2 seconds.
