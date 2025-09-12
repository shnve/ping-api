package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.InfosourceRecipient;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.enums.InfosourceNature;
import fr.shom.ping.model.enums.InfosourceStatus;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.model.enums.WarningStatus.PUBLISHED;
import static org.springframework.util.CollectionUtils.isEmpty;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Infosource Dto Model")
public abstract class AbstractInfosourceDto {
    @Schema(description = ID_DESCRIPTION, required = true)
    protected UUID id;

    @NoHtml
    protected String number;

    protected InfosourceNature nature;

    protected InfosourceStatus status;

    @NoHtml
    protected String comment;

    @NoHtml
    protected String rejectComment;

    protected Instant creationDate;

    protected Instant lastUpdate;

    protected Instant validationTime;

    @NoHtml
    protected String description;

    @Default
    @NoHtml
    protected Set<String> producedNauticalInformations = new HashSet<>();

    @Default
    protected Set<@Valid InfosourceRecipientDto> recipients = new HashSet<>();

    @Default
    protected Set<@Valid ReferenceFolderStudyDto> referenceFolderStudies = new HashSet<>();

    @Default
    @NoHtml
    protected Map<String, Object> object = new HashMap<>();

    public static AbstractInfosourceDto fromInfosource(AbstractInfosourceDto dto, Infosource infosource) {
        dto.setId(infosource.getId());
        dto.setNumber(infosource.getNumber());
        dto.setNature(infosource.getNature());
        dto.setStatus(infosource.getStatus());
        // dto.setComment(infosource.getComment());
        dto.setCreationDate(infosource.getCreationDate());
        dto.setLastUpdate(infosource.getLastUpdate());
        dto.setValidationTime(infosource.getValidationTime());
        dto.setObject(infosource.getObject());
        if (infosource.getNature() == InfosourceNature.SURVEY_TRANSMISSION) {
            final Survey linkedSurvey = infosource.getSurvey();
            // DPC_RG_13162
            final String ownerOrganization = linkedSurvey.getOwnerOrganization();
            dto.setDescription((StringUtils.isNotBlank(ownerOrganization) ? ownerOrganization.concat(", ") : "").concat(linkedSurvey.getNameAndLocation() + ", " + linkedSurvey.getIdentifier()));
        } else if (infosource.getNature() == InfosourceNature.BATHY) {
            // DPC_RG_13147
            dto.setDescription(infosource.getConfidentialityInformation());
        } else if (!infosource.getInfosourceParts().isEmpty()) {
            dto.setDescription(infosource.getInfosourceParts().first().getDescription());
        }

        if (!isEmpty(infosource.getRecipients())) {
            dto.setRecipients(infosource.getRecipients().stream()
                .map(InfosourceRecipient::asDto)
                .collect(Collectors.toSet()));
        }

        infosource.getLinkedWarnings().stream().filter(w -> w.getStatus() == PUBLISHED).forEach(warning -> {
            final String producedNauticalInformation =
                warning.getNameOfSeries() + " " + warning.getWarningNumber() + "/" + warning.getYear().toString().substring(2);
            dto.getProducedNauticalInformations().add(producedNauticalInformation);
        });
        return dto;
    }
}
