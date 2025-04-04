#!/bin/sh
# wait-for-backups.sh

echo "Waiting for backup servers and connection coordinator..."
echo "BACKUP_SERVER_1_IP=$BACKUP_SERVER_1_IP"
echo "BACKUP_SERVER_2_IP=$BACKUP_SERVER_2_IP"
echo "BACKUP_SERVER_3_IP=$BACKUP_SERVER_3_IP"
echo "COORDINATOR_IP=$COORDINATOR_IP"

until nc -z $BACKUP_SERVER_1_IP 6001; do sleep 1; done
until nc -z $BACKUP_SERVER_2_IP 7001; do sleep 1; done
until nc -z $BACKUP_SERVER_3_IP 4001; do sleep 1; done
until nc -z $COORDINATOR_IP 9999; do sleep 1; done

echo "All dependencies ready. Starting primary..."
exec "$@"
