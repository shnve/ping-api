package fr.shom.ping.model.dto;

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
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static java.util.Optional.ofNullable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Folder Create Dto Model")
public class FolderSaveDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 789315428709004077L;

    @Schema(description = "Area id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "validation.folder.area-id.not-null")
    private UUID areaId;

    private UUID localityId;

    @Default
    private Set<UUID> concernedMaritimeEntityIds = new HashSet<>();

    @Default
    private Set<UUID> sharedMaritimeServiceIds = new HashSet<>();

    @NoHtml
    private String object;

    private LocalDate reviewDate;

    private Instant lastUpdate;

    @Schema(description = "Comment")
    @NoHtml
    private String comment;

    @Schema(description = "Status")
    @Default
    private FolderStatus status = FolderStatus.OPEN;

    public static FolderSaveDto fromFolder(UUID id, Folder folder) {

        return FolderSaveDto.builder()
            .object(folder.getObject())
            .areaId(ofNullable(folder.getArea()).map(Area::getId).orElse(null))
            .localityId(ofNullable(folder.getLocality()).map(SubArea::getId).orElse(null))
            .lastUpdate(folder.getLastUpdate())
            .reviewDate(folder.getReviewDate())
            .comment(folder.getComment())
            .concernedMaritimeEntityIds(folder.getEntities().stream().map(MaritimeEntity::getId).collect(Collectors.toSet()))
            .sharedMaritimeServiceIds(folder.getServices().stream().map(MaritimeService::getId).collect(Collectors.toSet()))
            .status(folder.getStatus())
            .build();
    }
}
