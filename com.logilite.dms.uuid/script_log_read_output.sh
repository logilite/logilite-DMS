#!/bin/bash
#################################################
# Command to Execute this script
#
# Open terminal with /home/sachin/ path
#
# sudo bash script_log_read_output.sh
#
#################################################
#  .....READ ME FIRST.....
# 1) SCRIPT_PATH
#		- Require to configure local variable 
#		- Specify where the executable files log files location
#################################################
#

processStartTime=$(date +%s%N)
printf "Process Started with \t $(date +'%Y-%b-%d %H:%M:%S') \n\n"  >> output_log_file_analysis.log

# Folder path containing the files to process
SCRIPT_PATH="/home/sachin/DMS_UUID_20230721/"

# List of selected files to process
#selected_files=("s1.sh" "s2.sh")
selected_files=$(sudo find "$SCRIPT_PATH" -maxdepth 1 -type f -name "*.log" | sort)

# Function to process a file
process_file_log() 
{
	local filename="$1"
    local line_no=0
	printf "Reading Script \t $filename \t\n"

    while IFS= read -r line; do
        ((line_no++))
        if ! [[ $line =~ ^\s*rename ]]; then
            echo "File: $filename, Line: $line_no, Content: $line" >> output_log_file_analysis.log
        fi
    done < "$filename"
	echo " " >> output_log_file_analysis.log
}

# Exporting the function for access within the subshell
export -f process_file_log

# Execute the .sh files in parallel using xargs based on alphabetical order
printf "%s\n" "${selected_files[@]}" | xargs -I{} -P 1 bash -c 'process_file_log "$@"' _
#
printf "Process Finished with \t $(date +'%Y-%b-%d %H:%M:%S') \n"  >> output_log_file_analysis.log
processEndTime=$(date +%s%N)
processDiffTime=$((processEndTime-processStartTime))
printf "\nTotal Processed Time: \t %s.%s seconds \n" "${processDiffTime:0:-9}" "${processDiffTime:-9:3}"  >> output_log_file_analysis.log
