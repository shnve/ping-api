package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import fr.shom.ping.utils.S124FeatureCatalogue;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@Data
@EqualsAndHashCode
@Builder
@JsonPropertyOrder({"S124:generalArea", "S124:locality", "S124:messageSeriesIdentifier", "S124:cancellationDate", "S124:intService", "S124:navwarnTypeGeneral"
    , "S124:publicationTime", "S124:theReferences"})
public class S124v2NWPreambleDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 4119530959722652605L;

    @JacksonXmlProperty(isAttribute = true, localName = "gml:id")
    private String gmlId;

    @JacksonXmlProperty(localName = "S124:messageSeriesIdentifier")
    private S124v2MessageSeriesIdentifierDto messageSeriesIdentifier;

    @JacksonXmlProperty(localName = "S124:publicationTime")
    private Instant publicationDateTime;

    @JacksonXmlProperty(localName = "S124:cancellationDate")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Instant cancellationDate;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S124:generalArea")
    @Default
    private List<S124v2SubAreaDto> generalAreas = new ArrayList<>();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S124:locality")
    @Default
    private List<S124v2SubAreaDto> localities = new ArrayList<>();

    @JacksonXmlProperty(localName = "S124:navwarnTypeGeneral")
    private S124NavwarnTypeGeneral warningHazardTypeGeneral;

    @JacksonXmlProperty(localName = "S124:intService")
    @Default
    private Boolean intService = false;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S124:theReferences")
    @Default
    private List<S124Link> theReferences = new ArrayList<>();

    // S-124 2.0.0: the preamble/part association (navwarnPreambleContent) is encoded only from the part side (S124:header).
    // NavwarnPreambleType has no theWarningPart element, so it must not be serialized.

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class S124NavwarnTypeGeneral implements Serializable {

        @Serial
        private static final long serialVersionUID = -5442269393663034012L;

        @JacksonXmlProperty(isAttribute = true, localName = "code")
        private int code;

        @JacksonXmlText
        private String value;

        public static S124NavwarnTypeGeneral from(WarningHazardTypeGeneral warningHazardTypeGeneral) {
            return S124NavwarnTypeGeneral.builder()
                .code(warningHazardTypeGeneral.getS124Code())
                // the XSD requires the normative Feature Catalogue label as element text
                .value(S124FeatureCatalogue.label(S124FeatureCatalogue.NAVWARN_TYPE_GENERAL, warningHazardTypeGeneral.getS124Code()))
                .build();
        }
    }
}
