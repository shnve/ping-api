package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Area;
import fr.shom.ping.model.Folder;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.SubArea;
import fr.shom.ping.model.enums.FolderStatus;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static java.util.Optional.ofNullable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
@Builder
@Schema(name = "Folder DTO")
public class FolderDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1445796824896540778L;

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    private Integer number;

    @NoHtml
    private String object;

    private FolderStatus status;

    @NoHtml
    private String area;

    private UUID areaId;

    @NoHtml
    private String locality;

    @NoHtml
    private String issuingService;

    private UUID serviceId;

    private Instant lastUpdate;

    private Instant creationDate;

    @Builder.Default
    @NoHtml
    private Set<String> concernedMaritimeEntities = new HashSet<>();

    @Builder.Default
    @NoHtml
    private Set<String> sharedMaritimeServices = new HashSet<>();

    @Builder.Default
    private Set<@Valid NotionDto> notions = new HashSet<>();

    @Builder.Default
    private Set<@Valid StudyDto> studies = new HashSet<>();

    @Builder.Default
    private Set<@Valid FolderAttachmentDetailsDto> attachments = new HashSet<>();

    public static FolderDto fromFolder(Folder folder, List<UUID> serviceIdsWithOperatorOrAdminRole) {
        return FolderDto.builder()
            .id(folder.getId())
            .number(folder.getNumber())
            .object(folder.getObject())
            .status(folder.getStatus())
            .area(ofNullable(folder.getArea()).map(Area::getName).orElse(null))
            .areaId(ofNullable(folder.getArea()).map(Area::getId).orElse(null))
            .locality(ofNullable(folder.getLocality()).map(SubArea::getName).orElse(null))
            .issuingService(ofNullable(folder.getIssuingService()).map(MaritimeService::getName).orElse(null))
            .serviceId(folder.getIssuingService().getId())
            .lastUpdate(folder.getLastUpdate())
            .creationDate(folder.getCreationDate())
            .concernedMaritimeEntities(folder.getEntities().stream().map(MaritimeEntity::getName).collect(Collectors.toSet()))
            .sharedMaritimeServices(folder.getServices().stream().map(MaritimeService::getName).collect(Collectors.toSet()))
            .notions(folder.getNotions().stream().map(notion -> NotionDto.fromNotion(notion, serviceIdsWithOperatorOrAdminRole)).collect(Collectors.toSet()))
            .studies(folder.getStudies().stream().map(study -> StudyDto.fromStudy(study, serviceIdsWithOperatorOrAdminRole)).collect(Collectors.toSet()))
            .attachments(folder.getAttachments().stream().map(FolderAttachmentDetailsDto::fromFolderAttachment).collect(Collectors.toSet()))
            .build();
    }

}
