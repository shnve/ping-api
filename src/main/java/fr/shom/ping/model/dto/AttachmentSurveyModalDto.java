package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@Data
@Builder
@JsonInclude(NON_NULL)
public class AttachmentSurveyModalDto implements Serializable {

    @Serial
    private static final long serialVersionUID = -5828346985589849387L;

    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    private String filename;

    private Long totalSize;

    private Instant creationDate;

    private String type;

    private String statistics;

    public static AttachmentSurveyModalDto fromAttachment(Attachment attachment) {
        return AttachmentSurveyModalDto.builder()
            .id(attachment.getId())
            .type(attachment.getType())
            .filename(attachment.getFilename())
            .totalSize(attachment.getTotalSize())
            .build();
    }
}
