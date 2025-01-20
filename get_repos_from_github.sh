#!/bin/bash

# Specify the year as a variable
year="2024"

# Automatically determine the source folder based on the script's location
script_dir=$(dirname "$(realpath "$0")")
source_folder="$script_dir/files"

# # Check if the source folder exists
# if [ ! -d "$source_folder" ]; then
#     echo "The folder $source_folder does not exist. Creating it now."
#     mkdir -p "$source_folder"
# fi

# # Download the files to the source folder
# echo "Starting downloads for year $year..."
# for day in {01..31}; do
#     for hour in {0..23}; do
#         filename="${year}-01-${day}-${hour}.json.gz"
#         json_filename="${year}-01-${day}-${hour}.json"
#         file_path="$source_folder/$filename"
#         json_file_path="$source_folder/$json_filename"

#         # Check if the file is already downloaded or unzipped
#         if [ -f "$file_path" ] || [ -f "$json_file_path" ]; then
#             echo "File $filename or its unzipped version already exists. Skipping download."
#         else
#             url="https://data.gharchive.org/$filename"
#             echo "Downloading $filename..."
#             wget -P "$source_folder" "$url"
#         fi
#     done
# done

# # Check if there are any .json.gz files in the source folder
# json_gz_files=("$source_folder"/*.json.gz)

# # If there are no .json.gz files, print a message and skip the unzip step
# if [ ! -e "${json_gz_files[0]}" ]; then
#     echo "No .json.gz files found in $source_folder. Skipping unzip."
# else
#     # Unzip all .json.gz files in the source folder
#     echo "Unzipping files..."
#     for file in "${json_gz_files[@]}"; do
#         if [ -f "$file" ]; then
#             echo "Unzipping $file"
#             gunzip "$file"
#         fi
#     done
# fi

# # Directory containing the JSON files
# DIR="./files"

# # Get unique date prefixes from the filenames
# dates=$(ls "$DIR"/ | grep -oE '^[0-9]{4}-[0-9]{2}-[0-9]{2}' | sort | uniq)

# # Check if any dates were found
# if [[ -z "$dates" ]]; then
#     echo "No JSON files found in the directory."
#     exit 1
# fi

# # Loop through each unique date
# for date in $dates; do
#     # Define output file for the current date
#     OUTPUT_FILE="output/merged_output_$date.json"
    
#     # Remove the output file if it already exists
#     rm -f "$OUTPUT_FILE"
    
#     echo "Merging files for date: $date..."
    
#     # Loop through files matching the current date pattern
#     for file in "$DIR"/"$date"-*.json; do
#         if [[ -e "$file" ]]; then
#             echo "Processing file: $file"
#             # Copy content from the file, removing surrounding brackets
#             # Read the file without brackets and append it to the output file
#             sed '1d;$d' "$file" >> "$OUTPUT_FILE"
#             echo "Successfully merged content from: $file"

#             # Remove the original file after merging
#             rm "$file"
#             echo "Removed original file: $file"
#         else
#             echo "No files found for pattern: $DIR/$date-*.json"
#         fi
#     done

#     echo "Merging process for $date completed. Merged content saved to: $OUTPUT_FILE"
# done

# echo "All merging processes completed."

# Define the source folder where the JSON files are located
output_folder="output"

# Check if the source folder exists
if [ ! -d "$output_folder" ]; then
    echo "The folder $output_folder does not exist."
    exit 1
fi

# Initialize counters and data structures
line_count=0
repo_count=0
valid_json_object_count=0
unique_repo_names=()

# Function to check if the repository name is unique
is_unique_repo() {
    local repo_name=$1
    for name in "${unique_repo_names[@]}"; do
        if [ "$name" == "$repo_name" ]; then
            return 1 # not unique
        fi
    done
    return 0 # unique
}

# Iterate over all JSON files in the folder
for file in "$output_folder"/merged_output_*.json; do
    if [ -f "$file" ]; then
        echo "Processing file: $file"

        while IFS= read -r line; do
            # Increment line count
            line_count=$((line_count + 1))

            # Try to parse the JSON and extract the repo name
            repo_name=$(echo "$line" | jq -r '.repo.name // empty')

            # Check if the line is valid JSON and has a repository name
            if [ "$repo_name" != "null" ]; then
                valid_json_object_count=$((valid_json_object_count + 1))
                if is_unique_repo "$repo_name"; then
                    echo "Repository: $repo_name"
                    unique_repo_names+=("$repo_name")
                    repo_count=$((repo_count + 1))
                fi
            fi
        done < "$file"
    fi
done

# Create JSON output with the results
output_file="output.json"
{
    echo "{"
    echo "  \"totalLineCount\": $line_count,"
    echo "  \"totalRepoCount\": $repo_count,"
    echo "  \"totalValidJsonCount\": $valid_json_object_count,"
    echo "  \"uniqueRepoNames\": ["
    for i in "${!unique_repo_names[@]}"; do
        if [ $i -ne 0 ]; then
            echo ","
        fi
        echo "    \"${unique_repo_names[$i]}\""
    done
    echo "  ],"
    echo "  \"uniqueRepoCount\": ${#unique_repo_names[@]}"
    echo "}"
} > "$output_file"

echo "Process completed. Results saved to $output_file."



echo "Done."
