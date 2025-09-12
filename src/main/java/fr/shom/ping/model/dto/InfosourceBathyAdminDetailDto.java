package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.SurveyProcessingResult;
import fr.shom.ping.model.enums.InfosourceNature;
import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.model.enums.InfosourceNature.BATHY;
import static fr.shom.ping.model.enums.InfosourceSurveyTransmissionObjectKeys.NAME_AND_LOCATION;
import static fr.shom.ping.model.enums.InfosourceSurveyTransmissionObjectKeys.OWNER_ORGANIZATION;
import static fr.shom.ping.model.enums.InfosourceSurveyTransmissionObjectKeys.PERIOD;
import static fr.shom.ping.model.enums.InfosourceSurveyTransmissionObjectKeys.PING_ID;
import static fr.shom.ping.model.enums.InfosourceSurveyTransmissionObjectKeys.PRODUCER_ORGANIZATION;
import static fr.shom.ping.model.enums.InfosourceSurveyTransmissionObjectKeys.SHOM_ID;
import static fr.shom.ping.model.enums.InfosourceSurveyTransmissionObjectKeys.SURVEY_ID;
import static fr.shom.ping.model.enums.InfosourceSurveyTransmissionObjectKeys.TRANSMISSION_DATE;
import static java.util.Optional.ofNullable;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public class InfosourceBathyAdminDetailDto extends InfosourceAdminDetailDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -4605271816297245409L;

    private InfosourceNature nature;

    @Builder.Default
    private Set<SurveyDetectionDto> surveyDetections = newHashSet();

    public InfosourceBathyAdminDetailDto fromInfosource(Infosource infosource, ServiceUser serviceUser, boolean isBri) {
        super.fromInfosource(infosource, serviceUser);
        this.nature = BATHY;

        if (infosource.getNature() == BATHY) {
            this.surveyDetections = SurveyDetectionDto.mapToSurveyDetectionDto(infosource.getSurvey().getProcessingResults().stream()
                .max(Comparator.comparing(SurveyProcessingResult::getProcessingDate)).get().getDetections());

            // DPC_RG_13140
            final Survey linkedSurvey = infosource.getSurvey();
            final Map<String, Object> objectMap = Stream.of(new Object[][]{
                {PING_ID, linkedSurvey.getIdentifier()},
                {SHOM_ID, ofNullable(linkedSurvey.getShomId()).orElse("")}
            }).collect(Collectors.toMap(data -> data[0].toString(), data -> data[1]));

            if (isBri) {
                objectMap.put(SURVEY_ID.toString(), linkedSurvey.getId().toString());
            }

            this.setObject(objectMap);
        }
        return this;
    }
}
