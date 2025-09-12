package fr.shom.ping.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static java.util.Arrays.stream;

@Getter
@RequiredArgsConstructor
public enum InfosourceTableSortProperty {
    STATUS("status"),
    SERVICE_STATUS("serviceStatus"),
    EMITTER("emitter"),
    NATURE("nature"),
    LAST_UPDATE("lastUpdate"),
    NUMBER("number"),
    VALIDATION_TIME("validationTime");

    private final String property;

    public static InfosourceTableSortProperty fromProperty(String property) {
        return stream(values())
            .filter(name -> name.getProperty().equalsIgnoreCase(property))
            .findFirst().orElse(STATUS);
    }
}
