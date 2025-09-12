package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.MaritimeEntityReference;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Maritime Entity Reference Dto Model")
public class MaritimeEntityReferenceDto extends AbstractMaritimeEntityReferenceDto {

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    public MaritimeEntityReferenceDto(MaritimeEntityReference maritimeEntityReference) {
        this.id = maritimeEntityReference.getId();
        this.title = maritimeEntityReference.getTitle();
        this.internationalTitle = maritimeEntityReference.getInternationalTitle();
        this.url = maritimeEntityReference.getUrl();
        this.isPublic = maritimeEntityReference.isPublic();
        this.maritimeEntityId = maritimeEntityReference.getMaritimeEntity().getId();
        if(maritimeEntityReference.getAttachment() != null){
            this.attachmentId = maritimeEntityReference.getAttachment().getId();
            this.attachmentFile = AttachmentDownloadDto.builder()
                .id(maritimeEntityReference.getAttachment().getId())
                .filename(maritimeEntityReference.getAttachment().getFilename())
                .totalSize(maritimeEntityReference.getAttachment().getTotalSize())
                .build();
        }
        this.creationDate = maritimeEntityReference.getCreationDate();
    }
}
