#!/usr/bin/env bash
# test-api.sh — Month-1 API smoke test
# Flow: hermandad → claim update → titular → procesion → pasos/route → plan finalize
#       → marcha CRUD → per-paso cruceta → run-sheet → advance → tenant isolation
# All API calls go through the gateway (BASE_URL); Keycloak on KEYCLOAK_URL.
# Status assertions use curl -w "%{http_code}" — never curl exit codes (0 even on 403/404/500).
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

TMP=$(mktemp)
trap 'rm -f "$TMP"' EXIT

check() {
  local label="$1" status="$2"
  if [ "$status" -eq 0 ]; then
    echo -e "  ${GREEN}✓${NC} $label"; PASS=$((PASS + 1))
  else
    echo -e "  ${RED}✗${NC} $label"; FAIL=$((FAIL + 1))
  fi
}

check_code() {
  local label="$1" actual="$2" expected="$3"
  if [ "$actual" = "$expected" ]; then
    echo -e "  ${GREEN}✓${NC} $label (got $actual)"; PASS=$((PASS + 1))
  else
    echo -e "  ${RED}✗${NC} $label (expected $expected, got $actual)"; FAIL=$((FAIL + 1))
  fi
}

check_created() {
  local label="$1" actual="$2"
  if [ "$actual" = "200" ] || [ "$actual" = "201" ]; then
    echo -e "  ${GREEN}✓${NC} $label (got $actual)"; PASS=$((PASS + 1))
  else
    echo -e "  ${RED}✗${NC} $label (expected 200/201, got $actual)"; FAIL=$((FAIL + 1))
  fi
}

login() {
  curl -sf -X POST "$KEYCLOAK_URL/realms/semana-santa/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=$1&password=$2" \
    | jq -r '.access_token'
}

echo "=== 1. Login Admin + Regular ==="
ADMIN_TOKEN=$(login "$ADMIN_USER" "$ADMIN_PASSWORD")
check "Get admin token" $([ -n "$ADMIN_TOKEN" ] && echo 0 || echo 1)
REGULAR_TOKEN=$(login "$REGULAR_USER" "$REGULAR_PASSWORD")
check "Get regular token" $([ -n "$REGULAR_TOKEN" ] && echo 0 || echo 1)

echo "=== 2. Create Hermandad A ==="
SUFFIX=$(date +%s)$(shuf -i 1000-9999 -n 1)
H_BODY=$(jq -n --arg name "Test H $SUFFIX" '{name:$name, city:"Sevilla", foundedYear:1850}')
H_CODE=$(curl -s -o "$TMP" -w "%{http_code}" -X POST "$BASE_URL/api/hermandades" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "$H_BODY")
HID=$(jq -r '.id' "$TMP")
check_created "Create hermandad A (HID=$HID)" "$H_CODE"

echo "=== 3. Update Admin Claim + Re-login (KC24: PUT full user representation) ==="
KC_TOKEN=$(curl -sf -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  | jq -r '.access_token')
check "Get KC admin token" $([ -n "$KC_TOKEN" ] && echo 0 || echo 1)

ADMIN_KC_USER=$(curl -s "$KEYCLOAK_URL/admin/realms/semana-santa/users?username=$ADMIN_USER&exact=true" \
  -H "Authorization: Bearer $KC_TOKEN" | jq '.[0]')
ADMIN_KC_ID=$(echo "$ADMIN_KC_USER" | jq -r '.id')
check "Get admin KC user full representation" $([ -n "$ADMIN_KC_ID" ] && [ "$ADMIN_KC_ID" != "null" ] && echo 0 || echo 1)

MEMBERSHIP=$(printf '[{"hermandadId":"%s","role":"HERMANDAD_ADMIN"}]' "$HID")
# Modify ONLY the membership attribute, then PUT the FULL representation back.
# A body with only `attributes` wipes email/firstName/lastName → KC24 VERIFY_PROFILE
# blocks ALL logins realm-wide.
UPDATE_BODY=$(echo "$ADMIN_KC_USER" | jq --arg m "$MEMBERSHIP" '.attributes.hermandad_memberships = [$m]')
U_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$KEYCLOAK_URL/admin/realms/semana-santa/users/$ADMIN_KC_ID" \
  -H "Authorization: Bearer $KC_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$UPDATE_BODY")
check_code "PUT full user with updated membership claim" "$U_CODE" 204

