package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serial;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.hateoas.server.core.Relation;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Relation(collectionRelation = "items")
public class SurveyTableBriDto extends SurveyTableDto {
    @Serial
    private static final long serialVersionUID = -7582797533326905492L;

    private String shomId;

    private String ownerOrganization;

    private String producerOrganization;

    private String briInternalComment;
}
