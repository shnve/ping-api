package fr.shom.ping.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartographyValidationResult {
    private boolean georeferenced;
    private String srs;
    private int width;
    private int height;
    private String pixelSize;
    private int bandCount;
    private boolean hasColorTable;
    private boolean hasAlphaBand;
    private String rawGdalInfoJson;
    private String errorMessage;
}
