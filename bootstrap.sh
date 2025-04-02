#!/bin/bash

COMPOSE_FILE="$1"
ENV_IN=".env"
ENV_OUT=".env.generated"

if [ -z "$COMPOSE_FILE" ]; then
  echo "[!] Usage: sh bootstrap.sh <compose_file.yaml>"
  exit 1
fi

echo "[*] Reading local service from: $COMPOSE_FILE"

# Map docker service name to internal identifier
map_service_name() {
  case "$1" in
    primary_server) echo "primary" ;;
    backup_server_1) echo "backup_1" ;;
    backup_server_2) echo "backup_2" ;;
    backup_server_3) echo "backup_3" ;;
    connection_coordinator) echo "coordinator" ;;
    kafka) echo "kafka" ;;
    *) echo "" ;;
  esac
}

# Extract service name from YAML
LOCAL_SERVICE=$(awk '/services:/ {getline; print $1}' "$COMPOSE_FILE" | sed 's/://')
INFERRED_SERVICE=$(map_service_name "$LOCAL_SERVICE")

if [ -z "$INFERRED_SERVICE" ]; then
  echo "[!] Could not map service name '$LOCAL_SERVICE' to a known role"
  exit 1
fi

echo "[*] Inferred local service: $INFERRED_SERVICE"

# Load all base .env values
declare -A env_map
while IFS='=' read -r key value; do
  key=$(echo "$key" | xargs)
  value=$(echo "$value" | tr -d '\r' | xargs)
  if [[ -n "$key" && "$key" != \#* ]]; then
    env_map["$key"]="$value"
  fi
done < "$ENV_IN"

# Resolves IP depending on whether it's local or not
resolve_var() {
  local var_name=$1
  local service_name=$2
  local default_ip=${env_map[$var_name]}

  if printf '%s\n' "${LOCAL_SERVICES[@]}" | grep -qx "$service_name"; then
    echo "[*] $service_name is local → $var_name=127.0.0.1" >&2
    echo "$var_name=127.0.0.1"
  else
    echo "[*] $service_name is remote → $var_name=$default_ip" >&2
    echo "$var_name=$default_ip"
  fi
}

echo "[*] Writing resolved environment to $ENV_OUT..."
{
  echo "$(resolve_var PRIMARY_SERVER_IP primary)"
  echo "$(resolve_var BACKUP_SERVER_1_IP backup_1)"
  echo "$(resolve_var BACKUP_SERVER_2_IP backup_2)"
  echo "$(resolve_var BACKUP_SERVER_3_IP backup_3)"
  echo "$(resolve_var COORDINATOR_IP coordinator)"

  echo "PRIMARY_SERVER_TAILSCALE_IP=${env_map[PRIMARY_SERVER_IP]}"
  echo "BACKUP_SERVER_1_TAILSCALE_IP=${env_map[BACKUP_SERVER_1_IP]}"
  echo "BACKUP_SERVER_2_TAILSCALE_IP=${env_map[BACKUP_SERVER_2_IP]}"
  echo "BACKUP_SERVER_3_TAILSCALE_IP=${env_map[BACKUP_SERVER_3_IP]}"
  echo "COORDINATOR_TAILSCALE_IP=${env_map[COORDINATOR_IP]}"
  echo "KAFKA_IP=${env_map[KAFKA_IP]}"
} > "$ENV_OUT"

echo "[*] Done. Use it with:"
echo "    docker compose -f $COMPOSE_FILE --env-file $ENV_OUT up"
