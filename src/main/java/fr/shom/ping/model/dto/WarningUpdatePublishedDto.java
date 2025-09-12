package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@JsonInclude(NON_NULL)
@Schema(name = "Warning Admin Update Published Dto Model")
public class WarningUpdatePublishedDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 6401158435024315467L;

    // Required during @PreAuthorize
    private UUID serviceId;

    // Part 2
    @Default
    private List<UUID> parentWarningIds = new ArrayList<>();

    @Default
    public List<UUID> infosourcesIds = new ArrayList<>();

    @Default
    public List<UUID> maritimeEntitiesIds = new ArrayList<>();

    @Default
    public List<UUID> georegulationIds = new ArrayList<>();

    @Default
    private List<UUID> attachmentIds = new ArrayList<>();

    // Part 4
    @Default
    private Set<WarningReferenceAsPublicReferenceDto> warningPublicsReferences = new HashSet<>();

    @Default
    private Set<UUID> georegulationReferenceIds = new HashSet<>();

    @Default
    private Set<UUID> attachmentReferenceIds = new HashSet<>();

    @Default
    private Set<UUID> entityReferenceIds = new HashSet<>();

    @Default
    protected Set<WarningLinkDto> warningLinksPublicsReferences = new HashSet<>();

    // Part 6
    private LocalDate reviewDate;

    @NoHtml
    private String internalComment;
}
