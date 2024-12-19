#!/bin/bash

# Automatically determine the source folder based on the script's location
script_dir=$(dirname "$(realpath "$0")")
source_folder="$script_dir/files"

# Check if the source folder exists
if [ ! -d "$source_folder" ]; then
    echo "The folder $source_folder does not exist. Exiting script."
    exit 1
fi

# Loop through all json.gz files in the source folder and unzip them
for file in "$source_folder"/*.json.gz; do
    if [ -f "$file" ]; then
        echo "Unzipping $file"
        gunzip "$file"
        echo "Deleted $file"
    else
        echo "No .json.gz files found in $source_folder."
    fi
done

echo "Done."
