package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.ActionType;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import fr.shom.ping.validation.NoHtml;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.hateoas.server.core.Relation;

import jakarta.validation.Valid;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Relation(collectionRelation = "items")
public class InfosourceAdminTableDto extends AbstractInfosourceDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 3755279852041700694L;

    @NoHtml
    private String emitter;

    private ActionType serviceStatus;

    private UUID issuingServiceId;

    private UUID issuingServiceUserId;

    @Default
    @NoHtml
    private Set<String> tags = new HashSet<>();

    @Default
    private Set<@Valid MaritimeEntityConcernedDto> concernedMaritimeEntities = new HashSet<>();

    @Default
    @NoHtml
    private Set<String> folders = new HashSet<>();

    @Default
    @NoHtml
    private Set<String> warnings = new HashSet<>();
}
