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
public class S100v2CurveProperty implements Serializable {
    @Serial
    private static final long serialVersionUID = -4332922718477686377L;

    @JacksonXmlProperty(localName = "S100:Curve")
    private S100Curve curve;

    @ToString
    @EqualsAndHashCode(callSuper = false)
    @SuperBuilder
    @JsonInclude(NON_NULL)
    public static class S100Curve extends S100Property implements Serializable {
        @Serial
        private static final long serialVersionUID = -7097322585709273457L;

        @JacksonXmlProperty(localName = "gml:segments")
        private GmlSegments gmlSegments;
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    @JsonInclude(NON_NULL)
    public static class GmlSegments implements Serializable {
        @Serial
        private static final long serialVersionUID = -5607741111685079851L;

        @JacksonXmlProperty(localName = "gml:LineStringSegment")
        private GmlLineStringSegment gmlLineStringSegment;
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    @JsonInclude(NON_NULL)
    public static class GmlLineStringSegment implements Serializable {
        @Serial
        private static final long serialVersionUID = -3408755493474313795L;

        @JacksonXmlProperty(localName = "gml:posList")
        private String posList;
    }
}
