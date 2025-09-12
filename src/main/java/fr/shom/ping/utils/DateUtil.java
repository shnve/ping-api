package fr.shom.ping.utils;

import fr.shom.ping.model.FixedDateRange;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static fr.shom.ping.validation.ValidTimezoneValidator.convertToUtcHoursAndMinutes;
import static java.time.ZoneOffset.UTC;

public class DateUtil {
    private DateUtil() {}

    public static void convertDateRangeToUtc(FixedDateRange dateRange) {
        if (dateRange.getTimeOfDayStart() != null) {
            LocalTime timeOfDayStart = dateRange.getTimeOfDayStart();
            ZonedDateTime dateTimeStart = ZonedDateTime.of(dateRange.getDateStart(), timeOfDayStart, ZoneId.of(convertToUtcHoursAndMinutes(dateRange.getTimezone())));
            ZonedDateTime dateTimeStartToUtc = dateTimeStart.withZoneSameInstant(UTC);
            dateRange.setDateStart(dateTimeStartToUtc.toLocalDate());
            dateRange.setTimeOfDayStart(dateTimeStartToUtc.toLocalTime());
        }

        if (dateRange.getTimeOfDayEnd() != null) {
            LocalTime timeOfDayEnd = dateRange.getTimeOfDayEnd();
            ZonedDateTime dateTimeEnd = ZonedDateTime.of(dateRange.getDateEnd(), timeOfDayEnd, ZoneId.of(convertToUtcHoursAndMinutes(dateRange.getTimezone())));
            ZonedDateTime dateTimeEndToUtc = dateTimeEnd.withZoneSameInstant(UTC);
            dateRange.setDateEnd(dateTimeEndToUtc.toLocalDate());
            dateRange.setTimeOfDayEnd(dateTimeEndToUtc.toLocalTime());
        }
    }
}
