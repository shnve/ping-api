package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.WarningState;
import fr.shom.ping.model.enums.WarningStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.hateoas.server.core.Relation;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Relation(collectionRelation = "items")
public class WarningAdminTableDto extends WarningTableViewDto {
    @Serial
    private static final long serialVersionUID = -1658962174443778547L;

    @Default
    @Schema(description = "References of current warning")
    private Set<WarningReferenceDto> references = new HashSet<>();

    @Schema(description = "Creation start", required = true)
    private LocalDate creationDate;

    @Schema(description = "Last update")
    private LocalDate lastUpdate;

    @Schema(description = "Estimated publication date")
    private LocalDate estimatedPublicationDate;

    @Schema(description = "Publication date")
    private LocalDate publicationDate;

    @Schema(description = "Cancellation date")
    private LocalDate cancellationDate;

    @Schema(description = "End of broadcast date")
    private LocalDate endOfBroadcastDate;

    @Schema(description = "Review date")
    private LocalDate reviewDate;

    @Schema(description = "Internal comment")
    private String internalComment;

    @Schema(description = "State")
    private WarningState state;

    @Schema(description = "Status", required = true)
    private WarningStatus status;

    @Default
    @Schema(description = "Entities of current warning")
    private Set<String> entities = new HashSet<>();

    @Default
    @Schema(description = "Folders of current warning")
    private Set<String> folders = new HashSet<>();
}
