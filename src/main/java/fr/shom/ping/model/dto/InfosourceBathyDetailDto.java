package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.SurveyProcessingResult;
import fr.shom.ping.model.enums.InfosourceNature;
import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.model.enums.InfosourceNature.BATHY;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public class InfosourceBathyDetailDto extends InfosourceDetailDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -4605271816297245409L;

    private InfosourceNature nature;

    @Builder.Default
    private Set<SurveyDetectionDto> surveyDetections = newHashSet();

    public InfosourceBathyDetailDto fromInfosource(Infosource infosource) {
        super.fromInfosource(infosource);
        this.nature = BATHY;

        if (infosource.getNature() == BATHY) {
            this.surveyDetections = SurveyDetectionDto.mapToSurveyDetectionDto(infosource.getSurvey().getProcessingResults().stream()
                .max(Comparator.comparing(SurveyProcessingResult::getProcessingDate)).get().getDetections());
        }
        return this;
    }
}
