#!/usr/bin/env bash
# =====================================================================================================================
# PISAC - Seguridad de sesión y de acceso en Keycloak (realms ping-pro y ping-public)
#
#   - Inactividad máxima: 10 min (ping-pro, operadores SHN) y 30 min (ping-public).
#   - Duración máxima de la sesión: 8 h (ping-pro) y 12 h (ping-public). Token de acceso: 5 min.
#   - Rotación del token de renovación (cada token de renovación sirve una sola vez).
#   - Protección contra fuerza bruta: bloqueo temporal tras 5 intentos fallidos (espera creciente, máx. 15 min).
#   - Política de contraseñas: 12 caracteres, mayúscula, minúscula, número y símbolo; distinta del usuario y del
#     correo; no repetir las 5 últimas.
#   - PKCE (S256) obligatorio en el cliente ping-web.
#
# El portal (ping-web) aplica la misma inactividad: avisa 2 min antes y cierra la sesión (ver session-activity.service.ts).
# Si se cambian los tiempos aquí, cambiar también environment.keycloak.idleTimeoutMinutes en ping-web.
#
# Idempotente: se puede ejecutar varias veces. Uso (en el servidor, desde ping-api/db):
#   KC_ADMIN_PASSWORD='***' ./keycloak-seguridad-sesion.sh            # contenedor por defecto: ping_keycloak
#   KC_ADMIN_PASSWORD='***' ./keycloak-seguridad-sesion.sh otro_contenedor
# =====================================================================================================================
set -euo pipefail

CONTAINER="${1:-ping_keycloak}"
KC_ADMIN="${KC_ADMIN:-admin}"
: "${KC_ADMIN_PASSWORD:?Defina KC_ADMIN_PASSWORD con la contraseña del administrador de Keycloak}"
KCADM=(docker exec -i "$CONTAINER" /opt/keycloak/bin/kcadm.sh)

PASSWORD_POLICY='length(12) and upperCase(1) and lowerCase(1) and digits(1) and specialChars(1) and notUsername(undefined) and notEmail(undefined) and passwordHistory(5)'

"${KCADM[@]}" config credentials --server http://localhost:8080 --realm master --user "$KC_ADMIN" --password "$KC_ADMIN_PASSWORD"

for REALM in ping-pro ping-public; do
  if [ "$REALM" = "ping-pro" ]; then IDLE=600; MAX=28800; else IDLE=1800; MAX=43200; fi
  echo ">> $REALM: inactividad ${IDLE}s, máximo ${MAX}s, fuerza bruta, política de contraseñas y rotación de tokens"
  "${KCADM[@]}" update "realms/$REALM" \
    -s ssoSessionIdleTimeout=$IDLE \
    -s ssoSessionMaxLifespan=$MAX \
    -s accessTokenLifespan=300 \
    -s revokeRefreshToken=true \
    -s refreshTokenMaxReuse=0 \
    -s bruteForceProtected=true \
    -s permanentLockout=false \
    -s failureFactor=5 \
    -s waitIncrementSeconds=60 \
    -s maxFailureWaitSeconds=900 \
    -s maxDeltaTimeSeconds=43200 \
    -s minimumQuickLoginWaitSeconds=60 \
    -s quickLoginCheckMilliSeconds=1000 \
    -s "passwordPolicy=$PASSWORD_POLICY"

  CLIENT_ID=$("${KCADM[@]}" get clients -r "$REALM" -q clientId=ping-web --fields id --format csv --noquotes | tr -d '\r')
  echo ">> $REALM: PKCE S256 en ping-web ($CLIENT_ID)"
  echo '{"attributes":{"pkce.code.challenge.method":"S256"}}' | "${KCADM[@]}" update "clients/$CLIENT_ID" -r "$REALM" -f - --merge
done

echo ">> Listo. Verificación:"
for REALM in ping-pro ping-public; do
  "${KCADM[@]}" get "realms/$REALM" --fields ssoSessionIdleTimeout,ssoSessionMaxLifespan,revokeRefreshToken,bruteForceProtected,failureFactor,passwordPolicy
done
