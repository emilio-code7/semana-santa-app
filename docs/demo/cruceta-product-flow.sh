#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────────────────────────────────────
# Cruceta Product-Flow Demo — Hermandad → Titular → Procesion → Pasos → Route
#                        → Finalize → Cruceta → Run-sheet → Advance
# Requires: docker-compose stack running (core profile), curl, jq
# NOTE: uses REAL tokens and the seeded marcha 'Amarguras'. Re-runs get a fresh
# hermandad via a timestamp suffix, so no unique-name collisions.
# ─────────────────────────────────────────────────────────────────────────────

GATEWAY="http://localhost:8080"
KEYCLOAK="http://localhost:8180"
CLIENT_ID="semana-santa-client"
CLIENT_SECRET="secret"
USERNAME="qa-admin-user"
PASSWORD="test"
SEEDED_MARCHA="a0000001-0000-0000-0000-000000000001"
SUFFIX="$(date +%s)"

fail() {
  echo "❌ FAIL: $*" >&2
  exit 1
}

# Step 0 — Prerequisites
command -v curl >/dev/null 2>&1 || fail "curl is required"
command -v jq   >/dev/null 2>&1 || fail "jq is required"

# Step 1 — Get JWT (direct grant, semana-santa realm)
echo "🔐 Getting JWT from Keycloak..."
TOKEN=$(curl -s -X POST "$KEYCLOAK/realms/semana-santa/protocol/openid-connect/token" \
  -d "grant_type=password&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET&username=$USERNAME&password=$PASSWORD" \
  | jq -r '.access_token')
[[ -n "$TOKEN" && "$TOKEN" != "null" ]] || fail "Failed to obtain JWT"
echo "   Token acquired"

# Step 2 — Create hermandad A
echo "📋 Creating hermandad A..."
HERMANDAD_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY/api/hermandades" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Hermandad del Silencio $SUFFIX\",\"city\":\"Sevilla\",\"foundedYear\":1350}")
HERMANDAD_HTTP_CODE=$(echo "$HERMANDAD_RESPONSE" | tail -1)
HERMANDAD_BODY=$(echo "$HERMANDAD_RESPONSE" | sed '$d')
[[ "$HERMANDAD_HTTP_CODE" =~ ^2 ]] || fail "Create hermandad failed (HTTP $HERMANDAD_HTTP_CODE): $HERMANDAD_BODY"
HERMANDAD_ID=$(echo "$HERMANDAD_BODY" | jq -r '.id')
[[ -n "$HERMANDAD_ID" && "$HERMANDAD_ID" != "null" ]] || fail "Hermandad ID is empty"
echo "✅ Hermandad A created: $HERMANDAD_ID"

# Step 3 — Grant hermandad-admin claim + re-login (CRITICAL)
echo "🔑 Granting hermandad-admin claim to $USERNAME..."
ADMIN_TOKEN=$(curl -s -X POST "$KEYCLOAK/realms/master/protocol/openid-connect/token" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  | jq -r '.access_token')
[[ -n "$ADMIN_TOKEN" && "$ADMIN_TOKEN" != "null" ]] || fail "Failed to obtain Keycloak admin token"
KC_USER_ID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$KEYCLOAK/admin/realms/semana-santa/users?username=$USERNAME&exact=true" | jq -r '.[0].id')
[[ -n "$KC_USER_ID" && "$KC_USER_ID" != "null" ]] || fail "User $USERNAME not found in Keycloak"
# KC24 pitfall: PUT the FULL user representation back. A body with only
# `attributes` wipes email/firstName/lastName and blocks ALL logins.
FULL_USER=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$KEYCLOAK/admin/realms/semana-santa/users/$KC_USER_ID")
MEMBERSHIP_CLAIM=$(jq -nc --arg hid "$HERMANDAD_ID" '[{hermandadId: $hid, role: "HERMANDAD_ADMIN"}]')
UPDATED_USER=$(echo "$FULL_USER" | jq --argjson m "$MEMBERSHIP_CLAIM" \
  '.attributes.hermandad_memberships = [$m | tostring]')
CLAIM_HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$UPDATED_USER" \
  "$KEYCLOAK/admin/realms/semana-santa/users/$KC_USER_ID")
