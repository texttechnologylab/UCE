#!/usr/bin/env bash
set -euo pipefail

# --- CONFIGURATION ---

KEYCLOAK_URL="${KEYCLOAK_AUTH_SERVER_URL:-${UCE_AUTH_PUBLIC_URL:-${KEYCLOAK_URL:-http://localhost:8080}}}"
ADMIN_REALM="${ADMIN_REALM:-master}"
ADMIN_USER="${KC_ADMIN_USERNAME:-${ADMIN_USER:-admin}}"
ADMIN_PASSWORD="${KC_ADMIN_PW:-${ADMIN_PASSWORD:-admin}}"
REALM="${KEYCLOAK_REALM:-${KC_REALM:-uce}}"
GROUP_NAME="CORE-Proband"
CLIENT_ID="${KEYCLOAK_CLIENT:-${KC_CLIENT_ID:-uce-web}}"   # only for building login URL

INPUT_FILE="${1:-}"
OUTPUT_FILE="${2:-provisioned-users.csv}"

if [[ -z "$INPUT_FILE" || ! -f "$INPUT_FILE" ]]; then
  echo "Usage: $0 <hashes.txt> [output.csv]" >&2
  exit 1
fi

KCADM="${KCADM:-kcadm.sh}"
# KCADM="docker exec -i uce-keycloak-auth /opt/keycloak/bin/kcadm.sh"
command -v jq >/dev/null 2>&1 || { echo "jq is required" >&2; exit 1; }

# --- LOGIN ---

"$KCADM" config credentials \
  --server "$KEYCLOAK_URL" \
  --realm "$ADMIN_REALM" \
  --user "$ADMIN_USER" \
  --password "$ADMIN_PASSWORD"

# --- ENSURE/GUARANTEE GROUP EXISTS ---

GROUP_JSON=$("$KCADM" get groups -r "$REALM" -q "search=$GROUP_NAME")
GROUP_ID=$(echo "$GROUP_JSON" | jq -r '.[] | select(.name=="'"$GROUP_NAME"'") | .id')

if [[ -z "$GROUP_ID" || "$GROUP_ID" == "null" ]]; then
  echo "Group '$GROUP_NAME' not found in realm '$REALM', creating..."
  "$KCADM" create groups -r "$REALM" -s "name=$GROUP_NAME" >/dev/null

  # re-fetch to get ID
  GROUP_JSON=$("$KCADM" get groups -r "$REALM" -q "search=$GROUP_NAME")
  GROUP_ID=$(echo "$GROUP_JSON" | jq -r '.[] | select(.name=="'"$GROUP_NAME"'") | .id')

  if [[ -z "$GROUP_ID" || "$GROUP_ID" == "null" ]]; then
    echo "Failed to create or retrieve group '$GROUP_NAME' in realm '$REALM'." >&2
    exit 1
  fi
fi

echo "Using group '$GROUP_NAME' (id=$GROUP_ID)"

# --- PREPARE OUTPUT ---

echo "hash,username,userId,tempPassword,loginUrl" > "$OUTPUT_FILE"

KEYCLOAK_BASE="$(echo "${KEYCLOAK_URL}" | sed -E 's#/*$##; s#/auth$##')"
LOGIN_BASE="${KEYCLOAK_BASE}/realms/${REALM}/protocol/openid-connect/auth"
REDIRECT_URI="${UCE_AUTH_REDIRECT_URL:-${REDIRECT_URI:-https://your-app.example/uce/callback}}"

# --- MAIN LOOP ---

while IFS= read -r HASH || [[ -n "$HASH" ]]; do
  [[ -z "$HASH" || "$HASH" =~ ^# ]] && continue

  USERNAME="$HASH"

  # Check if user exists
  USERS_JSON=$("$KCADM" get users -r "$REALM" -q "username=$USERNAME" -q "exact=true")
  USER_ID=$(echo "$USERS_JSON" | jq -r '.[0].id // empty')

  if [[ -z "$USER_ID" ]]; then
    echo "Creating user: $USERNAME"

    "$KCADM" create users -r "$REALM" \
      -s "username=$USERNAME" \
      -s "enabled=true" \
      >/dev/null

    USERS_JSON=$("$KCADM" get users -r "$REALM" -q "username=$USERNAME" -q "exact=true")
    USER_ID=$(echo "$USERS_JSON" | jq -r '.[0].id')

    if [[ -z "$USER_ID" || "$USER_ID" == "null" ]]; then
      echo "Failed to obtain userId for $USERNAME" >&2
      continue
    fi
  else
    echo "User exists: $USERNAME (id=$USER_ID)"
  fi

  # Add user to group (idempotent)
  "$KCADM" create "users/$USER_ID/groups/$GROUP_ID" -r "$REALM" >/dev/null || true

  # Generate temp password
  TEMP_PW=$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 16)

  # Set temporary password
  "$KCADM" update "users/$USER_ID/reset-password" -r "$REALM" \
    -s "type=password" \
    -s "value=$TEMP_PW" \
    -s "temporary=true"

  # Build login URL (same for all users)
#   LOGIN_URL="${LOGIN_BASE}?client_id=${CLIENT_ID}&response_type=code&scope=openid&redirect_uri=$(python3 - <<EOF
# import urllib.parse, sys
# print(urllib.parse.quote(sys.argv[1], safe=""))
# EOF
# "$REDIRECT_URI"
# )"

  echo "$HASH,$USERNAME,$USER_ID,$TEMP_PW" >> "$OUTPUT_FILE"
done < "$INPUT_FILE"

echo "Done. Results written to: $OUTPUT_FILE"
