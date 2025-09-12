package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
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
public class S124Geometry implements Serializable {
    @Serial
    private static final long serialVersionUID = 4958571375536038458L;

    @JacksonXmlProperty(localName = "S100:pointProperty")
    private S100PointProperty pointProperty;

    @JacksonXmlProperty(localName = "S100:surfaceProperty")
    private S100SurfaceProperty surfaceProperty;

    @JacksonXmlProperty(localName = "S100:curveProperty")
    private S100CurveProperty curveProperty;
}
