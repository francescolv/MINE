-- MINE: start the Agent's programs from chat, with /mine agent run <program>, and tell the
-- server when the student's programs start and end.
--
-- While the shell waits for a command (prompt "> "), MINE marks it as free. The chat
-- command sends a "mine_run" event: the program starts as if the student had typed it,
-- and when it ends MINE tells the student in chat how it went.
if not mine or _G.__mine_original_read then
    return
end

local topShell = shell

-- MINE's welcome in place of CraftOS's banner and its tip of the day (motd, shown after the
-- autorun files): only for this boot, the computer's settings are not saved.
settings.set("motd.enable", false)
term.clear()
term.setCursorPos(1, 1)
for i, line in ipairs(mine._welcome()) do
    if i == 1 and term.isColour() then
        term.setTextColour(colours.yellow)
    end
    print(line)
    term.setTextColour(colours.white)
end

-- "help mine" in the student's language: the translated pages come first, the rest stays in English.
local language = mine._language()
if language:sub(1, 3) == "it_" then
    help.setPath("/rom/help/it:" .. help.path())
end

-- Byte is the Agent's name: byte.forward(), byte.dig()... are the turtle's functions (turtle.*
-- keeps working).
if turtle then
    _G.byte = {}
    for name, fn in pairs(turtle) do
        byte[name] = fn
    end
    -- true when the block below is a crop that has finished growing (wheat, carrots...).
    byte.ripeDown = function()
        local found, block = turtle.inspectDown()
        if not found or not block.state or block.state.age == nil then
            return false
        end
        local ripe = { ["minecraft:beetroots"] = 3, ["minecraft:nether_wart"] = 3 }
        return block.state.age >= (ripe[block.name] or 7)
    end
end

-- mine.say also writes the message in the terminal: while it is open, the chat can't be seen.
local say = mine.say
mine.say = function(message)
    say(message)
    print(tostring(message))
end

-- The board plugged into the student's PC (micro:bit, Arduino...), connected in MINE with
-- /mine device. It writes lines key:value; each one also arrives as the event
-- "mine_device", key, value.
mine.device = {
    -- The last value of key (a number, or a text): 0 until the board has sent it.
    read = function(key)
        local value = mine._deviceRead(tostring(key))
        if value == nil then
            return 0
        end
        return value
    end,
    -- Sends key:value to the board; false if no board is connected.
    send = function(key, value)
        return mine._deviceSend(tostring(key), value)
    end,
    -- Waits until the board sends key (any key if nil); returns the key and its value.
    wait = function(key)
        while true do
            local _, got, value = os.pullEvent("mine_device")
            if key == nil or got == string.lower(tostring(key)) then
                return got, value
            end
        end
    end,
    connected = function()
        return mine._deviceConnected()
    end,
}

local original = _G.read
_G.__mine_original_read = original

-- Tell MINE when one of the student's own programs starts and ends, so the server can look at
-- what the Agent did (lesson missions). Built-in programs (lua, edit, ls...) live in rom/ and
-- are not tracked; neither are programs started by another tracked program.
local originalRun = topShell.run
local tracking = false
-- The program MINE asked the shell to run, to report on in the chat when it ends.
local reportRun
-- The coroutine running the student's program: "mine_stop" ends that one, not the shell around
-- it (which reads commands in a coroutine of its own and passes events on) nor rednet.
local programThread

-- MINE can stop a student's program, e.g. one that listens forever once a lesson has tested it:
-- the server sends "mine_stop". Every event goes through here first, so the program stops
-- wherever it is waiting, whatever event it waits for.
local rawPull = os.pullEventRaw
os.pullEventRaw = function(filter)
    while true do
        local event = table.pack(rawPull())
        if event[1] == "mine_stop" and tracking and coroutine.running() == programThread then
            -- No message: it isn't an error (MINE has tested the program), and MINE says how it went.
            error("", 0)
        end
        -- Anyone else passes it on (the shell hands events to the coroutine running the program).
        if filter == nil or event[1] == filter or event[1] == "terminate" then
            return table.unpack(event, 1, event.n)
        end
    end
