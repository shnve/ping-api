package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.FolderReviewStatus;
import fr.shom.ping.model.enums.FolderStatus;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.hateoas.server.core.Relation;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Folder Dto Model")
@Relation(collectionRelation = "items")
public class FolderTableDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -7241238736152981334L;

    @Schema(description = ID_DESCRIPTION, required = true)
    protected UUID id;

    protected Integer number;

    @NoHtml
    protected String issuingService;

    protected FolderStatus status;

    @NoHtml
    protected String comment;

    @NoHtml
    protected String object;

    @NoHtml
    protected String locality;

    protected Instant creationDate;

    protected Instant lastUpdate;

    protected LocalDate reviewDate;

    protected FolderReviewStatus reviewStatus;

    @Builder.Default
    private Set<@Valid MaritimeEntityConcernedDto> concernedMaritimeEntities = new HashSet<>();

    @Builder.Default
    private Set<@Valid MaritimeServiceSharedDto> sharedMaritimeServices = new HashSet<>();

    @Builder.Default
    private Set<@Valid AreaSharedDto> sharedAreas = new HashSet<>();

    @Builder.Default
    private Set<@Valid FolderStudyDto> studies = new HashSet<>();
}
