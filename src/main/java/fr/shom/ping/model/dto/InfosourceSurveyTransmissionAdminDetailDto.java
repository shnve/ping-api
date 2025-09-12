package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.enums.InfosourceNature;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.model.enums.InfosourceSurveyTransmissionObjectKeys.NAME_AND_LOCATION;
import static fr.shom.ping.model.enums.InfosourceSurveyTransmissionObjectKeys.OWNER_ORGANIZATION;
import static fr.shom.ping.model.enums.InfosourceSurveyTransmissionObjectKeys.PERIOD;
import static fr.shom.ping.model.enums.InfosourceSurveyTransmissionObjectKeys.PING_ID;
import static fr.shom.ping.model.enums.InfosourceSurveyTransmissionObjectKeys.PRODUCER_ORGANIZATION;
import static fr.shom.ping.model.enums.InfosourceSurveyTransmissionObjectKeys.SHOM_ID;
import static fr.shom.ping.model.enums.InfosourceSurveyTransmissionObjectKeys.SURVEY_ID;
import static fr.shom.ping.model.enums.InfosourceSurveyTransmissionObjectKeys.TRANSMISSION_DATE;
import static java.time.ZoneOffset.UTC;
import static java.util.Optional.ofNullable;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public class InfosourceSurveyTransmissionAdminDetailDto extends InfosourceAdminDetailDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -4605271816297245409L;

    private InfosourceNature nature;

    public InfosourceSurveyTransmissionAdminDetailDto fromInfosource(Infosource infosource, ServiceUser serviceUser, boolean isBri) {
        super.fromInfosource(infosource, serviceUser);
        // DPC_RG_13161
        this.nature = InfosourceNature.SURVEY_TRANSMISSION;
        final Survey linkedSurvey = infosource.getSurvey();
        final InfosourceDateRangeDto period = InfosourceDateRangeDto.builder()
            .start(LocalDate.ofInstant(linkedSurvey.getStartDate(), UTC))
            .end(LocalDate.ofInstant(linkedSurvey.getEndDate(), UTC))
            .build();
        final String ownerOrganization = linkedSurvey.getOwnerOrganization();
        final Map<String, Object> objectMap = Stream.of(new Object[][]{
            {PING_ID, linkedSurvey.getIdentifier()},
            {SHOM_ID, ofNullable(linkedSurvey.getShomId()).orElse("")},
            {PRODUCER_ORGANIZATION, linkedSurvey.getProducerOrganization()},
            {OWNER_ORGANIZATION, StringUtils.defaultIfBlank(ownerOrganization, "")},
            {NAME_AND_LOCATION, linkedSurvey.getNameAndLocation()},
            {TRANSMISSION_DATE, linkedSurvey.getLastUpdateTransmissionDate()},
            {PERIOD, period}
        }).collect(Collectors.toMap(data -> data[0].toString(), data -> data[1]));

        if (isBri) {
            objectMap.put(SURVEY_ID.toString(), linkedSurvey.getId().toString());
        }

        this.setObject(objectMap);
        return this;
    }
}
