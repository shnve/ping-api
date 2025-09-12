package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.stream.Collectors.toSet;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
@Builder
@Schema(name = "Infosource Object Date Range DTO")
public class InfosourceObjectDateRangeDto implements Serializable, Comparable<InfosourceObjectDateRangeDto> {
    @Serial
    private static final long serialVersionUID = -4259390464232572422L;

    @JsonIgnore
    @Builder.Default
    private Integer position = 1;

    @NoHtml
    private String start;

    @NoHtml
    private String end;

    public static InfosourceObjectDateRangeDto fromFixedDateRange(FixedDateRange dateRange) {
        return InfosourceObjectDateRangeDto.builder()
            .position(dateRange.getPosition())
            .start(formatDateTime(dateRange.getDateStart(), dateRange.getTimeOfDayStart()))
            .end(formatDateTime(dateRange.getDateEnd(), dateRange.getTimeOfDayEnd()))
            .build();
    }

    public static SortedSet<FixedDateRange> getDateRanges(List<Map<String, String>> dateRangesMapList, String timezone) {
        if (dateRangesMapList == null) {
            return new TreeSet<>();
        }

        final SortedSet<InfosourceObjectDateRangeDto> dateRanges = new TreeSet<>();
        AtomicInteger dateRangePosition = new AtomicInteger(1);
        dateRangesMapList.forEach(dateRangesMap -> {
            final InfosourceObjectDateRangeDto dto = InfosourceObjectDateRangeDto.builder().position(dateRangePosition.getAndIncrement()).build();
            dateRangesMap.forEach((key, value) -> {
                if ("start".equals(key)) {
                    dto.setStart(value);
                } else {
                    dto.setEnd(value);
                }
            });
            dateRanges.add(dto);
        });
        return new TreeSet<>(dateRanges.stream().map(infosourceObjectDateRangeDto -> toFixedDateRange(infosourceObjectDateRangeDto, timezone)).collect(toSet()));
    }

    public static FixedDateRange toFixedDateRange(InfosourceObjectDateRangeDto dateRange, String timezone) {
        final FixedDateRange fixedDateRange = FixedDateRange.builder()
            .position(dateRange.getPosition())
            .timezone(timezone)
            .build();

        String rangeStart = dateRange.getStart();
        if (rangeStart != null) {
            if (rangeStart.length() > 10) {
                final LocalDateTime dateTimeStart = parseDateTime(rangeStart);
                final LocalDate dateStart = dateTimeStart.toLocalDate();
                final LocalTime timeOfDayStart = dateTimeStart.toLocalTime();
                fixedDateRange.setDateStart(dateStart);
                fixedDateRange.setTimeOfDayStart(timeOfDayStart);
            } else {
                fixedDateRange.setDateStart(parseDate(rangeStart));
            }
        }

        String rangeEnd = dateRange.getEnd();
        if (rangeEnd != null) {
            if (rangeEnd.length() > 10) {
                final LocalDateTime dateTimeEnd = parseDateTime(rangeEnd);
                final LocalDate dateEnd = dateTimeEnd.toLocalDate();
                final LocalTime timeOfDayEnd = dateTimeEnd.toLocalTime();
                fixedDateRange.setDateEnd(dateEnd);
                fixedDateRange.setTimeOfDayEnd(timeOfDayEnd);
            } else {
                fixedDateRange.setDateEnd(parseDate(dateRange.getEnd()));
            }
        }
        return fixedDateRange;
    }

    private static LocalDate parseDate(String date) {
        return LocalDate.parse(date);
    }

    private static LocalDateTime parseDateTime(String dateTime) {
        return LocalDateTime.parse(dateTime.replace(".000Z", "").replace("Z", ""));
    }

    private static String formatDateTime(LocalDate date, LocalTime time) {
        if (date == null) {
            return null;
        }
        String formatDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return time == null ? formatDate : formatDate + "T" + time.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + ".000Z";
    }

    @Override
    public int compareTo(InfosourceObjectDateRangeDto dateRangeDto) {
        return position.compareTo(dateRangeDto.getPosition());
    }
}
