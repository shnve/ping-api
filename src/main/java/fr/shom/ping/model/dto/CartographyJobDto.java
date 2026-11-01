package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Estado de un trabajo de procesamiento de carta en segundo plano.
 * status: QUEUED, VALIDATING, CONVERTING, PUBLISHING, SUCCESS o ERROR.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartographyJobDto {
    private String jobId;
    private String fileName;
    private String band;
    private volatile String status;
    private String requestedBy;
    private String createdAt;
    private String startedAt;
    private String finishedAt;
    /** Resultado final (sólo cuando status es SUCCESS o ERROR). */
    private CartographyUploadResponse result;

    @JsonIgnore
    public boolean isFinished() {
        return "SUCCESS".equals(status) || "ERROR".equals(status);
    }
}
