package fr.shom.ping.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Builder
public class S124SubAreaDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -4402189245040443601L;

    @JacksonXmlProperty(localName = "locationName")
    private LocationName locationName;

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class LocationName implements Serializable {
        @Serial
        private static final long serialVersionUID = -5318650664700593059L;

        @JacksonXmlProperty(localName = "text")
        private String text;
    }
}
