#!/bin/bash
set -euo pipefail

KCADM=/opt/keycloak/bin/kcadm.sh
export KC_CLI_CONFIG_FILE=/tmp/kcadm.config
kc() { "$KCADM" "$@" --config "$KC_CLI_CONFIG_FILE"; }
realm=bethel-report

# Import finishes before the Admin API becomes available. Bound startup retries.
ready=false
for ((attempt=1; attempt<=60; attempt++)); do
  if kc config credentials --server http://keycloak:8080 --realm master \
      --user "$KEYCLOAK_ADMIN" --password "$KEYCLOAK_ADMIN_PASSWORD" >/dev/null 2>&1 \
      && kc get "realms/$realm" >/dev/null 2>&1; then
    ready=true
    break
  fi
  sleep 2
done
if [[ "$ready" != true ]]; then
  echo 'Keycloak bootstrap: realm indisponível ou credenciais administrativas inválidas.' >&2
  exit 1
fi

client_id=$(kc get clients -r "$realm" -q clientId=bethel-backend-admin --fields id --format csv --noquotes)
[[ -n "$client_id" ]]
kc update "clients/$client_id" -r "$realm" \
  -s "secret=$KEYCLOAK_ADMIN_CLIENT_SECRET" -s enabled=true \
  -s serviceAccountsEnabled=true -s publicClient=false -s fullScopeAllowed=false

# Assign roles both to the service account and to its allowed token scope.
service_id=$(kc get "clients/$client_id/service-account-user" -r "$realm" --fields id --format csv --noquotes)
management_id=$(kc get clients -r "$realm" -q clientId=realm-management --fields id --format csv --noquotes)
for role in manage-users view-users query-users; do
  kc add-roles -r "$realm" --uid "$service_id" --cclientid realm-management --rolename "$role"
  kc get "clients/$management_id/roles/$role" -r "$realm" > /tmp/bootstrap-role.json
  # Role mapping endpoints expect an array.
  { echo '['; cat /tmp/bootstrap-role.json; echo ']'; } > /tmp/bootstrap-roles.json
  kc create "clients/$client_id/scope-mappings/clients/$management_id" -r "$realm" -f /tmp/bootstrap-roles.json
done

frontend_id=$(kc get clients -r "$realm" -q "clientId=$FRONTEND_CLIENT_ID" --fields id --format csv --noquotes)
if [[ -z "$frontend_id" ]]; then
  frontend_id=$(kc create clients -r "$realm" -s "clientId=$FRONTEND_CLIENT_ID" -s protocol=openid-connect -i)
fi
kc update "clients/$frontend_id" -r "$realm" -s enabled=true -s publicClient=true \
  -s standardFlowEnabled=true -s directAccessGrantsEnabled=false \
  -s "rootUrl=$FRONTEND_URL" -s "baseUrl=$FRONTEND_URL" \
  -s "redirectUris=[\"$FRONTEND_URL/*\",\"$INVITATION_REDIRECT_URL\"]" \
  -s "webOrigins=[\"$FRONTEND_URL\"]" \
  -s "attributes.\"post.logout.redirect.uris\"=$FRONTEND_URL/*"

# Keep the realm's email defaults usable by the Rabbit email SPI.
kc update "realms/$realm" -s "smtpServer.from=$KEYCLOAK_EMAIL_FROM" \
  -s 'smtpServer.fromDisplayName=Bethel Report'
if [[ -n "${GOOGLE_CLIENT_ID:-}" && -n "${GOOGLE_CLIENT_SECRET:-}" ]]; then
  kc update identity-provider/instances/google -r "$realm" -s enabled=true \
    -s "config.clientId=$GOOGLE_CLIENT_ID" -s "config.clientSecret=$GOOGLE_CLIENT_SECRET"
else
  kc update identity-provider/instances/google -r "$realm" -s enabled=false
fi

if [[ "$BOOTSTRAP_ADMIN_ENABLED" == true ]]; then
  user_id=$(kc get users -r "$realm" -q "email=$BOOTSTRAP_ADMIN_EMAIL" -q exact=true --fields id --format csv --noquotes)
  if [[ -z "$user_id" ]]; then
    user_id=$(kc create users -r "$realm" -s "username=$BOOTSTRAP_ADMIN_EMAIL" \
      -s "email=$BOOTSTRAP_ADMIN_EMAIL" -s 'firstName=Administrador' \
      -s 'lastName=do Grande Conselho' -s enabled=true -s emailVerified=true -i)
  fi
  # Recover a partially completed bootstrap without resetting an existing password.
  credentials=$(kc get "users/$user_id/credentials" -r "$realm" --fields type --format csv --noquotes)
  if ! [[ "$credentials" == *password* ]]; then
    kc set-password -r "$realm" --userid "$user_id" --new-password "$BOOTSTRAP_ADMIN_PASSWORD" --temporary
  fi
fi

echo 'Keycloak bootstrap concluído.'
