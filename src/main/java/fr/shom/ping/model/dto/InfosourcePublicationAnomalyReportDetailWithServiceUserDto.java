package fr.shom.ping.model.dto;

import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.ServiceUser;
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
@Schema(name = "Infosource Detail Dto (Publication Anomaly Report) (Service User)")
public class InfosourcePublicationAnomalyReportDetailWithServiceUserDto extends InfosourcePublicationAnomalyCreateWithServiceUserDto {

    @Serial
    private static final long serialVersionUID = -6459748783371361779L;

    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Builder.Default
    public List<@Valid AttachmentDownloadDto> attachmentFiles = new ArrayList<>();

    public InfosourcePublicationAnomalyReportDetailWithServiceUserDto(Infosource infosource) {
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

    public Infosource toInfosource(List<Attachment> attachments, ServiceUser serviceUser, MaritimeService service) {
        return super.toInfosourceBuilder(attachments)
            .issuingService(service)
            .issuingServiceUser(serviceUser)
            .id(id)
            .build();
    }
}
