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
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
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

    /** Carta recibida y guardada en disco, lista para procesarse (de inmediato o en segundo plano). */
    public static class PreparedUpload {
        private Path tempDir;
        private String originalFilename;
        private String band;
        private String ext;

        public String getOriginalFilename() {
            return originalFilename;
        }

        public String getBand() {
            return band;
        }

        /** Identificador de la carta (ej. "101 (2).pdf" -> "101") para detectar reintentos de la misma carta. */
        public String getChartKey() {
            return band + ":" + extractCanonicalChartId(originalFilename).toLowerCase();
        }
    }

    /**
     * Procesa la subida de una carta náutica (GeoPDF o TIFF), realiza la validación de
     * georreferenciación con GDAL, convierte a COG EPSG:3395 y la traslada a la carpeta
     * del servidor para su actualización en GeoServer.
     */
    public CartographyUploadResponse uploadAndConvert(MultipartFile file, String band, boolean reloadGeoserver) {
        PreparedUpload prepared = new PreparedUpload();
        CartographyUploadResponse error = prepareUpload(file, band, prepared);
        if (error != null) {
            return error;
        }
        return processPreparedUpload(prepared, reloadGeoserver, stage -> { });
    }

    /**
     * Valida banda y formato y guarda el archivo recibido en una carpeta temporal propia del trabajo.
     * Devuelve null si todo está bien (y completa {@code out}); si no, la respuesta de error.
     */
    public CartographyUploadResponse prepareUpload(MultipartFile file, String band, PreparedUpload out) {
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
            // Guardar archivo recibido en disco de forma segura usando Java NIO
            try (var in = file.getInputStream()) {
                Files.copy(in, tempDir.resolve(originalFilename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("No se pudo guardar el archivo recibido en la carpeta temporal: {}", e.getMessage());
            try {
                FileUtils.deleteDirectory(tempDir.toFile());
            } catch (Exception ignored) {}
            return CartographyUploadResponse.builder()
                    .success(false)
                    .fileName(originalFilename)
                    .band(cleanBand)
                    .message("Error guardando el archivo en el servidor: " + e.getMessage())
                    .build();
        }

        out.tempDir = tempDir;
        out.originalFilename = originalFilename;
        out.band = cleanBand;
        out.ext = ext;
        return null;
    }

    /** Descarta una carta preparada que no se va a procesar (borra su carpeta temporal). */
    public void discardPreparedUpload(PreparedUpload prepared) {
        try {
            if (prepared != null && prepared.tempDir != null) {
                FileUtils.deleteDirectory(prepared.tempDir.toFile());
            }
        } catch (Exception ignored) {}
    }

    /**
     * Valida, convierte a COG EPSG:3395, mueve a la carpeta de la banda y publica en GeoServer una carta ya
     * guardada por {@link #prepareUpload}. {@code stage} recibe la etapa en curso (VALIDATING, CONVERTING,
     * PUBLISHING) para informar el avance. Siempre elimina la carpeta temporal del trabajo.
     */
    public CartographyUploadResponse processPreparedUpload(PreparedUpload prepared, boolean reloadGeoserver,
                                                          Consumer<String> stage) {
        long startTime = System.currentTimeMillis();
        Path tempDir = prepared.tempDir;
        String originalFilename = prepared.originalFilename;
        String cleanBand = prepared.band;
        String ext = prepared.ext;

        try {
            stage.accept("VALIDATING");
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

            stage.accept("CONVERTING");
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
            boolean isSingleChart = "banda1".equals(cleanBand) || "banda2".equals(cleanBand);

            // 3. Preparar la carpeta de la banda con permisos que GeoServer (otro usuario) pueda leer y escribir
            stage.accept("PUBLISHING");
            Path targetBandDir = resolveStorageBandDir(cleanBand);
            Files.createDirectories(targetBandDir);
            String permissionProblem = ensureGeoserverCanWrite(targetBandDir);
            if (permissionProblem != null) {
                return CartographyUploadResponse.builder()
                        .success(false)
                        .fileName(originalFilename)
                        .band(cleanBand)
                        .isGeoreferenced(true)
                        .srsDetected(validation.getSrs())
                        .geoserverPublished(false)
                        .message(permissionProblem)
                        .error(permissionProblem)
                        .build();
            }

            // Cartas que la nueva reemplaza (misma carta con otro sufijo, o la carta única de banda1/banda2)
            List<Path> replaced = isSingleChart
                    ? listRasters(targetBandDir)
                    : findPreviousVariantsOfChart(targetBandDir, canonicalChartId, outputCogName);
            if (!isSingleChart) {
                replaced.addAll(findSyntheticTestRasters(targetBandDir));
                if (!replaced.isEmpty()) {
                    // Retirar del índice del mosaico sólo esas cartas (no se borra el índice completo)
                    geoserverSyncService.removeGranules(cleanBand, fileNames(replaced));
                }
                deleteFiles(replaced, targetBandDir);
            } else {
                // banda1/banda2: el archivo anterior se borra sólo después de publicar bien el nuevo
                replaced.removeIf(p -> p.getFileName().toString().equalsIgnoreCase(outputCogName));
            }

            if (!isSingleChart && !hasMosaicIndex(targetBandDir, cleanBand)) {
                // Primera carta de la banda: configurar el mosaico heterogéneo ANTES de crear el almacén
                writeIndexerProperties(targetBandDir, cleanBand);
            }

            Path finalDestination = targetBandDir.resolve(outputCogName);
            boolean overwritesExisting = Files.exists(finalDestination);
            Files.move(cogGeneratedPath, finalDestination, StandardCopyOption.REPLACE_EXISTING);
            makeReadableByGeoserver(finalDestination);
            log.info("Archivo COG movido exitosamente a: {}", finalDestination.toAbsolutePath());

            // 4. Publicar en GeoServer (almacén, capa, estilo, grupo y verificación)
            GeoserverSyncService.PublishResult publish = null;
            if (reloadGeoserver) {
                publish = isSingleChart
                        ? geoserverSyncService.publishSingleChart(cleanBand, outputCogName)
                        : geoserverSyncService.publishMosaicGranule(cleanBand, outputCogName);
                if (isSingleChart) {
                    if (publish.isSuccess()) {
                        deleteFiles(replaced, targetBandDir);
                    } else if (!overwritesExisting) {
                        Files.deleteIfExists(finalDestination);
                    }
                }
            } else if (isSingleChart) {
                deleteFiles(replaced, targetBandDir);
            }

            long totalTimeMs = System.currentTimeMillis() - startTime;
            boolean published = publish == null || publish.isSuccess();
            String message;
            if (publish == null) {
                message = "Archivo convertido a COG EPSG:3395. No se solicitó publicar en GeoServer.";
            } else if (publish.isSuccess()) {
                message = "Carta convertida y publicada. " + publish.getMessage();
            } else {
                message = "La carta se convirtió, pero NO se publicó en GeoServer: " + publish.getMessage();
            }

            return CartographyUploadResponse.builder()
                    .success(published)
                    .fileName(outputCogName)
                    .band(cleanBand)
                    .isGeoreferenced(true)
                    .srsDetected(validation.getSrs())
                    .outputFilePath(finalDestination.toAbsolutePath().toString())
                    .geoserverReloaded(publish != null && publish.isSuccess())
                    .geoserverPublished(publish != null ? publish.isSuccess() : null)
                    .geoserverMessage(publish != null ? publish.getMessage() : null)
                    .geoserverSteps(publish != null ? publish.getSteps() : null)
                    .convertedSizeBytes(convertedSize)
                    .processingTimeMs(totalTimeMs)
                    .pixelSize(validation.getPixelSize())
                    .message(message)
                    .error(published ? null : publish.getMessage())
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
     * Resincroniza la capa en GeoServer para la banda solicitada (GeoTIFF o ImageMosaic): crea lo que falte,
     * cosecha las cartas que no estén en el índice, asegura estilo y grupo, y verifica el resultado.
     */
    public GeoserverSyncService.PublishResult reloadBandLayer(String band) {
        String cleanBand = band != null ? band.toLowerCase().trim() : "";
        if (!VALID_BANDS.contains(cleanBand)) {
            return new GeoserverSyncService.PublishResult().failWith("Banda no válida: " + cleanBand);
        }
        Path bandDir = resolveStorageBandDir(cleanBand);

        if ("banda1".equals(cleanBand) || "banda2".equals(cleanBand)) {
            List<Path> rasters = Files.exists(bandDir) ? listRasters(bandDir) : new ArrayList<>();
            String singleFileName = rasters.isEmpty() ? null : rasters.get(0).getFileName().toString();
            return geoserverSyncService.resyncSingleChart(cleanBand, singleFileName);
        }

        List<String> files = new ArrayList<>();
        if (Files.exists(bandDir)) {
            String permissionProblem = ensureGeoserverCanWrite(bandDir);
            if (permissionProblem != null) {
                return new GeoserverSyncService.PublishResult().failWith(permissionProblem);
            }
            normalizeExistingMosaicRasters(bandDir);
            for (Path p : listRasters(bandDir)) {
                makeReadableByGeoserver(p);
                files.add(p.getFileName().toString());
            }
            if (!hasMosaicIndex(bandDir, cleanBand)) {
                writeIndexerProperties(bandDir, cleanBand);
            }
        }
        return geoserverSyncService.resyncMosaic(cleanBand, files);
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
     * Comprueba que GeoServer (que corre con otro usuario dentro de su contenedor) pueda escribir el índice del
     * mosaico en la carpeta de la banda. En Linux intenta dejar la carpeta con permisos 777 (como banda2..banda4).
     * Devuelve un mensaje de error si no es posible; null si todo está bien o si el sistema no es POSIX (Windows).
     */
    private String ensureGeoserverCanWrite(Path bandDir) {
        PosixFileAttributeView view = Files.getFileAttributeView(bandDir, PosixFileAttributeView.class);
        if (view == null) {
            return null;
        }
        try {
            Set<PosixFilePermission> perms = view.readAttributes().permissions();
            if (perms.contains(PosixFilePermission.OTHERS_WRITE) && perms.contains(PosixFilePermission.OTHERS_EXECUTE)
                    && perms.contains(PosixFilePermission.OTHERS_READ)) {
                return null;
            }
            view.setPermissions(PosixFilePermissions.fromString("rwxrwxrwx"));
            log.info("Permisos de {} ajustados a 777 para que GeoServer pueda escribir el índice", bandDir);
            return null;
        } catch (Exception e) {
            String msg = "GeoServer no puede escribir en la carpeta " + bandDir + " y no fue posible ajustar sus permisos ("
                    + e.getMessage() + "). Solicite al administrador: chmod 777 " + bandDir;
            log.error(msg);
            return msg;
        }
    }

    /** Deja el archivo legible por GeoServer (664). Sin efecto en Windows o si el archivo no es del usuario actual. */
    private void makeReadableByGeoserver(Path file) {
        try {
            if (Files.getFileAttributeView(file, PosixFileAttributeView.class) != null) {
                Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-rw-r--"));
            }
        } catch (Exception e) {
            log.warn("No se pudieron ajustar los permisos de {}: {}", file, e.getMessage());
        }
    }

    /** Indica si GeoServer ya creó el índice del mosaico (banda.properties o banda.shp). */
    private boolean hasMosaicIndex(Path bandDir, String band) {
        return Files.exists(bandDir.resolve(band + ".properties")) || Files.exists(bandDir.resolve(band + ".shp"));
    }

    /**
     * Escribe indexer.properties ANTES de crear el almacén para que GeoServer construya el mosaico como
     * heterogéneo desde el inicio (cartas de distinta resolución en una misma banda). Elimina restos de índices
     * de intentos anteriores que no correspondan a un almacén existente.
     */
    private void writeIndexerProperties(Path bandDir, String band) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bandDir)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString().toLowerCase();
                if (name.equals("sample_image.dat") || name.endsWith(".shp") || name.endsWith(".shx")
                        || name.endsWith(".dbf") || name.endsWith(".fix") || name.endsWith(".qix")
                        || name.endsWith(".prj") || name.equals(band + ".properties")) {
                    Files.deleteIfExists(entry);
                }
            }
            Path indexer = bandDir.resolve("indexer.properties");
            Files.writeString(indexer, "Heterogeneous=true\nCaching=false\nAbsolutePath=false\nName=" + band + "\n");
            makeReadableByGeoserver(indexer);
            log.info("indexer.properties (Heterogeneous=true) creado en {}", bandDir);
        } catch (Exception e) {
            log.warn("No se pudo preparar indexer.properties en {}: {}", bandDir, e.getMessage());
        }
    }

    /** Lista los .tif/.tiff de la carpeta (excluye archivos temporales de normalización). */
    private List<Path> listRasters(Path dir) {
        List<Path> list = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString().toLowerCase();
                if ((name.endsWith(".tif") || name.endsWith(".tiff")) && !name.startsWith("norm_")) {
                    list.add(entry);
                }
            }
        } catch (Exception e) {
            log.warn("Error listando cartas en {}: {}", dir, e.getMessage());
        }
        list.sort(null);
        return list;
    }

    /**
     * Versiones previas u obsoletas de la misma carta (ej. 101_cog.tif, 101__2__cog.tif) que la nueva reemplaza.
     */
    private List<Path> findPreviousVariantsOfChart(Path targetBandDir, String canonicalChartId, String newCogName) {
        List<Path> variants = new ArrayList<>();
        String lowerId = canonicalChartId.toLowerCase();
        for (Path entry : listRasters(targetBandDir)) {
            String name = entry.getFileName().toString().toLowerCase();
            boolean isExact = name.equals(newCogName.toLowerCase());
            boolean isVariant = name.startsWith(lowerId + "_") || name.startsWith(lowerId + "-");
            if (isExact || isVariant) {
                variants.add(entry);
            }
        }
        return variants;
    }

    /** Rasters sintéticos de prueba que deben desaparecer cuando la banda ya tiene cartas reales. */
    private List<Path> findSyntheticTestRasters(Path targetBandDir) {
        List<Path> synthetic = new ArrayList<>();
        for (Path entry : listRasters(targetBandDir)) {
            String name = entry.getFileName().toString().toLowerCase();
            if (name.equals("georeferenced_cog.tif") || name.equals("georeferenced_carta_cog.tif")) {
                synthetic.add(entry);
            }
        }
        return synthetic;
    }

    private static List<String> fileNames(List<Path> paths) {
        List<String> names = new ArrayList<>();
        for (Path p : paths) {
            names.add(p.getFileName().toString());
        }
        return names;
    }

    private void deleteFiles(List<Path> files, Path dir) {
        for (Path f : files) {
            try {
                if (Files.deleteIfExists(f)) {
                    log.info("Eliminando versión previa u obsoleta de la carta en {}: {}", dir, f.getFileName());
                }
            } catch (Exception ex) {
                log.warn("No se pudo eliminar archivo previo {}: {}", f.getFileName(), ex.getMessage());
            }
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

}
