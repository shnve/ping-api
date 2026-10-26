#!/bin/bash
echo "====================================================="
echo "Iniciando importación S-57 y Estilización S-52 ECDIS"
echo "====================================================="

BASE_DB="PG:host=db port=5432 user=ping-api password=ping-api dbname=ping-api"

# ==============================================================================
# FASE 0: LIMPIEZA ABSOLUTA DE FANTASMAS
# ==============================================================================
echo "-> Borrando datos cacheados viejos..."
ogrinfo $BASE_DB -sql "DROP SCHEMA IF EXISTS regional CASCADE" 2>/dev/null || true
ogrinfo $BASE_DB -sql "DROP SCHEMA IF EXISTS detalle CASCADE" 2>/dev/null || true
ogrinfo $BASE_DB -sql "CREATE SCHEMA regional" 2>/dev/null || true
ogrinfo $BASE_DB -sql "CREATE SCHEMA detalle" 2>/dev/null || true

# ==============================================================================
# FASE 1: ENRUTAMIENTO E IMPORTACIÓN ESTRICTAMENTE AISLADA
# ==============================================================================
find /data -type f -name "*.000" | while read -r filepath; do
    # Extraemos el nombre de la CARPETA contenedora
    foldername=$(basename "$(dirname "$filepath")")
    prefix=${foldername:0:3}

    if [ "$prefix" == "VE2" ]; then
        schema="regional"
        echo "-> [ESCALA GENERAL] Fusionando $foldername en esquema: $schema"
    else
        schema="detalle"
        echo "-> [ESCALA DETALLE] Fusionando $foldername en esquema: $schema"
    fi

    # TRUCO MAESTRO: Encerramos ogr2ogr en el esquema activo
    DB_WRITE="PG:host=db port=5432 user=ping-api password=ping-api dbname=ping-api active_schema=$schema"

    ogr2ogr -f "PostgreSQL" "$DB_WRITE" "$filepath" \
        -lco SCHEMA=$schema -append \
        -nlt PROMOTE_TO_MULTI \
        -skipfailures 2>/dev/null
done

echo "====================================================="
echo "FASE 2: INYECTANDO TOKENS DE COLOR S-52 (SHOM STYLE)"
echo "====================================================="

ejecutar_sql() {
    ogrinfo $BASE_DB -sql "$1" 2>/dev/null || true
}

for s in regional detalle; do
    echo "-> Calibrando colores para el esquema: $s"
    
    # 1. Crear columnas token
    ejecutar_sql "ALTER TABLE $s.lndare ADD COLUMN IF NOT EXISTS token VARCHAR(50);"
    ejecutar_sql "ALTER TABLE $s.coalne ADD COLUMN IF NOT EXISTS token VARCHAR(50);"
    ejecutar_sql "ALTER TABLE $s.depcnt ADD COLUMN IF NOT EXISTS token VARCHAR(50);"
    ejecutar_sql "ALTER TABLE $s.depare ADD COLUMN IF NOT EXISTS token VARCHAR(50);"
    ejecutar_sql "ALTER TABLE $s.unsare ADD COLUMN IF NOT EXISTS token VARCHAR(50);"

    # 2. Tierra, Costa, Contornos y Zonas sin cartografiar
    ejecutar_sql "UPDATE $s.lndare SET token = 'LANDA';"
    ejecutar_sql "UPDATE $s.coalne SET token = 'CSTLN';"
    ejecutar_sql "UPDATE $s.depcnt SET token = 'DEPCN';"
    ejecutar_sql "UPDATE $s.unsare SET token = 'NODTA';"

    # 3. Limpiar y recalibrar el Mar (Esquema SHOM adaptado a curvas 10m/20m SHN)
    ejecutar_sql "UPDATE $s.depare SET token = NULL;"
    ejecutar_sql "UPDATE $s.depare SET token = 'DEPIT' WHERE drval1 < 0;"
    ejecutar_sql "UPDATE $s.depare SET token = 'DEPVS' WHERE token IS NULL AND drval1 >= 0 AND drval1 < 10;"
    ejecutar_sql "UPDATE $s.depare SET token = 'DEPMD' WHERE token IS NULL AND drval1 >= 10 AND drval1 < 20;"
    ejecutar_sql "UPDATE $s.depare SET token = 'DEPDW' WHERE token IS NULL AND drval1 >= 20;"
    ejecutar_sql "UPDATE $s.depare SET token = 'DEPDW' WHERE token IS NULL;"
done

echo "====================================================="
echo "¡Cartografía procesada y lista para GeoServer!"
echo "====================================================="