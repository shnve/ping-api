package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serial;
import java.io.Serializable;
import fr.shom.ping.validation.NoHtml;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.validation.Valid;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@ToString
@EqualsAndHashCode
@Builder
@JsonInclude(NON_NULL)
@JacksonXmlRootElement(localName = "gml:CircleByCenterPoint")
public class GmlCircleByCenterPoint implements Serializable {
    @Serial
    private static final long serialVersionUID = -2832857387075189586L;

    @JacksonXmlProperty(isAttribute = true, localName = "srsName")
    @NoHtml
    private String srsName;

    @JacksonXmlProperty(isAttribute = true, localName = "numArc")
    @NoHtml
    private String numArc;

    @JacksonXmlProperty(localName = "gml:Pos")
    @NoHtml
    private String gmlPos;

    @JacksonXmlProperty(localName = "gml:radius")
    private @Valid GmlRadius gmlRadius;
}
