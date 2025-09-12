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
public class S124v2Geometry implements Serializable {
    @Serial
    private static final long serialVersionUID = -2730232244810727309L;

    @JacksonXmlProperty(localName = "S100:pointProperty")
    private S100v2PointProperty pointProperty;

    @JacksonXmlProperty(localName = "S100:surfaceProperty")
    private S100v2SurfaceProperty surfaceProperty;

    @JacksonXmlProperty(localName = "S100:curveProperty")
    private S100v2CurveProperty curveProperty;
}
