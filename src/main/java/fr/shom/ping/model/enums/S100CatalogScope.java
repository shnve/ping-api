package fr.shom.ping.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum S100CatalogScope {
    FEATURE_CALATOG("featureCatalogue"),
    PORTRAYAL_CATALOG("portrayalCatalogue"),
    INTEROPERABILITY_CATALOG("interoperabilityCatalogue");

    private final String value;
}
