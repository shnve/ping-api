package fr.shom.ping.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.shom.ping.properties.CartographyProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Publicación de las bandas cartográficas en GeoServer.
 *
 * <p>Procedimiento por banda (equivalente al manual):
 * <ol>
 *   <li>Almacén con el nombre de la banda (GeoTIFF para banda1/banda2, ImageMosaic para banda3..banda6).</li>
 *   <li>Cobertura/capa publicada con el estilo de escala {@code estilo_bandaN} (se crea si no existe).</li>
 *   <li>Capa agregada al grupo {@code cartografia_ve} en orden de banda (banda2 abajo ... banda6 arriba).</li>
 *   <li>Verificación con GetMap de la capa y del grupo; si el grupo deja de funcionar se revierte.</li>
 * </ol>
 *
 * <p>No se usa {@code /rest/reload}: recarga todo el catálogo y descarta cualquier elemento con errores
 * (fue lo que tumbó el grupo en desarrollo el 28/10). Sólo se vacía la caché de teselas de la banda y del grupo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeoserverSyncService {

    private static final Pattern BAND_PATTERN = Pattern.compile("banda(\\d+)$");
    private static final String MOUNT_BASE = "file:///mnt/geotiff/";

    private final CartographyProperties properties;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Resultado de una publicación en GeoServer, con el motivo legible en caso de fallo. */
    @Getter
    public static class PublishResult {
        private boolean success = true;
        private String message;
        private final List<String> steps = new ArrayList<>();

        void step(String text) {
            steps.add(text);
            log.info("[GeoServer] {}", text);
        }

        PublishResult fail(String reason) {
            this.success = false;
            this.message = reason;
            log.warn("[GeoServer] Publicación fallida: {}", reason);
            return this;
        }

        PublishResult ok(String text) {
            this.success = true;
            this.message = text;
            return this;
        }

        /** Resultado fallido sin llegar a GeoServer (validaciones previas). */
        public PublishResult failWith(String reason) {
            return fail(reason);
        }
    }

    private record Resp(int status, String body) {
        boolean ok() {
            return status >= 200 && status < 300;
        }

        String shortBody() {
            if (body == null) {
                return "";
            }
            String b = body.replaceAll("\\s+", " ").trim();
            return b.length() > 300 ? b.substring(0, 300) + "..." : b;
        }
    }

    private record Bounds(double minx, double miny, double maxx, double maxy, String crs) {
    }

    // =====================================================================================
    // API pública
    // =====================================================================================

    /**
     * Banda 1 / Banda 2 (GeoTIFF de carta única): apunta el almacén al nuevo archivo, o lo crea.
     * Si la verificación falla, restaura la URL anterior del almacén.
     */
    public synchronized PublishResult publishSingleChart(String band, String cogFileName) {
        PublishResult result = new PublishResult();
        String ws = workspace();
        String fileUrl = MOUNT_BASE + band + "/" + cogFileName;
        List<String[]> groupBefore = readGroupEntries(ws);

        String previousUrl = null;
        String previousCoverageName = null;
        String newCoverageName = cogFileName.contains(".") ? cogFileName.substring(0, cogFileName.lastIndexOf('.')) : cogFileName;
        JsonNode store = getJson(storePath(ws, band));
        if (store != null) {
            previousUrl = store.path("coverageStore").path("url").asText(null);
            Resp r = call("PUT", storePath(ws, band), storeXml(band, ws, "GeoTIFF", fileUrl), "application/xml");
            if (!r.ok()) {
                return result.fail("No se pudo actualizar el almacén " + band + " (" + r.status() + "): " + r.shortBody());
            }
            result.step("Almacén " + band + " apuntado a " + fileUrl);
            // El lector GeoTIFF exige que nativeCoverageName coincida con el nombre del archivo nuevo
            // (si no: "The specified coverageName VE002 is not supported")
            previousCoverageName = setNativeCoverageName(ws, band, newCoverageName, result);
        } else {
            Resp r = call("POST", "/rest/workspaces/" + ws + "/coveragestores", storeXml(band, ws, "GeoTIFF", fileUrl), "application/xml");
            if (!r.ok()) {
                return result.fail("No se pudo crear el almacén " + band + " (" + r.status() + "): " + r.shortBody());
            }
            String nativeName = newCoverageName;
            r = call("POST", storePath(ws, band) + "/coverages",
                    "<coverage><name>" + band + "</name><title>" + band + "</title><nativeName>" + nativeName
                            + "</nativeName><nativeCoverageName>" + nativeName + "</nativeCoverageName><enabled>true</enabled></coverage>",
                    "application/xml");
            if (!r.ok()) {
                call("DELETE", storePath(ws, band) + "?recurse=true", null, null);
                return result.fail("No se pudo publicar la capa " + band + " (" + r.status() + "): " + r.shortBody());
            }
            result.step("Almacén y capa " + band + " creados");
        }

        recalculateBBox(ws, band, result);
        finishPublication(ws, band, result);

        if (!verifyLayer(ws, band, result) || !verifyGroupOrRollback(ws, band, groupBefore, result)) {
            if (previousUrl != null) {
                call("PUT", storePath(ws, band), storeXml(band, ws, "GeoTIFF", previousUrl), "application/xml");
                if (previousCoverageName != null) {
                    setNativeCoverageName(ws, band, previousCoverageName, result);
                }
                result.step("Restaurada la URL anterior del almacén: " + previousUrl);
            }
            truncateGwc(ws, band);
            return result;
        }
        return result.ok("Publicada en GeoServer (" + ws + ":" + band + ") y visible en el grupo " + group() + ".");
    }

    /**
     * Banda 3 a 6 (ImageMosaic): crea el mosaico la primera vez (con {@code Heterogeneous=true} desde el inicio)
     * o agrega la carta nueva al índice existente cosechando sólo ese archivo.
     */
    public synchronized PublishResult publishMosaicGranule(String band, String cogFileName) {
        PublishResult result = new PublishResult();
        String ws = workspace();
        List<String[]> groupBefore = readGroupEntries(ws);

        boolean storeExists = getJson(storePath(ws, band)) != null;
        if (!storeExists) {
            if (!createMosaic(ws, band, result)) {
                return result;
            }
        } else if (listCoverageNames(ws, band).isEmpty()) {
            if (!publishMosaicCoverage(ws, band, result)) {
                return result;
            }
        } else {
            Resp r = harvestFile(ws, band, cogFileName);
            if (!r.ok()) {
                return result.fail("GeoServer no pudo agregar " + cogFileName + " al mosaico " + band
                        + " (" + r.status() + "): " + r.shortBody());
            }
            result.step("Carta " + cogFileName + " agregada al mosaico " + band);
        }

        recalculateBBox(ws, band, result);
        finishPublication(ws, band, result);

        if (!verifyLayer(ws, band, result) || !verifyGroupOrRollback(ws, band, groupBefore, result)) {
            truncateGwc(ws, band);
            return result;
        }
        return result.ok("Publicada en GeoServer (" + ws + ":" + band + ") y visible en el grupo " + group() + ".");
    }

    /**
     * Resincroniza un mosaico completo: lo crea si no existe; si existe, cosecha los archivos que falten en el
     * índice y retira del índice los que ya no estén en disco. Luego asegura estilo, grupo y verifica.
     */
    public synchronized PublishResult resyncMosaic(String band, Collection<String> filesOnDisk) {
        PublishResult result = new PublishResult();
        String ws = workspace();
        List<String[]> groupBefore = readGroupEntries(ws);

        if (getJson(storePath(ws, band)) == null) {
            if (filesOnDisk.isEmpty()) {
                return result.fail("La banda " + band + " no tiene cartas en disco ni almacén en GeoServer.");
            }
            if (!createMosaic(ws, band, result)) {
                return result;
            }
        } else if (listCoverageNames(ws, band).isEmpty()) {
            if (!publishMosaicCoverage(ws, band, result)) {
                return result;
            }
        } else {
            Set<String> indexed = listGranuleLocations(ws, band);
            List<String> failed = new ArrayList<>();
            for (String f : filesOnDisk) {
                if (!indexed.contains(f)) {
                    Resp r = harvestFile(ws, band, f);
                    result.step("Cosecha de " + f + ": " + r.status());
                    if (!r.ok()) {
                        failed.add(f);
                    }
                }
            }
            for (String loc : indexed) {
                if (!filesOnDisk.contains(loc)) {
                    removeGranules(band, List.of(loc));
                    result.step("Retirada del índice la carta inexistente en disco: " + loc);
                }
            }
            if (!failed.isEmpty()) {
                truncateGwc(ws, band);
                return result.fail("GeoServer no pudo agregar al mosaico " + band + ": " + String.join(", ", failed));
            }
        }

        recalculateBBox(ws, band, result);
        finishPublication(ws, band, result);
        if (!verifyLayer(ws, band, result) || !verifyGroupOrRollback(ws, band, groupBefore, result)) {
            truncateGwc(ws, band);
            return result;
        }
        return result.ok("Banda " + band + " sincronizada y visible en el grupo " + group() + ".");
    }

    /**
     * Resincroniza una banda de carta única (GeoTIFF) apuntando al archivo indicado.
     */
    public PublishResult resyncSingleChart(String band, String fileName) {
        if (fileName == null) {
            return new PublishResult().fail("La banda " + band + " no tiene ninguna carta en disco.");
        }
        return publishSingleChart(band, fileName);
    }

    /**
     * Retira del índice del mosaico los elementos cuyos archivos se van a reemplazar o borrar,
     * sin tocar el resto del índice (reemplaza el borrado completo del índice en cada subida).
     */
    public void removeGranules(String band, Collection<String> fileNames) {
        String ws = workspace();
        if (fileNames == null || fileNames.isEmpty() || getJson(storePath(ws, band)) == null) {
            return;
        }
        List<String> coverages = listCoverageNames(ws, band);
        if (coverages.isEmpty()) {
            return;
        }
        for (String name : fileNames) {
            String filter = URLEncoder.encode("location = '" + name.replace("'", "''") + "'", StandardCharsets.UTF_8);
            Resp r = call("DELETE", storePath(ws, band) + "/coverages/" + coverages.get(0) + "/index/granules?filter=" + filter, null, null);
            log.info("[GeoServer] Retiro del elemento {} del mosaico {}: {}", name, band, r.status());
        }
    }

    // =====================================================================================
    // Pasos de publicación
    // =====================================================================================

    /** Crea el almacén ImageMosaic y su cobertura. El directorio ya debe contener indexer.properties. */
    private boolean createMosaic(String ws, String band, PublishResult result) {
        Resp r = call("POST", "/rest/workspaces/" + ws + "/coveragestores",
                storeXml(band, ws, "ImageMosaic", MOUNT_BASE + band), "application/xml");
        if (!r.ok()) {
            result.fail("No se pudo crear el almacén " + band + " (" + r.status() + "): " + r.shortBody());
            return false;
        }
        result.step("Almacén ImageMosaic " + band + " creado");
        return publishMosaicCoverage(ws, band, result);
    }

    private boolean publishMosaicCoverage(String ws, String band, PublishResult result) {
        Resp r = call("POST", storePath(ws, band) + "/coverages",
                "<coverage><name>" + band + "</name><title>" + bandTitle(band) + "</title><nativeName>" + band
                        + "</nativeName><srs>EPSG:3395</srs><projectionPolicy>REPROJECT_TO_DECLARED</projectionPolicy>"
                        + "<enabled>true</enabled></coverage>",
                "application/xml");
        if (!r.ok()) {
            // No dejar un almacén vacío que confunda a la siguiente subida
            call("DELETE", storePath(ws, band) + "?recurse=true", null, null);
            String reason = "GeoServer no pudo crear el mosaico " + band + " (" + r.status() + "): " + r.shortBody();
            if (r.status() == 500) {
                reason += " Verifique que GeoServer pueda escribir el índice en la carpeta de la banda.";
            }
            result.fail(reason);
            return false;
        }
        result.step("Capa " + ws + ":" + band + " publicada");
        return true;
    }

    /** Estilo, estilo por defecto, grupo y caché de teselas. */
    private void finishPublication(String ws, String band, PublishResult result) {
        String style = ensureStyle(ws, band, result);
        if (style != null) {
            Resp r = call("PUT", "/rest/layers/" + ws + ":" + band,
                    "<layer><defaultStyle><name>" + style + "</name><workspace>" + ws + "</workspace></defaultStyle>"
                            + "<enabled>true</enabled></layer>",
                    "application/xml");
            result.step("Estilo por defecto " + style + " en " + band + ": " + r.status());
        }
        ensureInGroup(ws, band, style, result);
        truncateGwc(ws, band);
    }

    /**
     * Devuelve el nombre del estilo de la banda, creándolo si no existe con la escala configurada.
     * Nunca asigna un estilo inexistente (un layer.xml con estilo inexistente se descarta al recargar).
     */
    private String ensureStyle(String ws, String band, PublishResult result) {
        String style = "estilo_" + band;
        if (getJson("/rest/workspaces/" + ws + "/styles/" + style + ".json") != null) {
            return style;
        }
        Integer maxScale = properties.getBandMaxScale() != null ? properties.getBandMaxScale().get(band) : null;
        String scaleRule = maxScale != null && maxScale > 0
                ? "<!-- Visible desde 1:" + maxScale + " hacia adentro; no se apaga al acercarse -->"
                + "<MaxScaleDenominator>" + maxScale + "</MaxScaleDenominator>"
                : "<!-- Sin límite de escala: siempre visible -->";
        String sld = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<StyledLayerDescriptor version=\"1.0.0\" xmlns=\"http://www.opengis.net/sld\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<NamedLayer><Name>" + style + "</Name><UserStyle><Title>" + bandTitle(band) + "</Title>"
                + "<FeatureTypeStyle><Rule>" + scaleRule
                + "<RasterSymbolizer><Opacity>1.0</Opacity></RasterSymbolizer>"
                + "</Rule></FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>";
        Resp r = call("POST", "/rest/workspaces/" + ws + "/styles?name=" + style, sld, "application/vnd.ogc.sld+xml");
        if (!r.ok()) {
            result.step("No se pudo crear el estilo " + style + " (" + r.status() + "); la capa queda con el estilo por defecto");
            return null;
        }
        result.step("Estilo " + style + " creado" + (maxScale != null ? " (visible desde 1:" + maxScale + ")" : ""));
        return style;
    }

    /** Agrega la capa al grupo en orden de banda (banda menor abajo). No hace nada si ya está. */
    private void ensureInGroup(String ws, String band, String style, PublishResult result) {
        List<String[]> entries = readGroupEntries(ws);
        if (entries == null) {
            result.step("El grupo " + group() + " no existe; la capa no se agregó a ningún grupo");
            return;
        }
        String qualified = ws + ":" + band;
        for (String[] e : entries) {
            if (qualified.equals(e[0]) || band.equals(e[0])) {
                return;
            }
        }
        int newOrder = bandNumber(band);
        int insertAt = entries.size();
        for (int i = 0; i < entries.size(); i++) {
            int n = bandNumber(entries.get(i)[0]);
            if (n > newOrder) {
                insertAt = i;
                break;
            }
        }
        List<String[]> updated = new ArrayList<>(entries);
        updated.add(insertAt, new String[]{qualified, style != null ? ws + ":" + style : ""});
        Resp r = writeGroupEntries(ws, updated);
        if (r.ok()) {
            result.step("Capa " + qualified + " agregada al grupo " + group() + " (posición " + (insertAt + 1) + ")");
        } else {
            result.step("No se pudo agregar " + qualified + " al grupo (" + r.status() + "): " + r.shortBody());
        }
    }

    private boolean verifyLayer(String ws, String band, PublishResult result) {
        Bounds b = coverageBounds(ws, band);
        if (b == null) {
            result.fail("La capa " + ws + ":" + band + " no quedó publicada en GeoServer.");
            return false;
        }
        // Estilo "raster" para no depender de los límites de escala del estilo de la banda
        Resp r = getMap(ws + ":" + band, "raster", b);
        if (r == null) {
            result.fail("GeoServer no puede dibujar la capa " + band + " (GetMap con error).");
            return false;
        }
        result.step("Verificación GetMap de " + band + ": correcta");
        return true;
    }

    private boolean verifyGroupOrRollback(String ws, String band, List<String[]> groupBefore, PublishResult result) {
        if (groupBefore == null) {
            return true;
        }
        Bounds gb = groupBounds(ws);
        if (gb != null && getMap(ws + ":" + group(), "", gb) != null) {
            result.step("Verificación GetMap del grupo " + group() + ": correcta");
            return true;
        }
        Resp r = writeGroupEntries(ws, groupBefore);
        truncateGwc(ws, group());
        result.fail("La capa " + band + " impedía dibujar el grupo " + group()
                + "; se restauró el grupo anterior para no afectar el mapa (" + r.status() + ").");
        return false;
    }

    // =====================================================================================
    // Grupo de capas
    // =====================================================================================

    /** Lee las capas del grupo como pares [capa, estilo]. Devuelve null si el grupo no existe. */
    private List<String[]> readGroupEntries(String ws) {
        JsonNode root = getJson("/rest/workspaces/" + ws + "/layergroups/" + group() + ".json");
        if (root == null) {
            return null;
        }
        JsonNode lg = root.path("layerGroup");
        List<String> layers = new ArrayList<>();
        for (JsonNode p : items(lg.path("publishables").path("published"))) {
            if (p.isObject()) {
                layers.add(p.path("name").asText(""));
            }
        }
        List<String> styles = new ArrayList<>();
        for (JsonNode s : items(lg.path("styles").path("style"))) {
            styles.add(s.isObject() ? s.path("name").asText("") : s.asText(""));
        }
        List<String[]> entries = new ArrayList<>();
        for (int i = 0; i < layers.size(); i++) {
            entries.add(new String[]{layers.get(i), i < styles.size() ? styles.get(i) : ""});
        }
        return entries;
    }

    private Resp writeGroupEntries(String ws, List<String[]> entries) {
        StringBuilder xml = new StringBuilder("<layerGroup><name>").append(group()).append("</name><publishables>");
        for (String[] e : entries) {
            xml.append("<published type=\"layer\"><name>").append(e[0]).append("</name></published>");
        }
        xml.append("</publishables><styles>");
        for (String[] e : entries) {
            xml.append(e[1] == null || e[1].isEmpty() ? "<style/>" : "<style><name>" + e[1] + "</name></style>");
        }
        xml.append("</styles></layerGroup>");
        return call("PUT", "/rest/workspaces/" + ws + "/layergroups/" + group(), xml.toString(), "application/xml");
    }

    // =====================================================================================
    // Utilidades REST
    // =====================================================================================

    private Resp harvestFile(String ws, String band, String fileName) {
        // external.imagemosaic espera la ruta del archivo dentro del contenedor, sin el prefijo file://
        // (con file:// responde 400 "Failed to locate the input file")
        return call("POST", storePath(ws, band) + "/external.imagemosaic", "/mnt/geotiff/" + band + "/" + fileName, "text/plain");
    }

    /**
     * Ajusta nativeCoverageName de la(s) cobertura(s) de un almacén GeoTIFF y devuelve el valor anterior.
     */
    private String setNativeCoverageName(String ws, String band, String coverageName, PublishResult result) {
        String previous = null;
        for (String cov : listCoverageNames(ws, band)) {
            JsonNode c = getJson(storePath(ws, band) + "/coverages/" + cov + ".json");
            if (c != null && previous == null) {
                JsonNode n = c.path("coverage");
                previous = n.path("nativeCoverageName").asText(n.path("nativeName").asText(null));
            }
            Resp r = call("PUT", storePath(ws, band) + "/coverages/" + cov,
                    "<coverage><nativeCoverageName>" + coverageName + "</nativeCoverageName></coverage>", "application/xml");
            result.step("Nombre nativo de " + cov + " → " + coverageName + ": " + r.status());
        }
        return previous;
    }

    private void recalculateBBox(String ws, String band, PublishResult result) {
        for (String cov : listCoverageNames(ws, band)) {
            Resp r = call("PUT", storePath(ws, band) + "/coverages/" + cov + ".xml?recalculate=nativebbox,latlonbbox",
                    "<coverage><name>" + cov + "</name><enabled>true</enabled></coverage>", "application/xml");
            result.step("Recálculo de extensión de " + cov + ": " + r.status());
        }
    }

    private List<String> listCoverageNames(String ws, String store) {
        List<String> names = new ArrayList<>();
        JsonNode root = getJson(storePath(ws, store) + "/coverages.json");
        if (root == null) {
            return names;
        }
        for (JsonNode c : items(root.path("coverages").path("coverage"))) {
            if (c.isObject() && !c.path("name").asText("").isEmpty()) {
                names.add(c.path("name").asText());
            }
        }
        return names;
    }

    private Set<String> listGranuleLocations(String ws, String band) {
        Set<String> locations = new LinkedHashSet<>();
        List<String> coverages = listCoverageNames(ws, band);
        if (coverages.isEmpty()) {
            return locations;
        }
        JsonNode root = getJson(storePath(ws, band) + "/coverages/" + coverages.get(0) + "/index/granules.json?limit=10000");
        if (root != null) {
            for (JsonNode f : root.path("features")) {
                String loc = f.path("properties").path("location").asText("");
                if (!loc.isEmpty()) {
                    locations.add(loc.replace("\\", "/").replaceAll("^.*/", ""));
                }
            }
        }
        return locations;
    }

    private Bounds coverageBounds(String ws, String band) {
        List<String> coverages = listCoverageNames(ws, band);
        if (coverages.isEmpty()) {
            return null;
        }
        JsonNode root = getJson(storePath(ws, band) + "/coverages/" + coverages.get(0) + ".json");
        if (root == null) {
            return null;
        }
        JsonNode bb = root.path("coverage").path("latLonBoundingBox");
        if (bb.isMissingNode()) {
            return null;
        }
        return new Bounds(bb.path("minx").asDouble(), bb.path("miny").asDouble(),
                bb.path("maxx").asDouble(), bb.path("maxy").asDouble(), "EPSG:4326");
    }

    private Bounds groupBounds(String ws) {
        JsonNode root = getJson("/rest/workspaces/" + ws + "/layergroups/" + group() + ".json");
        if (root == null) {
            return null;
        }
        JsonNode bb = root.path("layerGroup").path("bounds");
        if (bb.isMissingNode()) {
            return null;
        }
        JsonNode crsNode = bb.path("crs");
        String crs = crsNode.isObject() ? crsNode.path("$").asText("EPSG:3857") : crsNode.asText("EPSG:3857");
        return new Bounds(bb.path("minx").asDouble(), bb.path("miny").asDouble(),
                bb.path("maxx").asDouble(), bb.path("maxy").asDouble(), crs);
    }

    /** GetMap de verificación. Devuelve null si GeoServer responde con una excepción en vez de una imagen. */
    private Resp getMap(String layer, String style, Bounds b) {
        try {
            String url = String.format(Locale.ROOT,
                    "%s/%s/wms?service=WMS&version=1.1.1&request=GetMap&layers=%s&styles=%s&bbox=%f,%f,%f,%f"
                            + "&width=256&height=256&srs=%s&format=image/png&transparent=true",
                    baseUrl(), workspace(), URLEncoder.encode(layer, StandardCharsets.UTF_8), style,
                    b.minx(), b.miny(), b.maxx(), b.maxy(), b.crs());
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Authorization", authHeader())
                    .GET().timeout(Duration.ofSeconds(60)).build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String ctype = response.headers().firstValue("Content-Type").orElse("");
            if (response.statusCode() == 200 && ctype.startsWith("image/")) {
                return new Resp(200, null);
            }
            log.warn("[GeoServer] GetMap de {} falló: {} {} {}", layer, response.statusCode(), ctype,
                    new String(response.body(), StandardCharsets.UTF_8).replaceAll("\\s+", " "));
            return null;
        } catch (Exception e) {
            log.warn("[GeoServer] GetMap de {} falló: {}", layer, e.getMessage());
            return null;
        }
    }

    /** Vacía la caché de teselas (GeoWebCache) de la capa y del grupo. */
    private void truncateGwc(String ws, String layer) {
        for (String name : new LinkedHashSet<>(List.of(ws + ":" + layer, ws + ":" + group()))) {
            call("POST", "/gwc/rest/masstruncate",
                    "<truncateLayer><layerName>" + name + "</layerName></truncateLayer>", "text/xml");
        }
    }

    private JsonNode getJson(String path) {
        Resp r = call("GET", path, null, null);
        if (r.status() != 200 || r.body() == null || r.body().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(r.body());
        } catch (Exception e) {
            return null;
        }
    }

    private Resp call(String method, String path, String body, String contentType) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                    .header("Authorization", authHeader())
                    .timeout(Duration.ofSeconds(120));
            if (contentType != null) {
                b.header("Content-Type", contentType);
            }
            HttpRequest.BodyPublisher publisher = body != null
                    ? HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)
                    : HttpRequest.BodyPublishers.noBody();
            b.method(method, publisher);
            HttpResponse<String> response = httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString());
            if (!"GET".equals(method)) {
                log.debug("[GeoServer] {} {} -> {}", method, path, response.statusCode());
            }
            return new Resp(response.statusCode(), response.body());
        } catch (Exception e) {
            log.warn("[GeoServer] {} {} falló: {}", method, path, e.getMessage());
            return new Resp(0, "GeoServer no disponible: " + e.getMessage());
        }
    }

    /** GeoServer devuelve un objeto cuando hay un solo elemento y un arreglo cuando hay varios. */
    private static List<JsonNode> items(JsonNode node) {
        List<JsonNode> list = new ArrayList<>();
        if (node == null || node.isMissingNode() || node.isNull()) {
            return list;
        }
        if (node.isArray()) {
            node.forEach(list::add);
        } else {
            list.add(node);
        }
        return list;
    }

    private static String storeXml(String name, String ws, String type, String url) {
        return "<coverageStore><name>" + name + "</name><workspace>" + ws + "</workspace><enabled>true</enabled>"
                + "<type>" + type + "</type><url>" + url + "</url></coverageStore>";
    }

    private static String storePath(String ws, String store) {
        return "/rest/workspaces/" + ws + "/coveragestores/" + store;
    }

    private static int bandNumber(String layerName) {
        Matcher m = BAND_PATTERN.matcher(layerName == null ? "" : layerName.toLowerCase(Locale.ROOT));
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static String bandTitle(String band) {
        return switch (band) {
            case "banda1" -> "Banda 1 - Overview";
            case "banda2" -> "Banda 2 - General";
            case "banda3" -> "Banda 3 - Costera";
            case "banda4" -> "Banda 4 - Aproximacion";
            case "banda5" -> "Banda 5 - Puerto";
            case "banda6" -> "Banda 6 - Atraque";
            default -> band;
        };
    }

    private String baseUrl() {
        return properties.getGeoserverUrl().replaceAll("/+$", "");
    }

    private String workspace() {
        String ws = properties.getGeoserverWorkspace();
        return ws == null || ws.isBlank() ? "pisac_shn" : ws.trim();
    }

    private String group() {
        String g = properties.getGeoserverLayerGroup();
        return g == null || g.isBlank() ? "cartografia_ve" : g.trim();
    }

    private String authHeader() {
        String auth = properties.getGeoserverUser() + ":" + properties.getGeoserverPassword();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }
}
