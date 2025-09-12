package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
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
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Warning Maritime Entity Reference Dto")
public class WarningMaritimeEntityReferenceDto extends AbstractMaritimeEntityReferenceDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -5752393962607856467L;

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    @Schema(description = "maritime entity name linked to this reference")
    private String maritimeEntityName;

    @Schema(description = "maritime entity international linked to this reference")
    private String maritimeEntityInternationalName;

    public WarningMaritimeEntityReferenceDto(String maritimeEntityName, String maritimeEntityInternationalName,
        MaritimeEntityReferenceDto maritimeEntityReferenceDto) {
        this.id = maritimeEntityReferenceDto.getId();
        this.maritimeEntityName = maritimeEntityName;
        this.maritimeEntityInternationalName = maritimeEntityInternationalName;
        this.title = maritimeEntityReferenceDto.getTitle();
        this.internationalTitle = maritimeEntityReferenceDto.getInternationalTitle();
        this.url = maritimeEntityReferenceDto.getUrl();
        this.attachmentFile = maritimeEntityReferenceDto.getAttachmentFile();
        this.attachmentId = maritimeEntityReferenceDto.getAttachmentId();
    }
}
