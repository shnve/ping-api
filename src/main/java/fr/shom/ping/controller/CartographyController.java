package fr.shom.ping.controller;

import fr.shom.ping.service.CartographyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cartography")
@RequiredArgsConstructor
@Tag(name = "Cartography", description = "Metadatos livianos de cobertura cartográfica para encuadre óptimo")
public class CartographyController {

    private final CartographyService cartographyService;

    @GetMapping(value = "/band-extents", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Devuelve la lista en memoria [banda, minLon, minLat, maxLon, maxLat] de las cartas náuticas publicadas")
    public ResponseEntity<List<Number[]>> getBandExtents() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(cartographyService.getBandExtents());
    }
}
