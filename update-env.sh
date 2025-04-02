#!/bin/bash
# This script renders all .yaml files in the current directory using envsubst

echo "Exporting environment variables from .env..."
export $(grep -v '^#' .env | xargs)

cd compose/
echo "Deleting old rendered files..."
rm -f rendered-*.yaml

echo "Rendering YAML files from .env..."
# Iterate over all .yaml files in the current directory
for file in *.yaml; do
  # Generate the rendered filename by prefixing "rendered-"
  rendered_file="rendered-${file}"
  # Use envsubst to substitute environment variables and write to the rendered file
  envsubst < "$file" > "$rendered_file"
  echo "Rendered $file to $rendered_file"
done