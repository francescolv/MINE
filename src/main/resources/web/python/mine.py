# MINE's Python: Byte, redstone and mine, with the same names as in Lua.
#
#   from mine import byte, redstone
#   import mine
#   byte.forward()
#   mine.say("Hello!")
#
# The program runs in the browser; each call runs on Byte, through the bridge (bridge.lua).
import json
import mine_js


def _call(name, args):
    answer = json.loads(mine_js.call(name, json.dumps(args)))
    if answer.get("stopped"):
        # Ctrl+T on Byte, Stop in the editor, or MINE after a lesson's test.
        raise SystemExit
    if not answer.get("ok"):
        raise RuntimeError(answer.get("error", "error"))
    values = answer.get("values", [])
    if not values:
        return None
    if len(values) == 1:
        return values[0]
    # byte.forward() gives false and why: in Python just False, so "if byte.forward():" works.
    if values[0] is False and isinstance(values[1], str):
        return False
    return tuple(values)


# --- The same in web/python/mine.py and web/python/thonny/mine.py (tools/check-mine-py.js checks it) ---


class _Api:
    def __init__(self, prefix):
        self._prefix = prefix

    def __getattr__(self, name):
        if name.startswith("_"):
            raise AttributeError(name)

        def function(*args):
            return _call(self._prefix + name, list(args))

        return function


byte = _Api("byte.")
redstone = _Api("redstone.")
device = _Api("mine.device.")


def say(message):
    return _call("mine.say", [str(message)])


def owner():
    return _call("mine.owner", [])


def challenge(n=None):
    return _call("mine.challenge", [] if n is None else [n])


def version():
    return _call("mine.version", [])


def _sleep(seconds):
    _call("os.sleep", [seconds])


def _pull_event(*filter):
    # Always a tuple, as in Lua: event[0] is the event's name, also for ("redstone",).
    event = _call("os.pullEvent", list(filter))
    return event if isinstance(event, tuple) else (event,)


# --- End of the part they share ---


# Waiting is Byte's: time.sleep(3) and os.sleep(3) wait 3 seconds of the game, as os.sleep in Lua;
# os.pullEvent("redstone") waits for a redstone change, as in Lua.
import time
import os

time.sleep = _sleep
os.sleep = _sleep
os.pullEvent = _pull_event
