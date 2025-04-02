#!/bin/bash
# This script builds all Docker images in the current directory using docker-compose
# For solo testing purposes
sh update-env.sh
# Build all Docker images using docker-compose
cd compose/
for file in rendered-*.yaml; do
  docker-compose -f "$file" build
  echo "Built image from $file"
done