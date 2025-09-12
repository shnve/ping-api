package fr.shom.ping.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@JacksonXmlRootElement(localName = "S124:ExchangeSet")
public class S124V2ExchangeSetDto extends AbstractS124ExchangeSetBaseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 8976617282973331723L;

    @Default
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:S100")
    private String s100Ns = "http://www.iho.int/s100gml/5.0";

    @Default
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:S124")
    private String s124Ns = "http://www.iho.int/S124/1.5";

    @Default
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S124:Dataset")
    private List<S124v2DataSetDto> dataSet = new ArrayList<>();
}
