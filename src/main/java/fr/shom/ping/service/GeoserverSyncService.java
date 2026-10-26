package fr.shom.ping.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.shom.ping.properties.CartographyProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoserverSyncService {

    private final CartographyProperties properties;
    private final ObjectMapper objectMapper;

    private HttpClient getHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private String getAuthHeader() {
        String auth = properties.getGeoserverUser() + ":" + properties.getGeoserverPassword();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Resetea todos los almacenes y caches en memoria de GeoServer (gránulos raster, esquemas).
     */
    public boolean resetCache() {
        try {
            String url = properties.getGeoserverUrl().replaceAll("/+$", "") + "/rest/reset";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.info("GeoServer reset cache response status: {}", response.statusCode());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            log.warn("No se pudo resetear el cache de GeoServer: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Recarga el catálogo global de GeoServer (relee cambios en disco y renueva configuraciones).
     */
    public boolean reloadCatalog() {
        try {
            String url = properties.getGeoserverUrl().replaceAll("/+$", "") + "/rest/reload";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.info("GeoServer catalog reload response status: {}", response.statusCode());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            log.warn("No se pudo recargar el catálogo de GeoServer: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sincroniza la capa náutica en GeoServer.
     * Para Banda 1 y Banda 2: Es un almacén de tipo GeoTIFF (carta única).
     * Para Banda 3 a Banda 6: Es un almacén de tipo ImageMosaic (mosaico de cartas).
     */
    public boolean syncBandStore(String band, String outputCogFilename) {
        String cleanBand = band != null ? band.toLowerCase().trim() : "";
        boolean isSingleChart = "banda1".equals(cleanBand) || "banda2".equals(cleanBand);

        if (isSingleChart) {
            return syncSingleGeoTiffStore(cleanBand, outputCogFilename);
        } else {
            return syncMosaicStore(cleanBand);
        }
    }

    /**
     * Sincronización para Banda 1 y Banda 2 (GeoTIFF de carta única).
     */
    private boolean syncSingleGeoTiffStore(String band, String outputCogFilename) {
        try {
            String baseUrl = properties.getGeoserverUrl().replaceAll("/+$", "");
            List<String> workspaces = resolveTargetWorkspaces(baseUrl);

            if (outputCogFilename == null || outputCogFilename.trim().isEmpty()) {
                resetCache();
                boolean reloaded = reloadCatalog();
                for (String ws : workspaces) {
                    truncateGwcForBandAndGroup(baseUrl, ws, band);
                }
                return reloaded;
            }

            String fileUrl = "file:///mnt/geotiff/" + band + "/" + outputCogFilename;
            boolean anySuccess = false;

            for (String workspace : workspaces) {
                boolean storeExists = checkCoverageStoreExists(baseUrl, workspace, band);
                if (storeExists) {
                    log.info("Actualizando almacén GeoTIFF {}:{} para apuntar a: {}", workspace, band, fileUrl);
                    boolean updated = updateGeoTiffStoreUrl(baseUrl, workspace, band, fileUrl);
                    recalculateStoreCoveragesBBox(baseUrl, workspace, band);
                    truncateGwcForBandAndGroup(baseUrl, workspace, band);
                    anySuccess |= updated;
                } else if (workspace.equals(properties.getGeoserverWorkspace()) || "pisac_shn".equals(workspace)) {
                    if (checkWorkspaceExists(baseUrl, workspace)) {
                        log.info("Creando nuevo almacén GeoTIFF {}:{} apuntando a: {}", workspace, band, fileUrl);
                        boolean created = createGeoTiffStore(baseUrl, workspace, band, fileUrl);
                        if (created) {
                            publishCoverage(baseUrl, workspace, band, outputCogFilename);
                            recalculateStoreCoveragesBBox(baseUrl, workspace, band);
                            truncateGwcForBandAndGroup(baseUrl, workspace, band);
                        }
                        anySuccess |= created;
                    }
                }
            }

            resetCache();
            reloadCatalog();
            return anySuccess;
        } catch (Exception e) {
            log.error("Error sincronizando almacén GeoTIFF (carta única) para {}: {}", band, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Sincronización para Banda 3 a Banda 6 (ImageMosaic de múltiples cartas).
     */
    private boolean syncMosaicStore(String band) {
        try {
            String baseUrl = properties.getGeoserverUrl().replaceAll("/+$", "");
            List<String> workspaces = resolveTargetWorkspaces(baseUrl);

            // Resetear primero para que GeoTools suelte cualquier bloqueo sobre archivos .shp / sample_image.dat previos
            resetCache();

            boolean success = false;
            for (String workspace : workspaces) {
                if (!checkWorkspaceExists(baseUrl, workspace)) {
                    continue;
                }

                List<String> matchingStores = findMatchingStores(baseUrl, workspace, band);
                if (matchingStores.isEmpty()
                        && (workspace.equals(properties.getGeoserverWorkspace()) || "pisac_shn".equals(workspace))) {
                    log.info("No se encontró almacén ImageMosaic existente para {}:{}. Intentando crearlo...", workspace, band);
                    boolean created = createMosaicStore(baseUrl, workspace, band);
                    if (created) {
                        matchingStores.add(band);
                    }
                }

                for (String storeName : matchingStores) {
                    // Refrescar configuración del almacén ImageMosaic para regenerar el índice si fue limpiado
                    updateMosaicStoreUrl(baseUrl, workspace, storeName, band);

                    boolean hasCoverage = hasAnyCoverage(baseUrl, workspace, storeName);
                    if (!hasCoverage) {
                        log.info("Almacén ImageMosaic {}:{} no tiene cobertura configurada. Publicando cobertura...", workspace, storeName);
                        boolean published = publishMosaicCoverage(baseUrl, workspace, storeName, band);
                        harvestMosaicStore(baseUrl, workspace, storeName, band);
                        recalculateStoreCoveragesBBox(baseUrl, workspace, storeName);
                        success |= published;
                    } else {
                        log.info("Cosechando (harvesting) gránulos y recalculando BBox en ImageMosaic {}:{}...", workspace, storeName);
                        boolean harvested = harvestMosaicStore(baseUrl, workspace, storeName, band);
                        recalculateStoreCoveragesBBox(baseUrl, workspace, storeName);
                        success |= harvested;
                    }

                    truncateGwcForBandAndGroup(baseUrl, workspace, storeName);
                }
            }

            resetCache();
            reloadCatalog();
            return success;
        } catch (Exception e) {
            log.error("Error sincronizando almacén ImageMosaic para {}: {}", band, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Obtiene la lista de workspaces candidatos en GeoServer (priorizando pisac_shn y el configurado).
     */
    private List<String> resolveTargetWorkspaces(String baseUrl) {
        List<String> workspaces = new ArrayList<>();
        String configured = properties.getGeoserverWorkspace();
        if (configured != null && !configured.trim().isEmpty()) {
            workspaces.add(configured.trim());
        }
        if (!workspaces.contains("pisac_shn")) {
            workspaces.add("pisac_shn");
        }
        if (!workspaces.contains("shn_s124")) {
            workspaces.add("shn_s124");
        }

        try {
            String url = baseUrl + "/rest/workspaces.json";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader())
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode wsArray = root.path("workspaces").path("workspace");
                if (wsArray.isArray()) {
                    for (JsonNode ws : wsArray) {
                        String name = ws.path("name").asText("");
                        if (!name.isEmpty() && !workspaces.contains(name)) {
                            workspaces.add(name);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return workspaces;
    }

    private boolean checkWorkspaceExists(String baseUrl, String workspace) {
        try {
            String url = String.format("%s/rest/workspaces/%s.json", baseUrl, workspace);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader())
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkCoverageStoreExists(String baseUrl, String workspace, String storeName) {
        try {
            String url = String.format("%s/rest/workspaces/%s/coveragestores/%s.json", baseUrl, workspace, storeName);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader())
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean updateGeoTiffStoreUrl(String baseUrl, String workspace, String storeName, String fileUrl) {
        try {
            String url = String.format("%s/rest/workspaces/%s/coveragestores/%s", baseUrl, workspace, storeName);
            String xml = String.format(
                    "<coverageStore><name>%s</name><workspace>%s</workspace><enabled>true</enabled><type>GeoTIFF</type><url>%s</url></coverageStore>",
                    storeName, workspace, fileUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader())
                    .header("Content-Type", "application/xml")
                    .PUT(HttpRequest.BodyPublishers.ofString(xml))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Actualización de almacén GeoTIFF {}:{} retornó status: {}", workspace, storeName, response.statusCode());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            log.warn("Error actualizando almacén GeoTIFF {}:{}: {}", workspace, storeName, e.getMessage());
            return false;
        }
    }

    private boolean updateMosaicStoreUrl(String baseUrl, String workspace, String storeName, String band) {
        try {
            String url = String.format("%s/rest/workspaces/%s/coveragestores/%s", baseUrl, workspace, storeName);
            String xml = String.format(
                    "<coverageStore><name>%s</name><workspace>%s</workspace><enabled>true</enabled><type>ImageMosaic</type><url>file:///mnt/geotiff/%s</url></coverageStore>",
                    storeName, workspace, band);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader())
                    .header("Content-Type", "application/xml")
                    .PUT(HttpRequest.BodyPublishers.ofString(xml))
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Actualización de almacén ImageMosaic {}:{} retornó status: {}", workspace, storeName, response.statusCode());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            log.warn("Error actualizando almacén ImageMosaic {}:{}: {}", workspace, storeName, e.getMessage());
            return false;
        }
    }

    private boolean createGeoTiffStore(String baseUrl, String workspace, String storeName, String fileUrl) {
        try {
            String url = String.format("%s/rest/workspaces/%s/coveragestores?configure=all", baseUrl, workspace);
            String xml = String.format(
                    "<coverageStore><name>%s</name><workspace>%s</workspace><enabled>true</enabled><type>GeoTIFF</type><url>%s</url></coverageStore>",
                    storeName, workspace, fileUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader())
                    .header("Content-Type", "application/xml")
                    .POST(HttpRequest.BodyPublishers.ofString(xml))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Creación de almacén GeoTIFF {}:{} retornó status: {}", workspace, storeName, response.statusCode());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            log.warn("Error creando almacén GeoTIFF {}:{}: {}", workspace, storeName, e.getMessage());
            return false;
        }
    }

    private void publishCoverage(String baseUrl, String workspace, String storeName, String outputCogFilename) {
        try {
            String nativeName = outputCogFilename != null && outputCogFilename.contains(".")
                    ? outputCogFilename.substring(0, outputCogFilename.lastIndexOf('.'))
                    : storeName;
            String url = String.format("%s/rest/workspaces/%s/coveragestores/%s/coverages", baseUrl, workspace, storeName);
            String xml = String.format("<coverage><name>%s</name><title>%s</title><nativeName>%s</nativeName></coverage>",
                    storeName, storeName, nativeName);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader())
                    .header("Content-Type", "application/xml")
                    .POST(HttpRequest.BodyPublishers.ofString(xml))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Publicación de cobertura para {}:{} (nativeName: {}) retornó status: {}", workspace, storeName, nativeName, response.statusCode());
        } catch (Exception e) {
            log.warn("Error publicando cobertura para {}:{}: {}", workspace, storeName, e.getMessage());
        }
    }

    private List<String> findMatchingStores(String baseUrl, String workspace, String band) {
        List<String> list = new ArrayList<>();
        try {
            String url = String.format("%s/rest/workspaces/%s/coveragestores.json", baseUrl, workspace);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader())
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode stores = root.path("coverageStores").path("coverageStore");
                if (stores.isArray()) {
                    for (JsonNode store : stores) {
                        String name = store.path("name").asText("");
                        if (isStoreForBand(name, band)) {
                            list.add(name);
                        }
                    }
                } else if (stores.isObject()) {
                    String name = stores.path("name").asText("");
                    if (isStoreForBand(name, band)) {
                        list.add(name);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error consultando almacenes de GeoServer en {} para {}: {}", workspace, band, e.getMessage());
        }
        return list;
    }

    private boolean isStoreForBand(String storeName, String band) {
        String s = storeName.toLowerCase().replace("-", "").replace("_", "");
        String b = band.toLowerCase().replace("-", "").replace("_", "");
        return s.contains(b);
    }

    private boolean harvestMosaicStore(String baseUrl, String workspace, String storeName, String band) {
        try {
            String url = String.format("%s/rest/workspaces/%s/coveragestores/%s/external.imagemosaic",
                    baseUrl, workspace, storeName);
            String folderUrl = "file:///mnt/geotiff/" + band;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader())
                    .header("Content-Type", "text/plain")
                    .POST(HttpRequest.BodyPublishers.ofString(folderUrl))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.info("GeoServer harvest store {}:{} con {} retornó status: {}", workspace, storeName, folderUrl, response.statusCode());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            log.warn("Error haciendo harvest en almacén {}:{} de GeoServer: {}", workspace, storeName, e.getMessage());
            return false;
        }
    }

    private boolean createMosaicStore(String baseUrl, String workspace, String band) {
        try {
            String url = String.format("%s/rest/workspaces/%s/coveragestores?configure=all", baseUrl, workspace);
            String xml = String.format(
                    "<coverageStore><name>%s</name><workspace>%s</workspace><enabled>true</enabled><type>ImageMosaic</type><url>file:///mnt/geotiff/%s</url></coverageStore>",
                    band, workspace, band);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader())
                    .header("Content-Type", "application/xml")
                    .POST(HttpRequest.BodyPublishers.ofString(xml))
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Creación de almacén ImageMosaic {}:{} retornó status: {}", workspace, band, response.statusCode());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            log.warn("Error creando almacén ImageMosaic {}:{} en GeoServer: {}", workspace, band, e.getMessage());
            return false;
        }
    }

    private boolean hasAnyCoverage(String baseUrl, String workspace, String storeName) {
        return !listCoverageNames(baseUrl, workspace, storeName).isEmpty();
    }

    private List<String> listCoverageNames(String baseUrl, String workspace, String storeName) {
        List<String> names = new ArrayList<>();
        try {
            String url = String.format("%s/rest/workspaces/%s/coveragestores/%s/coverages.json", baseUrl, workspace, storeName);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader())
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode coverages = root.path("coverages").path("coverage");
                if (coverages.isArray()) {
                    for (JsonNode cov : coverages) {
                        String n = cov.path("name").asText("");
                        if (!n.isEmpty()) {
                            names.add(n);
                        }
                    }
                } else if (coverages.isObject()) {
                    String n = coverages.path("name").asText("");
                    if (!n.isEmpty()) {
                        names.add(n);
                    }
                }
            }
        } catch (Exception ignored) {}
        return names;
    }

    /**
     * Recalcula explícitamente el nativeBoundingBox y latLonBoundingBox de todas las coberturas del almacén
     * para que las nuevas cartas agregadas fuera del BBox original sean visibles inmediatamente en GeoServer.
     */
    private void recalculateStoreCoveragesBBox(String baseUrl, String workspace, String storeName) {
        List<String> coverages = listCoverageNames(baseUrl, workspace, storeName);
        if (coverages.isEmpty()) {
            coverages.add(storeName);
        }
        for (String covName : coverages) {
            try {
                String url = String.format(
                        "%s/rest/workspaces/%s/coveragestores/%s/coverages/%s.xml?recalculate=nativebbox,latlonbbox",
                        baseUrl, workspace, storeName, covName
                );
                String xml = String.format("<coverage><name>%s</name><enabled>true</enabled></coverage>", covName);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", getAuthHeader())
                        .header("Content-Type", "application/xml")
                        .PUT(HttpRequest.BodyPublishers.ofString(xml))
                        .timeout(Duration.ofSeconds(20))
                        .build();
                HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                log.info("Recálculo de BoundingBox para cobertura {}:{} retornó status: {}", workspace, covName, response.statusCode());
            } catch (Exception e) {
                log.warn("No se pudo recalcular el BoundingBox de {}:{}: {}", workspace, covName, e.getMessage());
            }
        }
    }

    /**
     * Vacía la caché de teselas WMTS en GeoWebCache (masstruncate) tanto para la capa individual de la banda
     * como para los grupos de capas cartográficos (pisac_shn:cartografia_ve, cartografia_ve, etc.).
     */
    private void truncateGwcForBandAndGroup(String baseUrl, String workspace, String storeOrLayerName) {
        String configuredGroup = properties.getGeoserverLayerGroup() != null
                ? properties.getGeoserverLayerGroup().trim()
                : "cartografia_ve";

        List<String> gwcLayers = List.of(
                workspace + ":" + storeOrLayerName,
                workspace + ":" + configuredGroup,
                configuredGroup,
                workspace + ":cartografia_ve",
                "pisac_shn:cartografia_ve",
                workspace + ":cartas_nauticas_shn"
        );

        for (String layerName : gwcLayers) {
            truncateGwcLayer(baseUrl, layerName);
        }
    }

    private void truncateGwcLayer(String baseUrl, String qualifiedLayerName) {
        try {
            String url = baseUrl + "/gwc/rest/masstruncate";
            String xml = String.format("<truncateLayer><layerName>%s</layerName></truncateLayer>", qualifiedLayerName);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader())
                    .header("Content-Type", "text/xml")
                    .POST(HttpRequest.BodyPublishers.ofString(xml))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("GeoWebCache masstruncate ejecutado para capa WMTS: {}", qualifiedLayerName);
            }
        } catch (Exception ignored) {}
    }

    private boolean publishMosaicCoverage(String baseUrl, String workspace, String storeName, String band) {
        try {
            String coverageName = band;
            String url = String.format("%s/rest/workspaces/%s/coveragestores/%s/coverages", baseUrl, workspace, storeName);
            String xml = String.format("<coverage><name>%s</name><title>%s</title><nativeName>%s</nativeName><srs>EPSG:3395</srs><enabled>true</enabled></coverage>",
                    coverageName, coverageName, coverageName);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader())
                    .header("Content-Type", "application/xml")
                    .POST(HttpRequest.BodyPublishers.ofString(xml))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Publicación de cobertura ImageMosaic {} en almacén {}:{} retornó status: {}", coverageName, workspace, storeName, response.statusCode());
            boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
            if (ok) {
                assignBandDefaultStyle(baseUrl, workspace, coverageName);
            }
            return ok;
        } catch (Exception e) {
            log.warn("Error publicando cobertura ImageMosaic {} en {}:{}: {}", band, workspace, storeName, e.getMessage());
            return false;
        }
    }

    private void assignBandDefaultStyle(String baseUrl, String workspace, String band) {
        try {
            String styleName = "estilo_" + band;
            String url = String.format("%s/rest/workspaces/%s/layers/%s", baseUrl, workspace, band);
            String xml = String.format(
                    "<layer><defaultStyle><name>%s</name><workspace>%s</workspace></defaultStyle><enabled>true</enabled></layer>",
                    styleName, workspace);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader())
                    .header("Content-Type", "application/xml")
                    .PUT(HttpRequest.BodyPublishers.ofString(xml))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ignored) {}
    }
}
