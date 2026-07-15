#!/usr/bin/env bash
#
# Seed QA users into Keycloak realm semana-santa.
# Runs from the HOST (uses docker exec and curl to Keycloak REST API).
#
set -e

KC_HOST="${KC_HOST:-localhost:8180}"
REALM="${REALM:-semana-santa}"

echo "=== Step 1: Setup user profile attribute ==="

# Get admin token
ADMIN_TOKEN=$(curl -s -X POST "http://${KC_HOST}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

AUTH="Authorization: Bearer $ADMIN_TOKEN"

# Get current user profile
PROFILE=$(curl -s "http://${KC_HOST}/admin/realms/${REALM}/users/profile" -H "$AUTH")

# Check if hermandad_memberships exists
if echo "$PROFILE" | python3 -c "import sys,json; d=json.load(sys.stdin); exit(0 if any(a['name']=='hermandad_memberships' for a in d.get('attributes',[])) else 1)" 2>/dev/null; then
    echo "  hermandad_memberships attribute already exists in user profile."
else
    echo "  Adding hermandad_memberships attribute to user profile..."
    UPDATED=$(echo "$PROFILE" | python3 -c "
import sys, json
d = json.load(sys.stdin)
d['attributes'].append({
    'name': 'hermandad_memberships',
    'displayName': '',
    'validations': {},
    'annotations': {},
    'permissions': {'view': ['admin'], 'edit': ['admin']},
    'multivalued': False
})
if not any(g.get('name') == 'user-metadata' for g in d.get('groups', [])):
    d.setdefault('groups', []).append({
        'name': 'user-metadata',
        'displayHeader': 'User metadata',
        'displayDescription': 'Attributes, which refer to user metadata'
    })
print(json.dumps(d))
")
    curl -s -X PUT "http://${KC_HOST}/admin/realms/${REALM}/users/profile" \
        -H "$AUTH" -H "Content-Type: application/json" \
        -d "$UPDATED" -o /dev/null -w "  HTTP %{http_code}\n"
fi

echo ""
echo "=== Step 2: Seed QA users ==="

KCADM=("docker" "exec" "keycloak" "/opt/keycloak/bin/kcadm.sh")
KC_SERVER="http://localhost:8080"
KR=("--server" "$KC_SERVER" "--user" "admin" "--password" "admin" "-r" "$REALM")

# Authenticate kcadm
"${KCADM[@]}" config credentials --server "$KC_SERVER" --realm master --user admin --password admin >/dev/null 2>&1 || true

ensure_user() {
    local username="$1" password="$2"
    shift 2
    local roles=("$@")

    local user_id
    user_id=$("${KCADM[@]}" get users -q "username=${username}" --fields id "${KR[@]}" 2>/dev/null \
        | grep '"id"' | head -1 | sed 's/.*: "\(.*\)".*/\1/')

    if [ -z "$user_id" ]; then
        "${KCADM[@]}" create users "${KR[@]}" \
            -s "username=${username}" -s "enabled=true" -s "emailVerified=true" >/dev/null 2>&1
        echo "  Created user '${username}'."
        user_id=$("${KCADM[@]}" get users -q "username=${username}" --fields id "${KR[@]}" 2>/dev/null \
            | grep '"id"' | head -1 | sed 's/.*: "\(.*\)".*/\1/')
    else
        echo "  User '${username}' already exists."
    fi

    "${KCADM[@]}" set-password "${KR[@]}" --username "$username" --new-password "$password" >/dev/null 2>&1

    for role_name in "${roles[@]:-}"; do
        [ -z "$role_name" ] && continue
        "${KCADM[@]}" add-roles "${KR[@]}" --uusername "$username" --rolename "$role_name" 2>/dev/null || true
        echo "  Assigned role '${role_name}'."
    done
}

ensure_user "qa-user-no-hermandad" "test"
ensure_user "qa-admin-user"        "test" "HERMANDAD_ADMIN"
ensure_user "qa-musician-user"     "test" "MUSICIAN"
ensure_user "qa-band-director-user" "test" "BAND_DIRECTOR"

echo ""
echo "=== Done ==="
echo "QA users seeded. hermandad_memberships attribute is available in user profile."
echo "Runtime: When a user creates a hermandad, they should be auto-assigned as admin."
