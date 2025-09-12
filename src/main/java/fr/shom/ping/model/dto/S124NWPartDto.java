package fr.shom.ping.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@ToString
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
public class S124NWPartDto extends S124NWPartBaseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -5894913000443791910L;

    @JacksonXmlProperty(localName = "id")
    private String id;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "geometry")
    @Default
    private List<S124Geometry> geometries = new ArrayList<>();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "areaAffected")
    @Default
    private List<S124Geometry> affectedAreas = new ArrayList<>();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S124:fixedDateRange")
    @Default
    private List<S124FixedDateRange> fixedDateRanges = new ArrayList<>();

    @JacksonXmlProperty(localName = "S124:warningInformation")
    private S124WarningInformation warningInformation;

    @JacksonXmlProperty(localName = "header")
    private S124PreambleLink header;
}
