package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@ToString
@EqualsAndHashCode
@Builder
@JsonInclude(NON_NULL)
public class S124FixedDateRange implements Serializable {
    @Serial
    private static final long serialVersionUID = -5999215285529393918L;

    @JacksonXmlProperty(localName = "timeOfDayStart")
    private LocalTime timeOfDayStart;

    @JacksonXmlProperty(localName = "timeOfDayEnd")
    private LocalTime timeOfDayEnd;

    @JacksonXmlProperty(localName = "dateStart")
    private S124Date dateStart;

    @JacksonXmlProperty(localName = "dateEnd")
    private S124Date dateEnd;

    @ToString
    @EqualsAndHashCode
    @Builder
    @JsonInclude(NON_NULL)
    public static class S124Date implements Serializable {
        @Serial
        private static final long serialVersionUID = -3167999324937647628L;

        @JacksonXmlProperty(localName = "date")
        private LocalDate date;
    }
}
