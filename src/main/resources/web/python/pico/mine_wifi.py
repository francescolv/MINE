# MINE over Wi-Fi, for a Raspberry Pi Pico 2 W (or Pico W) with MicroPython: the board talks to
# the MINE server by itself, without a cable to the PC. The same lines key:value as over a cable,
# for the student's Byte and board blocks.
#
# Put on the Pico, next to this file, a mine.txt with:
#
#     wifi=the Wi-Fi's name
#     password=its password
#     code=your code (in the game: /mine python)
#     lang=it                   (optional: messages in Italian)
#     server=192.168.1.20       (optional: found by itself on the school's network)
#
# Then, in your program:
#
#     import mine_wifi as mine
#     mine.connect()
#     mine.send("temperature", 21)
#     for key, value in mine.receive():
#         print(key, value)

import json
import socket
import time

import network
from machine import Pin

_EDITOR_PORT = 25580
_ANNOUNCE_PORT = 4445
_CONFIG = "mine.txt"


def _read_config():
    config = {}
    try:
        with open(_CONFIG) as file:
            for line in file:
                if "=" in line and not line.lstrip().startswith("#"):
                    key, value = line.split("=", 1)
                    config[key.strip()] = value.strip()
    except OSError:
        pass
    return config


_config = _read_config()
_led = Pin("LED", Pin.OUT)
_wlan = network.WLAN(network.STA_IF)
_server = None
_outbox = []
_inbox = []
_said = None


def _say(english, italian):
    print("MINE: " + (italian if _config.get("lang") == "it" else english))


def _once(reason, english, italian):
    """A problem said once, not at every try."""
    global _said
    if _said != reason:
        _said = reason
        _say(english, italian)


def _wifi():
    if _wlan.isconnected():
        return True
    if not _config.get("wifi"):
        _once("config", "write wifi=, password= and code= in mine.txt on the Pico.",
              "scrivi wifi=, password= e code= in mine.txt sul Pico.")
        return False
    _say("connecting to the Wi-Fi " + _config["wifi"] + "...", "mi collego al Wi-Fi " + _config["wifi"] + "...")
    _wlan.active(True)
    _wlan.config(pm=0xa11140)  # no power saving: answers come at once
    _wlan.connect(_config["wifi"], _config.get("password", ""))
    for _ in range(40):
        if _wlan.isconnected():
            _led.off()
            _say("on the Wi-Fi, address " + _wlan.ifconfig()[0], "sono sul Wi-Fi, indirizzo " + _wlan.ifconfig()[0])
            return True
        if _wlan.status() < 0:
            break
        # The LED blinks while it gets on the Wi-Fi; then it's the program's.
        _led.toggle()
        time.sleep(0.5)
    _led.off()
    _say("I can't get on the Wi-Fi: check its name and password in mine.txt.",
         "non riesco a entrare nel Wi-Fi: controlla nome e password in mine.txt.")
    _wlan.active(False)
    return False


def _discover():
    """The MINE server announces itself on the school's network, as it does for Minecraft's list."""
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind(("0.0.0.0", _ANNOUNCE_PORT))
        sock.settimeout(5)
        while True:
            data, (address, port) = sock.recvfrom(256)
            if b"[MOTD]" in data:
                return address
    except OSError:
        return None
    finally:
        sock.close()


def _find_server():
    global _server
    if _server:
        return True
    server = _config.get("server") or _discover()
    if not server:
        _once("server", "I can't find the MINE server: is it on? Or write server= in mine.txt.",
              "non trovo il server MINE: è acceso? Oppure scrivi server= in mine.txt.")
        return False
    host, _, port = server.partition(":")
    _server = (host, int(port) if port else _EDITOR_PORT)
    _say("server " + host, "server " + host)
    return True


def _post(path, body):
    host, port = _server
    sock = socket.socket()
    sock.settimeout(5)
    try:
        sock.connect(socket.getaddrinfo(host, port)[0][-1])
        data = json.dumps(body).encode()
        sock.send(("POST %s?code=%s HTTP/1.0\r\nHost: %s\r\nContent-Type: application/json\r\n"
                   "Content-Length: %d\r\n\r\n" % (path, _config.get("code", ""), host, len(data))).encode())
        sock.send(data)
        answer = b""
        while True:
            chunk = sock.recv(512)
            if not chunk:
                break
            answer += chunk
    finally:
        sock.close()
    head, _, payload = answer.partition(b"\r\n\r\n")
    return int(head.split(b" ")[1]), json.loads(payload) if payload else {}


def _value(text):
    try:
        return int(text)
    except ValueError:
        return text


def _exchange():
    """Gives the server the lines to send and takes the ones for the board. False if it can't now."""
    global _server, _said
    if not _wifi() or not _find_server():
        return False
    values = _outbox[:]
    try:
        status, answer = _post("/api/device", {"values": values})
    except OSError:
        _once("network", "the server doesn't answer: I'll try again.", "il server non risponde: riprovo.")
        _server = None
        return False
    if status == 403:
        _once("code", "the code in mine.txt isn't right: in the game, /mine python shows yours.",
              "il codice in mine.txt non va: nel gioco, /mine python mostra il tuo.")
        return False
    if answer.get("reason") == "offline":
        _once("offline", "join the game first: the board talks to your Byte.",
              "prima entra nel gioco: la scheda parla con il tuo Byte.")
        return False
    del _outbox[:len(values)]
    for line in answer.get("send", []):
        key, _, value = line.partition(":")
        _inbox.append((key, _value(value)))
    if _said is not None:
        _said = None
        _say("connected to your Byte.", "collegata al tuo Byte.")
    return True


def connect():
    """Gets on the Wi-Fi and finds the server, waiting until the student is in the game."""
    while not _exchange():
        time.sleep(2)


def send(key, value):
    """Sends key:value, as print("key:value") over a cable."""
    _outbox.append("%s:%s" % (key, value))
    del _outbox[:-40]
    _exchange()


def receive():
    """The lines (key, value) sent to the board since the last time: ask often, at least every few seconds."""
    if not _inbox:
        _exchange()
    lines = _inbox[:]
    del _inbox[:]
    return lines
