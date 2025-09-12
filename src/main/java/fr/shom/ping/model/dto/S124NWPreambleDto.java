package fr.shom.ping.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Builder
public class S124NWPreambleDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 6983514243687041290L;

    @JacksonXmlProperty(isAttribute = true, localName = "gml:id")
    private String gmlId;

    @JacksonXmlProperty(localName = "id")
    private String id;

    @JacksonXmlProperty(localName = "messageSeriesIdentifier")
    private S124MessageSeriesIdentifierDto messageSeriesIdentifier;

    @JacksonXmlProperty(localName = "publicationDate")
    private Instant publicationDate;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "generalArea")
    @Default
    private List<S124SubAreaDto> generalAreas = new ArrayList<>();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "locality")
    @Default
    private List<S124SubAreaDto> localities = new ArrayList<>();

    @JacksonXmlProperty(localName = "warningHazardTypeGeneral")
    private WarningHazardTypeGeneral warningHazardTypeGeneral;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "theWarningPart")
    @Default
    private List<S124PartLink> partLinks = new ArrayList<>();

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class S124PartLink implements Serializable {
        @Serial
        private static final long serialVersionUID = -2512921737590566871L;

        @JacksonXmlProperty(isAttribute = true, localName = "xlink:href")
        private String href;
    }
}
