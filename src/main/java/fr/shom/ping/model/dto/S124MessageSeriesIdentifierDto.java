package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
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
@JsonPropertyOrder({ "country", "nameOfSeries", "productionAgency", "warningNumber", "warningType", "year" })
public class S124MessageSeriesIdentifierDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -5414201779478383396L;

    @JacksonXmlProperty(localName = "country")
    private String country;

    @JacksonXmlProperty(localName = "nameOfSeries")
    private String nameOfSeries;

    @JacksonXmlProperty(localName = "productionAgency")
    private String productionAgency;

    @JacksonXmlProperty(localName = "warningNumber")
    private Integer warningNumber;

    @JacksonXmlProperty(localName = "warningType")
    private WarningType warningType;

    @JacksonXmlProperty(localName = "year")
    private Integer year;
}
