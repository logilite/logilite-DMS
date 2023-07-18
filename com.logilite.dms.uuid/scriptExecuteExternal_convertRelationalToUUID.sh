#!/bin/bash
#################################################
# Command to Execute this script
#
# sudo bash scriptExecuteExternal_convertRelationalToUUID.sh > scriptExecuteExternal_convertRelationalToUUID.log
#
#################################################
#  .....READ ME FIRST.....
# 1) SCRIPT_PATH
#		- Require to configure local variable 
#		- Specify where the executable files need to executes for rename the content
# 2) DMS_STRUCTURE_PATH
#		- Require to configure local variable 
#		- Specify where the DMS directory and files structure path
#################################################
#

processStartTime=$(date +%s%N)
printf "Process Started with \t $(date +'%Y-%b-%d %H:%M:%S') \n"

# Folder path containing the files to process
SCRIPT_PATH="/home/sachin/"

# List of selected files to process
#selected_files=("s1.sh" "s2.sh")
selected_files=$(sudo find "$SCRIPT_PATH" -maxdepth 1 -type f -name "*.sh" ! -name "scriptExecuteExternal_convertRelationalToUUID.sh" | sort)

# Function to process a file
process_file() 
{
    local file=$1
	local DMS_STRUCTURE_PATH="/DMS_Content"
	local log_file="${file}.log"
	local startTime=$(date +%s%N)
    printf "1 Executing Script       $file \t %s\n" "${PWD#*/}"
	
	#cd ..
	cd ./"$DMS_STRUCTURE_PATH"
    printf "2 Processing Path <><><> $file \t %s\n" "${PWD#*/}"
	
    # Run the .sh script using bash command
	sudo su sachin "$file" &> $log_file &
	
	# Add your additional processing commands here
    sleep 0.25  # Example: Sleep for 5 seconds as a placeholder
	
	local endTime=$(date +%s%N)
	diffTime=$((endTime-startTime))
	printf "3 Script Completed ##### $file \t %s.%s seconds passed\n" "${diffTime:0:-9}" "${diffTime:-9:4}"
}

# Exporting the function for access within the subshell
export -f process_file

# Execute the .sh files in parallel using xargs based on alphabetical order
printf "%s\n" "${selected_files[@]}" | xargs -I{} -P 2 bash -c 'process_file "$@"' _ {}

#
printf "Process Finished with \t $(date +'%Y-%b-%d %H:%M:%S') \n"
processEndTime=$(date +%s%N)
processDiffTime=$((processEndTime-processStartTime))
printf "Total Processed Time: \t %s.%s seconds \n" "${processDiffTime:0:-9}" "${processDiffTime:-9:3}"