ADMIN_TOKEN=$(login "$ADMIN_USER" "$ADMIN_PASSWORD")
check "Re-login admin (fresh token, claim fixed at issuance)" $([ -n "$ADMIN_TOKEN" ] && echo 0 || echo 1)

echo "=== 4. Create Titular ==="
TITULAR_BODY=$(jq -n '{name:"Jesús Nazareno",description:"Titular principal de la hermandad"}')
T_CODE=$(curl -s -o "$TMP" -w "%{http_code}" -X POST "$BASE_URL/api/hermandades/$HID/titulares" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "$TITULAR_BODY")
TID=$(jq -r '.id' "$TMP")
check_created "Create titular (TID=$TID)" "$T_CODE"

echo "=== 5. Create Procesion ==="
PROCESION_BODY=$(jq -n --arg hid "$HID" '{hermandadId:$hid, date:"2026-04-13", time:"18:00:00"}')
P_CODE=$(curl -s -o "$TMP" -w "%{http_code}" -X POST "$BASE_URL/api/procesiones" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "$PROCESION_BODY")
PID=$(jq -r '.id' "$TMP")
check_created "Create procesion (PID=$PID)" "$P_CODE"

echo "=== 6. Wait for outbox → Kafka propagation (titular→procesion, procesion→repertorio) ==="
sleep 12

echo "=== 7. Replace Pasos ==="
PASOS_BODY=$(jq -n --arg tid "$TID" '{pasos:[{position:0,titularId:$tid,notes:"Paso de salida"},{position:1,titularId:$tid,notes:"Paso de recogida"}]}')
PS_CODE=$(curl -s -o "$TMP" -w "%{http_code}" -X PUT "$BASE_URL/api/hermandades/$HID/procesiones/$PID/pasos" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "$PASOS_BODY")
P1=$(jq -r '.pasos[0].id' "$TMP")
P2=$(jq -r '.pasos[1].id' "$TMP")
check_code "Replace pasos (P1=$P1, P2=$P2)" "$PS_CODE" 200

echo "=== 8. Replace Route ==="
ROUTE_BODY=$(jq -n '{sections:[{name:"Salida",position:0},{name:"Calle Feria",position:1}]}')
R_CODE=$(curl -s -o "$TMP" -w "%{http_code}" -X PUT "$BASE_URL/api/hermandades/$HID/procesiones/$PID/route" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "$ROUTE_BODY")
R1=$(jq -r '.sections[0].id' "$TMP")
R2=$(jq -r '.sections[1].id' "$TMP")
check_code "Replace route (R1=$R1, R2=$R2)" "$R_CODE" 200

echo "=== 9. Finalize Plan ==="
F_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/hermandades/$HID/procesiones/$PID/plan/finalize" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check_code "Finalize plan" "$F_CODE" 200

echo "=== 10. Wait for plan-finalized projection (→ repertorio KnownPaso/KnownRouteSection) ==="
sleep 12

echo "=== 11. Marcha CRUD ==="
MARCHA_BODY=$(jq -n '{title:"Test Marcha",composer:"Test Composer",bandType:"BANDA_PALIO",durationSeconds:420}')
M_CODE=$(curl -s -o "$TMP" -w "%{http_code}" -X POST "$BASE_URL/api/marchas" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "$MARCHA_BODY")
MID=$(jq -r '.id' "$TMP")
check_created "Create marcha (MID=$MID)" "$M_CODE"

MG_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/marchas/$MID" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check_code "Get marcha" "$MG_CODE" 200

MARCHA_UPDATE=$(jq -n '{title:"Updated Marcha",composer:"Test Composer",bandType:"AGRUPACION_MUSICAL",durationSeconds:480}')
MU_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/marchas/$MID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "$MARCHA_UPDATE")
check_code "Update marcha" "$MU_CODE" 200

echo "=== 12. Per-Paso Cruceta ==="
CRUCETA_BODY=$(jq -n --arg mid "$MID" --arg r1 "$R1" --arg r2 "$R2" \
  '{items:[{marchaId:$mid, routeSectionId:$r1, sequenceWithinSection:0, notes:"Salida"},{marchaId:$mid, routeSectionId:$r2, sequenceWithinSection:0, notes:"Recogida"}]}')
