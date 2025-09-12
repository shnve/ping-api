package fr.shom.ping.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Builder
public class S124NWReferencesDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 441847898138981176L;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "messageSeriesIdentifier")
    @Builder.Default
    private List<S124MessageSeriesIdentifierDto> messageSeries = new ArrayList<>();

    @JacksonXmlProperty(localName = "noMessageOnHand")
    private Boolean noMessageOnHand;

    @JacksonXmlProperty(localName = "referenceCategory")
    private String referenceCategory;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "theWarning")
    @Builder.Default
    private S124NWReferencesDto.S124WarningLink theWarning;

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class S124WarningLink implements Serializable {
        @Serial
        private static final long serialVersionUID = 6586918705875728742L;

        @JacksonXmlProperty(isAttribute = true, localName = "xlink:href")
        private String href;
    }
}
