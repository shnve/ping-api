package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static java.util.stream.Collectors.toList;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
@Schema(name = "Infosource Activity Forecast Part Detail DTO (Service User)")
public class InfosourceActivityForecastDetailWithServiceUserDto extends InfosourceActivityForecastCreateWithServiceUserDto {

    @Serial
    private static final long serialVersionUID = 2133597336845540822L;
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Builder.Default
    public List<@Valid AttachmentDownloadDto> attachmentFiles = new ArrayList<>();

    public InfosourceActivityForecastDetailWithServiceUserDto(Infosource infosource) {
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
