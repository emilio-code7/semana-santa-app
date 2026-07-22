#!/usr/bin/env bash
# AWS smoke test — verifies the deployment in eu-south-2.
# Uses Cognito admin-initiate-auth (server-side) so no client secret is needed.
set -e

IP="${1:-35.42.55.101}"
POOL="eu-south-2_ETraAnuAq"
CLIENT="3bhlf9f4686bv6fpho7ap7mmp"
REGION="eu-south-2"
PASSWORD="Test1234!"

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
  cmd+=("http://$IP$path")
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
  --auth-parameters "USERNAME=qa-admin-user,PASSWORD=$PASSWORD" \
  --query "AuthenticationResult.AccessToken" --output text)
echo "  Admin token: ${ADMIN_TOKEN:0:30}..."

# No-hermandad user token (for negative tests)
NO_H_TOKEN=$(aws cognito-idp admin-initiate-auth --region "$REGION" \
  --user-pool-id "$POOL" --client-id "$CLIENT" \
  --auth-flow ADMIN_USER_PASSWORD_AUTH \
  --auth-parameters "USERNAME=qa-user-no-hermandad,PASSWORD=$PASSWORD" \
  --query "AuthenticationResult.AccessToken" --output text)
echo "  No-hermandad token: ${NO_H_TOKEN:0:30}..."
echo ""

# ── Hermandad ─────────────────────────────────────
echo "--- Hermandad ---"
assert "List hermandades (admin)"   200 GET    "/api/hermandades"              "$ADMIN_TOKEN"
assert "Create hermandad"           201 POST   "/api/hermandades"              "$ADMIN_TOKEN" '{"name":"Smoke Test Hermandad","city":"Sevilla","foundedYear":2024}'

# Extract the created hermandad ID for later use
HERMANDAD_ID=$(cat /tmp/smoke.json | jq -r '.id // empty' 2>/dev/null)
echo "  Created hermandad: $HERMANDAD_ID"

# ── Marchas ───────────────────────────────────────
echo ""
echo "--- Marchas ---"
assert "List marchas (admin)"       200 GET    "/api/marchas"                  "$ADMIN_TOKEN"
assert "Create marcha"              201 POST   "/api/marchas"                  "$ADMIN_TOKEN" '{"title":"Cristo del Amor","composer":"López Farfán","bandType":"BANDA_PALIO","durationSeconds":300}'

MARCHA_ID=$(cat /tmp/smoke.json | jq -r '.id // empty' 2>/dev/null)
echo "  Created marcha: $MARCHA_ID"

# ── Auth enforcement ──────────────────────────────
echo ""
echo "--- Auth enforcement ---"
assert "List marchas (no auth)"     401 GET    "/api/marchas"                  ""
assert "Create hermandad (no auth)" 401 POST   "/api/hermandades"              ""  '{"name":"Test","city":"Test","foundedYear":2000}'

# ── Results ───────────────────────────────────────
echo ""
echo "=== Summary ==="
TOTAL=$((PASSED + FAIL))
echo "Passed: $PASSED / $TOTAL"
[ $FAIL -gt 0 ] && red "Failed: $FAIL" && exit 1 || green "All $PASSED tests passed"
