package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
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
public class SurveyTableDto extends AbstractSurveyDto {
    @Serial
    private static final long serialVersionUID = 1034856851419155600L;

    private String compliant;

    private Instant expectedDeletionDate;

    private String testProcessingStatus;

    private Instant testProcessingDate;

    private String transmissionStatus;

    private Instant lastUpdateTransmissionDate;
}
