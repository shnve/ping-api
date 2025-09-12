package fr.shom.ping.model.dto;

import fr.shom.ping.model.Infosource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import jakarta.validation.Valid;

import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static java.util.stream.Collectors.toList;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Infosource Detail Dto (Map Anomaly Report) (Public User)")
public class InfosourceMapAnomalyDetailWithPublicUserDto extends InfosourceMapAnomalyCreateWithPublicUserDto {

    @Serial
    private static final long serialVersionUID = 1786328419430047431L;

    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Builder.Default
    public List<@Valid AttachmentDownloadDto> attachmentFiles = new ArrayList<>();

    public InfosourceMapAnomalyDetailWithPublicUserDto(Infosource infosource) {
        super(infosource);
        this.id = infosource.getId();
        this.attachmentFiles = infosource.getAttachments()
            .stream()
            .map(attachment -> AttachmentDownloadDto.builder()
                .id(attachment.getId())
                .filename(attachment.getFilename())
                .totalSize(attachment.getTotalSize())
                .build())
            .collect(toList());
    }
}
