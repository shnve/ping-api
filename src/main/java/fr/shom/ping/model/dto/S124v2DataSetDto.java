package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import fr.shom.ping.utils.S124FeatureCatalogue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JacksonXmlRootElement(localName = "S124:Dataset")
@JsonPropertyOrder({"S100:DatasetIdentificationInformation", "S124:members"})
public class S124v2DataSetDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 8309195474566392903L;

    @Default
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xs")
    private String xsNs = "http://www.w3.org/2001/XMLSchema";

    @Default
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:gml")
    private String gmlNs = "http://www.opengis.net/gml/3.2";

    @Default
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:S100")
    private String s100Ns = "http://www.iho.int/s100gml/5.0";

    @Default
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:S124")
    private String s124Ns = S124FeatureCatalogue.NAMESPACE;

    @Default
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xlink")
    private String xLinkNs = "http://www.w3.org/1999/xlink";

    @JacksonXmlProperty(isAttribute = true, localName = "gml:id")
    private String gmlId;

    @Default
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S124:members")
    private List<S124v2MemberDto> imember = new ArrayList<>();

    @JacksonXmlProperty(localName = "S100:DatasetIdentificationInformation")
    private S100DatasetIdentificationInformation identificationInformation;
}
