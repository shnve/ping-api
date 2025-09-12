package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
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
@JsonPropertyOrder({"S124:dateEnd", "S124:timeOfDayEnd", "S124:dateStart", "S124:timeOfDayStart"})
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

    @ToString
    @EqualsAndHashCode
    @Builder
    @JsonInclude(NON_NULL)
    public static class S124Time implements Serializable {
        @Serial
        private static final long serialVersionUID = -3167999324937647628L;

        @JsonFormat(pattern = "HH:mm:ssXXX", timezone = "UTC")
        @JacksonXmlProperty(localName = "S100:Time")
        private ZonedDateTime time;

        public static S124Time buildTime(ZonedDateTime time) {
            return S124Time.builder().time(time).build();
        }
    }
}
