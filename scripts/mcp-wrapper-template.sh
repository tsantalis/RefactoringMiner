#!/bin/bash
# Template: Wrapper script to ensure a shared RefactoringMiner container is running and execute MCP.
# Populated variables should point to your local environment.

# --- USER CONFIGURATION ---
# Name of the shared container.
CONTAINER_NAME="refactoringminer-server"

# Port mapping for the WebDiff server. 
PORT_MAPPING="6785-6795:6785-6795"

# Path on host to mount inside the container. Use absolute path.
# This can be one repository or a common parent directory containing multiple repositories.
HOST_VOLUME_PATH="/absolute/path/to/your/repository"

# Path inside the container. Usually /workspace.
# For multiple repositories under HOST_VOLUME_PATH, use the MCP source.workingDirectory
# field as a relative path under this directory.
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
        --pull always \
        -v "${HOST_VOLUME_PATH}:${CONTAINER_WORKDIR}" \
        -w ${CONTAINER_WORKDIR} \
        -p ${PORT_MAPPING} \
        -e OAuthToken=${TOKEN} \
        --entrypoint tail \
        tsantalis/refactoringminer:latest -f /dev/null >/dev/null 2>&1
fi

# Only clean up orphans if no other active sessions are using the MCP server on the host
if ! pgrep -f "docker exec .*${CONTAINER_NAME}.*refactoringminer mcp" > /dev/null; then
    echo "No active sessions found on host. Cleaning up orphans in container..." >&2
    docker exec ${CONTAINER_NAME} pkill -f "RefactoringMiner-DockerBuild.jar" || true
fi

# Execute the MCP command in the running container
docker exec -i ${CONTAINER_NAME} refactoringminer mcp
