package fr.shom.ping.utils;

import fr.shom.ping.PingApiTranslationsRegistry;
import fr.shom.ping.model.FeaturePart;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.GeneralArea;
import fr.shom.ping.model.LocalizedText;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.WarningLocalityNames;
import fr.shom.ping.model.WarningReference;
import fr.shom.ping.model.enums.WarningReferenceCategoryEnum;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static fr.shom.ping.PingApiTranslationsRegistry.isInternationalLanguage;
import static fr.shom.ping.utils.DateUtil.convertDateRangeToUtc;
import static fr.shom.ping.utils.PingStringUtil.indentMultilineText;
import static fr.shom.ping.utils.S124v2Util.getLocalitiesOrderedByGeneralAreas;
import static fr.shom.ping.utils.WarningUtil.generateUsualNumber;
import static java.util.Comparator.comparing;
import static java.util.Locale.ENGLISH;

import static org.apache.commons.lang3.StringUtils.leftPad;

@Component
public final class TelexUtil {

    @Autowired
    private PingApiTranslationsRegistry translationsRegistry;

    private static final String TELEX_LINE_SEPARATOR = "\n";
    private static final String START_DATE_END_DATE_SEPARATOR = " ";
    private static final int SPACES_FOR_FEATURE_INDENT = 1;

    private TelexUtil() {
    }

    public String generateTelex(final Warning warning) {
        return generateTelex(warning, null);
    }

    public String generateTelex(final Warning warning, final String usualNumberLink) {
        // for each serie mandatory language, generate matching telex
        return Stream.concat(
                warning.getSerie().getMandatoryLanguages().stream(),
                warning.getSerie().getOptionalLanguages().stream())
            .sorted(comparing(lang -> !"es".equalsIgnoreCase(lang)))
            .map(language -> generateTelex(warning, language, usualNumberLink))
            .map(StringUtils::stripAccents)
            .map(String::toUpperCase)
            .collect(Collectors.joining(TELEX_LINE_SEPARATOR + TELEX_LINE_SEPARATOR));
    }


    public String generateTelex(final Warning warning, final String lang, final String usualNumberLink) {
        final boolean internationalLanguage = isInternationalLanguage(lang);
        final AtomicInteger itemCounter = new AtomicInteger(1);
        return new StringBuilder()
            .append(generateUsualNumber(warning, internationalLanguage, usualNumberLink))
            .append(TELEX_LINE_SEPARATOR)
            .append(generateGeneralAreasAndLocalities(warning, internationalLanguage, lang))
            .append(TELEX_LINE_SEPARATOR)
            .append(translationsRegistry.getWarningHazardTypeGeneralByEnumNameAndLang(warning.getWarningHazardTypeGeneral().name(), lang))
            .append(TELEX_LINE_SEPARATOR)
            .append(generateFeatures(warning, internationalLanguage, lang, itemCounter))
            .append(generateCancelledWarnings(warning, internationalLanguage, itemCounter))
            .append(generateAutoCancellation(warning, internationalLanguage, itemCounter))
            .toString();
    }

    private String generateGeneralAreasAndLocalities(final Warning warning, final boolean internationalLanguage, final String lang) {
        final List<String> generalAreaNames = warning.getGeneralAreas().stream()
            .map(generalArea -> internationalLanguage && StringUtils.isNotBlank(generalArea.getInternationalName()) ? generalArea.getInternationalName()
                : generalArea.getName())
            .collect(Collectors.toList());

        final List<String> localityNames = new ArrayList<>();

        final Predicate<String> predicateEnglishLanguage = s -> ENGLISH.getLanguage().equals(s);

        final Predicate<WarningLocalityNames> predicateHasNoInternationalName = v -> Objects.isNull(v.getInternationalName());

        final BiFunction<WarningLocalityNames, String, String> getLangLocalizedText = (list, langToFilter) ->
            "es".equals(langToFilter) ? list.getName() : list.getInternationalName();

        getLocalitiesOrderedByGeneralAreas(warning)
            .forEach(value -> {
                // handle case when no international text is available
                if (internationalLanguage && predicateEnglishLanguage.test(lang) && predicateHasNoInternationalName.test(value)) {
                    localityNames.add(getLangLocalizedText.apply(value, "es"));
                } else {
                    localityNames.add(getLangLocalizedText.apply(value, lang));
                }
            });

        return Stream.of(generalAreaNames, localityNames).flatMap(Collection::stream).collect(Collectors.joining(" - "));
    }

