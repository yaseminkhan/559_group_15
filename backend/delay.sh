#!/bin/bash
host=$1
port=$2

until nc -z $host $port; do
  echo "Waiting for $host:$port..."
  sleep 2
done

exec "${@:3}"  # Execute the passed command
