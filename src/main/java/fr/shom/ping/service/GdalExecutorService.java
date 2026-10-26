package fr.shom.ping.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.shom.ping.model.dto.CartographyValidationResult;
import fr.shom.ping.properties.CartographyProperties;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GdalExecutorService {

    private final CartographyProperties properties;
    private final ObjectMapper objectMapper;

    private Boolean nativeGdalAvailable = null;

    @Data
    @Builder
    public static class ProcessResult {
        private int exitCode;
        private String stdout;
        private String stderr;
        private boolean success;
    }

    /**
     * Determina si el binario gdalinfo está disponible nativamente en el PATH del host.
     */
    public synchronized boolean isNativeGdalAvailable() {
        if (nativeGdalAvailable != null) {
            return nativeGdalAvailable;
        }
        try {
            boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
            String checkCmd = isWindows ? "where" : "which";
            Process process = new ProcessBuilder(checkCmd, "gdalinfo").start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            nativeGdalAvailable = finished && process.exitValue() == 0;
        } catch (Exception e) {
            nativeGdalAvailable = false;
        }
        log.info("Soporte GDAL nativo en el sistema: {}", nativeGdalAvailable ? "SI" : "NO (usando Docker)");
        return nativeGdalAvailable;
    }

    /**
     * Ejecuta un comando GDAL (nativamente o mediante contenedor Docker).
     * Todos los archivos referenciados deben encontrarse en el directorio workDir.
     */
    public ProcessResult executeGdalCommand(Path workDir, List<String> commandArgs) {
        boolean nativeMode = isNativeGdalAvailable();
        List<String> fullCommand = new ArrayList<>();

        if (nativeMode) {
            fullCommand.addAll(commandArgs);
        } else {
            // Modo Docker: montamos workDir en /work dentro del contenedor
            String hostWorkPath = workDir.toAbsolutePath().toString().replace("\\", "/");
            fullCommand.add("docker");
            fullCommand.add("run");
            fullCommand.add("--rm");
            fullCommand.add("-v");
            fullCommand.add(hostWorkPath + ":/work");
            fullCommand.add(properties.getDockerImage());

            // Traducir argumentos: si un argumento referencia un archivo (.tif, .pdf, .tiff), mapearlo a /work/
            for (String arg : commandArgs) {
                String lower = arg.toLowerCase();
                if (lower.endsWith(".tif") || lower.endsWith(".pdf") || lower.endsWith(".tiff")) {
                    int lastSlash = Math.max(arg.lastIndexOf('/'), arg.lastIndexOf('\\'));
                    String simpleName = (lastSlash != -1) ? arg.substring(lastSlash + 1) : arg;
                    fullCommand.add("/work/" + simpleName);
                    continue;
                }
                fullCommand.add(arg);
            }
        }

        log.info("Ejecutando comando GDAL: {}", String.join(" ", fullCommand));

        try {
            ProcessBuilder pb = new ProcessBuilder(fullCommand);
            if (nativeMode) {
                pb.directory(workDir.toFile());
            }

            Process process = pb.start();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread outThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                } catch (Exception ignored) {}
            });

            Thread errThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (Exception ignored) {}
            });

            outThread.start();
            errThread.start();

            // Timeout generoso de 10 minutos para cartas grandes a 300 DPI
            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            outThread.join(2000);
            errThread.join(2000);

            if (!finished) {
                process.destroyForcibly();
                log.error("Comando GDAL superó el tiempo límite de 10 minutos.");
                return ProcessResult.builder()
                        .exitCode(-1)
                        .stdout(stdout.toString())
                        .stderr("Timeout: el proceso tardó más de 10 minutos.")
                        .success(false)
                        .build();
            }

            int exitCode = process.exitValue();
            boolean success = (exitCode == 0);
            if (!success) {
                log.warn("GDAL finalizó con código de error {}. Stderr: {}", exitCode, stderr);
            }

            return ProcessResult.builder()
                    .exitCode(exitCode)
                    .stdout(stdout.toString())
                    .stderr(stderr.toString())
                    .success(success)
                    .build();

        } catch (Exception e) {
            log.error("Error al ejecutar proceso GDAL: {}", e.getMessage(), e);
            return ProcessResult.builder()
                    .exitCode(-1)
                    .stdout("")
                    .stderr(e.getMessage())
                    .success(false)
                    .build();
        }
    }

    /**
     * Inspecciona y valida un archivo cartográfico (GeoPDF o TIFF) mediante gdalinfo -json.
     */
    public CartographyValidationResult validateCartographyFile(Path workDir, String filename) {
        List<String> args = List.of("gdalinfo", "-json", filename);
        ProcessResult res = executeGdalCommand(workDir, args);

        if (!res.isSuccess() || res.getStdout().trim().isEmpty()) {
            return CartographyValidationResult.builder()
                    .georeferenced(false)
                    .srs("Desconocido / Error")
                    .errorMessage("No se pudo leer la información del archivo con GDAL: " + res.getStderr())
                    .rawGdalInfoJson(res.getStdout())
                    .build();
        }

        try {
            JsonNode root = objectMapper.readTree(res.getStdout());

            // 1. Detectar si está georreferenciado (incluyendo coordinateSystem.wkt y gcps)
            boolean hasWgs84 = root.has("wgs84Extent") && !root.get("wgs84Extent").isNull();
            String coordSysWkt = "";
            if (root.has("coordinateSystem") && !root.get("coordinateSystem").isNull()) {
                JsonNode csNode = root.get("coordinateSystem");
                coordSysWkt = csNode.isObject()
                        ? csNode.path("wkt").asText(csNode.toString())
                        : csNode.asText("");
            }
            if (coordSysWkt.trim().isEmpty() && root.has("gcps") && !root.get("gcps").isNull()) {
                JsonNode gcpCs = root.path("gcps").path("coordinateSystem");
                coordSysWkt = gcpCs.isObject()
                        ? gcpCs.path("wkt").asText(gcpCs.toString())
                        : gcpCs.asText("");
            }
            boolean hasCoordSys = !coordSysWkt.trim().isEmpty();
            boolean hasGcps = root.has("gcps") && root.path("gcps").path("gcpList").isArray()
                    && root.path("gcps").path("gcpList").size() > 0;
            boolean hasStacEpsg = root.has("stac") && root.get("stac").has("proj:epsg") && !root.get("stac").get("proj:epsg").isNull();
            boolean hasStacWkt = root.has("stac") && root.get("stac").has("proj:wkt2") && !root.get("stac").get("proj:wkt2").isNull();

            boolean hasGeoTransform = false;
            if (root.has("geoTransform") && root.get("geoTransform").isArray()) {
                JsonNode gt = root.get("geoTransform");
                // Comprobar si no es la identidad trivial [0, 1, 0, 0, 0, 1]
                double gt1 = gt.path(1).asDouble(0);
                double gt5 = gt.path(5).asDouble(0);
                if (gt1 != 1.0 || gt5 != 1.0 || gt.path(0).asDouble(0) != 0 || gt.path(3).asDouble(0) != 0) {
                    hasGeoTransform = true;
                }
            }

            boolean isGeoreferenced = hasWgs84 || hasCoordSys || hasGcps || hasStacEpsg || hasStacWkt || hasGeoTransform;

            // 2. Extraer SRS / Proyección
            String detectedSrs = "Sin georreferenciación";
            if (hasStacEpsg) {
                detectedSrs = "EPSG:" + root.get("stac").get("proj:epsg").asText();
            } else if (hasCoordSys) {
                String cs = coordSysWkt;
                if (cs.contains("3395")) {
                    detectedSrs = "EPSG:3395 (World Mercator)";
                } else if (cs.contains("4326")) {
                    detectedSrs = "EPSG:4326 (WGS 84)";
                } else if (cs.contains("3857")) {
                    detectedSrs = "EPSG:3857 (Web Mercator)";
                } else if (cs.contains("ID[\"EPSG\",")) {
                    int idx = cs.lastIndexOf("ID[\"EPSG\",");
                    int end = cs.indexOf("]", idx);
                    if (idx != -1 && end != -1) {
                        detectedSrs = "EPSG:" + cs.substring(idx + 10, end).replace("\"", "").trim();
                    } else {
                        detectedSrs = "Proyectado";
                    }
                } else {
                    detectedSrs = "Georreferenciado (Custom SRS)";
                }
            } else if (isGeoreferenced) {
                detectedSrs = "Georreferenciado (SRS No estándar)";
            }

            // 3. Dimensiones y resolución de píxel
            int width = 0;
            int height = 0;
            if (root.has("size") && root.get("size").isArray() && root.get("size").size() >= 2) {
                width = root.get("size").get(0).asInt();
                height = root.get("size").get(1).asInt();
            }

            String pixelSize = "N/A";
            if (root.has("geoTransform") && root.get("geoTransform").isArray()) {
                double pxX = root.get("geoTransform").path(1).asDouble(0);
                double pxY = root.get("geoTransform").path(5).asDouble(0);
                pixelSize = String.format("%.2f x %.2f m", Math.abs(pxX), Math.abs(pxY));
            }

            // 4. Estructura de bandas y tabla de color (crítico para homogeneidad de ImageMosaic)
            int bandCount = 0;
            boolean hasColorTable = false;
            boolean hasAlphaBand = false;
            if (root.has("bands") && root.get("bands").isArray()) {
                JsonNode bandsNode = root.get("bands");
                bandCount = bandsNode.size();
                for (int i = 0; i < bandCount; i++) {
                    JsonNode b = bandsNode.get(i);
                    if (b.has("colorTable") && !b.get("colorTable").isNull()) {
                        hasColorTable = true;
                    }
                    String ci = b.path("colorInterpretation").asText("");
                    if ("Alpha".equalsIgnoreCase(ci)) {
                        hasAlphaBand = true;
                    }
                }
            }

            return CartographyValidationResult.builder()
                    .georeferenced(isGeoreferenced)
                    .srs(detectedSrs)
                    .width(width)
                    .height(height)
                    .pixelSize(pixelSize)
                    .bandCount(bandCount)
                    .hasColorTable(hasColorTable)
                    .hasAlphaBand(hasAlphaBand)
                    .rawGdalInfoJson(res.getStdout())
                    .build();

        } catch (Exception e) {
            log.error("Error parseando resultado JSON de gdalinfo: {}", e.getMessage());
            return CartographyValidationResult.builder()
                    .georeferenced(false)
                    .srs("Error de parseo")
                    .errorMessage("Error analizando metadatos espaciales: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Sobrecarga compatible para conversión a COG EPSG:3395.
     */
    public ProcessResult convertToCog3395(Path workDir, String inputFilename, String outputCogFilename, String format, String srs) {
        CartographyValidationResult validation = validateCartographyFile(workDir, inputFilename);
        return convertToCog3395(workDir, inputFilename, outputCogFilename, format, validation);
    }

    /**
     * Realiza el pipeline de conversión a Cloud Optimized GeoTIFF (COG) en EPSG:3395.
     * Estandariza SIEMPRE la salida a 4 bandas RGBA (Red, Green, Blue, Alpha) para garantizar
     * compatibilidad total entre gránulos dentro de un almacén ImageMosaic de GeoServer (Banda 3 a Banda 6)
     * y bordes nodata transparentes.
     */
    public ProcessResult convertToCog3395(
            Path workDir,
            String inputFilename,
            String outputCogFilename,
            String format,
            CartographyValidationResult validation
    ) {
        String lowerFormat = format != null ? format.toLowerCase().trim() : "";
        int bandCount = validation != null ? validation.getBandCount() : 0;
        boolean hasColorTable = validation != null && validation.isHasColorTable();
        boolean hasAlphaBand = validation != null && validation.isHasAlphaBand();

        String currentSource = inputFilename;
        String tempExpanded = null;
        String tempWarped = "temp_stage_" + System.currentTimeMillis() + ".tif";

        try {
            // Paso 0 (si aplica): Si es un TIFF indexado con paleta de colores (1 banda ColorTable),
            // expandir primero a 4 bandas RGBA para evitar rechazo en gdalwarp bilinear y en GeoServer ImageMosaic.
            if (!"pdf".equals(lowerFormat) && (hasColorTable || bandCount == 1 || bandCount == 2)) {
                tempExpanded = "temp_rgba_" + System.currentTimeMillis() + ".tif";
                List<String> expandCmd = List.of(
                        "gdal_translate",
                        "-expand", "rgba",
                        "-of", "GTiff",
                        "-co", "TILED=YES",
                        "-co", "COMPRESS=LZW",
                        currentSource,
                        tempExpanded
                );
                ProcessResult expandRes = executeGdalCommand(workDir, expandCmd);
                if (expandRes.isSuccess()) {
                    currentSource = tempExpanded;
                    bandCount = 4;
                    hasAlphaBand = true;
                } else {
                    log.warn("gdal_translate -expand rgba no aplicable para {}, continuando con gdalwarp directo", inputFilename);
                }
            }

            // Paso 1: Reproyectar / normalizar a EPSG:3395 asegurando 4 bandas RGBA con transparencia en bordes
            List<String> step1 = new ArrayList<>();
            step1.add("gdalwarp");
            if ("pdf".equals(lowerFormat)) {
                step1.add("--config");
                step1.add("GDAL_PDF_DPI");
                step1.add(String.valueOf(properties.getPdfDpi()));
            }
            step1.add("-t_srs");
            step1.add("EPSG:3395");
            if (!hasAlphaBand && bandCount < 4) {
                step1.add("-dstalpha");
            } else if (bandCount == 4 && !hasAlphaBand) {
                step1.add("-srcalpha");
            }
            step1.add("-r");
            step1.add("bilinear");
            step1.add("-of");
            step1.add("GTiff");
            step1.add("-co");
            step1.add("TILED=YES");
            step1.add("-co");
            step1.add("COMPRESS=LZW");
            step1.add(currentSource);
            step1.add(tempWarped);

            ProcessResult res1 = executeGdalCommand(workDir, step1);
            if (!res1.isSuccess()) {
                return res1;
            }

            // Paso 2: Generar Cloud Optimized GeoTIFF (COG) final con pirámides internas y compresión DEFLATE
            List<String> step2 = List.of(
                    "gdal_translate",
                    "-of", "COG",
                    "-co", "COMPRESS=DEFLATE",
                    tempWarped,
                    outputCogFilename
            );
            return executeGdalCommand(workDir, step2);

        } finally {
            try {
                if (tempExpanded != null) {
                    Files.deleteIfExists(workDir.resolve(tempExpanded));
                }
            } catch (Exception ignored) {}
            try {
                Files.deleteIfExists(workDir.resolve(tempWarped));
            } catch (Exception ignored) {}
        }
    }
}
