package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Folder Attachment Dto Model")
public abstract class AbstractFolderAttachmentDto {

    @Schema(description = "Folder attachment comment")
    @NoHtml
    protected String comment;

    @Schema(description = "Folder attachment reception date")
    protected Instant receptionDate;

    @Schema(description = "Folder attachment maritime service id")
    protected UUID maritimeServiceId;

    @Schema(description = "Folder attachment file id", required = true)
    protected UUID attachmentId;
}
