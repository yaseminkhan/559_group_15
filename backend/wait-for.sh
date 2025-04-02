#!/bin/bash
# Usage: wait-for.sh host:port [host2:port2 ...] -- command to run
set -e

while [[ "$1" != "--" ]]; do
  HOST_PORT="$1"
  HOST="${HOST_PORT%%:*}"
  PORT="${HOST_PORT##*:}"
  echo "[*] Waiting for $HOST:$PORT..."
  while ! nc -z "$HOST" "$PORT"; do
    sleep 1
  done
  echo "[+] $HOST:$PORT is available"
  shift
done

shift
exec "$@"
