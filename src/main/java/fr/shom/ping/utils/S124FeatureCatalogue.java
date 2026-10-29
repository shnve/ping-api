package fr.shom.ping.utils;

import fr.shom.ping.model.enums.WarningHazardTypeDetails;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Normative enumeration labels of the IHO S-124 Edition 2.0.0 Feature Catalogue.
 * <p>
 * In the S-124 2.0.0 XSD, coded attributes ({@code warningType}, {@code navwarnTypeGeneral}, {@code navwarnTypeDetails},
 * {@code referenceCategory}) are simple-content elements whose text MUST be one of the enumerated English labels; the
 * {@code code} attribute alone is not enough. Labels are loaded from {@code s124/fc-2.0.0-labels.properties}, generated
 * from the official 124_FC_2.0.0.xml. They must never be translated.
 */
@Slf4j
public final class S124FeatureCatalogue {

    public static final String PRODUCT_EDITION = "2.0.0";
    public static final String NAMESPACE = "http://www.iho.int/S124/gml/2.0";

    public static final String WARNING_TYPE = "warningType";
    public static final String NAVWARN_TYPE_GENERAL = "navwarnTypeGeneral";
    public static final String NAVWARN_TYPE_DETAILS = "navwarnTypeDetails";
    public static final String REFERENCE_CATEGORY = "referenceCategory";

    private static final String LABELS_RESOURCE = "s124/fc-2.0.0-labels.properties";
    private static final Properties LABELS = new Properties();

    static {
        try (InputStream is = S124FeatureCatalogue.class.getClassLoader().getResourceAsStream(LABELS_RESOURCE)) {
            if (is == null) {
                throw new IllegalStateException("Missing resource " + LABELS_RESOURCE);
            }
            LABELS.load(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load " + LABELS_RESOURCE, e);
        }
    }

    private S124FeatureCatalogue() {
    }

    /**
     * @return the normative label for the given attribute and code
     * @throws IllegalArgumentException if the code does not exist in the S-124 2.0.0 Feature Catalogue
     */
    public static String label(String attribute, int code) {
        final String label = LABELS.getProperty(attribute + "." + code);
        if (label == null) {
            throw new IllegalArgumentException("Code " + code + " of " + attribute + " does not exist in S-124 " + PRODUCT_EDITION
                + " Feature Catalogue");
        }
        return label;
    }

    /**
     * Checks that every internal hazard code has a label in the Feature Catalogue.
     *
     * @return the list of inconsistencies (empty when everything is consistent)
     */
    public static List<String> checkCoverage() {
        final List<String> errors = new ArrayList<>();
        for (WarningHazardTypeGeneral type : WarningHazardTypeGeneral.values()) {
            if (LABELS.getProperty(NAVWARN_TYPE_GENERAL + "." + type.getS124Code()) == null) {
                errors.add(NAVWARN_TYPE_GENERAL + " " + type + " (" + type.getS124Code() + ")");
            }
        }
        for (WarningHazardTypeDetails type : WarningHazardTypeDetails.values()) {
            if (LABELS.getProperty(NAVWARN_TYPE_DETAILS + "." + type.getS124Code()) == null) {
                errors.add(NAVWARN_TYPE_DETAILS + " " + type + " (" + type.getS124Code() + ")");
            }
        }
        return errors;
    }
}
