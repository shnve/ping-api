package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
public class AttachmentSurveyResultDownloadDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -5828346985589849387L;

    private UUID id;

    @NoHtml
    private String resultFileName;

    @NoHtml
    private String analyzedFileName;

    private Long totalSize;
}
