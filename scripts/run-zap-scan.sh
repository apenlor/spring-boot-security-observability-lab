#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

# --- Configuration ---
SERVICE_NAME="resource-server"
TARGET_URL="http://localhost:8081/v3/api-docs"
REPORT_DIR="reports"
REPORT_FILE="zap-report.html"
ZAP_IMAGE="zaproxy/zap-stable"

# --- Environment Configuration ---
# Determine which environment file to use.
# In CI, we use the committed .env.ci file with test defaults.
# Locally, we prefer the user's private .env file.
if [[ "${GITHUB_ACTIONS}" == "true" ]]; then
  echo "CI environment detected. Creating .env file from .env.ci"
  cp .env.ci .env
fi

# --- Environment-aware command detection ---
if docker compose &> /dev/null; then
    COMPOSE_CMD="docker compose"
elif docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
else
    echo "ERROR: Neither 'docker compose' nor 'docker-compose' could be found." >&2
    exit 1
fi
echo "Using compose command: '${COMPOSE_CMD}'"

# --- Main Script ---
mkdir -p ${REPORT_DIR}
echo "Starting DAST Scan Orchestration..."

# 1. Start the service to be scanned in the background
echo "Starting dependent services (Keycloak) and target service (${SERVICE_NAME})..."
${COMPOSE_CMD} up -d postgres keycloak
echo "Waiting 30 seconds for Keycloak to initialize..."
sleep 30
${COMPOSE_CMD} up -d --build ${SERVICE_NAME}

echo "Waiting 30 seconds for application (${SERVICE_NAME}) to be fully available..."
sleep 30

# 2. Run the OWASP ZAP Baseline scan in a container
#    --network=host: Allows the ZAP container to access services running on localhost.
#    -v "$(pwd)/reports:/zap/wrk/": Mounts the local reports directory into the container.
#    -t: Target URL -> The OpenAPI spec URL.
#    -g: Generate -> Creates a config file with issues to ignore (for managing false positives).
#    -r: Report -> The output HTML report file.
echo "Running OWASP ZAP baseline scan against ${TARGET_URL}..."
docker run --rm --network=host \
  -v "$(pwd)/${REPORT_DIR}:/zap/wrk/" \
  "${ZAP_IMAGE}" zap-baseline.py \
  -t "${TARGET_URL}" \
  -g "zap-generated.conf" \
  -r "${REPORT_FILE}"

# 3. Clean up the environment
echo "Shutting down all services..."
${COMPOSE_CMD} down

echo "✅ DAST scan complete. Report available in ./${REPORT_DIR}/${REPORT_FILE}"