package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.SurveyFileAnalysisError;
import fr.shom.ping.model.enums.SurveyFileComplianceAnalysisState;
import fr.shom.ping.model.enums.SurveyFileStatisticFields;
import fr.shom.ping.utils.survey.SurveyStatistics;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.model.enums.SurveyFileComplianceAnalysisState.NOT_AVAILABLE;
import static java.util.UUID.randomUUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Survey File in archive Model")
public class SurveyFileInArchive implements Serializable {
    @Serial
    private static final long serialVersionUID = -3485845389766975953L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Column(nullable = false)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "validation.survey-file-in-archive.compliant.not-null")
    @Builder.Default
    private SurveyFileComplianceAnalysisState compliant = NOT_AVAILABLE;

    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    @Builder.Default
    private Map<SurveyFileStatisticFields, String> statistics = new HashMap<>();

    @Transient
    private SurveyStatistics fileStats;

    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    @Builder.Default
    private Map<SurveyFileAnalysisError, String> compliantAnalysisResults = new HashMap<>();

    @Column(nullable = false)
    @NotNull(message = "validation.survey-file-in-archive.cartonaut-file-id.not-null")
    private Integer cartonautFileId;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }
}