end
topShell.run = function(...)
    local name = table.concat({ ... }, " "):match("^%S+")
    local path = name and topShell.resolveProgram(name)
    -- A program MINE started (chat, blocks, run key): the chat says how it went.
    local report = reportRun
    reportRun = nil
    if tracking or not path or path:find("^rom/") then
        local ok = originalRun(...)
        if report then
            mine._report(ok and "ok" or "error", report)
        end
        return ok
    end

    tracking = true
    mine._started(table.concat({ ... }, " "))
    programThread = coroutine.running()
    local ok, result = pcall(originalRun, ...)
    tracking = false
    -- Redstone the program switched on reaches the blocks around on the next tick: wait for it,
    -- so MINE sees the lamp the program lit (lesson 6).
    if ok then
        pcall(os.sleep, 0.1)
    end
    -- A program that listens forever, stopped by MINE once a lesson has seen enough: it went well.
    local stopped = mine._stoppedByMine()
    -- How it went for the lesson, if there is one: again, the chat can't be seen from here.
    for _, line in ipairs(mine._finished((ok and result == true) or stopped)) do
        print(line)
    end
    if report then
        mine._report(((ok and result) or stopped) and "ok" or "error", report)
    end
    if not ok and not stopped then
        error(result, 0)
    end
    return result or stopped
end

-- What MINE prepares on the Agent when the shell is ready: the pickaxe, items for the lesson
-- (given by the server) and programs to copy. Programs are copied once per token: when the
-- server sends a new one (the lesson was restarted), they are copied again, overwriting the
-- old ones. Returns the lines to show, or nil if there is no news.
local MARKER = "/.mine_lesson"

-- Programs sent from MINE's block editor: written every time, over the old ones. Returns the
-- lines announcing them.
local function receiveSent()
    local sent, sentLines = mine._inbox()
    for name, content in pairs(sent or {}) do
        local handle = fs.open("/" .. name, "w")
        handle.write(content)
        handle.close()
    end
    return sentLines
end

local function receiveKit()
    local token, files, filesLines, lines = mine._prepare()
    local news = {}
    if token then
        local handle = fs.open(MARKER, "r")
        local last = handle and handle.readAll()
        if handle then
            handle.close()
        end
        if last ~= token then
            for name, content in pairs(files) do
                handle = fs.open("/" .. name, "w")
                handle.write(content)
                handle.close()
            end
            handle = fs.open(MARKER, "w")
            handle.write(token)
            handle.close()
            for _, line in ipairs(filesLines) do
                table.insert(news, line)
            end
        end
    end
    for _, line in ipairs(lines or {}) do
        table.insert(news, line)
    end
    for _, line in ipairs(receiveSent() or {}) do
        table.insert(news, line)
    end
    return #news > 0 and news or nil
end

local function printNews(lines)
    if term.isColour() then
        term.setTextColour(colours.lightBlue)
    end
    for _, line in ipairs(lines) do
        print(line)
    end
    term.setTextColour(colours.white)
end

_G.read = function(replace, history, complete, default)
    -- Only the shell prompt: other read() calls (lua, students' programs) stay unchanged.
    if complete == nil or complete ~= topShell.complete then
        return original(replace, history, complete, default)
    end

    -- Not free while asking: events that arrive while waiting for the server are lost. If new
    -- something was offered in the meantime, _idle says so and we ask again.
    repeat
        local news = receiveKit()
        if news then
            -- The shell has already written its prompt: show the news in its place, then prompt again.
            local _, line = term.getCursorPos()
            term.setCursorPos(1, line)
            term.clearLine()
            printNews(news)
            if term.isColour() then
                term.setTextColour(colours.yellow)
            end
            write(topShell.dir() .. "> ")
            term.setTextColour(colours.white)
        end
        local offeredMeanwhile = mine._idle(true)
        if offeredMeanwhile then
            mine._idle(false)
        end
    until not offeredMeanwhile

    local x, y = term.getCursorPos()
    local typed, program, offered
    local ok, err = pcall(parallel.waitForAny,
        function() typed = original(replace, history, complete, default) end,
        function() program = select(2, os.pullEvent("mine_run")) end,
        function() os.pullEvent("mine_kit"); offered = true end)
    mine._idle(false)
    if not ok then
        error(err, 0)
    end
    if typed then
        return typed
    end
    if offered then
        -- News while waiting for a command: show it, and the shell prompts again.
        local news = receiveKit()
        if news then
            term.setCursorPos(1, y)
            term.clearLine()
            printNews(news)
        else
            print()
        end
        return ""
    end

    -- Show the command on the terminal, in place of whatever the student was typing.
    term.setCursorPos(x, y)
    term.write(program .. string.rep(" ", (term.getSize())))
    print()
    -- A program sent from the blocks to run straight away: on the disk before it starts (no
    -- need to say how to run it).
    receiveSent()

    if not topShell.resolveProgram(program:match("^%S+")) then
        printError("No such program")
        mine._report("missing", program)
        return ""
    end
    -- The shell runs it, as if typed: not here, inside the coroutine reading the command, where
    -- Ctrl+T would never reach it. How it went is reported when it ends (topShell.run).
    reportRun = program
    return program
end
