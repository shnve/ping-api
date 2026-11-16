#!/usr/bin/env bash
# =============================================================================================
# PISAC: respaldo diario del servidor (desarrollo y producción).
#
# Qué respalda (en /respaldos/pisac/diario/AAAA-MM-DD_HHMM/):
#   postgres/   pg_dumpall --globals-only (roles) + pg_dump -Fc de cada base (ping-api, keycloak si existe)
#   archivos/   PING_ROOT_FOLDER (adjuntos, S-124 generados, GPX, NAVTEX...) y cartografía (db/geotiff)
#   volumenes/  Keycloak (con H2 en start-dev se detiene ~20 s para una copia consistente) y GeoServer
#   config/     .env de ping-api y db, unidad systemd de ping-api, Nginx y esta configuración (contiene secretos)
#   MANIFIESTO.txt y SHA256SUMS
#
# Retención: diarios, semanales (domingo) y mensuales (día 1), con enlaces duros (sin duplicar espacio).
# Copia fuera del servidor: rsync por SSH si DESTINO_REMOTO está definido en /etc/pisac-respaldo.conf.
#
# Instalación, prueba y restauración: docs-internos/Respaldos_PISAC.md
# Códigos de salida: 0 correcto · 1 error (respaldo local incompleto) · 2 respaldo local correcto pero falló la copia externa
# =============================================================================================
set -Eeuo pipefail
umask 077

CONF=/etc/pisac-respaldo.conf
# shellcheck source=/dev/null
[[ -r "$CONF" ]] && source "$CONF"

PISAC_DIR="${PISAC_DIR:-/home/pisac/pisac_dev}"
RESPALDO_DIR="${RESPALDO_DIR:-/respaldos/pisac}"
RETENER_DIARIOS="${RETENER_DIARIOS:-14}"
RETENER_SEMANALES="${RETENER_SEMANALES:-8}"
RETENER_MENSUALES="${RETENER_MENSUALES:-12}"
MIN_LIBRE_GB="${MIN_LIBRE_GB:-20}"
CONTENEDOR_DB="${CONTENEDOR_DB:-ping_api_db}"
CONTENEDOR_KC="${CONTENEDOR_KC:-ping_keycloak}"
CONTENEDOR_GS="${CONTENEDOR_GS:-geoserver_local}"
KC_DETENER="${KC_DETENER:-auto}"          # auto: solo si Keycloak corre con start-dev (H2) · si · no
DESTINO_REMOTO="${DESTINO_REMOTO:-}"      # p. ej. respaldos@10.0.0.50:/srv/respaldos/pisac-dev
SSH_CLAVE="${SSH_CLAVE:-/root/.ssh/pisac_respaldo}"

INICIO=$(date +%s)
log() { echo "[$(date '+%F %T')] $*"; }
trap 'log "ERROR en la línea $LINENO: $BASH_COMMAND"' ERR

# Un solo respaldo a la vez
exec 9>/run/pisac-respaldo.lock
flock -n 9 || { log "Ya hay un respaldo en curso; se cancela este."; exit 1; }

# Valor de una variable en un archivo .env (sin ejecutarlo)
valor_env() { sed -n "s/^$2=//p" "$1" 2>/dev/null | tail -1 | tr -d "\"' \r"; }

# tar que tolera "archivo cambió mientras se leía" (código 1) y falla con cualquier otro error
tar_seguro() {
  local rc=0
  tar "$@" || rc=$?
  if [[ $rc -eq 1 ]]; then log "  aviso: algunos archivos cambiaron durante la copia"; rc=0; fi
  return $rc
}

# Ruta en el host del volumen montado en <contenedor>:<destino>
volumen_de() {
  local nombre
  nombre=$(docker inspect -f "{{range .Mounts}}{{if eq .Destination \"$2\"}}{{.Name}}{{end}}{{end}}" "$1" 2>/dev/null || true)
  [[ -n "$nombre" ]] && docker volume inspect -f '{{.Mountpoint}}' "$nombre"
}

# ---------------------------------------------------------------------------------------------
log "=== Respaldo PISAC en $(hostname) ==="
mkdir -p "$RESPALDO_DIR"/{diario,semanal,mensual}
chmod 700 "$RESPALDO_DIR"

libre_gb=$(( $(df --output=avail -B1 "$RESPALDO_DIR" | tail -1) / 1024 / 1024 / 1024 ))
if (( libre_gb < MIN_LIBRE_GB )); then
  log "ERROR: solo hay ${libre_gb} GB libres en $RESPALDO_DIR (mínimo ${MIN_LIBRE_GB} GB)."
  exit 1
fi

