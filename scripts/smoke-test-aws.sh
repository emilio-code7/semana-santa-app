#!/usr/bin/env bash
# AWS smoke test — verifies the deployment in eu-south-2.
# Uses Cognito admin-initiate-auth (server-side) so no client secret is needed.
set -e

API_URL="${API_URL:?API_URL must be set}"
POOL="${COGNITO_POOL_ID:?COGNITO_POOL_ID must be set}"
CLIENT="${COGNITO_CLIENT_ID:?COGNITO_CLIENT_ID must be set}"
ADMIN_USERNAME="${ADMIN_USERNAME:?ADMIN_USERNAME must be set}"
MEMBER_USERNAME="${MEMBER_USERNAME:?MEMBER_USERNAME must be set}"
PASSWORD="${TEST_PASSWORD:?TEST_PASSWORD must be set}"
REGION="${AWS_REGION:-eu-south-2}"

PASSED=0
FAIL=0

green() { echo -e "\033[32m$1\033[0m"; }
red()   { echo -e "\033[31m$1\033[0m"; }

assert() {
  local label="$1" expected="$2" method="$3" path="$4" token="$5" body="${6:-}"
  local cmd=(curl -sL -o /tmp/smoke.json -w "%{http_code}" --max-time 15
             -X "$method" -H "Authorization: Bearer $token"
             -H "Content-Type: application/json")
  [ -n "$body" ] && cmd+=(-d "$body")
  cmd+=("$API_URL$path")
  local code=$("${cmd[@]}")
  if [ "$code" = "$expected" ]; then
    green "✓ $label → HTTP $code"
    PASSED=$((PASSED + 1))
  else
    red "✗ $label → HTTP $code (expected $expected)"
    FAIL=$((FAIL + 1))
  fi
}

echo "=== AWS Smoke Test — eu-south-2 ==="
echo ""

# ── Auth ──────────────────────────────────────────
echo "--- Auth ---"
ADMIN_TOKEN=$(aws cognito-idp admin-initiate-auth --region "$REGION" \
  --user-pool-id "$POOL" --client-id "$CLIENT" \
  --auth-flow ADMIN_USER_PASSWORD_AUTH \
  --auth-parameters "USERNAME=$ADMIN_USERNAME,PASSWORD=$PASSWORD" \
  --query "AuthenticationResult.AccessToken" --output text)
echo "  Admin authentication succeeded"

# No-hermandad user token (for negative tests)
NO_H_TOKEN=$(aws cognito-idp admin-initiate-auth --region "$REGION" \
  --user-pool-id "$POOL" --client-id "$CLIENT" \
  --auth-flow ADMIN_USER_PASSWORD_AUTH \
  --auth-parameters "USERNAME=$MEMBER_USERNAME,PASSWORD=$PASSWORD" \
  --query "AuthenticationResult.AccessToken" --output text)
echo "  No-hermandad authentication succeeded"
echo ""

# ── Hermandad ─────────────────────────────────────
# Unique suffix so repeated runs don't 409 on duplicate names
SUFFIX=$(date +%s)
echo "  Run ID: $SUFFIX"

echo "--- Hermandad ---"
assert "List hermandades (admin)"   200 GET    "/api/hermandades"              "$ADMIN_TOKEN"
assert "Create hermandad"           201 POST   "/api/hermandades"              "$ADMIN_TOKEN" "{\"name\":\"Smoke Test Hermandad $SUFFIX\",\"city\":\"Sevilla\",\"foundedYear\":2024}"

# Extract the created hermandad ID for later use
HERMANDAD_ID=$(cat /tmp/smoke.json | jq -r '.id // empty' 2>/dev/null)
echo "  Created hermandad: $HERMANDAD_ID"

# ── Marchas ───────────────────────────────────────
echo ""
echo "--- Marchas ---"
assert "List marchas (admin)"       200 GET    "/api/marchas"                  "$ADMIN_TOKEN"
assert "Create marcha"              201 POST   "/api/marchas"                  "$ADMIN_TOKEN" "{\"title\":\"Cristo del Amor $SUFFIX\",\"composer\":\"López Farfán\",\"bandType\":\"BANDA_PALIO\",\"durationSeconds\":300}"

MARCHA_ID=$(cat /tmp/smoke.json | jq -r '.id // empty' 2>/dev/null)
echo "  Created marcha: $MARCHA_ID"

# ── Procesiones ────────────────────────────────────
echo ""
echo "--- Procesiones ---"
assert "Create procesion"            201 POST   "/api/procesiones"              "$ADMIN_TOKEN" "{\"hermandadId\":\"$HERMANDAD_ID\",\"date\":\"2026-04-01\",\"time\":\"18:00:00\"}"

PROCESION_ID=$(cat /tmp/smoke.json | jq -r '.id // empty' 2>/dev/null)
echo "  Created procesion: $PROCESION_ID"

assert "List procesiones (admin)"    200 GET    "/api/procesiones?hermandadId=$HERMANDAD_ID" "$ADMIN_TOKEN"
assert "Get procesion by ID"         200 GET    "/api/procesiones/$PROCESION_ID" "$ADMIN_TOKEN"
assert "Change status to IN_PROGRESS" 200 PATCH "/api/procesiones/$PROCESION_ID/status" "$ADMIN_TOKEN" '{"newStatus":"IN_PROGRESS"}'
assert "Change status to COMPLETED"  200 PATCH "/api/procesiones/$PROCESION_ID/status" "$ADMIN_TOKEN" '{"newStatus":"COMPLETED"}'

# ── Auth enforcement ──────────────────────────────
echo ""
echo "--- Auth enforcement ---"
assert "List marchas (no auth)"      401 GET    "/api/marchas"                  ""
assert "Create hermandad (no auth)"  401 POST   "/api/hermandades"              ""  '{"name":"Test","city":"Test","foundedYear":2000}'
assert "Create procesion (no auth)"  401 POST   "/api/procesiones"              ""  "{\"hermandadId\":\"$HERMANDAD_ID\",\"date\":\"2026-04-01\",\"time\":\"18:00:00\"}"

# ── Results ───────────────────────────────────────
echo ""
echo "=== Summary ==="
TOTAL=$((PASSED + FAIL))
echo "Passed: $PASSED / $TOTAL"
[ $FAIL -gt 0 ] && red "Failed: $FAIL" && exit 1 || green "All $PASSED tests passed"
