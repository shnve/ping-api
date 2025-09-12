package fr.shom.ping.model.enums;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static fr.shom.ping.model.enums.GlobalWarningType.NOTICE;
import static fr.shom.ping.model.enums.GlobalWarningType.NOTICE_TP;
import static fr.shom.ping.model.enums.GlobalWarningType.WARNING;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

@Getter
@AllArgsConstructor
public enum SerieType {
    LOCAL_NAVIGATIONAL_WARNING(1, 4, WARNING),
    COASTAL_NAVIGATIONAL_WARNING(2, 3, WARNING),
    SUB_AREA_NAVIGATIONAL_WARNING(3, 2, WARNING),
    NAVAREA_NAVIGATIONAL_WARNING(4, 1, WARNING),

    LOCAL_NOTICE_TO_MARINER(180, 5, NOTICE),
    PORT_NOTICE_TO_MARINER(190, 6, NOTICE),

    TEMPORARY_NOTICE_TO_MARINER(13, null, NOTICE_TP),
    PRELIMINARY_NOTICE_TO_MARINER(14, null, NOTICE_TP),
    TEMPORARY_AND_PRELIMINARY_NOTICE_TO_MARINER(200, 7, NOTICE_TP);

    private final int code;
    private final Integer order;
    private final GlobalWarningType globalWarningType;


    public static Set<SerieType> fromGlobalWarningType(Set<GlobalWarningType> globalWarningType) {
        return stream(values())
            .filter(serieType -> globalWarningType.contains(serieType.getGlobalWarningType()))
            .collect(toSet());
    }
}
