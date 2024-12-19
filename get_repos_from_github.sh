#!/bin/bash

# Specify the year as a variable
year="2024"

# Automatically determine the source folder based on the script's location
script_dir=$(dirname "$(realpath "$0")")
source_folder="$script_dir/files"

# Check if the source folder exists
if [ ! -d "$source_folder" ]; then
    echo "The folder $source_folder does not exist. Creating it now."
    mkdir -p "$source_folder"
fi

# Download the files to the source folder
echo "Starting downloads for year $year..."
for day in {01..31}; do
    for hour in {0..23}; do
        url="https://data.gharchive.org/${year}-01-${day}-${hour}.json.gz"
        wget -P "$source_folder" "$url"
    done
done

# Unzip all .json.gz files in the source folder
echo "Unzipping files..."
for file in "$source_folder"/*.json.gz; do
    if [ -f "$file" ]; then
        echo "Unzipping $file"
        gunzip "$file"
    else
        echo "No .json.gz files found in $source_folder."
    fi
done

# Directory containing the JSON files
DIR="./files"

# Get unique date prefixes from the filenames
dates=$(ls "$DIR"/ | grep -oE '^[0-9]{4}-[0-9]{2}-[0-9]{2}' | sort | uniq)

# Check if any dates were found
if [[ -z "$dates" ]]; then
    echo "No JSON files found in the directory."
    exit 1
fi

# Loop through each unique date
for date in $dates; do
    # Define output file for the current date
    OUTPUT_FILE="merged_output_$date.json"
    
    # Remove the output file if it already exists
    rm -f "$OUTPUT_FILE"
    
    echo "Merging files for date: $date..."
    
    # Loop through files matching the current date pattern
    for file in "$DIR"/"$date"-*.json; do
        if [[ -e "$file" ]]; then
            echo "Processing file: $file"
            # Copy content from the file, removing surrounding brackets
            # Read the file without brackets and append it to the output file
            sed '1d;$d' "$file" >> "$OUTPUT_FILE"
            echo "Successfully merged content from: $file"

            # Remove the original file after merging
            rm "$file"
            echo "Removed original file: $file"
        else
            echo "No files found for pattern: $DIR/$date-*.json"
        fi
    done

    echo "Merging process for $date completed. Merged content saved to: $OUTPUT_FILE"
done

echo "All merging processes completed."



echo "Done."
