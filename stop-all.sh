#!/bin/bash
# This script stops all Docker containers in the current directory using docker-compose
# For solo testing purposes

cd compose/
for file in rendered-*.yaml; do
  docker-compose -f "$file" down
  echo "Stopped containers from $file"
done