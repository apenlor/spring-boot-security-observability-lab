#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

# --- Configuration ---
SERVICE_NAME="resource-server"
TARGET_URL="http://localhost:8081/v3/api-docs"
REPORT_DIR="reports"
REPORT_FILE="zap-report.html"
ZAP_IMAGE="zaproxy/zap-stable"
VAULT_SETUP_SERVICE="vault-setup"

# --- Environment Configuration ---
# Determine which environment files to use.
# In CI, we use the committed .env.ci and .secrets.env.ci files.
# Locally, we prefer the user's private .env and .secrets.env files.
if [[ "${GITHUB_ACTIONS}" == "true" ]]; then
  echo "CI environment detected. Creating .env file from .env.ci and .secrets.env from .secrets.env.ci"
  cp .env.ci .env
  cp .secrets.env.ci .secrets.env
else
  # For local runs, ensure .env and .secrets.env exist
  if [[ ! -f .env ]]; then
      echo "ERROR: Local .env file not found. Please create it from .env.example." >&2
      exit 1
  fi
  if [[ ! -f .secrets.env ]]; then
      echo "ERROR: Local .secrets.env file not found. Please create it from .secrets.env.example." >&2
      exit 1
  fi
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

# 1. Start the necessary services
echo "Starting infrastructure services (postgres, keycloak, vault, ${VAULT_SETUP_SERVICE}) and target service (${SERVICE_NAME})..."
# Ensure Vault and Vault Setup are included in the startup list
${COMPOSE_CMD} up -d postgres keycloak vault ${VAULT_SETUP_SERVICE} ${SERVICE_NAME}

echo "Waiting 30 seconds for all services to initialize and Vault to be populated..."
sleep 30

# 2. Run the OWASP ZAP Baseline scan in a container
#    --network=host: Allows the ZAP container to access services running on localhost.
#    -v "$(pwd)/reports:/zap/wrk/": Mounts the local reports directory into the container.
#    -t: Target URL -> The OpenAPI spec URL.
#    -g: Generate -> Creates a config file with issues to ignore (for managing false positives).
#    -r: Report -> The output HTML report file.
#    -I: do not return error on warnings
echo "Running OWASP ZAP baseline scan against ${TARGET_URL}..."

chmod 777 ${REPORT_DIR}

docker run --rm --network=host \
  -v "$(pwd)/${REPORT_DIR}:/zap/wrk/" \
  "${ZAP_IMAGE}" zap-baseline.py \
  -t "${TARGET_URL}" \
  -g "zap-generated.conf" \
  -r "${REPORT_FILE}" \
  -I

# 3. Clean up the environment
echo "Shutting down all services..."
${COMPOSE_CMD} down

# --- Post-Script Cleanup ---
# In CI, clean up the copied .env and .secrets.env files
if [[ "${GITHUB_ACTIONS}" == "true" ]]; then
    echo "CI environment detected. Cleaning up .env and .secrets.env files."
    rm -f .env .secrets.env
fi

echo "DAST Scan Orchestration Complete."