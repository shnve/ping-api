package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import fr.shom.ping.validation.NoHtml;
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
@JacksonXmlRootElement(localName = "gml:PolygonPatch")
public class GmlPolygonPatch implements Serializable {
    @Serial
    private static final long serialVersionUID = -5839399967351405880L;

    @JacksonXmlProperty(localName = "gml:exterior")
    @NoHtml
    private GmlExterior exterior;

    @ToString
    @EqualsAndHashCode
    @Builder
    @JsonInclude(NON_NULL)
    public static class GmlExterior implements Serializable {
        @Serial
        private static final long serialVersionUID = 928188986184415425L;

        @JacksonXmlProperty(localName = "gml:LinearRing")
        private GmlLinearRing linearRing;

        @JacksonXmlProperty(localName = "gml:Ring")
        private GmlRing ring;
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    @JsonInclude(NON_NULL)
    public static class GmlLinearRing implements Serializable {
        @Serial
        private static final long serialVersionUID = -6630167090730991031L;

        @JacksonXmlProperty(localName = "gml:posList")
        private String posList;
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    @JsonInclude(NON_NULL)
    public static class GmlRing implements Serializable {
        @Serial
        private static final long serialVersionUID = 6487064687572045370L;

        @JacksonXmlProperty(localName = "gml:curveMember")
        private GmlCurveMember curveMember;
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    @JsonInclude(NON_NULL)
    public static class GmlCurveMember implements Serializable {
        @Serial
        private static final long serialVersionUID = -7919829058346830582L;

        @JacksonXmlProperty(localName = "gml:Curve")
        private GmlCurve gmlCurve;
    }

    @ToString
    @EqualsAndHashCode(callSuper = false)
    @SuperBuilder
    @JsonInclude(NON_NULL)
    public static class GmlCurve extends S100Property implements Serializable {
        @Serial
        private static final long serialVersionUID = -600469050796753567L;

        @JacksonXmlProperty(localName = "gml:segments")
        private GmlSegments gmlSegments;
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    @JsonInclude(NON_NULL)
    public static class GmlSegments implements Serializable {
        @Serial
        private static final long serialVersionUID = -6767154027724758741L;

        @JsonRawValue
        @JacksonXmlText()
        private String text;
    }
}
