#!/usr/bin/env bash
# MINE server installer: Minecraft Java + Fabric + Fabric API + CC: Tweaked + MINE.
#
#   sudo ./install.sh                    install in /opt/mine-server as a systemd service
#   ./install.sh --dir ~/mine-server --no-service   just the files, to start by hand
#
# Options:
#   --dir DIR          where the server goes (default /opt/mine-server)
#   --memory SIZE      Java memory, e.g. 4G (default 4G: about 25 students)
#   --port PORT        server port (default 25565)
#   --teacher NAME     a teacher's Minecraft name: made operator and whitelisted (repeatable)
#   --accept-eula      you accept the Minecraft EULA (https://aka.ms/MinecraftEULA)
#   --no-service       don't install the systemd service, the backup timer and mine-cmd
#
# Run it again to update MINE and the mods: the world and the settings are kept.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
DIR=/opt/mine-server
MEMORY=4G
PORT=25565
TEACHERS=()
ACCEPT_EULA=no
SERVICE=yes
MINECRAFT="$(cat "$HERE/minecraft.version")"
LOADER="$(cat "$HERE/fabric-loader.version")"

while [ $# -gt 0 ]; do
	case "$1" in
		--dir) DIR="$2"; shift 2 ;;
		--memory) MEMORY="$2"; shift 2 ;;
		--port) PORT="$2"; shift 2 ;;
		--teacher) TEACHERS+=("$2"); shift 2 ;;
		--accept-eula) ACCEPT_EULA=yes; shift ;;
		--no-service) SERVICE=no; shift ;;
		-h|--help) sed -n '2,17p' "$0"; exit 0 ;;
		*) echo "Unknown option: $1 (see --help)" >&2; exit 2 ;;
	esac
done

say() { printf '\033[1;32m==>\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31mError:\033[0m %s\n' "$*" >&2; exit 1; }

if [ "$SERVICE" = yes ] && [ "$(id -u)" -ne 0 ]; then
	fail "installing the service needs root: run it with sudo, or add --no-service"
fi
for tool in curl sha512sum; do
	command -v "$tool" >/dev/null || fail "$tool is missing: install it first"
done

# --- Java 25 ---
JAVA="$(command -v java || true)"
JAVA_VERSION="$([ -n "$JAVA" ] && "$JAVA" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1 || true)"
if [ -z "$JAVA" ] || [ "${JAVA_VERSION:-0}" -lt 25 ]; then
	echo "Minecraft $MINECRAFT needs Java 25 or newer (found: ${JAVA_VERSION:-none}). Install it with:"
	echo "  Fedora:         sudo dnf install java-25-openjdk-headless"
	echo "  Ubuntu/Debian:  sudo apt install openjdk-25-jre-headless"
	echo "  (or Eclipse Temurin 25 from https://adoptium.net if your distribution doesn't have it)"
	exit 1
fi
say "Java $JAVA_VERSION: $JAVA"

mkdir -p "$DIR/mods"
cd "$DIR"

# --- Fabric server launcher ---
INSTALLER="$(curl -fsSL https://meta.fabricmc.net/v2/versions/installer | grep -o '"version": *"[^"]*"' | head -1 | sed 's/.*"\([^"]*\)"$/\1/')"
[ -n "$INSTALLER" ] || fail "can't reach meta.fabricmc.net"
say "Fabric $LOADER for Minecraft $MINECRAFT (installer $INSTALLER)"
curl -fsSL -o fabric-server-launch.jar \
	"https://meta.fabricmc.net/v2/versions/loader/$MINECRAFT/$LOADER/$INSTALLER/server/jar"

# --- Mods: exactly the versions MINE is tested with ---
# Old versions of these mods are removed, so an update doesn't leave two of them.
rm -f mods/fabric-api-*.jar mods/cc-tweaked-*.jar mods/mine-*.jar
while read -r path size sha512 url; do
	[ -z "$path" ] && continue
	say "Downloading $(basename "$path")"
	curl -fsSL -o "$path" "$url"
	echo "$sha512  $path" | sha512sum -c --quiet - || fail "$path: the file is not the expected one"
done < "$HERE/mods.list"
cp "$HERE"/mine-*.jar mods/
say "MINE: $(basename "$HERE"/mine-*.jar)"

# --- Settings: only the first time, so the teacher's changes are kept ---
if [ ! -f server.properties ]; then
	sed "s/^server-port=.*/server-port=$PORT/" "$HERE/server.properties.defaults" > server.properties
	say "server.properties written for a class (flat world, Creative, whitelist)"
fi

