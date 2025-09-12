package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Study;
import fr.shom.ping.model.StudyReference;
import fr.shom.ping.model.enums.StudyStatus;
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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import jakarta.validation.Valid;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.EMPTY_LIST_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Study Dto Model")
public class StudyDto extends AbstractStudyDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 8098146149957096874L;

    @Schema(description = ID_DESCRIPTION, requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;

    @Schema(description = "Creation date", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant creationDate;

    @Schema(description = "Last update", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant lastUpdate;

    @Schema(description = "Last update user", requiredMode = Schema.RequiredMode.REQUIRED)
    @NoHtml
    private String lastUpdateUser;

    @Schema(description = "Study status", requiredMode = Schema.RequiredMode.REQUIRED)
    private StudyStatus status;

    @Schema(description = "List of notions linked", example = EMPTY_LIST_EXAMPLE)
    @Builder.Default
    private Set<@Valid NotionDto> notions = new HashSet<>();

    @Schema(description = "List of attachments linked", example = EMPTY_LIST_EXAMPLE)
    @Builder.Default
    private Set<@Valid FolderAttachmentDetailsDto> attachments = new HashSet<>();

    public static StudyDto fromStudy(Study study, List<UUID> serviceIdsWithOperatorOrAdminRole) {
        return StudyDto.builder()
            .id(study.getId())
            .folderId(study.getFolder().getId())
            .number(study.getNumber())
            .creationDate(study.getCreationDate())
            .lastUpdate(study.getLastUpdate())
            .lastUpdateUser(study.getLastUpdateUser().getFirstname() + ' ' + study.getLastUpdateUser().getLastname())
            .notions(study.getNotions().stream().map(notion -> NotionDto.fromNotion(notion, serviceIdsWithOperatorOrAdminRole)).collect(Collectors.toSet()))
            .attachments(study.getAttachments().stream().map(FolderAttachmentDetailsDto::fromFolderAttachment).collect(Collectors.toSet()))
            .references(study.getReferences().stream().map(StudyReference::asDto).collect(Collectors.toSet()))
            .status(study.getStatus())
            .build();
    }
}
