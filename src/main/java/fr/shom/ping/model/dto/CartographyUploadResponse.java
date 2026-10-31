package fr.shom.ping.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartographyUploadResponse {
    private boolean success;
    private String fileName;
    private String band;
    private String message;
    private String srsDetected;
    private Boolean isGeoreferenced;
    private String outputFilePath;
    private Boolean geoserverReloaded;
    /** Resultado real de la publicación en GeoServer (null si no se solicitó publicar). */
    private Boolean geoserverPublished;
    /** Motivo legible del resultado de GeoServer. */
    private String geoserverMessage;
    /** Pasos ejecutados en GeoServer (almacén, capa, estilo, grupo, verificación). */
    private List<String> geoserverSteps;
    private String error;
    private Long convertedSizeBytes;
    private Long processingTimeMs;
    private String pixelSize;
}
