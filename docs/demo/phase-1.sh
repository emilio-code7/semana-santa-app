#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────────────────────────────────────
# Phase 1 End-to-End Demo — Hermandad → Procesion → Cruceta
# Requires: docker-compose stack running (core profile), curl, jq
# ─────────────────────────────────────────────────────────────────────────────

GATEWAY="http://localhost:8080"
KEYCLOAK="http://localhost:8180"
CLIENT_ID="semana-santa-client"
CLIENT_SECRET="secret"
USERNAME="qa-admin-user"
PASSWORD="test"
SEEDED_MARCHA="a0000001-0000-0000-0000-000000000001"

fail() {
  echo "❌ FAIL: $*" >&2
  exit 1
}

# Step 0 — Prerequisites
command -v curl >/dev/null 2>&1 || fail "curl is required"
command -v jq   >/dev/null 2>&1 || fail "jq is required"

# Step 1 — Get JWT
echo "🔐 Getting JWT from Keycloak..."
TOKEN=$(curl -s -X POST "$KEYCLOAK/realms/semana-santa/protocol/openid-connect/token" \
  -d "grant_type=password&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET&username=$USERNAME&password=$PASSWORD" \
  | jq -r '.access_token')
[[ -n "$TOKEN" && "$TOKEN" != "null" ]] || fail "Failed to obtain JWT"
echo "   Token acquired"

# Step 2 — Create hermandad
echo "📋 Creating hermandad..."
HERMANDAD_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY/api/hermandades" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Hermandad del Silencio","city":"Sevilla","foundedYear":1350}')
HERMANDAD_HTTP_CODE=$(echo "$HERMANDAD_RESPONSE" | tail -1)
HERMANDAD_BODY=$(echo "$HERMANDAD_RESPONSE" | sed '$d')
[[ "$HERMANDAD_HTTP_CODE" =~ ^2 ]] || fail "Create hermandad failed (HTTP $HERMANDAD_HTTP_CODE): $HERMANDAD_BODY"
HERMANDAD_ID=$(echo "$HERMANDAD_BODY" | jq -r '.id')
[[ -n "$HERMANDAD_ID" && "$HERMANDAD_ID" != "null" ]] || fail "Hermandad ID is empty"
echo "✅ Hermandad created: $HERMANDAD_ID"

# Step 3 — Create procesion
echo "📋 Creating procesion..."
PROCESION_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY/api/procesiones" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"hermandadId\":\"$HERMANDAD_ID\",\"date\":\"2026-04-13\",\"time\":\"18:00\"}")
PROCESION_HTTP_CODE=$(echo "$PROCESION_RESPONSE" | tail -1)
PROCESION_BODY=$(echo "$PROCESION_RESPONSE" | sed '$d')
[[ "$PROCESION_HTTP_CODE" =~ ^2 ]] || fail "Create procesion failed (HTTP $PROCESION_HTTP_CODE): $PROCESION_BODY"
PROCESION_ID=$(echo "$PROCESION_BODY" | jq -r '.id')
[[ -n "$PROCESION_ID" && "$PROCESION_ID" != "null" ]] || fail "Procesion ID is empty"
echo "✅ Procesion created: $PROCESION_ID"

# Step 4 — Wait for outbox poller + Kafka consumer
echo "⏳ Waiting for outbox poller and Kafka consumer (~6s)..."
sleep 6

# Step 5 — Define cruceta
echo "📋 Defining cruceta..."
CRUCETA_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$GATEWAY/api/hermandades/$HERMANDAD_ID/procesiones/$PROCESION_ID/cruceta" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"items\":[{\"marchaId\":\"$SEEDED_MARCHA\",\"orderIndex\":0,\"notes\":\"Salida\"}]}")
CRUCETA_HTTP_CODE=$(echo "$CRUCETA_RESPONSE" | tail -1)
CRUCETA_BODY=$(echo "$CRUCETA_RESPONSE" | sed '$d')
[[ "$CRUCETA_HTTP_CODE" =~ ^2 ]] || fail "Define cruceta failed (HTTP $CRUCETA_HTTP_CODE): $CRUCETA_BODY"
echo "✅ Cruceta defined successfully"

# Step 6 — Change procesion status
echo "📋 Changing procesion status to IN_PROGRESS..."
STATUS_RESPONSE=$(curl -s -w "\n%{http_code}" -X PATCH "$GATEWAY/api/procesiones/$PROCESION_ID/status" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"newStatus":"IN_PROGRESS"}')
STATUS_HTTP_CODE=$(echo "$STATUS_RESPONSE" | tail -1)
STATUS_BODY=$(echo "$STATUS_RESPONSE" | sed '$d')
[[ "$STATUS_HTTP_CODE" =~ ^2 ]] || fail "Change status failed (HTTP $STATUS_HTTP_CODE): $STATUS_BODY"
echo "✅ Procesion status changed to IN_PROGRESS"

# Done
echo ""
echo "🎉 Phase 1 demo complete!"
echo "   Hermandad: $HERMANDAD_ID"
echo "   Procesion: $PROCESION_ID"
echo "   Check procesion-events in Kafka UI at http://localhost:9000"
