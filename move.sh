#!/bin/bash

# Usage:
# ./move-plugin.sh YourPlugin.jar

PLUGIN_DIR=~/server/1.16.5/plugins

if [ -z "$1" ]; then
  echo "Usage: $0 <file>"
  exit 1
fi

FILE="$1"

if [ ! -f "$FILE" ]; then
  echo "File not found: $FILE"
  exit 1
fi

echo "Stopping server is recommended before replacing plugins!"

# Remove old version if exists
rm -f "$PLUGIN_DIR/$(basename "$FILE")"

# Move new file
cp -f "$FILE" "$PLUGIN_DIR/"

echo "Done: $(basename "$FILE") moved to plugins folder"