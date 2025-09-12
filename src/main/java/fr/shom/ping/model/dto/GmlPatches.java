package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@ToString
@EqualsAndHashCode
@Builder
@JsonInclude(NON_NULL)
@JacksonXmlRootElement(localName = "gml:patches")
public class GmlPatches implements Serializable {
    @Serial
    private static final long serialVersionUID = 271935090600377263L;

    @Builder.Default
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "gml:PolygonPatch")
    private List<GmlPolygonPatch> polygons = new ArrayList<>();
}
