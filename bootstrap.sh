#!/bin/bash
export $(grep -v '^#' .env | xargs)

# Default to remote
BOOTSTRAP=${KAFKA_BOOTSTRAP_SERVERS}:9092

# Check local Kafka
if nc -z -w 1 127.0.0.1 9092; then
  echo "Kafka is local. Using 127.0.0.1:9092"
  BOOTSTRAP=127.0.0.1:9092
else
  echo "Kafka is remote. Using ${KAFKA_BOOTSTRAP_SERVERS}:9092"
fi

# Export for use in Docker Compose or your app
export KAFKA_BOOTSTRAP_SERVERS=$BOOTSTRAP