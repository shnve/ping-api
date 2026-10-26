package fr.shom.ping.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Arrays;

import static java.util.Arrays.stream;

@Getter
@AllArgsConstructor
public enum WarningType {
    // Warning
    LOCAL_NAVIGATIONAL_WARNING(1, SerieType.LOCAL_NAVIGATIONAL_WARNING, false),
    COASTAL_NAVIGATIONAL_WARNING(2, SerieType.COASTAL_NAVIGATIONAL_WARNING, false),
    SUB_AREA_NAVIGATIONAL_WARNING(3, SerieType.SUB_AREA_NAVIGATIONAL_WARNING, false),
    NAVAREA_NAVIGATIONAL_WARNING(4, SerieType.NAVAREA_NAVIGATIONAL_WARNING, false),
    NAVAREA_IN_FORCE_BULLETIN(9, SerieType.NAVAREA_NAVIGATIONAL_WARNING, true),
    SUB_AREA_IN_FORCE_BULLETIN(10, SerieType.SUB_AREA_NAVIGATIONAL_WARNING, true),
    COASTAL_IN_FORCE_BULLETIN(11, SerieType.COASTAL_NAVIGATIONAL_WARNING, true),
    LOCAL_IN_FORCE_BULLETIN(12, SerieType.LOCAL_NAVIGATIONAL_WARNING, true),

    // Notice
    LOCAL_NOTICE_TO_MARINER(180, SerieType.LOCAL_NOTICE_TO_MARINER, false),
    LOCAL_NOTICE_TO_MARINER_IN_FORCE_BULLETIN(181, SerieType.LOCAL_NOTICE_TO_MARINER, true),
    PORT_NOTICE_TO_MARINER(190, SerieType.PORT_NOTICE_TO_MARINER, false),
    PORT_NOTICE_TO_MARINER_IN_FORCE_BULLETIN(191, SerieType.PORT_NOTICE_TO_MARINER, true),

    // Notice TP
    TEMPORARY_NOTICE_TO_MARINER(13, SerieType.TEMPORARY_NOTICE_TO_MARINER, false),
    PRELIMINARY_NOTICE_TO_MARINER(14, SerieType.PRELIMINARY_NOTICE_TO_MARINER, false),
    TEMPORARY_AND_PRELIMINARY_NOTICE_TO_MARINER_IN_FORCE_BULLETIN(17, SerieType.TEMPORARY_AND_PRELIMINARY_NOTICE_TO_MARINER, true),
    TEMPORARY_AND_PRELIMINARY_NOTICE_TO_MARINER(200, SerieType.TEMPORARY_AND_PRELIMINARY_NOTICE_TO_MARINER, false);

    private final int code;
    private final SerieType serieType;
    private final boolean isBulletin;

    public static WarningType fromSerieTypeAndWarningHazardTypeGeneral(SerieType serieType, WarningHazardTypeGeneral typeGeneral) {
        System.out.println("DEBUG WARNINGTYPE: serieType=" + serieType + ", typeGeneral=" + typeGeneral);
        return stream(WarningType.values())
            .filter(type -> type.isBulletin() == typeGeneral.isBulletin())
            .filter(type -> serieType.name().equals(type.getSerieType().name()))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Incompatible SerieType (" + serieType + ") and WarningHazardTypeGeneral (" + typeGeneral + ") combination"));
    }
}
