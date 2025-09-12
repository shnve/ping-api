package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import fr.shom.ping.validation.NoHtml;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.hateoas.server.core.Relation;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Relation(collectionRelation = "items")
public class InfosourceTableDto extends AbstractInfosourceDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 3755279852041700694L;

    private UUID issuingPublicUserId;

    @Builder.Default
    @NoHtml
    private Set<String> warnings = new HashSet<>();
}
