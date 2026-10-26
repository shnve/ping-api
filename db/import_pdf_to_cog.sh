#!/bin/bash

# Moverse al directorio de datos
cd /data || exit 1

# Validación inicial: comprobar si existen archivos .pdf
count=$(ls -1 *.pdf 2>/dev/null | wc -l)
if [ "$count" -eq 0 ]; then
    echo "No se encontraron archivos .pdf en el directorio /data/. Finalizando sin errores."
    exit 0
fi

echo "Se encontraron $count archivo(s) .pdf. Iniciando procesamiento por lotes..."

# Bucle de procesamiento
for pdf in *.pdf; do
    # Manejo de nombres: Extraer el nombre del archivo sin la extensión .pdf
    filename=$(basename "$pdf" .pdf)
    
    echo "----------------------------------------"
    echo "Procesando archivo: $pdf"
    
    # Paso 1: Traducción, Rasterización y asignación de EPSG
    echo "Paso 1: Generando raster temporal..."
    gdal_translate --config GDAL_PDF_DPI 300 -a_srs EPSG:3395 -of GTiff -co TILED=YES -co COMPRESS=LZW "$pdf" "temp_${filename}.tif"
    
    # Paso 2: Pirámides
    echo "Paso 2: Generando pirámides (vistas generales)..."
    gdaladdo -r average "temp_${filename}.tif" 2 4 8 16 32 64
    
    # Paso 3: Optimización Final COG
    echo "Paso 3: Estructurando COG final..."
    gdal_translate -of GTiff -co COPY_SRC_OVERVIEWS=YES -co COMPRESS=LZW -co TILED=YES "temp_${filename}.tif" "${filename}_cog.tif"
    
    # Paso 4: Limpieza
    echo "Paso 4: Eliminando archivo temporal..."
    rm "temp_${filename}.tif"
    
    echo ">>> ¡Archivo $pdf convertido exitosamente a ${filename}_cog.tif!"
done

echo "========================================"
echo "Proceso por lotes completado exitosamente para todos los archivos."
