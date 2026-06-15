#!/usr/bin/env bash
set +e

KCADM="/opt/keycloak/bin/kcadm.sh"
REALM="semana-santa"

echo "Authenticating as admin..."
"$KCADM" config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin
echo "Authenticated."

ensure_user() {
  local username="$1" password="$2"
  shift 2
  local roles=("$@")

  local user_id
  user_id=$("$KCADM" get users -r "$REALM" --query "username=${username}&exact=true" --fields id 2>/dev/null \
    | grep '"id"' | head -1 | sed 's/.*: "\(.*\)".*/\1/')

  if [ -z "$user_id" ]; then
    "$KCADM" create users -r "$REALM" \
      -s "username=${username}" \
      -s "enabled=true" \
      -s "emailVerified=true"
    echo "Created user '${username}'."
  else
    echo "User '${username}' already exists."
  fi

  "$KCADM" set-password -r "$REALM" \
    --username "$username" \
    --new-password "$password"

  for role_name in "${roles[@]:-}"; do
    [ -z "$role_name" ] && continue
    "$KCADM" add-roles -r "$REALM" \
      --uusername "$username" \
      --rolename "$role_name" 2>/dev/null || echo "  Role '${role_name}' may already be assigned to '${username}'."
    echo "  Assigned role '${role_name}' to '${username}'."
  done
}

echo "Seeding users..."
ensure_user "qa-user-no-hermandad" "test"
ensure_user "qa-admin-user"        "test" "HERMANDAD_ADMIN"
ensure_user "qa-musician-user"     "test" "MUSICIAN"
ensure_user "qa-band-director-user" "test" "BAND_DIRECTOR"

echo "QA user seeding complete."
