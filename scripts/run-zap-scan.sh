#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

# --- Configuration ---
SERVICE_NAME="resource-server"
TARGET_URL="http://localhost:8081/v3/api-docs"
REPORT_DIR="reports"
REPORT_FILE="zap-report.html"
ZAP_IMAGE="zaproxy/zap-stable"

# Create reports directory if it doesn't exist
mkdir -p ${REPORT_DIR}

# --- Main Script ---

echo "Starting DAST Scan Orchestration..."

# 1. Start the service to be scanned in the background
echo "Starting dependent services (Keycloak) and target service (${SERVICE_NAME})..."
# We bring up dependencies first to ensure they are ready.
docker-compose up -d postgres keycloak
# Give Keycloak a moment to initialize before the resource-server starts and tries to connect.
echo "Waiting 30 seconds for Keycloak to initialize..."
sleep 30
docker-compose up -d --build ${SERVICE_NAME}

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
#    Using `docker-compose down` ensures all services are stopped and networks are removed.
echo "Shutting down all services..."
docker-compose down

echo "✅ DAST scan complete. Report available in ./${REPORT_DIR}/${REPORT_FILE}"