#!/usr/bin/env bash
# test-api.sh — Full API smoke test
# Usage: ./test-api.sh [base_url] [keycloak_url] [admin_user] [admin_password] [regular_user] [regular_password]
set -uo pipefail

BASE_URL="${1:-http://localhost:8080}"
KEYCLOAK_URL="${2:-http://localhost:8180}"
ADMIN_USER="${3:-qa-admin-user}"
ADMIN_PASSWORD="${4:-test}"
REGULAR_USER="${5:-qa-user-no-hermandad}"
REGULAR_PASSWORD="${6:-test}"

GREEN='\033[0;32m'; RED='\033[0;31m'; NC='\033[0m'
PASS=0; FAIL=0

check() {
  local label="$1" status="$2"
  if [ "$status" -eq 0 ]; then
    echo -e "  ${GREEN}✓${NC} $label"; PASS=$((PASS + 1))
  else
    echo -e "  ${RED}✗${NC} $label"; FAIL=$((FAIL + 1))
  fi
}

# Login
echo "=== 1. Login as Admin ==="
ADMIN_TOKEN=$(curl -sf -X POST "$KEYCLOAK_URL/realms/semana-santa/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=$ADMIN_USER&password=$ADMIN_PASSWORD" \
  | jq -r '.access_token')
check "Get admin token" $([ -n "$ADMIN_TOKEN" ] && echo 0 || echo 1)

echo "=== 2. Login as Regular User ==="
REGULAR_TOKEN=$(curl -sf -X POST "$KEYCLOAK_URL/realms/semana-santa/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=$REGULAR_USER&password=$REGULAR_PASSWORD" \
  | jq -r '.access_token')
check "Get regular token" $([ -n "$REGULAR_TOKEN" ] && echo 0 || echo 1)

# Create hermandad with unique name
echo "=== 3. Hermandad Service ==="
SUFFIX=$(date +%s)$(shuf -i 1000-9999 -n 1)
HERMANDAD=$(curl -s -X POST "$BASE_URL/api/hermandades" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{\"name\":\"Test H $SUFFIX\",\"city\":\"Sevilla\",\"foundedYear\":1850}")
HID=$(echo "$HERMANDAD" | jq -r '.id')
check "Create hermandad → $HID" $([ "$HID" != "null" ] && [ -n "$HID" ] && echo 0 || echo 1)

curl -s -o /dev/null "$BASE_URL/api/hermandades/$HID" -H "Authorization: Bearer $ADMIN_TOKEN"
check "Get hermandad" $?

curl -s -o /dev/null "$BASE_URL/api/hermandades" -H "Authorization: Bearer $ADMIN_TOKEN"
check "List hermandads" $?

# Create procesion
echo "=== 4. Procesion Service ==="
PROCESION=$(curl -s -X POST "$BASE_URL/api/procesiones" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{\"hermandadId\":\"$HID\",\"date\":\"2026-04-01\",\"time\":\"18:00:00\"}")
PID=$(echo "$PROCESION" | jq -r '.id')
check "Create procesion → $PID" $([ "$PID" != "null" ] && [ -n "$PID" ] && echo 0 || echo 1)

# Wait for outbox poller (5s) + Kafka consumer: max 12s should be enough
echo "  Waiting for procesion event to propagate (outbox→Kafka→consumer)..."
sleep 12

curl -s -o /dev/null "$BASE_URL/api/procesiones/$PID" -H "Authorization: Bearer $ADMIN_TOKEN"
check "Get procesion" $?

curl -s -o /dev/null "$BASE_URL/api/procesiones?hermandadId=$HID&size=5" -H "Authorization: Bearer $ADMIN_TOKEN"
check "List procesiones" $?

# Create marcha
echo "=== 5. Repertorio Service — Marchas ==="
MARCHA=$(curl -s -X POST "$BASE_URL/api/marchas" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"title":"Test Marcha","composer":"Test Composer","bandType":"BANDA_PALIO","durationSeconds":420}')
MID=$(echo "$MARCHA" | jq -r '.id')
check "Create marcha → $MID" $([ "$MID" != "null" ] && [ -n "$MID" ] && echo 0 || echo 1)

curl -s -o /dev/null "$BASE_URL/api/marchas/$MID" -H "Authorization: Bearer $ADMIN_TOKEN"
check "Get marcha" $?

curl -s -o /dev/null "$BASE_URL/api/marchas" -H "Authorization: Bearer $ADMIN_TOKEN"
check "List marchas" $?

curl -s -o /dev/null -X PUT "$BASE_URL/api/marchas/$MID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"title":"Updated Marcha","composer":"Test Composer","bandType":"AGRUPACION_MUSICAL","durationSeconds":480}'
check "Update marcha" $?

echo "=== 6. Update Admin Claim ==="
KC_TOKEN=$(curl -sf -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  | jq -r '.access_token')
check "Get KC admin token" $([ -n "$KC_TOKEN" ] && echo 0 || echo 1)

ADMIN_KC_ID=$(curl -sf "$KEYCLOAK_URL/admin/realms/semana-santa/users?username=$ADMIN_USER&exact=true" \
  -H "Authorization: Bearer $KC_TOKEN" | jq -r '.[0].id')
check "Get admin KC user ID" $([ -n "$ADMIN_KC_ID" ] && [ "$ADMIN_KC_ID" != "null" ] && echo 0 || echo 1)

UPDATE_BODY=$(python3 -c "
import json
hid = '$HID'
payload = json.dumps([{'hermandadId': hid, 'role': 'HERMANDAD_ADMIN'}])
body = {'attributes': {'hermandad_memberships': [payload]}}
print(json.dumps(body))
")
curl -s -X PUT "$KEYCLOAK_URL/admin/realms/semana-santa/users/$ADMIN_KC_ID" \
  -H "Authorization: Bearer $KC_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$UPDATE_BODY" \
  -o /dev/null
check "Update admin membership claim for new hermandad" $?

echo "=== 7. Re-login as Admin (fresh token with claim) ==="
ADMIN_TOKEN=$(curl -sf -X POST "$KEYCLOAK_URL/realms/semana-santa/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=$ADMIN_USER&password=$ADMIN_PASSWORD" \
  | jq -r '.access_token')
check "Get fresh admin token" $([ -n "$ADMIN_TOKEN" ] && echo 0 || echo 1)

echo "=== 8. Repertorio Service — Cruceta ==="
HC=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/hermandades/$HID/procesiones/$PID/cruceta" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{\"items\":[{\"marchaId\":\"$MID\",\"orderIndex\":0,\"notes\":\"Salida\"}]}")
check "Define cruceta (expect 200)" $([ "$HC" = "200" ] && echo 0 || echo 1)

curl -s -o /dev/null "$BASE_URL/api/hermandades/$HID/procesiones/$PID/cruceta" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
check "Get cruceta" $?

echo "=== 9. Auth Enforcement ==="
HC=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/hermandades/$HID/procesiones/$PID/cruceta" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $REGULAR_TOKEN" \
  -d "{\"items\":[{\"marchaId\":\"$MID\",\"orderIndex\":1}]}")
check "Regular user denied from cruceta (403)" $([ "$HC" = "403" ] && echo 0 || echo 1)

HC=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/marchas/$MID" \
  -H "Content-Type: application/json" \
  -d '{"title":"T","composer":"T","bandType":"BANDA_PALIO","durationSeconds":120}')
check "No auth on protected endpoint returns 401" $([ "$HC" = "401" ] && echo 0 || echo 1)

echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="
[ "$FAIL" -eq 0 ] || exit 1
