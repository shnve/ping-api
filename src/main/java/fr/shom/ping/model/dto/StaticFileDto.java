package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.StaticFile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.PATH_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.TITLE_DESCRIPTION;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
@Builder
@Schema(name = "Static file DTO")
public class StaticFileDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3248890758510356594L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @NotEmpty(message = "validation.static-file.path.not-empty")
    @Pattern(regexp = "^[a-z\\d-]+$", message = "validation.static-file.path.invalid-pattern")
    @Schema(description = PATH_DESCRIPTION, required = true)
    private String path;

    @NotEmpty(message = "validation.static-file.description.not-empty")
    @Schema(description = "Description", required = true)
    private String description;

    @Schema(description = "attachment id")
    private UUID attachmentId;

    @Schema(description = "attachment file")
    private AttachmentDownloadDto attachmentFile;

    @Builder.Default
    private Boolean locked = false;

    public StaticFileDto(StaticFile staticFile) {
        id = staticFile.getId();
        path = staticFile.getPath();
        description = staticFile.getDescription();
        attachmentFile = new AttachmentDownloadDto(staticFile.getAttachment());
        attachmentId = attachmentFile.getId();
        locked = staticFile.getLocked();
    }

    public StaticFile toStaticFile(Attachment attachment) {
        return StaticFile.builder()
            .id(id)
            .path(path)
            .description(description)
            .attachment(attachment)
            .locked(locked)
            .build();
    }
}
