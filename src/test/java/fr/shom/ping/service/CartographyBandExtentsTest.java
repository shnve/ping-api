package fr.shom.ping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.shom.ping.properties.CartographyProperties;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartographyBandExtentsTest {

    @Test
    void pdfPropertiesHaveExpectedDefaultThresholds() {
        CartographyProperties props = new CartographyProperties();
        assertThat(props.getGeoserverLayerGroup()).isEqualTo("cartografia_ve");
        assertThat(props.getGeoserverPdfLayerGroup()).isEqualTo("cartografia_ve_pdf");
        assertThat(props.getPdfBandMaxScale())
                .containsEntry("banda3", 1500000)
                .containsEntry("banda4", 350000)
                .containsEntry("banda5", 90000)
                .containsEntry("banda6", 25000);
        // Los umbrales del visor interactivo permanecen intactos
        assertThat(props.getBandMaxScale())
                .containsEntry("banda3", 350000)
                .containsEntry("banda4", 90000)
                .containsEntry("banda5", 25000)
                .containsEntry("banda6", 4000);
    }

    @Test
    void mercator3395ConversionMatchesWgs84Coordinates() {
        // La Guaira aprox. en EPSG:3395
        double[] lonLat = GeoserverSyncService.mercator3395ToLonLat(-7451000.0, 1180000.0);
        assertThat(lonLat[0]).isCloseTo(-66.93345, within(0.001));
        assertThat(lonLat[1]).isCloseTo(10.605, within(0.01));
    }

    @Test
    void bandExtentsAreCachedInMemoryAndInvalidatedOnUpdate() {
        CartographyProperties props = new CartographyProperties();
        GdalExecutorService gdal = new GdalExecutorService(props, new ObjectMapper());
        GeoserverSyncService sync = mock(GeoserverSyncService.class);
        when(sync.fetchPublishedBandExtents()).thenReturn(List.of(
                new Number[]{2, -76.04139, 5.80105, -54.89532, 20.72081},
                new Number[]{6, -68.01131, 10.48282, -67.96827, 10.51838},
                new Number[]{4, -68.16897, 10.41309, -67.72890, 10.75603}
        ));

        CartographyService service = new CartographyService(props, gdal, sync);
        List<Number[]> first = service.getBandExtents();
        List<Number[]> second = service.getBandExtents();

        // Orden descendente por banda (6 > 4 > 2) y cacheado en memoria sin repetir llamadas a GeoServer
        assertThat(first).hasSize(3);
        assertThat(first.get(0)[0].intValue()).isEqualTo(6);
        assertThat(first.get(1)[0].intValue()).isEqualTo(4);
        assertThat(first.get(2)[0].intValue()).isEqualTo(2);
        assertThat(second).isSameAs(first);
        verify(sync, times(1)).fetchPublishedBandExtents();

        // Al invalidar caché (p. ej. tras subir o recargar una carta), vuelve a consultar una sola vez
        service.invalidateBandExtentsCache();
        service.getBandExtents();
        verify(sync, times(2)).fetchPublishedBandExtents();
    }

    @Test
    void diskHeaderFallbackReadsLocalCogTiffsWhenGeoserverUnavailable() {
        if (!Files.isDirectory(Paths.get("db/geotiff/banda2")) && !Files.isDirectory(Paths.get("ping-api/db/geotiff/banda2"))) {
            return;
        }
        CartographyProperties props = new CartographyProperties();
        GdalExecutorService gdal = new GdalExecutorService(props, new ObjectMapper());
        GeoserverSyncService sync = mock(GeoserverSyncService.class);
        when(sync.fetchPublishedBandExtents()).thenReturn(null);

        CartographyService service = new CartographyService(props, gdal, sync);
        List<Number[]> extents = service.getBandExtents();

        assertThat(extents).isNotEmpty();
        assertThat(extents.get(0)[0].intValue()).isEqualTo(6);
        assertThat(extents.get(extents.size() - 1)[0].intValue()).isEqualTo(2);
    }
}