# --- EULA: accepted by whoever installs, never on their behalf ---
if ! grep -q '^eula=true' eula.txt 2>/dev/null; then
	if [ "$ACCEPT_EULA" != yes ]; then
		echo
		echo "Minecraft servers need you to accept the Minecraft EULA: https://aka.ms/MinecraftEULA"
		read -r -p "Do you accept it? [y/N] " answer
		case "$answer" in [yYsS]*) ACCEPT_EULA=yes ;; esac
	fi
	[ "$ACCEPT_EULA" = yes ] || fail "the EULA was not accepted: the server can't start"
	echo "eula=true" > eula.txt
fi

if [ "$SERVICE" = no ]; then
	say "Done. Start the server with:"
	echo "  cd $DIR && java -Xmx$MEMORY -jar fabric-server-launch.jar nogui"
	[ ${#TEACHERS[@]} -gt 0 ] && echo "  then in its console: op <name> and whitelist add <name> for: ${TEACHERS[*]}"
	exit 0
fi

# --- systemd service, console and backups ---
id mine >/dev/null 2>&1 || useradd --system --home-dir "$DIR" --shell /usr/sbin/nologin mine
mkdir -p "$DIR/backups"
chown -R mine:mine "$DIR"

# The first packages had the console as a systemd socket unit, which SELinux blocks on Fedora: remove it.
if [ -f /etc/systemd/system/mine-server.socket ]; then
	systemctl disable --now mine-server.socket >/dev/null 2>&1 || true
	rm -f /etc/systemd/system/mine-server.socket
fi
for unit in mine-server.service mine-backup.service mine-backup.timer; do
	sed -e "s|@DIR@|$DIR|g" -e "s|@JAVA@|$JAVA|g" -e "s|@MEMORY@|$MEMORY|g" \
		"$HERE/systemd/$unit" > "/etc/systemd/system/$unit"
done
install -m 755 "$HERE/mine-cmd" /usr/local/bin/mine-cmd
sed "s|@DIR@|$DIR|g" "$HERE/mine-backup" > /usr/local/bin/mine-backup
chmod 755 /usr/local/bin/mine-backup

STARTED="$(date '+%Y-%m-%d %H:%M:%S')"
systemctl daemon-reload
systemctl enable mine-server.service mine-backup.timer
RESTARTS="$(systemctl show -p NRestarts --value mine-server)"
systemctl restart mine-server.service
systemctl start mine-backup.timer

# --- Firewall: let the class in ---
# 4445/udp: LAN games' announcements, so a game on this PC too sees the server in its list.
# 25580/tcp: MINE's block editor, opened in the students' browsers.
if command -v firewall-cmd >/dev/null && firewall-cmd --state >/dev/null 2>&1; then
	firewall-cmd --quiet --permanent --add-port="$PORT/tcp" --add-port=4445/udp --add-port=25580/tcp && firewall-cmd --quiet --reload
	say "firewalld: ports $PORT and 25580 (block editor) open"
elif command -v ufw >/dev/null && ufw status | grep -q active; then
	ufw allow "$PORT/tcp" >/dev/null
	ufw allow 4445/udp >/dev/null
	ufw allow 25580/tcp >/dev/null
	say "ufw: ports $PORT and 25580 (block editor) open"
fi

# --- Wait until the server is really up, and say so if it isn't ---
say "Waiting for the server to start (the first time it takes a minute or two)..."
up=no
for _ in $(seq 1 300); do
	if journalctl -u mine-server --since "$STARTED" --no-pager -o cat 2>/dev/null | grep -q 'Done ('; then
		up=yes
		break
	fi
	# Crashed: failed, or already restarting by itself (Restart=on-failure).
	systemctl is-failed --quiet mine-server && break
	[ "$(systemctl show -p NRestarts --value mine-server)" -gt "${RESTARTS:-0}" ] && break
	sleep 1
done
if [ "$up" != yes ]; then
	echo
	journalctl -u mine-server --since "$STARTED" --no-pager -o cat | tail -n 30
	echo
	fail "the server didn't start (its last messages are above). Full log: journalctl -u mine-server"
fi
[ -p /run/mine-server/console ] || fail "the server started, but its console (/run/mine-server/console) is missing"

# --- Teachers: operators and on the whitelist ---
for teacher in "${TEACHERS[@]}"; do
	mine-cmd "whitelist add $teacher" >/dev/null
	mine-cmd "op $teacher" >/dev/null
	say "$teacher: operator (teacher) and on the whitelist"
done

say "MINE server installed in $DIR and running."
echo "  Status:         systemctl status mine-server"
echo "  Log:            journalctl -u mine-server -f"
echo "  Commands:       sudo mine-cmd whitelist add <student>"
echo "  Backup now:     sudo mine-backup   (every night at 2:00, kept 14 days, in $DIR/backups)"
