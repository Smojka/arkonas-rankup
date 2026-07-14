#!/usr/bin/env bash
# ArkonasRanks — local test-server provisioner.
# Downloads Paper + the dependency plugins, builds ArkonasRanks, and drops it all in place.
# Running this accepts the Minecraft EULA (https://aka.ms/MinecraftEULA) on your machine — you are
# the server operator. online-mode is set false for easy local testing; never expose this publicly.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/.." && pwd)"
PAPER_VER="1.21.4"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"

mkdir -p "$HERE/plugins"
cd "$HERE"

echo "==> Building ArkonasRanks…"
( cd "$REPO" && ./gradlew shadowJar -q )
cp "$REPO"/build/libs/ArkonasRanks-*.jar plugins/ArkonasRanks.jar

echo "==> Downloading Paper $PAPER_VER…"
BUILD="$(curl -fsSL "https://api.papermc.io/v2/projects/paper/versions/$PAPER_VER/builds" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['builds'][-1]['build'])")"
curl -fsSL -o paper.jar \
  "https://api.papermc.io/v2/projects/paper/versions/$PAPER_VER/builds/$BUILD/downloads/paper-$PAPER_VER-$BUILD.jar"

echo "==> Downloading Vault (HARD dependency)…"
curl -fsSL -o plugins/Vault.jar \
  "https://github.com/MilkBowl/Vault/releases/download/1.7.3/Vault.jar"

echo "==> Downloading LuckPerms (permission groups — a rank IS a group)…"
LP="$(curl -fsSL "https://metadata.luckperms.net/data/downloads" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['downloads']['bukkit'])")"
curl -fsSL -o plugins/LuckPerms.jar "$LP"

echo "==> Downloading EssentialsX (Vault economy for 'money' requirements)…"
curl -fsSL -o plugins/EssentialsX.jar \
  "https://github.com/EssentialsX/Essentials/releases/download/2.20.1/EssentialsX-2.20.1.jar"

echo "eula=true" > eula.txt
cat > server.properties <<'PROPS'
online-mode=false
motd=ArkonasRanks test server
max-players=5
spawn-protection=0
view-distance=6
gamemode=creative
PROPS

echo
echo "==> Provisioned. Start the server with:   bash run.sh"
echo "    Then connect a Minecraft 1.21.4 client to  localhost:25565"
echo "    (Optional deps for extra features: WorldGuard, Citizens, DiscordSRV, PlayerPoints,"
echo "     TokenManager, mcMMO, etc. — drop their jars in plugins/ and enable in config.)"
