package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Infosource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static java.util.stream.Collectors.toList;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Infosource Detail Dto (Activity Forecast) (Public User)")
public class InfosourceActivityForecastDetailWithPublicUserDto extends InfosourceActivityForecastCreateWithPublicUserDto {
    @Serial
    private static final long serialVersionUID = -7051068334452080283L;

    @Schema(description = ID_DESCRIPTION, requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;

    @Builder.Default
    public List<AttachmentDownloadDto> attachmentFiles = new ArrayList<>();

    public InfosourceActivityForecastDetailWithPublicUserDto(Infosource infosource) {
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