    private static boolean checkIfTelexHasMoreThanOnePart(SortedSet<FeaturePart> featureParts, Set<WarningReference> cancelledWarnings,
        Instant cancellationDate) {
        return (cancellationDate != null && ((featureParts.size() + cancelledWarnings.size()) >= 1)) || (featureParts.size() + cancelledWarnings.size()) >= 2;
    }

    private String generateFeatures(final Warning warning, final boolean internationalLanguage, final String lang, AtomicInteger itemCounter) {
        final boolean hasMoreThanOnePart =
            checkIfTelexHasMoreThanOnePart(
                warning.getFeatureParts(),
                getCancelledWarnings(warning),
                warning.getCancellationDate()
            );

        return warning.getFeatureParts().stream()
            .map(featurePart -> generateFeature(featurePart, internationalLanguage, lang, hasMoreThanOnePart ? itemCounter.getAndIncrement() : 0))
            .collect(Collectors.joining(TELEX_LINE_SEPARATOR))
            + (warning.getFeatureParts().isEmpty() ? "" : TELEX_LINE_SEPARATOR);
    }

    private String generateFeature(final FeaturePart featurePart, final boolean internationalLanguage, final String lang, int position) {
        final boolean isIndentRequired = position > 0;
        final StringBuilder sbFeature = new StringBuilder(isIndentRequired ? position + ". " : "");   // TODO: compteur de feature

        final boolean hasWarningHazardTypeDetails = !featurePart.getWarningHazardTypeDetails().isEmpty();
        if (hasWarningHazardTypeDetails) {
            // add warning hazard type details
            final String warningHazardTypeDetailsLine = featurePart.getWarningHazardTypeDetails().stream()
                .map(Enum::name)
                .sorted()
                .map(enumName -> translationsRegistry.getWarningHazardTypeDetailsByEnumNameAndLang(enumName, lang))
                .collect(Collectors.joining(" - "));
            sbFeature.append(warningHazardTypeDetailsLine).append(TELEX_LINE_SEPARATOR);
        }

        final boolean hasDateRanges = !featurePart.getFixedDateRanges().isEmpty();
        if (hasDateRanges) {
            // add fixed date ranges
            final String dateRangesLines = featurePart.getFixedDateRanges().stream()
                .sorted(comparing(FixedDateRange::getPosition))
                .map(fixedDateRange -> generateFixedDateRange(fixedDateRange, internationalLanguage, position > 0))
                .collect(Collectors.joining(TELEX_LINE_SEPARATOR));
            sbFeature.append(hasWarningHazardTypeDetails || !isIndentRequired ? dateRangesLines : dateRangesLines.substring(SPACES_FOR_FEATURE_INDENT))
                .append(TELEX_LINE_SEPARATOR);
        }

        // add information
        final boolean noContentPreviouslyAdded = !hasWarningHazardTypeDetails && !hasDateRanges;
        final Optional<String> information = featurePart.getInformation().stream()
            .filter(localizedText -> localizedText.getLanguage().equals(lang))
            .map(LocalizedText::getText)
            .findFirst();
        if (information.isPresent()) {
            final String formattedInformation = isIndentRequired ? indentMultilineText(information.get(), SPACES_FOR_FEATURE_INDENT) : information.get();
            sbFeature.append(noContentPreviouslyAdded && formattedInformation.startsWith(" ") ? formattedInformation.substring(SPACES_FOR_FEATURE_INDENT) : formattedInformation);
        }

        return sbFeature.toString();
    }

    public static String generateFixedDateRange(final FixedDateRange fixedDateRange, final boolean internationalLanguage, final boolean isIndentRequired) {
        final String fixedDateRangeFormatted = formatFixedDateRange(fixedDateRange, internationalLanguage);
        return isIndentRequired ? leftPad(fixedDateRangeFormatted, SPACES_FOR_FEATURE_INDENT + fixedDateRangeFormatted.length()) : fixedDateRangeFormatted;
    }

