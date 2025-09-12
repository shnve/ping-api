package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.FolderAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.Valid;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Folder Attachment Details Dto")
public class FolderAttachmentDetailsDto extends AbstractFolderAttachmentDto {

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    @Schema(description = "Folder attachment creation date")
    private Instant creationDate;

    @Schema(description = "Folder attachment maritime service")
    private @Valid MaritimeServiceDto maritimeService;

    @Schema(description = "Folder attachment file")
    private @Valid AttachmentDownloadDto attachmentFile;

    public static FolderAttachmentDetailsDto fromFolderAttachment(FolderAttachment folderAttachment) {
        return FolderAttachmentDetailsDto.builder()
            .id(folderAttachment.getId())
            .creationDate(folderAttachment.getCreationDate())
            .comment(folderAttachment.getComment())
            .receptionDate(folderAttachment.getReceptionDate())
            .maritimeService(folderAttachment.getMaritimeService() != null ? folderAttachment.getMaritimeService().asDto() : null)
            .maritimeServiceId(folderAttachment.getMaritimeService() != null ? folderAttachment.getMaritimeService().getId() : null)
            .attachmentFile(new AttachmentDownloadDto(folderAttachment.getAttachment()))
            .attachmentId(folderAttachment.getAttachment().getId())
            .build();
    }
}
