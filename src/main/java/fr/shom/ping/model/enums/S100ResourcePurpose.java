package fr.shom.ping.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum S100ResourcePurpose {
    DATASET("dataset"),
    FEATURE_CALATOG("featureCatalogue"),
    PORTRAYAL_CATALOG("portrayalCatalogue"),
    INTEROPERABILITY_CATALOG("interoperabilityCatalogue"),
    SUPPORT_FILE("supportFile"),
    PRODUCT_VERSION("productVersion"),
    PRODUCT_FAMILY("productFamily"),
    SOFTWARE("software"),
    SYSTEM("system"),
    EXCHANGE_CATALOG("exchangeCatalogue"),
    ISO_METADATA("ISOMetadata"),
    LANGUAGE_PACK("languagePack"),
    GML_SCHEMA("GMLSchema"),
    OTHER("other");

    private final String value;
}
