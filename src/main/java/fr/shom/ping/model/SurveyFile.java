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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Type;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.model.enums.SurveyFileComplianceAnalysisState.NOT_AVAILABLE;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.CascadeType.ALL;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Survey File Model")
public class SurveyFile implements Serializable {

    @Serial
    private static final long serialVersionUID = -2002171437227139441L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @OneToOne(cascade = ALL)
    @ToString.Exclude
    @Schema(description = "Attachment")
    private Attachment attachment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "validation.survey-file.compliant.not-null")
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

    @Builder.Default
    private boolean archive = false;

    @OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinTable(
        name = "survey_file_archive_content",
        joinColumns = @JoinColumn(name = "survey_archive_file_id"),
        inverseJoinColumns = @JoinColumn(name = "survey_file_in_archive_id"))
    @ToString.Exclude
    @Schema(description = "Survey files in archive")
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<SurveyFileInArchive> filesInArchive = new HashSet<>();

    @Column(nullable = false)
    @NotNull(message = "validation.survey-file.cartonaut-file-id.not-null")
    private Integer cartonautFileId;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }

    public Path getPath() {
        return Path.of(getAttachment() != null ? getAttachment().getLocation() : null);
    }

    public Map<String, String> getRelativePathsOfFilesInArchive() {
        if (!isArchive()) {
            return new HashMap<>();
        }

        return filesInArchive.stream().collect(
            Collectors.toMap(surveyFileInArchive -> String.valueOf(surveyFileInArchive.getCartonautFileId()),
                surveyFileInArchive -> attachment.getFilename() + "/" + surveyFileInArchive.getFilename())
        );
    }
}