    public static String formatFixedDateRange(final FixedDateRange fixedDateRange, final boolean internationalLanguage) {
        FixedDateRange dateRange = SerializationUtils.clone(fixedDateRange);
        convertDateRangeToUtc(dateRange);

        final Locale chosenLocale = internationalLanguage ? ENGLISH : Locale.forLanguageTag("es");
        final Predicate<FixedDateRange> hasStartTime = p -> p.getDateStart() != null && p.getTimeOfDayStart() != null;
        final Predicate<FixedDateRange> hasEndTime = p -> p.getDateEnd() != null && p.getTimeOfDayEnd() != null;
        final Predicate<FixedDateRange> hasStartDate = p -> p.getDateStart() != null;
        final Predicate<FixedDateRange> hasEndDate = p -> p.getDateEnd() != null;
        final Predicate<FixedDateRange> hasBothStartEndDates = p -> hasStartDate.and(hasEndDate).and(hasStartTime.negate()).and(hasEndTime.negate()).test(p);
        final Predicate<FixedDateRange> hasStartDateOnly =
            p -> hasStartDate.and(hasStartTime.negate()).and(hasEndDate.negate()).and(hasEndTime.negate()).test(p);
        final Predicate<FixedDateRange> hasEndDateOnly = p -> hasEndDate.and(hasEndTime.negate()).and(hasStartDate.negate()).and(hasStartTime.negate()).test(p);
        final Predicate<FixedDateRange> hasStartDateWithHourOnly = p -> hasStartTime.and(hasEndDate.negate()).and(hasEndTime.negate()).test(p);
        final Predicate<FixedDateRange> hasEndDateWithHourOnly = p -> hasEndTime.and(hasStartDate.negate()).and(hasStartTime.negate()).test(p);
        final Predicate<FixedDateRange> hasBothStartEndDatesWithHour = p -> hasStartTime.and(hasEndTime).test(p);
        final Predicate<FixedDateRange> hasStartDateTimeAndEndDateOnly = p -> hasStartDate.and(hasStartTime).and(hasEndDate).and(hasEndTime.negate()).test(p);
        final Predicate<FixedDateRange> hasStartDateOnlyAndEndDateTime = p -> hasStartDate.and(hasStartTime.negate()).and(hasEndDate).and(hasEndTime).test(p);

        final Function<LocalDate, String> formatDateOnly = p ->
            String.format(chosenLocale, " %1$td ", p)
                + String.format((p.getMonth() == Month.JUNE || p.getMonth() == Month.JULY ? ENGLISH : chosenLocale), "%1$tb", p).substring(0, 3)
                + String.format(chosenLocale, " %1$ty", p);

        final BiFunction<LocalDate, LocalTime, String> formatDateHour = getFormatterDateHour(chosenLocale);

        final String prefixForDateStartOnly = internationalLanguage ? "from" : "desde el";
        final String prefixForDateEndOnly = internationalLanguage ? "until" : "hasta el";
        final String prefixForStartDate = internationalLanguage ? "from" : "del";
        final String prefixForEndDate = START_DATE_END_DATE_SEPARATOR + (internationalLanguage ? "to" : "al");

        final Function<FixedDateRange, String> formatDateStartOnly = p -> prefixForDateStartOnly + formatDateOnly.apply(p.getDateStart());

        final Function<FixedDateRange, String> formatDateEndOnly = p -> prefixForDateEndOnly + formatDateOnly.apply(p.getDateEnd());

        final Function<FixedDateRange, String> formatDateStartWithHourOnly = p -> prefixForDateStartOnly
            + formatDateHour.apply(p.getDateStart(), p.getTimeOfDayStart());

        final Function<FixedDateRange, String> formatDateEndWithHourOnly = p -> prefixForDateEndOnly
            + formatDateHour.apply(p.getDateEnd(), p.getTimeOfDayEnd());

        final Function<FixedDateRange, String> formatStartDateOnlyAndEndDateOnly = p -> prefixForStartDate + formatDateOnly.apply(p.getDateStart())
            + prefixForEndDate + formatDateOnly.apply(p.getDateEnd());

        final Function<FixedDateRange, String> formatStartDateWithHourAndEndDateOnly = p -> prefixForStartDate
            + formatDateHour.apply(p.getDateStart(), p.getTimeOfDayStart())
            + prefixForEndDate + formatDateOnly.apply(p.getDateEnd());

        final Function<FixedDateRange, String> formatStartDateOnlyAndEndDateWithHour = p -> prefixForStartDate + formatDateOnly.apply(p.getDateStart())
            + prefixForEndDate + formatDateHour.apply(p.getDateEnd(), p.getTimeOfDayEnd());

        final Function<FixedDateRange, String> formatStartDateWithHourAndEndDateWithHour = p -> prefixForStartDate
            + formatDateHour.apply(p.getDateStart(), p.getTimeOfDayStart())
            + prefixForEndDate + formatDateHour.apply(p.getDateEnd(), p.getTimeOfDayEnd());

        if (hasStartDateOnly.test(dateRange)) {
            return formatDateStartOnly.apply(dateRange);
        }
        if (hasEndDateOnly.test(dateRange)) {
            return formatDateEndOnly.apply(dateRange);
        }
        if (hasStartDateWithHourOnly.test(dateRange)) {
            return formatDateStartWithHourOnly.apply(dateRange);
        }
        if (hasEndDateWithHourOnly.test(dateRange)) {
            return formatDateEndWithHourOnly.apply(dateRange);
        }
        if (hasStartDateTimeAndEndDateOnly.test(dateRange)) {
            return formatStartDateWithHourAndEndDateOnly.apply(dateRange);
        }
        if (hasStartDateOnlyAndEndDateTime.test(dateRange)) {
            return formatStartDateOnlyAndEndDateWithHour.apply(dateRange);
        }
        if (hasBothStartEndDates.test(dateRange)) {
            return formatStartDateOnlyAndEndDateOnly.apply(dateRange);
        }
        if (hasBothStartEndDatesWithHour.test(dateRange)) {
            return formatStartDateWithHourAndEndDateWithHour.apply(dateRange);
        }

        return dateRange.toString();
    }

