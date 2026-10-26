package fr.shom.ping.controller;

import fr.shom.ping.model.dto.CartographyLayerStatusDto;
import fr.shom.ping.model.dto.CartographyReloadRequest;
import fr.shom.ping.model.dto.CartographyUploadResponse;
import fr.shom.ping.service.CartographyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin-api/cartography")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cartography Automation (Admin)", description = "Automatización y conversión de cartografía a COG EPSG:3395")
@PreAuthorize("isAuthenticated()")
public class CartographyAdminController {

    private final CartographyService cartographyService;

    @PostMapping(value = "/upload-and-convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sube y convierte una carta náutica (GeoPDF o TIFF) a Cloud Optimized GeoTIFF (COG) en EPSG:3395")
    public ResponseEntity<CartographyUploadResponse> uploadAndConvert(
            @RequestParam("file") MultipartFile file,
            @RequestParam("band") String band,
            @RequestParam(value = "reloadGeoserver", defaultValue = "true") boolean reloadGeoserver) {

        log.info("Recibida solicitud de conversión de cartografía: archivo={}, banda={}, reloadGeoserver={}",
                file != null ? file.getOriginalFilename() : "null", band, reloadGeoserver);

        CartographyUploadResponse response = cartographyService.uploadAndConvert(file, band, reloadGeoserver);

        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/reload-layer", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Solicita la sincronización y recarga de la capa raster de una banda en GeoServer")
    public ResponseEntity<Map<String, Object>> reloadLayer(@RequestBody CartographyReloadRequest request) {
        String band = request != null ? request.getBand() : null;
        log.info("Solicitud manual de recarga de capa GeoServer para banda: {}", band);

        boolean success = cartographyService.reloadBandLayer(band);
        String msg = success
                ? "Capa para " + band + " sincronizada exitosamente con GeoServer."
                : "No se pudo sincronizar automáticamente la capa de " + band + " en GeoServer (verifique el log).";

        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", msg
        ));
    }

    @GetMapping(value = "/layers-status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consulta el estado actual, conteo de archivos y última actualización de cada banda raster")
    public ResponseEntity<Map<String, CartographyLayerStatusDto>> getLayersStatus() {
        Map<String, CartographyLayerStatusDto> status = cartographyService.getLayersStatus();
        return ResponseEntity.ok(status);
    }
}
