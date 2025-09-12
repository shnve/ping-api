package fr.shom.ping.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serial;
import java.io.Serializable;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public abstract class AbstractS124ExchangeSetBaseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -6215135721546268645L;

    @Default
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:gml")
    protected String gmlNs = "http://www.opengis.net/gml/3.2";

    @Default
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:S100")
    protected String s100Ns = "http://www.iho.int/s100gml/1.0";

    @Default
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:S124")
    protected String s124Ns = "http://www.iho.int/S124/gml/cs0/0.1";

    @Default
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xlink")
    protected String xLinkNs = "http://www.w3.org/1999/xlink";

    protected AbstractS124ExchangeSetBaseDto(AbstractS124ExchangeSetBaseDto abstractS124ExchangeSetBaseDto) {
        this.gmlNs = abstractS124ExchangeSetBaseDto.getGmlNs();
        this.s124Ns = abstractS124ExchangeSetBaseDto.getS124Ns();
        this.xLinkNs = abstractS124ExchangeSetBaseDto.getXLinkNs();
        this.s100Ns = abstractS124ExchangeSetBaseDto.getS100Ns();
    }
}