# Restos de respaldos interrumpidos
find "$RESPALDO_DIR/diario" -mindepth 1 -maxdepth 1 -type d -name '*.parcial' -mmin +60 -exec rm -rf {} +

FECHA=$(date +%F_%H%M)
DEST="$RESPALDO_DIR/diario/$FECHA.parcial"
mkdir -p "$DEST"/{postgres,archivos,volumenes,config}

API_ENV="$PISAC_DIR/ping-api/.env"
DB_ENV="$PISAC_DIR/ping-api/db/.env"
PGUSUARIO=$(valor_env "$DB_ENV" POSTGRES_USER); PGUSUARIO="${PGUSUARIO:-ping-api}"

# --- 1. PostgreSQL -----------------------------------------------------------------------------
log "1/5 PostgreSQL ($CONTENEDOR_DB, usuario $PGUSUARIO)"
docker exec "$CONTENEDOR_DB" pg_dumpall -U "$PGUSUARIO" --globals-only | gzip > "$DEST/postgres/globales.sql.gz"
mapfile -t BASES < <(docker exec "$CONTENEDOR_DB" psql -U "$PGUSUARIO" -d postgres -Atc \
  "SELECT datname FROM pg_database WHERE NOT datistemplate AND datname <> 'postgres' ORDER BY 1")
for base in "${BASES[@]}"; do
  docker exec "$CONTENEDOR_DB" pg_dump -U "$PGUSUARIO" -Fc -d "$base" > "$DEST/postgres/$base.dump"
  # Verificación: el volcado debe poder leerse completo
  docker exec -i "$CONTENEDOR_DB" pg_restore -l < "$DEST/postgres/$base.dump" > /dev/null
  log "  $base: $(du -h "$DEST/postgres/$base.dump" | cut -f1)"
done

# --- 2. Archivos de ping-api y cartografía -----------------------------------------------------
log "2/5 Archivos"
ROOT_FOLDER=$(valor_env "$API_ENV" PING_ROOT_FOLDER)
if [[ -n "$ROOT_FOLDER" && -d "$ROOT_FOLDER" ]]; then
  tar_seguro -C / -czf "$DEST/archivos/ping-api-datos.tar.gz" "${ROOT_FOLDER#/}"
  log "  $ROOT_FOLDER: $(du -h "$DEST/archivos/ping-api-datos.tar.gz" | cut -f1)"
else
  log "  ERROR: PING_ROOT_FOLDER no está definido en $API_ENV o la carpeta no existe."
  exit 1
fi
GEOTIFF="$PISAC_DIR/ping-api/db/geotiff"
if [[ -d "$GEOTIFF" ]]; then
  tar_seguro -C "$(dirname "$GEOTIFF")" -czf "$DEST/archivos/geotiff.tar.gz" geotiff
  log "  cartografía: $(du -h "$DEST/archivos/geotiff.tar.gz" | cut -f1)"
fi

# --- 3. Volúmenes de Docker --------------------------------------------------------------------
log "3/5 Volúmenes"
if KC_VOL=$(volumen_de "$CONTENEDOR_KC" /opt/keycloak/data) && [[ -n "$KC_VOL" ]]; then
  detener=no
  KC_CMD=$(docker inspect -f '{{join .Config.Cmd " "}}' "$CONTENEDOR_KC")
  if [[ "$KC_DETENER" == si ]] || { [[ "$KC_DETENER" == auto ]] && [[ " $KC_CMD " == *" start-dev "* ]]; }; then
    detener=si
  fi
  if [[ $detener == si ]]; then
    log "  Keycloak usa H2 (start-dev): se detiene durante la copia"
    docker stop -t 30 "$CONTENEDOR_KC" > /dev/null
    if ! tar_seguro -C "$KC_VOL" -czf "$DEST/volumenes/keycloak_data.tar.gz" .; then
      docker start "$CONTENEDOR_KC" > /dev/null
      log "  ERROR al copiar el volumen de Keycloak (Keycloak se volvió a iniciar)"
      exit 1
    fi
    docker start "$CONTENEDOR_KC" > /dev/null
    log "  Keycloak iniciado de nuevo"
  else
    tar_seguro -C "$KC_VOL" -czf "$DEST/volumenes/keycloak_data.tar.gz" .
  fi
  log "  keycloak_data: $(du -h "$DEST/volumenes/keycloak_data.tar.gz" | cut -f1)"
else
  log "  aviso: no se encontró el volumen de Keycloak"
