package fr.shom.ping.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import fr.shom.ping.model.enums.WarningHazardTypeDetails;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Builder
public class S124WarningInformation implements Serializable {
    @Serial
    private static final long serialVersionUID = -7095739953745493741L;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "warningHazardTypeDetails")
    @Default
    private List<WarningHazardTypeDetails> warningHazardTypeDetails = new ArrayList<>();

    @JacksonXmlProperty(localName = "information")
    private String information;
}
