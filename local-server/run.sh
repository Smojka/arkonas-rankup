#!/usr/bin/env bash
# Start the local ArkonasRanks test server. Run `bash setup.sh` first.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
cd "$HERE"
if [ ! -f paper.jar ]; then
  echo "paper.jar missing — run 'bash setup.sh' first." >&2
  exit 1
fi
exec "$JAVA_HOME/bin/java" -Xms1G -Xmx2G -jar paper.jar nogui