[[ "$CLAIM_HTTP_CODE" =~ ^2 ]] || fail "Grant claim failed (HTTP $CLAIM_HTTP_CODE)"
echo "   Claim granted — re-logging in for a fresh token..."
TOKEN=$(curl -s -X POST "$KEYCLOAK/realms/semana-santa/protocol/openid-connect/token" \
  -d "grant_type=password&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET&username=$USERNAME&password=$PASSWORD" \
  | jq -r '.access_token')
[[ -n "$TOKEN" && "$TOKEN" != "null" ]] || fail "Re-login failed after claim grant"
echo "✅ Claim granted and token refreshed"

# Step 4 — Create titular
echo "📋 Creating titular..."
TITULAR_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY/api/hermandades/$HERMANDAD_ID/titulares" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Jesús Nazareno","description":"Paso de misterio"}')
TITULAR_HTTP_CODE=$(echo "$TITULAR_RESPONSE" | tail -1)
TITULAR_BODY=$(echo "$TITULAR_RESPONSE" | sed '$d')
[[ "$TITULAR_HTTP_CODE" =~ ^2 ]] || fail "Create titular failed (HTTP $TITULAR_HTTP_CODE): $TITULAR_BODY"
TITULAR_ID=$(echo "$TITULAR_BODY" | jq -r '.id')
[[ -n "$TITULAR_ID" && "$TITULAR_ID" != "null" ]] || fail "Titular ID is empty"
echo "✅ Titular created: $TITULAR_ID"

# Step 5 — Create procesion
echo "📋 Creating procesion..."
PROCESION_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY/api/procesiones" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"hermandadId\":\"$HERMANDAD_ID\",\"date\":\"2026-04-13\",\"time\":\"18:00:00\"}")
PROCESION_HTTP_CODE=$(echo "$PROCESION_RESPONSE" | tail -1)
PROCESION_BODY=$(echo "$PROCESION_RESPONSE" | sed '$d')
[[ "$PROCESION_HTTP_CODE" =~ ^2 ]] || fail "Create procesion failed (HTTP $PROCESION_HTTP_CODE): $PROCESION_BODY"
PROCESION_ID=$(echo "$PROCESION_BODY" | jq -r '.id')
[[ -n "$PROCESION_ID" && "$PROCESION_ID" != "null" ]] || fail "Procesion ID is empty"
echo "✅ Procesion created: $PROCESION_ID"

# Step 6 — Wait for outbox poller + Kafka consumers
echo "⏳ Waiting for outbox → Kafka → consumers (~12s)..."
sleep 12

# Step 7 — Replace pasos
echo "📋 Replacing pasos..."
PASOS_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$GATEWAY/api/hermandades/$HERMANDAD_ID/procesiones/$PROCESION_ID/pasos" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"pasos\":[{\"position\":0,\"titularId\":\"$TITULAR_ID\",\"notes\":\"Cristo\"},{\"position\":1,\"titularId\":\"$TITULAR_ID\",\"notes\":\"Palio\"}]}")
PASOS_HTTP_CODE=$(echo "$PASOS_RESPONSE" | tail -1)
PASOS_BODY=$(echo "$PASOS_RESPONSE" | sed '$d')
[[ "$PASOS_HTTP_CODE" =~ ^2 ]] || fail "Replace pasos failed (HTTP $PASOS_HTTP_CODE): $PASOS_BODY"
PASO_1_ID=$(echo "$PASOS_BODY" | jq -r '.pasos[] | select(.position==0) | .id')
PASO_2_ID=$(echo "$PASOS_BODY" | jq -r '.pasos[] | select(.position==1) | .id')
[[ -n "$PASO_1_ID" && -n "$PASO_2_ID" && "$PASO_1_ID" != "null" && "$PASO_2_ID" != "null" ]] \
  || fail "Paso IDs missing from response: $PASOS_BODY"
echo "✅ Pasos defined: P1=$PASO_1_ID P2=$PASO_2_ID"

# Step 8 — Replace route
echo "🗺️  Replacing route sections..."
ROUTE_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$GATEWAY/api/hermandades/$HERMANDAD_ID/procesiones/$PROCESION_ID/route" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sections":[{"name":"Salida","position":0},{"name":"Calle Feria","position":1}]}')
ROUTE_HTTP_CODE=$(echo "$ROUTE_RESPONSE" | tail -1)
ROUTE_BODY=$(echo "$ROUTE_RESPONSE" | sed '$d')
[[ "$ROUTE_HTTP_CODE" =~ ^2 ]] || fail "Replace route failed (HTTP $ROUTE_HTTP_CODE): $ROUTE_BODY"
SECTION_1_ID=$(echo "$ROUTE_BODY" | jq -r '.sections[] | select(.position==0) | .id')
SECTION_2_ID=$(echo "$ROUTE_BODY" | jq -r '.sections[] | select(.position==1) | .id')
[[ -n "$SECTION_1_ID" && -n "$SECTION_2_ID" && "$SECTION_1_ID" != "null" && "$SECTION_2_ID" != "null" ]] \
  || fail "Section IDs missing from response: $ROUTE_BODY"
