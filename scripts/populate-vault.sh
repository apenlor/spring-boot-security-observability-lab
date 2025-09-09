#!/bin/sh
set -e

# ==============================================================================
# POPULATE VAULT SCRIPT
# ==============================================================================
# This script runs inside a container on the same Docker network as the Vault
# server. It uses the vault CLI to populate secrets over the network.
# ==============================================================================

# --- Helper Functions ---
check_env_vars() {
  required_vars="VAULT_ADDR VAULT_TOKEN ACTUATOR_USERNAME ACTUATOR_PASSWORD ACTUATOR_ROLES WEB_CLIENT_SECRET"
  for var in ${required_vars}; do
    if [ -z "$(eval echo \"\$$var\")" ]; then
      echo "ERROR: Required environment variable ${var} is not set."
      exit 1
    fi
  done
}

# --- Main Execution ---
check_env_vars

echo "--- Populating Vault with application secrets ---"
vault kv put secret/resource-server \
  actuator.username="${ACTUATOR_USERNAME}" \
  actuator.password="${ACTUATOR_PASSWORD}" \
  actuator.roles="${ACTUATOR_ROLES}"

vault kv put secret/web-client \
  client-secret="${WEB_CLIENT_SECRET}" \
  actuator.username="${ACTUATOR_USERNAME}" \
  actuator.password="${ACTUATOR_PASSWORD}" \
  actuator.roles="${ACTUATOR_ROLES}"

echo "✅ Vault population complete."