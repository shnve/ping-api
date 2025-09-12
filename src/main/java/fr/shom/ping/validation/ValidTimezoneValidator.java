package fr.shom.ping.validation;

import fr.shom.ping.exception.BadRequestException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.TimeZone;
import java.util.regex.Pattern;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import static java.util.Arrays.asList;

public class ValidTimezoneValidator implements ConstraintValidator<ValidTimezone, String> {
    private static final String UTC_PATTERN_REGEX = "^UTC[+-](\\d{1,2}(?:\\.\\d{1,2})?|\\.\\d{1,2})$";
    private final static Pattern UTC_PATTERN = Pattern.compile(UTC_PATTERN_REGEX);

    @Override
    public void initialize(ValidTimezone constraint) {
        // Default initialize override
    }

    /**
     * Convert the timezone to UTC hours and minutes
     *
     * @param timezone the timezone
     * @return the timezone in UTC hours and minutes
     *
     * @example "UTC+2" -> "UTC+2", "UTC-3.5" -> "UTC-03:30", "UTC-3.75" -> "UTC-03:45", "UTC-3.125" -> "UTC-03:07"
     */
    public static String convertToUtcHoursAndMinutes(String timezone) {
        String[] timezoneSplitted = timezone.split("\\.");
        if (timezoneSplitted.length == 1) {
            return timezoneSplitted[0];
        }

        return (timezone.substring(0, 4)) + StringUtils.leftPad(timezoneSplitted[0].substring(4), 2, "0").concat(":" + (int) (Double.parseDouble(("0.").concat(timezoneSplitted[1])) * 60));
    }

    public static void checkIfUtcPatternIsvalid(String timezone) {
        if (!UTC_PATTERN.matcher(timezone).matches()) {
            throw new BadRequestException("validation.timezone.pattern.invalid");
        }
    }

    public boolean isValid(String timezone, ConstraintValidatorContext context) {
        if (timezone == null) {
            return true;
        }

        checkIfUtcPatternIsvalid(timezone);

        final String zoneIdOfTimezone;
        try {
            zoneIdOfTimezone = String.valueOf(ZoneId.of(convertToUtcHoursAndMinutes(timezone)));
        } catch (DateTimeException dateTimeException) {
            throw new BadRequestException("validation.timezone.invalid");
        }

        return asList(TimeZone.getAvailableIDs()).contains(TimeZone.getTimeZone(zoneIdOfTimezone).getID());
    }
}