echo "✅ Route defined: R1=$SECTION_1_ID R2=$SECTION_2_ID"

# Step 9 — Finalize plan
echo "🚦 Finalizing plan..."
FINALIZE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY/api/hermandades/$HERMANDAD_ID/procesiones/$PROCESION_ID/plan/finalize" \
  -H "Authorization: Bearer $TOKEN")
FINALIZE_HTTP_CODE=$(echo "$FINALIZE_RESPONSE" | tail -1)
FINALIZE_BODY=$(echo "$FINALIZE_RESPONSE" | sed '$d')
[[ "$FINALIZE_HTTP_CODE" =~ ^2 ]] || fail "Finalize plan failed (HTTP $FINALIZE_HTTP_CODE): $FINALIZE_BODY"
echo "✅ Plan finalized"

# Step 10 — Wait for plan-finalized → repertorio KnownPaso projection
echo "⏳ Waiting for KnownPaso projection (~12s)..."
sleep 12

# Step 11 — Define cruceta per paso
echo "📋 Defining cruceta for P1..."
CRUCETA_1_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$GATEWAY/api/hermandades/$HERMANDAD_ID/procesiones/$PROCESION_ID/pasos/$PASO_1_ID/cruceta" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"items\":[{\"marchaId\":\"$SEEDED_MARCHA\",\"routeSectionId\":\"$SECTION_1_ID\",\"sequenceWithinSection\":0,\"notes\":\"Salida\"},{\"marchaId\":\"$SEEDED_MARCHA\",\"routeSectionId\":\"$SECTION_2_ID\",\"sequenceWithinSection\":0,\"notes\":\"Recogida\"}]}")
CRUCETA_1_HTTP_CODE=$(echo "$CRUCETA_1_RESPONSE" | tail -1)
[[ "$CRUCETA_1_HTTP_CODE" =~ ^2 ]] || fail "Define cruceta P1 failed (HTTP $CRUCETA_1_HTTP_CODE): $(echo "$CRUCETA_1_RESPONSE" | sed '$d')"
echo "✅ Cruceta defined for P1"

echo "📋 Defining cruceta for P2..."
CRUCETA_2_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$GATEWAY/api/hermandades/$HERMANDAD_ID/procesiones/$PROCESION_ID/pasos/$PASO_2_ID/cruceta" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"items\":[{\"marchaId\":\"$SEEDED_MARCHA\",\"routeSectionId\":\"$SECTION_1_ID\",\"sequenceWithinSection\":0,\"notes\":\"Salida\"},{\"marchaId\":\"$SEEDED_MARCHA\",\"routeSectionId\":\"$SECTION_2_ID\",\"sequenceWithinSection\":0,\"notes\":\"Recogida\"}]}")
CRUCETA_2_HTTP_CODE=$(echo "$CRUCETA_2_RESPONSE" | tail -1)
[[ "$CRUCETA_2_HTTP_CODE" =~ ^2 ]] || fail "Define cruceta P2 failed (HTTP $CRUCETA_2_HTTP_CODE): $(echo "$CRUCETA_2_RESPONSE" | sed '$d')"
echo "✅ Cruceta defined for P2"

# Step 12 — Run-sheet for P1
echo "📜 Fetching run-sheet for P1..."
RUNSHEET_RESPONSE=$(curl -s -w "\n%{http_code}" "$GATEWAY/api/hermandades/$HERMANDAD_ID/procesiones/$PROCESION_ID/pasos/$PASO_1_ID/cruceta/run-sheet" \
  -H "Authorization: Bearer $TOKEN")
RUNSHEET_HTTP_CODE=$(echo "$RUNSHEET_RESPONSE" | tail -1)
RUNSHEET_BODY=$(echo "$RUNSHEET_RESPONSE" | sed '$d')
[[ "$RUNSHEET_HTTP_CODE" =~ ^2 ]] || fail "Get run-sheet failed (HTTP $RUNSHEET_HTTP_CODE): $RUNSHEET_BODY"
echo "   Run-sheet:"
echo "$RUNSHEET_BODY" | jq .
ITEM_ID=$(echo "$RUNSHEET_BODY" | jq -r '.sections[0].items[0].itemId')
[[ -n "$ITEM_ID" && "$ITEM_ID" != "null" ]] || fail "No items in first run-sheet section"
echo "   First item: $ITEM_ID"

