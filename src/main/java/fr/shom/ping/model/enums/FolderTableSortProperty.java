package fr.shom.ping.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static java.util.Arrays.stream;

@Getter
@RequiredArgsConstructor
public enum FolderTableSortProperty {

    NUMBER("number"),
    REVIEW_DATE("reviewDate"),
    LAST_UPDATE("lastUpdate");

    private final String property;

    public static FolderTableSortProperty fromProperty(String property) {
        return stream(values())
            .filter(name -> name.getProperty().equalsIgnoreCase(property))
            .findFirst().orElse(LAST_UPDATE);
    }
}
