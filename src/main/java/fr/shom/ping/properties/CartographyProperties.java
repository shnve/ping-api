package fr.shom.ping.properties;

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
}
