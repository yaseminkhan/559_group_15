#!/bin/sh
# wait-for-backups.sh

echo "Waiting for backup servers and connection coordinator..."
echo "KAFKA_IP=$KAKFA_IP"
echo "COORDINATOR_IP=$COORDINATOR_IP"

until nc -z $KAFKA_IP 9092; do sleep 1; done
until nc -z $COORDINATOR_IP 9999; do sleep 1; done

echo "All dependencies ready. Starting primary..."
exec "$@"