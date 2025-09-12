package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import fr.shom.ping.model.enums.WarningType;
import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@ToString
@EqualsAndHashCode
@Builder
@JsonInclude(NON_NULL)
@JsonPropertyOrder({"S124:agencyResponsibleForProduction", "S124:countryName", "S124:nameOfSeries", "S124:warningNumber", "S124:warningType", "S124:year"})
public class S124v2MessageSeriesIdentifierDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -7123684198633048450L;

    @JacksonXmlProperty(localName = "S124:countryName")
    private String country;

    @JacksonXmlProperty(localName = "S124:nameOfSeries")
    private String nameOfSeries;

    @JacksonXmlProperty(localName = "S124:agencyResponsibleForProduction")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String productionAgency;

    @JacksonXmlProperty(localName = "S124:warningNumber")
    private Integer warningNumber;

    @JacksonXmlProperty(localName = "S124:warningType")
    private S124WarningType warningType;

    @JacksonXmlProperty(localName = "S124:year")
    private Integer year;

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class S124WarningType implements Serializable {

        @Serial
        private static final long serialVersionUID = -5442269393663034012L;

        @JacksonXmlProperty(isAttribute = true, localName = "code")
        private int code;

        @JacksonXmlText
        private String value;

        public static S124WarningType from(WarningType warningType) {
            return S124WarningType.builder().code(warningType.getCode())
                .build();
        }
    }
}
