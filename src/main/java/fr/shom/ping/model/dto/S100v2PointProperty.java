package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
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
public class S100v2PointProperty implements Serializable {
    @Serial
    private static final long serialVersionUID = 5716828242296179022L;

    @JacksonXmlProperty(localName = "S100:Point")
    private S100Point point;

    @ToString
    @EqualsAndHashCode(callSuper = false)
    @SuperBuilder
    @JsonInclude(NON_NULL)
    public static class S100Point extends S100Property {
        @Serial
        private static final long serialVersionUID = -7097322585709273457L;

        @JacksonXmlProperty(localName = "gml:pos")
        private String gmlPos;
    }
}
