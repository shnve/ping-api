package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@ToString
@EqualsAndHashCode
@Builder
@JsonInclude(NON_NULL)
public class S124NWAreaAffectedDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -4825557432735903072L;

    @JacksonXmlProperty(isAttribute = true, localName = "gml:id")
    private String gmlId;

    @JacksonXmlProperty(localName = "S124:impacts")
    private S124NWPartLink impacts;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S124:geometry")
    @Builder.Default
    private List<S124v2Geometry> geometries = new ArrayList<>();

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class S124NWPartLink implements Serializable {
        @Serial
        private static final long serialVersionUID = 1731403062130927756L;

        @JacksonXmlProperty(isAttribute = true, localName = "xlink:href")
        private String href;
    }
}
