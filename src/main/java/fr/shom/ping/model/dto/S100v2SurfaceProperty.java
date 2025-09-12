package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@ToString
@EqualsAndHashCode
@Builder
@JsonInclude(NON_NULL)
public class S100v2SurfaceProperty implements Serializable {
    @Serial
    private static final long serialVersionUID = -8230369846687745262L;

    @JacksonXmlProperty(localName = "S100:Surface")
    private S100Surface surface;

    @ToString
    @EqualsAndHashCode(callSuper = false)
    @SuperBuilder
    @JsonInclude(NON_NULL)
    @JacksonXmlRootElement(localName = "S100:Surface")
    public static class S100Surface extends S100Property implements Serializable {
        @Serial
        private static final long serialVersionUID = 4404647979003084722L;

        @JacksonXmlProperty(localName = "gml:patches")
        private GmlPatches gmlPatches;
    }
}