# Step 13 — Advance current
echo "⏩ Advancing to first item..."
ADVANCE_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$GATEWAY/api/hermandades/$HERMANDAD_ID/procesiones/$PROCESION_ID/pasos/$PASO_1_ID/cruceta/current" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"routeSectionId\":\"$SECTION_1_ID\",\"crucetaItemId\":\"$ITEM_ID\"}")
ADVANCE_HTTP_CODE=$(echo "$ADVANCE_RESPONSE" | tail -1)
ADVANCE_BODY=$(echo "$ADVANCE_RESPONSE" | sed '$d')
[[ "$ADVANCE_HTTP_CODE" =~ ^2 ]] || fail "Advance current failed (HTTP $ADVANCE_HTTP_CODE): $ADVANCE_BODY"
echo "✅ Advanced — updated run-sheet (current marker should have moved):"
echo "$ADVANCE_BODY" | jq .

# Step 14 — Tenant isolation (demo-grade)
echo "🚧 Verifying tenant isolation..."
REGULAR_TOKEN=$(curl -s -X POST "$KEYCLOAK/realms/semana-santa/protocol/openid-connect/token" \
  -d "grant_type=password&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET&username=qa-user-no-hermandad&password=test" \
  | jq -r '.access_token')
[[ -n "$REGULAR_TOKEN" && "$REGULAR_TOKEN" != "null" ]] || fail "Failed to obtain regular-user token"
ISOLATION_HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  "$GATEWAY/api/hermandades/$HERMANDAD_ID/procesiones/$PROCESION_ID/pasos/$PASO_1_ID/cruceta" \
  -H "Authorization: Bearer $REGULAR_TOKEN")
[[ "$ISOLATION_HTTP_CODE" = "403" ]] || fail "No-membership user expected 403, got HTTP $ISOLATION_HTTP_CODE"
echo "✅ User without memberships gets 403 on cruceta (HTTP $ISOLATION_HTTP_CODE)"

# Hermandad B: claim only covers hermandad A, so B's route is untouchable
echo "📋 Creating hermandad B (no admin claim granted)..."
HERMANDAD_B_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY/api/hermandades" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Hermandad de la Otra $SUFFIX\",\"city\":\"Sevilla\",\"foundedYear\":1350}")
HERMANDAD_B_HTTP_CODE=$(echo "$HERMANDAD_B_RESPONSE" | tail -1)
HERMANDAD_B_BODY=$(echo "$HERMANDAD_B_RESPONSE" | sed '$d')
[[ "$HERMANDAD_B_HTTP_CODE" =~ ^2 ]] || fail "Create hermandad B failed (HTTP $HERMANDAD_B_HTTP_CODE): $HERMANDAD_B_BODY"
HERMANDAD_B_ID=$(echo "$HERMANDAD_B_BODY" | jq -r '.id')
[[ -n "$HERMANDAD_B_ID" && "$HERMANDAD_B_ID" != "null" ]] || fail "Hermandad B ID is empty"
CROSS_TENANT_HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT \
  "$GATEWAY/api/hermandades/$HERMANDAD_B_ID/procesiones/$PROCESION_ID/route" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sections":[{"name":"No autorizado","position":0}]}')
[[ "$CROSS_TENANT_HTTP_CODE" = "403" ]] || fail "Cross-tenant write expected 403, got HTTP $CROSS_TENANT_HTTP_CODE"
echo "✅ Cross-tenant route write on hermandad B rejected (HTTP $CROSS_TENANT_HTTP_CODE)"

# Done
echo ""
echo "🎉 Cruceta product-flow demo complete!"
echo "   Hermandad A: $HERMANDAD_ID"
echo "   Procesion:   $PROCESION_ID"
echo "   Paso P1:     $PASO_1_ID"
echo "   Paso P2:     $PASO_2_ID"
echo "   Hermandad B (isolation): $HERMANDAD_B_ID"
echo "   Marchas used: seeded 'Amarguras' ($SEEDED_MARCHA)"
echo "   Inspect topics in Kafka UI at http://localhost:9000"
