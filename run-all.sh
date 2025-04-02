#!/bin/bash
# Run all Docker images using docker-compose

echo "Starting KAFKA..."
docker compose -f rendered-kafka.yaml up -d

echo "Starting COORDINATOR..."
docker compose -f rendered-coordinator.yaml up -d

echo "Sleep for 10 seconds to allow services to start..."
sleep 10

echo "Starting BACKUPS..."
docker compose -f rendered-bak1.yaml up -d
docker compose -f rendered-bak2.yaml up -d
docker compose -f rendered-bak3.yaml up -d

echo "Sleep for 5 seconds to allow services to start..."
echo "Starting PRIMARY..."
docker compose -f rendered-primary.yaml up -d