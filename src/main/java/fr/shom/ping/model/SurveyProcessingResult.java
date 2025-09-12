package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.SurveyAnalysisProgressStatus;
import fr.shom.ping.model.enums.SurveyProcessingResultError;
import fr.shom.ping.model.enums.SurveyProcessingStatus;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Type;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.model.enums.SurveyAnalysisProgressStatus.NOT_STARTED;
import static fr.shom.ping.model.enums.SurveyProcessingStatus.NOT_AVAILABLE;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Survey processing result Model")
public class SurveyProcessingResult implements Serializable {

    @Serial
    private static final long serialVersionUID = -116006932384678432L;

    @Id
    @Schema(description = ID_DESCRIPTION, requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;

    @NotNull(message = "validation.survey-processing-result.status.not-null")
    @Column(nullable = false)
    @Enumerated(STRING)
    @Schema(description = "Survey processing result status")
    @Builder.Default
    private SurveyProcessingStatus status = NOT_AVAILABLE;

    @Column(nullable = false)
    @Schema(description = "Processing date (UTC)")
    private Instant processingDate;

    @Type(JsonType.class)
    @Column(columnDefinition = "json", nullable = false)
    @Builder.Default
    private Map<SurveyProcessingResultError, String> errors = new HashMap<>();

    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    @Builder.Default
    private Map<String, Object> detectedObjects = new HashMap<>();

    @ElementCollection(fetch = EAGER)
    @CollectionTable(
        name = "survey_enc",
        joinColumns = @JoinColumn(name = "processing_result_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<SurveyEnc> enc = newHashSet();

    @ElementCollection(fetch = EAGER)
    @CollectionTable(
        name = "survey_danger",
        joinColumns = @JoinColumn(name = "processing_result_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<SurveyDanger> dangers = newHashSet();

    @ElementCollection(fetch = EAGER)
    @CollectionTable(
        name = "survey_detection",
        joinColumns = @JoinColumn(name = "processing_result_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<SurveyDetection> detections = newHashSet();

    @Enumerated(STRING)
    @Schema(description = "Survey external analysis progress status")
    @Builder.Default
    private SurveyAnalysisProgressStatus externalAnalysisStatus = NOT_STARTED;

    @Builder.Default
    private Float externalAnalysisProgress = 0.0f;

    @OneToMany(cascade = ALL)
    @JoinTable(
        name = "survey_processing_result_attachments",
        joinColumns = @JoinColumn(name = "processing_result_id"),
        inverseJoinColumns = @JoinColumn(name = "attachment_id")
    )
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }
}
