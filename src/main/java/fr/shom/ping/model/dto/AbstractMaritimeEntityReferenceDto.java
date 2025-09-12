package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Maritime Entity Reference Dto Model")
public class AbstractMaritimeEntityReferenceDto {

    @NotEmpty(message = "validation.maritime-entity-reference.title.not-empty")
    @Schema(description = "maritime entity reference title", required = true)
    @NoHtml
    protected String title;

    @Schema(description = "maritime entity reference international title")
    @NoHtml
    protected String internationalTitle;

    @NotNull(message = "validation.maritime-entity-reference.maritime-entity-id.not-null")
    @Schema(description = "maritime entity id", required = true)
    protected UUID maritimeEntityId;

    @Schema(description = "maritime entity reference url")
    @ValidUrl
    protected String url;

    @Schema(description = "maritime entity is public", required = true)
    protected Boolean isPublic;

    @Schema(description = "attachment id")
    protected UUID attachmentId;

    @Schema(description = "attachment files")
    protected @Valid AttachmentDownloadDto attachmentFile;

    @Schema(description = "Creation date time (UTC)")
    protected Instant creationDate;
}
