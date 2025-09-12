package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import fr.shom.ping.validation.NoHtml;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@ToString
@EqualsAndHashCode
@Builder
@JsonInclude(NON_NULL)
@JacksonXmlRootElement(localName = "S100:Point")
public class S100PointDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 2439975553662085238L;

    @JacksonXmlProperty(isAttribute = true, localName = "srsName")
    @NoHtml
    private String srsName;

    @JacksonXmlProperty(localName = "gml:pos")
    @NoHtml
    private String gmlPos;

    @JacksonXmlProperty(isAttribute = true, localName = "gml:id")
    @NoHtml
    private String gmlId;
}
