package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@ToString
@EqualsAndHashCode
@Builder
@JsonInclude(NON_NULL)
// Element order imposed by S-124 2.0.0 fixedDateRangeType (xs:sequence)
@JsonPropertyOrder({"S124:dateEnd", "S124:dateStart", "S124:timeOfDayEnd", "S124:timeOfDayStart"})
public class S124v2FixedDateRange implements Serializable {
    @Serial
    private static final long serialVersionUID = -6860502307078459681L;

    @JacksonXmlProperty(localName = "S124:dateStart")
    private S124Date dateStart;

    @JacksonXmlProperty(localName = "S124:timeOfDayStart")
    private S124Time timeOfDayStart;

    @JacksonXmlProperty(localName = "S124:dateEnd")
    private S124Date dateEnd;

    @JacksonXmlProperty(localName = "S124:timeOfDayEnd")
    private S124Time timeOfDayEnd;

    @ToString
    @EqualsAndHashCode
    @Builder
    @JsonInclude(NON_NULL)
    public static class S124Date implements Serializable {
        @Serial
        private static final long serialVersionUID = -3167999324937647628L;

        @DateTimeFormat(iso = DATE)
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
        @JacksonXmlProperty(localName = "S100:date")
        private LocalDate date;

        public static S124Date buildDate(LocalDate date) {
            return S124Date.builder().date(date).build();
        }
    }

    /**
     * S-124 2.0.0 timeOfDayStart / timeOfDayEnd are simple xs:time values (e.g. {@code 18:00:00Z}), not complex elements.
     */
    @ToString
    @EqualsAndHashCode
    @Builder
    @JsonInclude(NON_NULL)
    public static class S124Time implements Serializable {
        @Serial
        private static final long serialVersionUID = -3167999324937647628L;

        private static final DateTimeFormatter XS_TIME_UTC = DateTimeFormatter.ofPattern("HH:mm:ssXXX");

        private ZonedDateTime time;

        public static S124Time buildTime(ZonedDateTime time) {
            return S124Time.builder().time(time).build();
        }

        @JsonValue
        public String toXsTime() {
            return time == null ? null : XS_TIME_UTC.format(time.withZoneSameInstant(ZoneOffset.UTC));
        }
    }
}
