-- Python: %NAME%.py, written in MINE's editor. The Python program runs in the browser; this
-- program runs each of its calls (byte.forward(), redstone.getInput("left")...) on Byte.
-- Run it from the editor (Run), not by hand.
local token = mine._pyReady()
if not token then
    print("%NAME% is a Python program: run it from MINE's editor (/mine blocks).")
    return
end

-- What Python can call: Byte, redstone, MINE, and waiting.
local roots = {
    byte = byte, redstone = redstone, mine = mine,
    os = { sleep = os.sleep, pullEvent = os.pullEvent },
}
local function find(name)
    local value = roots
    for part in name:gmatch("[^.]+") do
        if type(value) ~= "table" or part:sub(1, 1) == "_" then
            return nil
        end
        value = value[part]
    end
    return type(value) == "function" and value or nil
end

local function answer(ok, ...)
    local values = {}
    for i = 1, select("#", ...) do
        local value = select(i, ...)
        if value == nil then
            value = textutils.json_null
        end
        values[i] = value
    end
    return ok, values
end

-- Ctrl+T, or MINE stopping the program after a lesson's test: the bridge ends too.
local function stopping(message)
    return message == "Terminated" or message == ""
end

-- Events that come between two calls (a board's value, a redstone change): kept, and given back
-- just before the next call, so os.pullEvent and mine.device.wait find them, as in Lua.
local kept = {}

-- From here on Byte listens: the calls waiting can come (not earlier, see PythonBridge).
mine._pyListening(token)
local done, failure = pcall(function()
    while true do
        -- An editor that stopped talking (closed, or the PC went to sleep) ends the program.
        local timer = os.startTimer(120)
        local event = table.pack(os.pullEvent())
        local id, name, args = event[2], event[3], event[4]
        if event[1] == "mine_py" then
            os.cancelTimer(timer)
            for _, old in ipairs(kept) do
                os.queueEvent(table.unpack(old, 1, old.n))
            end
            kept = {}
            if name == "end" then
                if args ~= "" then
                    error(args, 0)
                end
                return
            end
            local fn = find(name)
            local ok, values
            if fn then
                ok, values = answer(pcall(fn, table.unpack(textutils.unserializeJSON(args) or {})))
            else
                ok, values = false, { name .. " isn't one of Byte's functions" }
            end
            if not ok and stopping(values[1]) then
                error(values[1], 0)
            end
            mine._pyReply(token, id, ok, textutils.serializeJSON(values))
        elseif event[1] == "timer" and id == timer then
            error("The Python program stopped talking to Byte: is the editor still open?", 0)
        else
            os.cancelTimer(timer)
            if #kept < 64 then
                kept[#kept + 1] = event
            end
        end
    end
end)
mine._pyEnd(token)
if not done then
    error(failure, 0)
end
