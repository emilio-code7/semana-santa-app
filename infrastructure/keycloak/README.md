# Keycloak Token Acquisition

Realm: `semana-santa` — available at `http://localhost:8180`

## QA User Tokens

```bash
TOKEN_ADMIN=$(curl -s -X POST http://localhost:8180/realms/semana-santa/protocol/openid-connect/token \
  -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=qa-admin-user&password=test" \
  | jq -r '.access_token')

TOKEN_MUSICIAN=$(curl -s -X POST http://localhost:8180/realms/semana-santa/protocol/openid-connect/token \
  -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=qa-musician-user&password=test" \
  | jq -r '.access_token')

TOKEN_BAND_DIRECTOR=$(curl -s -X POST http://localhost:8180/realms/semana-santa/protocol/openid-connect/token \
  -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=qa-band-director-user&password=test" \
  | jq -r '.access_token')

TOKEN_PUBLIC=$(curl -s -X POST http://localhost:8180/realms/semana-santa/protocol/openid-connect/token \
  -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=qa-user-no-hermandad&password=test" \
  | jq -r '.access_token')
```

## Admin CLI Token (Keycloak master realm)

```bash
ADMIN_CLI_TOKEN=$(curl -s -X POST http://localhost:8180/realms/master/protocol/openid-connect/token \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  | jq -r '.access_token')
```

## Admin Service Account Token (semana-santa-admin-client)

```bash
ADMIN_SERVICE_TOKEN=$(curl -s -X POST http://localhost:8180/realms/semana-santa/protocol/openid-connect/token \
  -d "grant_type=client_credentials&client_id=semana-santa-admin-client&client_secret=admin-secret" \
  | jq -r '.access_token')
```

## Inspecting a Token

```bash
echo "$TOKEN_ADMIN" | cut -d. -f2 | tr '_-' '/+' | base64 -d 2>/dev/null | jq .
```
