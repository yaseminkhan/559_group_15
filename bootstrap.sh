#!/bin/bash

ENV_IN=".env"
ENV_OUT=".env.generated"
RENDERED_COMPOSE="compose.yaml"

if [ "$#" -lt 1 ]; then
  echo "Usage: sh bootstrap.sh <service yaml files...>"
  exit 1
fi

# Load .env
declare -A env_map
while IFS='=' read -r key value; do
  key=$(echo "$key" | xargs)
  value=$(echo "$value" | xargs)
  if [[ -n "$key" && "$key" != \#* ]]; then
    export "$key"="$value"
    env_map[$key]="$value"
  fi
done < "$ENV_IN"

# Map docker service name to friendly service type
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

# Detect which services you intend to run locally
LOCAL_SERVICES=()
for yaml in "$@"; do
  SERVICE_NAMES=$(awk '/services:/,0' "$yaml" | grep -E '^[[:space:]]+[a-zA-Z0-9_-]+:' | sed 's/^[[:space:]]*\(.*\):.*/\1/')

  for SERVICE_LINE in $SERVICE_NAMES; do
    SERVICE_ROLE=$(map_service_name "$SERVICE_LINE")
    if [ -n "$SERVICE_ROLE" ]; then
      LOCAL_SERVICES+=("$SERVICE_ROLE")
      echo "[*] Will run $SERVICE_ROLE from $yaml (service name: $SERVICE_LINE)"
    fi
  done
done

# Resolve IPs based on intended local services
resolve_var() {
  local var_name=$1
  local service_name=$2
  local default_ip=${env_map[$var_name]}

  if printf '%s\n' "${LOCAL_SERVICES[@]}" | grep -qx "$service_name"; then
    echo "$var_name=127.0.0.1"
  else
    echo "$var_name=$default_ip"
  fi
}

# Write .env.generated with resolved IPs
echo "[*] Writing resolved environment to $ENV_OUT..."
{
  resolve_var PRIMARY_SERVER_IP primary
  resolve_var BACKUP_SERVER_1_IP backup_1
  resolve_var BACKUP_SERVER_2_IP backup_2
  resolve_var BACKUP_SERVER_3_IP backup_3
  resolve_var COORDINATOR_IP coordinator

  echo "PRIMARY_SERVER_TAILSCALE_IP=${env_map[PRIMARY_SERVER_IP]}"
  echo "BACKUP_SERVER_1_TAILSCALE_IP=${env_map[BACKUP_SERVER_1_IP]}"
  echo "BACKUP_SERVER_2_TAILSCALE_IP=${env_map[BACKUP_SERVER_2_IP]}"
  echo "BACKUP_SERVER_3_TAILSCALE_IP=${env_map[BACKUP_SERVER_3_IP]}"
  echo "COORDINATOR_TAILSCALE_IP=${env_map[COORDINATOR_IP]}"
  echo "KAFKA_TAILSCALE_IP=${env_map[KAFKA_IP]}"

  # Determine correct bootstrap IP and port based on locality
  if printf '%s\n' "${LOCAL_SERVICES[@]}" | grep -qx "kafka"; then
    echo "KAFKA_IP=127.0.0.1"
    echo "KAFKA_BOOTSTRAP_SERVERS=127.0.0.1:9092"
    echo "KAFKA_WAIT_FOR=127.0.0.1:9092"
  else
    echo "KAFKA_IP=${env_map[KAFKA_IP]}"
    echo "KAFKA_BOOTSTRAP_SERVERS=${env_map[KAFKA_IP]}:9093"
    echo "KAFKA_WAIT_FOR=${env_map[KAFKA_IP]}:9093"
  fi
} > "$ENV_OUT"

# Export all resolved variables
set -a
source "$ENV_OUT"
set +a



# Render all selected compose templates
echo "[*] Rendering YAMLs..."
RENDERED_TEMP_FILES=()
for yaml in "$@"; do
  base=$(basename "$yaml")
  rendered="rendered-$base"
  envsubst < "$yaml" > "$rendered"
  RENDERED_TEMP_FILES+=("$rendered")
  echo "[*] Rendered $base → $rendered"
done

# Combine all rendered YAMLs into a final compose.yaml
echo "[*] Combining into compose.yaml"
echo "version: '3.9'" > "$RENDERED_COMPOSE"
echo "services:" >> "$RENDERED_COMPOSE"

for rendered in "${RENDERED_TEMP_FILES[@]}"; do
  sed -n '/^  [^[:space:]]/,$p' "$rendered" >> "$RENDERED_COMPOSE"
  echo >> "$RENDERED_COMPOSE"
done

# Clean up temporary rendered YAMLs
rm -f rendered-*.yaml

echo "[*] Final compose.yaml generated! Run with:"
echo "    docker compose --env-file $ENV_OUT -f $RENDERED_COMPOSE up"