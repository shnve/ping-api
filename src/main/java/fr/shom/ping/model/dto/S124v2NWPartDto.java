package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@ToString
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@JsonInclude(NON_NULL)
@JsonPropertyOrder({"S124:fixedDateRange", "S124:warningInformation", "S124:header", "S124:affects", "S124:geometry"})
@Data
public class S124v2NWPartDto extends S124NWPartBaseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -5894913000443791910L;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S124:fixedDateRange")
    @Default
    private List<S124v2FixedDateRange> fixedDateRanges = new ArrayList<>();

    @JacksonXmlProperty(localName = "S124:warningInformation")
    private S124v2WarningInformation warningInformation;

    @JacksonXmlProperty(localName = "S124:header")
    private S124PreambleLink header;

    @JacksonXmlProperty(localName = "S124:affects")
    private S124Link affects;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S124:geometry")
    @Default
    private List<S124v2Geometry> geometries = new ArrayList<>();
}
