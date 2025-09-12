package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Builder
@Data
@JsonPropertyOrder({"S124:messageSeriesIdentifier", "S124:noMessageOnHand", "S124:referenceCategory"})
public class S124v2NWReferencesDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -2611597124855465249L;

    @JacksonXmlProperty(isAttribute = true, localName = "gml:id")
    private String gmlId;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S124:messageSeriesIdentifier")
    @Builder.Default
    private List<S124v2MessageSeriesIdentifierDto> messageSeries = new ArrayList<>();

    @JacksonXmlProperty(localName = "S124:noMessageOnHand")
    private Boolean noMessageOnHand;

    @JacksonXmlProperty(localName = "S124:referenceCategory")
    private S124ReferenceCategory referenceCategory;

    @JacksonXmlProperty(localName = "S124:theWarning")
    private S124Link theWarning;

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class S124ReferenceCategory implements Serializable {
        @Serial
        private static final long serialVersionUID = -5682656944630216411L;

        @JacksonXmlProperty(isAttribute = true, localName = "code")
        private String code;

        @JacksonXmlText
        private String value;
    }
}
