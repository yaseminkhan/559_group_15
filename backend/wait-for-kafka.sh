#!/bin/sh
# wait-for-backups.sh

set -e
echo "Waiting for backup servers and connection coordinator..."
echo "KAFKA_IP=$KAFKA_IP"

until nc -z $KAFKA_IP 9092; do sleep 1; done

echo "All dependencies ready. Starting primary..."
exec "$@"