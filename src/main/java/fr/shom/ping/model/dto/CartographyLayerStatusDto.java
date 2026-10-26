package fr.shom.ping.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartographyLayerStatusDto {
    private boolean active;
    private int fileCount;
    private String lastModified;
    private long totalSizeBytes;
    private String storageFolder;
}
