package fr.shom.ping.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import static java.util.Arrays.stream;

@Getter
@RequiredArgsConstructor
public enum SurveyTableSortProperty {
    PING_ID("identifier"),
    SHOM_ID("shomId"),
    NAME_AND_LOCATION("nameAndLocation"),
    PRODUCER_ORGANIZATION("producerOrganization"),
    OWNER_ORGANIZATION("ownerOrganization"),
    PERIOD_END_DATE("period"),
    COMPLIANT("compliant"),
    LAST_UPDATE("lastUpdate"),
    TRANSMISSION_STATUS_WITH_DATE("transmissionStatus"),
    EXPECTED_DELETION_DATE("expectedDeletionDate");

    private final String property;

    public static SurveyTableSortProperty fromProperty(String property) {
        return stream(values())
            .filter(name -> name.getProperty().equalsIgnoreCase(property))
            .findFirst().orElse(LAST_UPDATE);
    }
}