fi
if GS_VOL=$(volumen_de "$CONTENEDOR_GS" /opt/geoserver/data_dir) && [[ -n "$GS_VOL" ]]; then
  # Se excluye la caché de teselas: se regenera sola
  tar_seguro -C "$GS_VOL" --exclude='./gwc' -czf "$DEST/volumenes/geoserver_data.tar.gz" .
  log "  geoserver_data: $(du -h "$DEST/volumenes/geoserver_data.tar.gz" | cut -f1)"
else
  log "  aviso: no se encontró el volumen de GeoServer"
fi

# --- 4. Configuración (contiene secretos: queda solo para root) --------------------------------
log "4/5 Configuración"
CONFIG=()
for f in "$API_ENV" "$DB_ENV" /etc/systemd/system/ping-api.service /etc/nginx "$CONF" \
         "$PISAC_DIR/ping-web/src/environments/environment.prod.ts"; do
  [[ -e "$f" ]] && CONFIG+=("${f#/}")
done
# Almacén de certificados SECOM/S-124 del servidor (KEYSTORE_PATH absoluto o con file:). Si es relativo, es el de
# prueba incluido en el .jar y no hace falta respaldarlo.
KEYSTORE=$(valor_env "$API_ENV" KEYSTORE_PATH); KEYSTORE="${KEYSTORE#file:}"
if [[ "$KEYSTORE" == /* && -f "$KEYSTORE" ]]; then
  CONFIG+=("${KEYSTORE#/}")
  log "  almacén de certificados: $KEYSTORE"
fi
tar_seguro -C / -czf "$DEST/config/config.tar.gz" "${CONFIG[@]}"

# --- 5. Manifiesto, sumas de verificación y rotación -------------------------------------------
log "5/5 Manifiesto y rotación"
{
  echo "Respaldo PISAC $FECHA en $(hostname)"
  echo "ping-api: $(git -C "$PISAC_DIR/ping-api" rev-parse --short HEAD 2>/dev/null || echo '?') ($(git -C "$PISAC_DIR/ping-api" rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?'))"
  echo "ping-web: $(git -C "$PISAC_DIR/ping-web" rev-parse --short HEAD 2>/dev/null || echo '?') ($(git -C "$PISAC_DIR/ping-web" rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?'))"
  echo "Bases: ${BASES[*]}"
  echo; echo "Contenedores:"; docker ps -a --format '  {{.Names}}  {{.Image}}  {{.Status}}'
  echo; echo "Contenido:"; (cd "$DEST" && find . -type f ! -name MANIFIESTO.txt -exec du -h {} + | sort -k2)
} > "$DEST/MANIFIESTO.txt"
(cd "$DEST" && find . -type f ! -name SHA256SUMS -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS)

FINAL="$RESPALDO_DIR/diario/$FECHA"
mv "$DEST" "$FINAL"
ln -sfn "diario/$FECHA" "$RESPALDO_DIR/ultimo"

# Semanal (domingo) y mensual (día 1): enlaces duros, no ocupan espacio adicional
[[ $(date +%u) == 7 ]] && cp -al "$FINAL" "$RESPALDO_DIR/semanal/$FECHA"
[[ $(date +%d) == 01 ]] && cp -al "$FINAL" "$RESPALDO_DIR/mensual/$FECHA"

purgar() {  # purgar <carpeta> <cuántos conservar>
  find "$1" -mindepth 1 -maxdepth 1 -type d ! -name '*.parcial' | sort | head -n -"$2" | xargs -r rm -rf
}
purgar "$RESPALDO_DIR/diario" "$RETENER_DIARIOS"
purgar "$RESPALDO_DIR/semanal" "$RETENER_SEMANALES"
purgar "$RESPALDO_DIR/mensual" "$RETENER_MENSUALES"

TAM=$(du -sh "$FINAL" | cut -f1)
TOTAL=$(du -sh "$RESPALDO_DIR" | cut -f1)
log "Respaldo local correcto: $FINAL ($TAM; total en $RESPALDO_DIR: $TOTAL; $(( $(date +%s) - INICIO )) s)"

# --- Copia fuera del servidor ------------------------------------------------------------------
if [[ -z "$DESTINO_REMOTO" ]]; then
  log "AVISO: DESTINO_REMOTO no está definido: NO hay copia fuera del servidor."
  exit 0
fi
log "Copia externa hacia $DESTINO_REMOTO"
if rsync -aH --delete -e "ssh -i $SSH_CLAVE -o BatchMode=yes -o StrictHostKeyChecking=accept-new" \
     "$RESPALDO_DIR/" "$DESTINO_REMOTO/"; then
  log "Copia externa correcta ($(( $(date +%s) - INICIO )) s en total)"
else
  log "ERROR: falló la copia externa (el respaldo local sí está completo)."
  exit 2
fi