C1_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/hermandades/$HID/procesiones/$PID/pasos/$P1/cruceta" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "$CRUCETA_BODY")
check_code "PUT cruceta P1" "$C1_CODE" 200
C2_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/hermandades/$HID/procesiones/$PID/pasos/$P2/cruceta" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "$CRUCETA_BODY")
check_code "PUT cruceta P2" "$C2_CODE" 200

echo "=== 13. Run-Sheet ==="
RS_CODE=$(curl -s -o "$TMP" -w "%{http_code}" "$BASE_URL/api/hermandades/$HID/procesiones/$PID/pasos/$P1/cruceta/run-sheet" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check_code "Get run-sheet for P1" "$RS_CODE" 200
FIRST_ITEM_ID=$(jq -r '.sections[0].items[0].itemId' "$TMP")
check "Extract first cruceta item id → $FIRST_ITEM_ID" $([ -n "$FIRST_ITEM_ID" ] && [ "$FIRST_ITEM_ID" != "null" ] && echo 0 || echo 1)

echo "=== 14. Advance Current Item ==="
ADVANCE_BODY=$(jq -n --arg rid "$R1" --arg iid "$FIRST_ITEM_ID" '{routeSectionId:$rid, crucetaItemId:$iid}')
A_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/hermandades/$HID/procesiones/$PID/pasos/$P1/cruceta/current" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "$ADVANCE_BODY")
check_code "Advance to first cruceta item" "$A_CODE" 200

echo "=== 15. Tenant Isolation (403 reads AND writes) ==="
# Regular user (no membership claim)
REG_GET_CRUCETA=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/hermandades/$HID/procesiones/$PID/pasos/$P1/cruceta" \
  -H "Authorization: Bearer $REGULAR_TOKEN")
check_code "Regular GET cruceta → 403" "$REG_GET_CRUCETA" 403
REG_PUT_CRUCETA=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/hermandades/$HID/procesiones/$PID/pasos/$P1/cruceta" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $REGULAR_TOKEN" \
  -d "$CRUCETA_BODY")
check_code "Regular PUT cruceta → 403" "$REG_PUT_CRUCETA" 403
# NOTE: procesion-service's @EnableMethodSecurity is INACTIVE (open issue) — a
# no-membership user can read/write pasos on its own-hermandad path. Tenant
# isolation there is enforced at the SERVICE level for cross-tenant paths only
# (hermandad B checks below). Repertorio cruceta IS claim-guarded (active).
REG_GET_RUNSHEET=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/hermandades/$HID/procesiones/$PID/pasos/$P1/cruceta/run-sheet" \
  -H "Authorization: Bearer $REGULAR_TOKEN")
check_code "Regular GET run-sheet → 403" "$REG_GET_RUNSHEET" 403

# Hermandad B: refreshed admin token has NO claim for B (guards are claim-based)
B_BODY=$(jq -n --arg name "Test H B $SUFFIX" '{name:$name, city:"Sevilla", foundedYear:1860}')
B_CODE=$(curl -s -o "$TMP" -w "%{http_code}" -X POST "$BASE_URL/api/hermandades" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "$B_BODY")
BID=$(jq -r '.id' "$TMP")
check_created "Create hermandad B (BID=$BID)" "$B_CODE"

BID_GET_PASOS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/hermandades/$BID/procesiones/$PID/pasos" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check_code "Admin GET pasos for B → 403" "$BID_GET_PASOS" 403
BID_PUT_PASOS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/hermandades/$BID/procesiones/$PID/pasos" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "$PASOS_BODY")
check_code "Admin PUT pasos for B → 403" "$BID_PUT_PASOS" 403
BID_PUT_ROUTE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/hermandades/$BID/procesiones/$PID/route" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "$ROUTE_BODY")
check_code "Admin PUT route for B → 403" "$BID_PUT_ROUTE" 403
BID_PUT_CRUCETA=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/hermandades/$BID/procesiones/$PID/pasos/$P1/cruceta" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "$CRUCETA_BODY")
check_code "Admin PUT cruceta for B → 403" "$BID_PUT_CRUCETA" 403

# No token at all
NOAUTH_PUT_MARCHA=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/marchas/$MID" \
  -H "Content-Type: application/json" \
  -d "$MARCHA_UPDATE")
check_code "No token PUT marcha → 401" "$NOAUTH_PUT_MARCHA" 401

echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="
[ "$FAIL" -eq 0 ] || exit 1
