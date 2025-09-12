package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import java.io.Serial;
import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@ToString
@EqualsAndHashCode
@SuperBuilder
@JsonInclude(NON_NULL)
public abstract class S124NWPartBaseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 7372978155948089335L;

    @JacksonXmlProperty(isAttribute = true, localName = "gml:id")
    protected String gmlId;

    @Data
    @ToString
    @EqualsAndHashCode
    @SuperBuilder
    public static class S124PreambleLink implements Serializable {
        @Serial
        private static final long serialVersionUID = -3114665602577964884L;

        @JacksonXmlProperty(isAttribute = true, localName = "xlink:href")
        private String href;
    }
}
