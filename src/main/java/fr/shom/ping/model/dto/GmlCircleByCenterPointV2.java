package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import fr.shom.ping.validation.NoHtml;
import jakarta.validation.Valid;
import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@ToString
@EqualsAndHashCode
@Builder
@JsonInclude(NON_NULL)
@JacksonXmlRootElement(localName = "S100:S100_CircleByCenterPoint")
@JsonPropertyOrder({"gml:pos", "S100:radius", "S100:startAngle", "S100:angularDistance"})
public class GmlCircleByCenterPointV2 implements Serializable {
    @Serial
    private static final long serialVersionUID = -1114282206964903677L;

    @JacksonXmlProperty(localName = "S100:startAngle")
    @NoHtml
    @Builder.Default
    private String startAngle = "0.0";

    @JacksonXmlProperty(localName = "S100:angularDistance")
    @NoHtml
    @Builder.Default
    private String angularDistance = "360";

    @JacksonXmlProperty(localName = "gml:pos")
    @NoHtml
    private String gmlPos;

    @JacksonXmlProperty(localName = "S100:radius")
    private @Valid GmlRadius gmlRadius;
}
