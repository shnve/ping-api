package fr.shom.ping.properties;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ping.cartography")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartographyProperties {
    /**
     * Carpeta base donde residen las subcarpetas de bandas (banda1..banda6).
     * En local por defecto apunta a la carpeta montada en geoserver: "./db/geotiff".
     */
    @Builder.Default
    private String storageBasePath = "db/geotiff";

    /**
     * Carpeta temporal donde se reciben las cargas y se realizan los procesos de GDAL.
     */
    @Builder.Default
    private String tempPath = System.getProperty("os.name", "").toLowerCase().contains("win")
            ? "C:/tmp/pisac-cartography"
            : "/tmp/pisac-cartography";

    /**
     * URL base de GeoServer (ej. http://localhost:8082/geoserver).
     */
    @Builder.Default
    private String geoserverUrl = "http://localhost:8082/geoserver";

    /**
     * Usuario administrador de GeoServer.
     */
    @Builder.Default
    private String geoserverUser = "admin";

    /**
     * Contraseña de administrador de GeoServer.
     */
    @Builder.Default
    private String geoserverPassword = "geoserver";

    /**
     * Workspace de GeoServer para las capas raster cartográficas.
     */
    @Builder.Default
    private String geoserverWorkspace = "pisac_shn";

    /**
     * Nombre del grupo de capas (LayerGroup) principal en GeoServer.
     */
    @Builder.Default
    private String geoserverLayerGroup = "cartografia_ve";

    /**
     * Imagen Docker de GDAL a utilizar en caso de no disponer de GDAL nativo en el sistema.
     */
    @Builder.Default
    private String dockerImage = "ghcr.io/osgeo/gdal:ubuntu-full-latest";

    /**
     * Resolución DPI para rasterizar documentos PDF marítimos (por defecto 300 DPI).
     */
    @Builder.Default
    private int pdfDpi = 300;

    /**
     * Tiempo máximo (minutos) de cada comando GDAL. La conversión corre en segundo plano.
     */
    @Builder.Default
    private int gdalTimeoutMinutes = 30;

    /**
     * Escala a partir de la cual se muestra cada banda (MaxScaleDenominator del estilo estilo_bandaN).
     * Sólo se usa al crear un estilo que no existe; los estilos ya creados en GeoServer no se modifican.
     * Valores según la tabla de bandas del área funcional; banda1/banda2 sin límite (fondo siempre visible).
     * Zoom web en que aparece cada banda (latitud de Venezuela): banda3 11, banda4 13, banda5 15, banda6 18
     * (banda6 = 1:4.000 por norma; en zoom 17 la escala real es ~1:4.190).
     */
    @Builder.Default
    private Map<String, Integer> bandMaxScale = new HashMap<>(Map.of(
            "banda3", 350000,
            "banda4", 90000,
            "banda5", 25000,
            "banda6", 4000
    ));
}