    private Set<WarningReference> getCancelledWarnings(Warning warning) {
        final Predicate<WarningReference> isCancelledWarning = p -> WarningReferenceCategoryEnum.CANCELLATION.equals(p.getCategory());
        return warning.getWarningReferences().stream()
            .filter(isCancelledWarning).collect(Collectors.toSet());
    }

    private String generateCancelledWarnings(final Warning warning, final boolean internationalLanguage, AtomicInteger itemCounter) {
        final Set<WarningReference> cancelledWarnings = getCancelledWarnings(warning);
        final boolean hasMoreThanOnePart =
            checkIfTelexHasMoreThanOnePart(
                warning.getFeatureParts(),
                cancelledWarnings,
                warning.getCancellationDate()
            );

        return cancelledWarnings.stream()
            .map(WarningReference::getWarnings)
            .flatMap(Collection::stream)
            .map(w -> generateUsualNumber(w, internationalLanguage))
            .map(usualNumber -> formatCancelledUsualNumber(usualNumber, internationalLanguage, hasMoreThanOnePart ? itemCounter.getAndIncrement() : 0))
            .collect(Collectors.joining(TELEX_LINE_SEPARATOR))
            + (cancelledWarnings.isEmpty() ? "" : TELEX_LINE_SEPARATOR);
    }

    private String formatCancelledUsualNumber(final String usualNumber, final boolean internationalLanguage, int itemCounter) {
        final String prefix = internationalLanguage ? "CANCEL" : "CANCELA";
        final String format = itemCounter > 0 ? "%d. %s %s" : "%s %s";
        return itemCounter > 0 ? format.formatted(itemCounter, prefix, usualNumber) : format.formatted(prefix, usualNumber);
    }

    private String generateAutoCancellation(final Warning warning, final boolean internationalLanguage, AtomicInteger itemCounter) {
        if (warning.getCancellationDate() == null) {
            return "";
        }
        final String prefix = internationalLanguage ? "CANCEL THIS MSG" : "CANCELAR ESTE MENSAJE";
        final Locale chosenLocale = internationalLanguage ? ENGLISH : Locale.forLanguageTag("es");
        final BiFunction<LocalDate, LocalTime, String> formatDateHour = getFormatterDateHour(chosenLocale);
        final ZoneOffset zoneUtc = ZoneOffset.UTC;
        String formatCancellationDateTime = (!internationalLanguage && warning.isAddCancellationTimeToTelex() ? " EL" : "") +
            (warning.isAddCancellationTimeToTelex() ? formatDateHour.apply(LocalDate.ofInstant(warning.getCancellationDate(), zoneUtc),
                LocalTime.ofInstant(warning.getCancellationDate(), zoneUtc)) : "");
        return (itemCounter.get() > 1 ? (itemCounter.getAndIncrement() + ". ") : "") + prefix + formatCancellationDateTime;
    }

    private static BiFunction<LocalDate, LocalTime, String> getFormatterDateHour(Locale chosenLocale) {
        return (p1, p2) ->
            String.format(chosenLocale, " %1$td", p1)
                + String.format(chosenLocale, "%1$tH%1$tM UTC ", p2)
                + String.format((p1.getMonth() == Month.JUNE || p1.getMonth() == Month.JULY ? ENGLISH : chosenLocale), "%1$tb", p1).substring(0, 3)
                + String.format(chosenLocale, " %1$ty", p1);
    }
}
