package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Builder
@JsonPropertyOrder({"S124:localityIdentifier", "S124:locationName"})
public class S124v2SubAreaDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5256067724758657705L;

    @JacksonXmlProperty(localName = "S124:localityIdentifier")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String localityIdentifier;

    @JacksonXmlProperty(localName = "S124:locationName")
    private LocationName locationName;

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class LocationName implements Serializable {
        @Serial
        private static final long serialVersionUID = -5318650664700593059L;

        @JacksonXmlProperty(localName = "S124:language")
        private String language;

        @JacksonXmlProperty(localName = "S124:text")
        private String text;
    }
}
