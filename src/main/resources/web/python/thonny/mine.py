# MINE for Thonny (or any Python 3 on your PC): program your Byte from here, with the same names
# as in MINE's Python editor and in Lua.
#
#   1. Make a folder for your Python programs (e.g. MINE-Python in your home) and put this file in it.
#   2. In Thonny, save your programs in the same folder, e.g. scale.py:
#
#        from mine import byte
#        import mine
#        mine.say("Hello!")
#        byte.forward()
#
#   3. F5: the first time MINE asks for your code (in the game: /mine python) and finds the
#      school's server by itself; it remembers both in mine.txt, next to this file.
#   4. Go back to Minecraft: after 3-2-1 your program starts.
import atexit
import json
import os
import re
import socket
import struct
import sys
import time
import urllib.error
import urllib.request

_HERE = os.path.dirname(os.path.abspath(__file__))
_CONFIG = os.path.join(_HERE, "mine.txt")
_ITALIAN = os.environ.get("LANG", "").startswith("it")
_EDITOR_PORT = 25580


def _say(english, italian):
    print("MINE: " + (italian if _ITALIAN else english), file=sys.stderr)


def _read_config():
    config = {}
    try:
        with open(_CONFIG, encoding="utf-8") as file:
            for line in file:
                if "=" in line:
                    key, value = line.split("=", 1)
                    config[key.strip()] = value.strip()
    except OSError:
        pass
    return config


def _write_config(config):
    with open(_CONFIG, "w", encoding="utf-8") as file:
        file.write("# MINE: the school's server and your code (in the game: /mine python)\n")
        for key in ("server", "code"):
            if config.get(key):
                file.write(key + "=" + config[key] + "\n")


def _discover():
    """The MINE server announces itself on the school's network, as it does for Minecraft's list."""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind(("", 4445))
        group = struct.pack("4sl", socket.inet_aton("224.0.2.60"), socket.INADDR_ANY)
        sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, group)
        sock.settimeout(4)
        while True:
            data, (address, port) = sock.recvfrom(1024)
            if b"[MOTD]" in data:
                return address
    except OSError:
        return None


def _ask(english, italian):
    return input((italian if _ITALIAN else english) + " ").strip()


_config = _read_config()


def _post(path, body):
    server = _config["server"]
    # The address, with the editor's port unless it has its own ("192.168.1.20:25580").
    address = server if ":" in server else "%s:%d" % (server, _EDITOR_PORT)
    request = urllib.request.Request("http://%s%s?code=%s" % (address, path, _config["code"]),
                                     data=json.dumps(body).encode("utf-8"), headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=None) as answer:
        return json.loads(answer.read().decode("utf-8"))


def _call(name, args):
    answer = _post("/api/py/call", {"fn": name, "args": list(args)})
    if answer.get("stopped"):
        # Ctrl+T on Byte, or MINE after a lesson's test.
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


def _start():
    if not _config.get("server"):
        _say("looking for the school's MINE server…", "cerco il server MINE della scuola…")
        _config["server"] = _discover() or _ask("The MINE server's address (ask your teacher, e.g. 192.168.1.20):",
                                                "L'indirizzo del server MINE (chiedilo all'insegnante, es. 192.168.1.20):")
    if not _config.get("code"):
        _config["code"] = _ask("Your MINE code (in the game type /mine python):",
                               "Il tuo codice MINE (nel gioco scrivi /mine python):").upper()
    _write_config(_config)
    main = sys.modules.get("__main__")
    path = getattr(main, "__file__", None)
    name = "python"
    source = ""
    if path:
        name = re.sub(r"[^a-z0-9_]", "_", os.path.splitext(os.path.basename(path))[0].lower())[:24] or "python"
        try:
            with open(path, encoding="utf-8") as file:
                source = file.read()
        except OSError:
            source = ""
    try:
        answer = _post("/api/py/run", {"name": name, "source": source})
    except urllib.error.HTTPError as e:
        if e.code == 403:
            _say("the code " + _config["code"] + " isn't right. In the game type /mine python: F5 again and type it.",
                 "il codice " + _config["code"] + " non è giusto. Nel gioco scrivi /mine python: di nuovo F5 e scrivilo.")
            _config.pop("code")
            _write_config(_config)
            os._exit(1)
        raise
    except (urllib.error.URLError, OSError):
        _say("the server " + _config["server"] + " doesn't answer: is it on? F5 again to look for it.",
             "il server " + _config["server"] + " non risponde: è acceso? Di nuovo F5 per cercarlo.")
        _config.pop("server")
        _write_config(_config)
        os._exit(1)
    if answer.get("reason") == "offline":
        _say("join the game first: Byte listens while you play.", "entra prima nel gioco: Byte ascolta mentre giochi.")
        os._exit(1)
    if not answer.get("ok"):
        _say(answer.get("message", ""), answer.get("message", ""))
        os._exit(1)
    print("MINE: " + answer.get("message", ""), file=sys.stderr)


_error = []
_excepthook = sys.excepthook


def _record(kind, value, trace):
    if kind is not SystemExit:
        _error.append("%s: %s" % (kind.__name__, value))
    _excepthook(kind, value, trace)


sys.excepthook = _record


@atexit.register
def _end():
    try:
        _post("/api/py/end", {"error": _error[0] if _error else ""})
    except Exception:
        pass


# Waiting is Byte's: time.sleep(3) and os.sleep(3) wait 3 seconds of the game, as in Lua.
time.sleep = _sleep
os.sleep = _sleep
os.pullEvent = _pull_event

if __name__ != "__main__":
    _start()
