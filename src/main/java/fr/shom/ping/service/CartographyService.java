package fr.shom.ping.service;

import fr.shom.ping.model.dto.CartographyLayerStatusDto;
import fr.shom.ping.model.dto.CartographyUploadResponse;
import fr.shom.ping.model.dto.CartographyValidationResult;
import fr.shom.ping.properties.CartographyProperties;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartographyService {

    private final CartographyProperties properties;
    private final GdalExecutorService gdalExecutorService;
    private final GeoserverSyncService geoserverSyncService;

    private static final List<String> VALID_BANDS = List.of(
            "banda1", "banda2", "banda3", "banda4", "banda5", "banda6"
    );

    /**
     * Procesa la subida de una carta náutica (GeoPDF o TIFF), realiza la validación de
     * georreferenciación con GDAL, convierte a COG EPSG:3395 y la traslada a la carpeta
     * del servidor para su actualización en GeoServer.
     */
    public CartographyUploadResponse uploadAndConvert(MultipartFile file, String band, boolean reloadGeoserver) {
        long startTime = System.currentTimeMillis();

        if (file == null || file.isEmpty()) {
            return CartographyUploadResponse.builder()
                    .success(false)
                    .message("No se recibió ningún archivo para procesar.")
                    .build();
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            originalFilename = "archivo_cartografico";
        }

        String cleanBand = band != null ? band.toLowerCase().trim() : "";
        if (!VALID_BANDS.contains(cleanBand)) {
            return CartographyUploadResponse.builder()
                    .success(false)
                    .fileName(originalFilename)
                    .band(cleanBand)
                    .message("La banda especificada '" + cleanBand + "' no es válida. Debe ser una de: " + VALID_BANDS)
                    .build();
        }

        int dotIdx = originalFilename.lastIndexOf('.');
        String ext = (dotIdx != -1) ? originalFilename.substring(dotIdx + 1).toLowerCase() : "";
        if (!ext.equals("pdf") && !ext.equals("tif") && !ext.equals("tiff")) {
            return CartographyUploadResponse.builder()
                    .success(false)
                    .fileName(originalFilename)
                    .band(cleanBand)
                    .message("Formato '" + ext + "' no soportado. Sólo se admiten archivos .pdf, .tif y .tiff.")
                    .build();
        }

        // Crear directorio temporal único para la ejecución (asegurando compatibilidad Linux/Windows)
        String configuredTemp = properties.getTempPath();
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (!isWindows && (configuredTemp.startsWith("c:") || configuredTemp.startsWith("C:"))) {
            configuredTemp = "/tmp/pisac-cartography";
        }
        Path baseTempPath = Paths.get(configuredTemp);
        if (!baseTempPath.isAbsolute()) {
            baseTempPath = Paths.get(System.getProperty("java.io.tmpdir", "/tmp")).resolve("pisac-cartography");
        }

        Path tempDir = baseTempPath.resolve("job_" + UUID.randomUUID()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(tempDir);
        } catch (IOException e) {
            log.error("No se pudo crear la carpeta temporal: {}", e.getMessage());
            return CartographyUploadResponse.builder()
                    .success(false)
                    .fileName(originalFilename)
                    .band(cleanBand)
                    .message("Error creando carpeta temporal del servidor: " + e.getMessage())
                    .build();
        }

        Path uploadedPath = tempDir.resolve(originalFilename);

        try {
            // Guardar archivo recibido en disco de forma segura usando Java NIO
            try (var in = file.getInputStream()) {
                Files.copy(in, uploadedPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 1. Validar georreferenciación y metadatos con gdalinfo
            CartographyValidationResult validation = gdalExecutorService.validateCartographyFile(tempDir, originalFilename);

            if (!validation.isGeoreferenced()) {
                log.warn("Archivo {} descartado por no contener georreferenciación.", originalFilename);
                return CartographyUploadResponse.builder()
                        .success(false)
                        .fileName(originalFilename)
                        .band(cleanBand)
                        .isGeoreferenced(false)
                        .srsDetected(validation.getSrs())
                        .message("El archivo no posee información de georreferenciación ni coordenadas asignadas. " +
                                "Por favor verifique que la carta náutica esté georreferenciada antes de subirla.")
                        .error(validation.getErrorMessage())
                        .build();
            }

            // 2. Extraer nombre canónico de la carta (ej. "101 (2).pdf" -> "101") y proceder a COG EPSG:3395
            String canonicalChartId = extractCanonicalChartId(originalFilename);
            String outputCogName = canonicalChartId + "_cog.tif";

            GdalExecutorService.ProcessResult convResult = gdalExecutorService.convertToCog3395(
                    tempDir, originalFilename, outputCogName, ext, validation
            );

            if (!convResult.isSuccess()) {
                log.error("Falló la conversión GDAL para {}: {}", originalFilename, convResult.getStderr());
                return CartographyUploadResponse.builder()
                        .success(false)
                        .fileName(originalFilename)
                        .band(cleanBand)
                        .isGeoreferenced(true)
                        .srsDetected(validation.getSrs())
                        .message("Error durante la conversión a COG EPSG:3395.")
                        .error(convResult.getStderr())
                        .build();
            }

            Path cogGeneratedPath = tempDir.resolve(outputCogName);
            if (!Files.exists(cogGeneratedPath)) {
                return CartographyUploadResponse.builder()
                        .success(false)
                        .fileName(originalFilename)
                        .band(cleanBand)
                        .message("El archivo COG optimizado no fue generado en la ruta esperada.")
                        .build();
            }

            long convertedSize = Files.size(cogGeneratedPath);

            // 3. Mover archivo COG al directorio final de la banda, REEMPLAZANDO cualquier versión previa de la carta
            Path targetBandDir = resolveStorageBandDir(cleanBand);
            Files.createDirectories(targetBandDir);

            if ("banda1".equals(cleanBand) || "banda2".equals(cleanBand)) {
                // Para Banda 1 y Banda 2 (carta única), limpiar cualquier carta previa de la banda
                cleanAllRastersInDir(targetBandDir);
            } else {
                // Para ImageMosaic (Banda 3 a Banda 6), eliminar cualquier carta previa u obsoleta
                // con este mismo identificador (ej. 101_cog.tif, 101__2__cog.tif, etc.) para que sólo quede la recién procesada
                cleanPreviousVariantsOfChart(targetBandDir, canonicalChartId, outputCogName);
            }

            Path finalDestination = targetBandDir.resolve(outputCogName);
            Files.move(cogGeneratedPath, finalDestination, StandardCopyOption.REPLACE_EXISTING);
            log.info("Archivo COG movido exitosamente a: {}", finalDestination.toAbsolutePath());

            if (!"banda1".equals(cleanBand) && !"banda2".equals(cleanBand)) {
                prepareMosaicDirectoryForSync(targetBandDir, cleanBand);
            }

            // 4. Actualizar / sincronizar GeoServer si se solicitó
            boolean geoserverReloaded = false;
            if (reloadGeoserver) {
                geoserverReloaded = geoserverSyncService.syncBandStore(cleanBand, outputCogName);
                if (!"banda1".equals(cleanBand) && !"banda2".equals(cleanBand)) {
                    ensureHeterogeneousMosaicProperties(targetBandDir, cleanBand);
                }
            }

            long totalTimeMs = System.currentTimeMillis() - startTime;

            return CartographyUploadResponse.builder()
                    .success(true)
                    .fileName(outputCogName)
                    .band(cleanBand)
                    .isGeoreferenced(true)
                    .srsDetected(validation.getSrs())
                    .outputFilePath(finalDestination.toAbsolutePath().toString())
                    .geoserverReloaded(geoserverReloaded)
                    .convertedSizeBytes(convertedSize)
                    .processingTimeMs(totalTimeMs)
                    .pixelSize(validation.getPixelSize())
                    .message("¡Archivo procesado y convertido exitosamente a COG EPSG:3395!")
                    .build();

        } catch (Exception e) {
            log.error("Excepción inesperada procesando cartografía {}: {}", originalFilename, e.getMessage(), e);
            return CartographyUploadResponse.builder()
                    .success(false)
                    .fileName(originalFilename)
                    .band(cleanBand)
                    .message("Ocurrió un error inesperado durante el procesamiento: " + e.getMessage())
                    .error(e.getMessage())
                    .build();
        } finally {
            // Limpieza integral del directorio temporal del trabajo
            try {
                FileUtils.deleteDirectory(tempDir.toFile());
            } catch (Exception ignored) {}
        }
    }

    /**
     * Recarga la capa en GeoServer para la banda solicitada (GeoTIFF o ImageMosaic).
     */
    public boolean reloadBandLayer(String band) {
        String cleanBand = band != null ? band.toLowerCase().trim() : "";
        String singleFileName = null;
        Path bandDir = resolveStorageBandDir(cleanBand);

        if ("banda1".equals(cleanBand) || "banda2".equals(cleanBand)) {
            if (Files.exists(bandDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(bandDir)) {
                    for (Path p : stream) {
                        String n = p.getFileName().toString().toLowerCase();
                        if (n.endsWith(".tif") || n.endsWith(".tiff")) {
                            singleFileName = p.getFileName().toString();
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        } else if (Files.exists(bandDir)) {
            prepareMosaicDirectoryForSync(bandDir, cleanBand);
            normalizeExistingMosaicRasters(bandDir);
        }

        boolean synced = geoserverSyncService.syncBandStore(cleanBand, singleFileName);
        if (!"banda1".equals(cleanBand) && !"banda2".equals(cleanBand) && Files.exists(bandDir)) {
            ensureHeterogeneousMosaicProperties(bandDir, cleanBand);
        }
        return synced;
    }

    /**
     * Obtiene el estado actual de los archivos y capas por cada banda náutica.
     */
    public Map<String, CartographyLayerStatusDto> getLayersStatus() {
        Map<String, CartographyLayerStatusDto> statusMap = new LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (String band : VALID_BANDS) {
            Path bandDir = resolveStorageBandDir(band);
            int count = 0;
            long totalBytes = 0;
            FileTime newestTime = null;

            if (Files.exists(bandDir) && Files.isDirectory(bandDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(bandDir)) {
                    for (Path entry : stream) {
                        String name = entry.getFileName().toString().toLowerCase();
                        if (name.endsWith(".tif") || name.endsWith(".tiff")) {
                            count++;
                            try {
                                totalBytes += Files.size(entry);
                                FileTime time = Files.getLastModifiedTime(entry);
                                if (newestTime == null || time.compareTo(newestTime) > 0) {
                                    newestTime = time;
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error leyendo directorio de banda {}: {}", band, e.getMessage());
                }
            }

            String lastMod = newestTime != null ? sdf.format(new Date(newestTime.toMillis())) : null;

            statusMap.put(band, CartographyLayerStatusDto.builder()
                    .active(count > 0)
                    .fileCount(count)
                    .totalSizeBytes(totalBytes)
                    .lastModified(lastMod)
                    .storageFolder(bandDir.toAbsolutePath().toString().replace("\\", "/"))
                    .build());
        }

        return statusMap;
    }

    /**
     * Resuelve la ruta absoluta de la carpeta de almacenamiento de una banda.
     */
    public Path resolveStorageBandDir(String band) {
        Path base = Paths.get(properties.getStorageBasePath());
        if (!base.isAbsolute()) {
            Path userDir = Paths.get(System.getProperty("user.dir"));
            Path candidate = userDir.resolve(base);
            if (!Files.exists(candidate) && Files.exists(userDir.resolve("ping-api").resolve(base))) {
                base = userDir.resolve("ping-api").resolve(base);
            } else {
                base = candidate;
            }
        }
        return base.resolve(band.toLowerCase().trim());
    }

    /**
     * Extrae el identificador canónico de una carta náutica eliminando sufijos de descarga,
     * copias o extensiones repetidas (ej. "101 (2)" -> "101", "408 (1)" -> "408", "101_cog" -> "101").
     */
    public static String extractCanonicalChartId(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "carta";
        }
        int dotIdx = filename.lastIndexOf('.');
        String base = (dotIdx != -1) ? filename.substring(0, dotIdx).trim() : filename.trim();

        // Eliminar sufijo _cog o -cog si ya venía en el nombre original
        base = base.replaceAll("(?i)[_-]cog$", "").trim();

        // Eliminar sufijos de copias típicos de Windows/navegadores (ej. " (1)", " (2)", " - copia", "_copia")
        base = base.replaceAll("(?i)\\s*\\([0-9]+\\)$", "").trim();
        base = base.replaceAll("(?i)\\s*-[0-9]+$", "").trim();
        base = base.replaceAll("(?i)[_\\s-]+(copia|copy)(\\s*\\([0-9]+\\))?$", "").trim();

        // Sanitizar caracteres restantes: sólo letras, números y guiones
        String sanitized = base.replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        return sanitized.isEmpty() ? "carta" : sanitized;
    }

    /**
     * Limpia archivos sintéticos de prueba (georeferenced_cog.tif) cuando ya existen cartas reales en la banda
     * y purga los archivos de índice antiguos de GeoTools ImageMosaic (.shp, .dbf, .shx, .fix, .qix, .properties, sample_image.dat)
     * para forzar a GeoServer a reconstruir el índice con todas las cartas reales y sus resoluciones verdaderas.
     */
    private void prepareMosaicDirectoryForSync(Path targetBandDir, String band) {
        try {
            boolean hasRealCharts = false;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetBandDir)) {
                for (Path entry : stream) {
                    String name = entry.getFileName().toString().toLowerCase();
                    if ((name.endsWith(".tif") || name.endsWith(".tiff"))
                            && !name.equals("georeferenced_cog.tif")
                            && !name.equals("georeferenced_carta_cog.tif")) {
                        hasRealCharts = true;
                        break;
                    }
                }
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetBandDir)) {
                for (Path entry : stream) {
                    String name = entry.getFileName().toString().toLowerCase();
                    if (hasRealCharts && (name.equals("georeferenced_cog.tif") || name.equals("georeferenced_carta_cog.tif"))) {
                        log.info("Eliminando raster sintético de prueba {} en {}", name, targetBandDir);
                        Files.deleteIfExists(entry);
                        continue;
                    }
                    if (name.equals("sample_image.dat")
                            || name.endsWith(".shp")
                            || name.endsWith(".shx")
                            || name.endsWith(".dbf")
                            || name.endsWith(".fix")
                            || name.endsWith(".qix")
                            || name.endsWith(".prj")
                            || name.endsWith(".properties")) {
                        Files.deleteIfExists(entry);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Advertencia preparando directorio ImageMosaic para {}: {}", band, e.getMessage());
        }
    }

    /**
     * Verifica que todas las cartas TIFF existentes en un directorio ImageMosaic tengan 4 bandas RGBA homogéneas.
     * Si alguna carta subida previamente tenía 1 banda (paleta) o 3 bandas (RGB sin alfa), la normaliza in-situ a 4 bandas RGBA COG.
     */
    private void normalizeExistingMosaicRasters(Path bandDir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bandDir)) {
            for (Path entry : stream) {
                String fileName = entry.getFileName().toString();
                String lower = fileName.toLowerCase();
                if (!lower.endsWith(".tif") && !lower.endsWith(".tiff")) {
                    continue;
                }
                if (lower.equals("georeferenced_cog.tif") || lower.equals("georeferenced_carta_cog.tif")) {
                    continue;
                }
                CartographyValidationResult val = gdalExecutorService.validateCartographyFile(bandDir, fileName);
                if (val.isGeoreferenced() && (val.getBandCount() != 4 || val.isHasColorTable() || !val.isHasAlphaBand())) {
                    log.info("Normalizando carta existente {} en {} a 4 bandas RGBA EPSG:3395 (bandas actuales: {}, colorTable: {})",
                            fileName, bandDir, val.getBandCount(), val.isHasColorTable());
                    String normalizedName = "norm_" + System.currentTimeMillis() + "_" + fileName;
                    GdalExecutorService.ProcessResult res = gdalExecutorService.convertToCog3395(
                            bandDir, fileName, normalizedName, "tif", val
                    );
                    Path normalizedPath = bandDir.resolve(normalizedName);
                    if (res.isSuccess() && Files.exists(normalizedPath)) {
                        Files.move(normalizedPath, entry, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        Files.deleteIfExists(normalizedPath);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Advertencia normalizando rasters existentes en {}: {}", bandDir, e.getMessage());
        }
    }

    /**
     * Asegura que el archivo <banda>.properties generado por GeoServer tenga Heterogeneous=true
     * para admitir cartas náuticas de distintas escalas dentro de una misma banda (ej. 1:50.000 y 1:150.000 en Banda 4).
     */
    private void ensureHeterogeneousMosaicProperties(Path targetBandDir, String band) {
        try {
            Path propsFile = targetBandDir.resolve(band + ".properties");
            if (Files.exists(propsFile)) {
                String content = Files.readString(propsFile);
                if (content.contains("Heterogeneous=false")) {
                    content = content.replace("Heterogeneous=false", "Heterogeneous=true");
                    Files.writeString(propsFile, content);
                }
            }
        } catch (Exception e) {
            log.debug("No se pudo ajustar Heterogeneous=true en {}.properties: {}", band, e.getMessage());
        }
    }

    private void cleanPreviousVariantsOfChart(Path targetBandDir, String canonicalChartId, String newCogName) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetBandDir)) {
            String lowerId = canonicalChartId.toLowerCase();
            for (Path entry : stream) {
                String name = entry.getFileName().toString().toLowerCase();
                if (!name.endsWith(".tif") && !name.endsWith(".tiff")) {
                    continue;
                }
                boolean isExact = name.equals(newCogName.toLowerCase());
                boolean isVariant = name.startsWith(lowerId + "_") || name.startsWith(lowerId + "-");
                if (isExact || isVariant) {
                    log.info("Eliminando versión previa u obsoleta de la carta en {}: {}", targetBandDir, name);
                    try {
                        Files.deleteIfExists(entry);
                    } catch (Exception ex) {
                        log.warn("No se pudo eliminar archivo previo {}: {}", name, ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error escaneando cartas previas en {}: {}", targetBandDir, e.getMessage());
        }
    }

    private void cleanAllRastersInDir(Path targetBandDir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetBandDir)) {
            for (Path oldFile : stream) {
                String oldName = oldFile.getFileName().toString().toLowerCase();
                if (oldName.endsWith(".tif") || oldName.endsWith(".tiff")) {
                    Files.deleteIfExists(oldFile);
                }
            }
        } catch (Exception ex) {
            log.warn("Advertencia limpiando cartas previas en {}: {}", targetBandDir, ex.getMessage());
        }
    }
}
