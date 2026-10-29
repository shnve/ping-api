package fr.shom.ping.model.enums;

import fr.shom.ping.model.Warning;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

import static fr.shom.ping.model.enums.GlobalWarningType.WARNING;

/**
 * IHO S-124 Edition 2.0.0 {@code warningType} enumeration (Feature Catalogue 124_FC_2.0.0).
 * <p>
 * S-124 only encodes Navigational Warnings (S-53). Internal PING notice types (local/port notices to mariners and T&amp;P
 * notices) are national products managed outside S-124 and therefore have no mapping here (Scenario B: they are kept in
 * the system but never exported as S-124).
 */
@Getter
@AllArgsConstructor
public enum S124v2OhiWarningType {
    LOCAL_NAVIGATIONAL_WARNING(1, "Local Navigational Warning"),
    COASTAL_NAVIGATIONAL_WARNING(2, "Coastal Navigational Warning"),
    SUB_AREA_NAVIGATIONAL_WARNING(3, "Sub-Area Navigational Warning"),
    NAVAREA_NAVIGATIONAL_WARNING(4, "NAVAREA Navigational Warning"),
    NAVAREA_NO_WARNING(5, "NAVAREA No Warning"),
    SUB_AREA_NO_WARNING(6, "Sub-Area No Warning"),
    COASTAL_NO_WARNING(7, "Coastal No Warning"),
    LOCAL_NO_WARNING(8, "Local No Warning"),
    NAVAREA_IN_FORCE_BULLETIN(9, "NAVAREA In-Force Bulletin"),
    SUB_AREA_IN_FORCE_BULLETIN(10, "Sub-Area In-Force Bulletin"),
    COASTAL_IN_FORCE_BULLETIN(11, "Coastal In-Force Bulletin"),
    LOCAL_IN_FORCE_BULLETIN(12, "Local In-Force Bulletin");

    private final int code;
    private final String label;

    /**
     * Maps an internal PING warning type to its S-124 2.0.0 equivalent.
     *
     * @return the OHI warning type, or empty when the internal type is not a Navigational Warning (notices, T&amp;P)
     */
    public static Optional<S124v2OhiWarningType> fromInternal(WarningType warningType) {
        if (warningType == null) {
            return Optional.empty();
        }
        return switch (warningType) {
            case LOCAL_NAVIGATIONAL_WARNING -> Optional.of(LOCAL_NAVIGATIONAL_WARNING);
            case COASTAL_NAVIGATIONAL_WARNING -> Optional.of(COASTAL_NAVIGATIONAL_WARNING);
            case SUB_AREA_NAVIGATIONAL_WARNING -> Optional.of(SUB_AREA_NAVIGATIONAL_WARNING);
            case NAVAREA_NAVIGATIONAL_WARNING -> Optional.of(NAVAREA_NAVIGATIONAL_WARNING);
            case NAVAREA_IN_FORCE_BULLETIN -> Optional.of(NAVAREA_IN_FORCE_BULLETIN);
            case SUB_AREA_IN_FORCE_BULLETIN -> Optional.of(SUB_AREA_IN_FORCE_BULLETIN);
            case COASTAL_IN_FORCE_BULLETIN -> Optional.of(COASTAL_IN_FORCE_BULLETIN);
            case LOCAL_IN_FORCE_BULLETIN -> Optional.of(LOCAL_IN_FORCE_BULLETIN);
            default -> Optional.empty();
        };
    }

    /**
     * A warning can be exported as S-124 only if its serie belongs to the Navigational Warning group.
     */
    public static boolean isS124Exportable(Warning warning) {
        return warning != null
            && warning.getSerie() != null
            && warning.getSerie().getType() != null
            && warning.getSerie().getType().getGlobalWarningType() == WARNING;
    }
}
