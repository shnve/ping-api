package fr.shom.ping.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WarningHazardTypeGeneral {
    AIDS_TO_NAVIGATION(1, false),
    AQUACULTURE_AND_FISHING_INSTALLATIONS(2, false),
    DRIFTING_HAZARDS(3, false),
    ECDIS_OPERATING_ANOMALIES_INCLUDING_OFFICIAL_DATA_ISSUES(4, false),
    HAZARD_OTHER(5, false),
    HEALTH_ADVISORY(6, false),
    ICE(7, false),
    IN_FORCE_BULLETIN(8, true),
    NATURAL_PHENOMENA(9, false),
    NEWLY_DISCOVERED_DANGERS(10, false),
    OFFSHORE_STRUCTURES(11, false),
    PIRACY(12, false),
    RADIO_SERVICES(13, false),
    ROUTING_INFORMATION(14, false),
    SCIENTIFIC_INSTRUMENTS(15, false),
    SECURITY(16, false),
    SPECIAL_OPERATIONS(17, false),
    TOWING_OPERATIONS(18, false),
    WORKS(19, false),
    RIG_LIST(20, false);

    private final int s124Code;
    private final boolean isBulletin;
}
