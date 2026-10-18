# Roadmap

What MINE doesn't do yet. Every item is open: if you'd like to work on one, open an issue to
say so, then a pull request (see [CONTRIBUTING.md](CONTRIBUTING.md)). Lessons are the most
welcome contribution of all, and the easiest to start with: they are JSON files and Markdown
guides ([how to write one](docs/guide/lesson-format.md)).

Before every new feature we ask one question:

> **Does it make a lesson better, one a teacher can run this week?**

## Lessons

- [ ] More lessons for older students in Python: algorithms (sorting, searching), data from
  the boards over a whole lesson, the Pico 2 W on both sides
- [ ] More lessons from Minecraft Education's catalogue that work well with Byte: sustainability
  and energy, digital citizenship, AI literacy
- [ ] Lesson steps that ask for a photo ("photograph your cube"), checked by the server with the
  [portfolio](docs/guide/portfolio.md)
- [ ] Lessons in more languages: today English and Italian; a translation is a language file and
  the guides

## Byte and the editors

- [ ] New `mine` Lua functions when a lesson needs them (the API only grows: nothing is removed)
- [ ] Python in the browser with the editor's tab in the background for a long time: check that
  the program keeps talking to Byte
- [ ] The **block editor** for the lessons that are Lua or Python only today (16, secret messages)

## Boards

- [ ] Windows lab PCs: COM ports, and MakeCode's WebUSB download while MINE has the port open
- [ ] `/mine device`: the window shows only boards on a cable; one on Wi-Fi is told in the chat,
  it could show its values live too
- [ ] Raspberry Pi Zero 2 W: GPIO with Python, as a board over the network
- [ ] Raspberry Pi 5: as a lab PC, and as the class server
- [ ] Linux lab notes for more distributions (serial ports and the `dialout` group: Ubuntu,
  school distributions)

## Classroom

- [ ] Tests on school PCs of different tiers: how many students a server holds, the lightest
  settings for old PCs

## Further away

- [ ] [Luanti](https://www.luanti.org) (a fully free engine with Lua mods): a parallel project
  rather than a port; shared lessons and the same Byte API in Lua, together with its community
