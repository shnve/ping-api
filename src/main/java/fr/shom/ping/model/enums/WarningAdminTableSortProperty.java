package fr.shom.ping.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static java.util.Arrays.stream;

@Getter
@RequiredArgsConstructor
public enum WarningAdminTableSortProperty {
    NUMBER("number"),
    STATE("state"),
    PUBLICATION_DATE("publicationDate"),
    CANCELLATION_DATE("cancellationDate"),
    REVIEW_DATE("reviewDate"),
    ESTIMATED_PUBLICATION_DATE("estimatedPublicationDate"),
    END_OF_BROADCAST_DATE("endOfBroadcastDate"),
    ENTITIES("entities"),
    FOLDERS("folders");

    private final String property;

    public static WarningAdminTableSortProperty fromProperty(String property) {
        return stream(values())
            .filter(name -> name.getProperty().equalsIgnoreCase(property))
            .findFirst().orElse(STATE);
    }
}
