package fr.shom.ping.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum S100TopicCategoryCode {
    FARMING("farming"),
    BIOTA("biota"),
    BOUNDARIES("boundaries"),
    CLIMATOLOGY_METEOROLOGY_ATMOSPHERE("climatologyMeteorologyAtmosphere"),
    ECONOMY("economy"),
    ELEVATION("elevation"),
    ENVIRONMENT("environment"),
    GEOSCIENTIFIC_INFORMATION("geoscientificInformation"),
    HEALTH("health"),
    IMAGERY_BASE_MAPS_EARTH_COVER("imageryBaseMapsEarthCover"),
    INTELLIGENCE_MILITARY("intelligenceMilitary"),
    INLAND_WATERS("inlandWaters"),
    LOCATION("location"),
    OCEANS("oceans"),
    PLANNING_CADASTRE("planningCadastre"),
    SOCIETY("society"),
    STRUCTURE("structure"),
    TRANSPORTATION("transportation"),
    UTILITIES_COMMUNICATION("utilitiesCommunication");

    private final String code;
}
