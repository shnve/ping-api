package fr.shom.ping.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum S124ReferenceCategoryCode {
    WARNING_CANCELLATION("1", "Cancellation", "Warning Cancellation"),
    WARNING_REFERENCE("2", "Warning Reference", "Warning Reference"),
    IN_FORCE("3", "In-Force", "In-Force");

    private final String code;
    /** Internal label, also used to build gml:id values (must stay a valid xs:ID, i.e. without spaces). */
    private final String label;
    /** Normative S-124 2.0.0 Feature Catalogue label (referenceCategory element text). */
    private final String fcLabel;
}
