package fr.shom.ping.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static java.util.Arrays.stream;

@Getter
@RequiredArgsConstructor
public enum SubscriptionTableSortProperty {

    TITLE("title"),
    MARITIME_SERVICE_SUBSCRIBER("maritimeServiceSubscriber"),
    CREATION_DATE("creation_date");

    private final String property;

    public static SubscriptionTableSortProperty fromProperty(String property) {
        return stream(values())
            .filter(name -> name.getProperty().equalsIgnoreCase(property))
            .findFirst().orElse(CREATION_DATE);
    }
}
