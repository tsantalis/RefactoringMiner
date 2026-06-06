#!/bin/bash
# Template: Wrapper script to ensure a shared RefactoringMiner container is running and execute MCP.
# Populated variables should point to your local environment.

# --- USER CONFIGURATION ---
# Name of the shared container.
CONTAINER_NAME="refactoringminer-server"

# Port mapping for the WebDiff server. 
# Example: 6789:6789. If you want to be able to use different custom ports, 
# you can map a range like 6785-6795:6785-6795.
PORT_MAPPING="6789:6789"

# Path on host to mount inside the container. Use absolute path.
# This is the repository you want the MCP server to analyze.
HOST_VOLUME_PATH="/absolute/path/to/your/repository"

# Path inside the container. Usually /workspace.
# When using tools, use this path as 'repositoryPath' instead of the host path.
CONTAINER_WORKDIR="/workspace"

# Your GitHub OAuth token for private repos and higher rate limits.
# Can be obtained from GitHub Developer Settings.
TOKEN="your_github_pat_here"
# --------------------------

# Check if container exists and is running
if ! docker ps --filter "name=^${CONTAINER_NAME}$" --filter "status=running" | grep -q ${CONTAINER_NAME}; then
    echo "Starting shared RefactoringMiner container..." >&2
    # Remove old container if it exists but is stopped
    docker rm -f ${CONTAINER_NAME} >/dev/null 2>&1
    
    # Start the container in the background with a command that keeps it alive
    docker run -d \
        --name ${CONTAINER_NAME} \
        -p ${PORT_MAPPING} \
        -v "${HOST_VOLUME_PATH}:${CONTAINER_WORKDIR}" \
        -w ${CONTAINER_WORKDIR} \
        -e OAuthToken=${TOKEN} \
        --entrypoint tail \
        tsantalis/refactoringminer:latest -f /dev/null >/dev/null 2>&1
fi

# Execute the MCP command in the running container
docker exec -i ${CONTAINER_NAME} refactoringminer mcp
