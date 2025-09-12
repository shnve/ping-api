package fr.shom.ping.model.dto;

import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.InfosourcePart;
import fr.shom.ping.model.enums.InfosourcePartObjectKeys;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidTimezone;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static fr.shom.ping.model.enums.InfosourceNature.ACTIVITY_FORECAST;
import static fr.shom.ping.validation.ValidTimezoneValidator.convertToUtcHoursAndMinutes;
import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Infosource Create Dto")
public abstract class AbstractInfosourceActivityForecastCreateDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -1154191015302632263L;

    @NotEmpty(message = "validation.infosource.activity-forecast.timezone.not-empty")
    @NoHtml
    @ValidTimezone(message = "validation.infosource.activity-forecast.timezone.invalid")
    private String timezone;

    @Builder.Default
    @NotEmpty(message = "validation.infosource.activity-forecast.parts.not-empty")
    protected List<@Valid InfosourceActivityForecastPartCreateDto> parts = new ArrayList<>();

    @Builder.Default
    protected List<UUID> attachmentIds = new ArrayList<>();

    public AbstractInfosourceActivityForecastCreateDto(Infosource infosource) {
        this.timezone = infosource.getTimezone();
        this.parts = infosource.getInfosourceParts().stream()
            .map(InfosourceActivityForecastPartCreateDto::fromInfosourcePart)
            .collect(toList());
        this.attachmentIds = infosource.getAttachments().stream().map(Attachment::getId).collect(Collectors.toList());
    }

    public Infosource.InfosourceBuilder toInfosourceBuilder(List<Attachment> attachments) {
        return Infosource.builder()
            .nature(ACTIVITY_FORECAST)
            .timezone(timezone)
            .attachments(new HashSet<>(attachments))
            .infosourceParts(new TreeSet<>(getInfosourceParts(this)));
    }

    protected static List<InfosourcePart> getInfosourceParts(AbstractInfosourceActivityForecastCreateDto dto) {
        final AtomicInteger partPosition = new AtomicInteger(1);
        final List<InfosourcePart> infosourceParts = dto.getParts().stream()
            .map(infosourcePartDto -> {
                final Map<String, Object> object = new HashMap<>();
                AtomicInteger dateRangePosition = new AtomicInteger(1);
                List<FixedDateRange> dateRanges = infosourcePartDto.getDateRanges();
                dateRanges.forEach(dateRange -> {
                    dateRange.setPosition(dateRangePosition.getAndIncrement());
                });

                if (infosourcePartDto.getInformation() != null) {
                    object.put(InfosourcePartObjectKeys.INFORMATION.getKeyName(), infosourcePartDto.getInformation());
                }

                if (dateRanges != null) {
                    object.put(InfosourcePartObjectKeys.DATE_RANGES.getKeyName(),
                        new TreeSet<>(dateRanges.stream().map(InfosourceObjectDateRangeDto::fromFixedDateRange).collect(toSet())));
                }

                return InfosourcePart.builder()
                    .position(partPosition.getAndIncrement())
                    .localisation(infosourcePartDto.getLocalisation().stream().map(FeatureDto::toFeature).collect(Collectors.toSet()))
                    .description(infosourcePartDto.getDescription())
                    .internationalDescription(infosourcePartDto.getInternationalDescription())
                    .object(object)
                    .build();
            })
            .collect(toList());

        return infosourceParts;
    }

    protected static void convertDateStartToUtc(String timezone, FixedDateRange dateRange) {
        final LocalTime timeOfDayStart = Optional.ofNullable(dateRange.getTimeOfDayStart()).orElse(LocalTime.of(0, 0, 0));
        final ZonedDateTime dateTimeStart = ZonedDateTime.of(dateRange.getDateStart(), timeOfDayStart, ZoneId.of(convertToUtcHoursAndMinutes(timezone)));
        final ZonedDateTime dateTimeStartToUtc = dateTimeStart.withZoneSameInstant(UTC);
        dateRange.setDateStart(dateTimeStartToUtc.toLocalDate());
        dateRange.setTimeOfDayStart(dateTimeStartToUtc.toLocalTime());
    }

    protected static void convertDateEndToUtc(String timezone, FixedDateRange dateRange) {
        final LocalTime timeOfDayEnd = Optional.ofNullable(dateRange.getTimeOfDayEnd()).orElse(LocalTime.of(23, 59, 59));
        final ZonedDateTime dateTimeEnd = ZonedDateTime.of(dateRange.getDateEnd(), timeOfDayEnd, ZoneId.of(convertToUtcHoursAndMinutes(timezone)));
        final ZonedDateTime dateTimeEndToUtc = dateTimeEnd.withZoneSameInstant(UTC);
        dateRange.setDateEnd(dateTimeEndToUtc.toLocalDate());
        dateRange.setTimeOfDayEnd(dateTimeEndToUtc.toLocalTime());
    }
}
