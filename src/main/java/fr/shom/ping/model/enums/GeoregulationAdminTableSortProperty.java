package fr.shom.ping.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static java.util.Arrays.stream;

@Getter
@RequiredArgsConstructor
public enum GeoregulationAdminTableSortProperty {
    NUMBER("number"),
    PUBLICATION_DATE("publicationDate"),
    LAST_UPDATE("lastUpdate"),
    REPEAL_DATE("repealDate");

    private final String property;

    public static GeoregulationAdminTableSortProperty fromProperty(String property) {
        return stream(values())
            .filter(name -> name.getProperty().equalsIgnoreCase(property))
            .findFirst().orElse(LAST_UPDATE);
    }
}
