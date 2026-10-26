package fr.shom.ping.model.dto;

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
    private String error;
    private Long convertedSizeBytes;
    private Long processingTimeMs;
    private String pixelSize;
}
