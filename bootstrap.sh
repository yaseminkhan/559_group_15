#!/bin/bash

echo "[*] Exporting environment from .env..."

# Load and export .env variables (handles whitespace and ignores comments)
while IFS='=' read -r key value; do
  key=$(echo "$key" | xargs)
  value=$(echo "$value" | xargs)

  if [ -n "$key" ] && [ -n "$value" ] && [[ $key != \#* ]]; then
    export "$key"="$value"
  fi
done < .env

# Detect this machine's Tailscale IP
LOCAL_IP=$(tailscale ip -4 | head -n 1)

echo "[*] Detected local Tailscale IP: $LOCAL_IP"
echo "[*] Replacing matching service IPs with 127.0.0.1 if they match local IP..."

override_if_local() {
  VAR_NAME=$1
  CURRENT=${!VAR_NAME}

  if [ "$CURRENT" = "$LOCAL_IP" ]; then
    echo "[*] $VAR_NAME matches local IP ($CURRENT), setting to 127.0.0.1"
    export "$VAR_NAME"="127.0.0.1"
  else
    echo "[*] $VAR_NAME is $CURRENT (remote), unchanged"
  fi
}

# Apply override to known service IP vars
override_if_local BACKUP_SERVER_1_IP
override_if_local BACKUP_SERVER_2_IP
override_if_local BACKUP_SERVER_3_IP
override_if_local COORDINATOR_IP
override_if_local PRIMARY_SERVER_IP

echo
echo "[*] Final resolved environment:"
echo "  BACKUP_SERVER_1_IP=$BACKUP_SERVER_1_IP"
echo "  BACKUP_SERVER_2_IP=$BACKUP_SERVER_2_IP"
echo "  BACKUP_SERVER_3_IP=$BACKUP_SERVER_3_IP"
echo "  COORDINATOR_IP=$COORDINATOR_IP"
echo "  PRIMARY_SERVER_IP=$PRIMARY_SERVER_IP"

# Render templates
cd compose/ || { echo "[!] Failed to enter compose/ directory"; exit 1; }

echo "[*] Deleting old rendered files..."
rm -f rendered-*.yaml

echo "[*] Rendering YAML files using envsubst..."
for file in *.yaml; do
  rendered_file="rendered-${file}"
  envsubst < "$file" > "$rendered_file"
  echo "[*] Rendered $file -> $rendered_file"
done
