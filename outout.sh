#!/bin/bash

# Directories and files
INPUT_DIR="files"
OUTPUT_FILE="output.json"

# Initialize counters and sets
totalLineCount=0
totalRepoCount=0
totalValidJsonCount=0
declare -A uniqueRepoNames

# Check if the input directory exists
if [ ! -d "$INPUT_DIR" ]; then
    echo "Directory '$INPUT_DIR' not found!"
    exit 1
fi

# Process each file in the 'files' directory
for file in "$INPUT_DIR"/*; do
    if [ -f "$file" ] && [ -s "$file" ]; then
        echo "Processing file: $file"
        
        while IFS= read -r line; do
            totalLineCount=$((totalLineCount + 1))
            
            # Clean up the line by removing carriage return characters
            cleaned_line=$(echo "$line" | tr -d '\r')

            # Attempt to parse the JSON line
            if echo "$cleaned_line" | jq empty > /dev/null 2>&1; then
                totalValidJsonCount=$((totalValidJsonCount + 1))
                
                # Extract repo.name if JSON contains it
                repo_name=$(echo "$cleaned_line" | jq -r '.repo.name // empty')
                if [ -n "$repo_name" ]; then
                    totalRepoCount=$((totalRepoCount + 1))
                    uniqueRepoNames["$repo_name"]=1
                    # Optionally print each repo name as it's found
                    echo "Found repo: $repo_name"
                fi
            else
                # If the JSON line is invalid, print a message but do not track it.
                echo "Invalid JSON in file $file: $line"
            fi
        done < "$file"
    fi
done

# Prepare the output JSON content
uniqueRepoNamesArray=()
for repo in "${!uniqueRepoNames[@]}"; do
    uniqueRepoNamesArray+=("\"$repo\"")
done

# Build JSON response
response=$(cat <<EOF
{
  "totalLineCount": $totalLineCount,
  "totalRepoCount": $totalRepoCount,
  "totalValidJsonCount": $totalValidJsonCount,
  "uniqueRepoNames": [${uniqueRepoNamesArray[*]}],
  "uniqueRepoCount": ${#uniqueRepoNames[@]}
}
EOF
)

# Output the response to the output.json file
echo "$response" > "$OUTPUT_FILE"
echo "Results have been written to $OUTPUT_FILE"
