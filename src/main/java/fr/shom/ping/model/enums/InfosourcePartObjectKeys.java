package fr.shom.ping.model.enums;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InfosourcePartObjectKeys {

    MARKUP_TYPE("markupType"),
    OBSERVATION_DATE("observationDate"),
    OBJECT_NAME("objectName"),
    MAP_NUMBER("mapNumber"),
    EDITION_NUMBER("editionNumber"),
    LAST_PATCH_NUMBER("lastPatchNumber"),
    DATE_RANGES("dateRanges"),
    ISSUE_REFERENCE("issueReference"),
    UP_TO_DATE_AT("upToDateAt"),
    CHAPTER_OR_PAGE("chapterOrPage"),
    OBJECT_FUTURE_INFO("objectFutureInfo"),
    INFORMATION("information"),
    GEOREGULATION_NUMBER("georegulationNumber"),
    GEOREGULATION_ADDITIONAL_NUMBERS("georegulationAdditionalNumbers"),
    GEOREGULATION_TEMPORARY("georegulationTemporary"),
    GEOREGULATION_CALENDAR_PERIODS("georegulationCalendarPeriods"),
    GEOREGULATION_SEASONAL_PERIODS("georegulationSeasonalPeriods"),
    GEOREGULATION_TYPE("georegulationType"),
    GEOREGULATION_ACTIVITIES("georegulationActivities"),
    GEOREGULATION_TITLE("georegulationTitle"),
    GEOREGULATION_MARITIME_ENTITY("georegulationMaritimeEntity"),
    GEOREGULATION_SUMMARY("georegulationSummary"),
    GEOREGULATION_PART_DETAILS("georegulationPartDetails"),
    GEOREGULATION_PART_NUMBER("georegulationPartNumber"),
    GEOREGULATION_PART_TYPES("georegulationPartTypes"),
    GEOREGULATION_PART_INFORMATION("georegulationPartInformation"),
    GEOREGULATION_PART_INTERNATIONAL_INFORMATION("georegulationPartInternationalInformation");

    private String keyName;

    public static Optional<InfosourcePartObjectKeys> fromKeyName(String keyName) {
        return Arrays.stream(values())
            .filter(e -> e.keyName.equalsIgnoreCase(keyName))
            .findFirst();
    }
}
