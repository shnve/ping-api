package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@JsonInclude(NON_NULL)
public class S124MemberDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -7127882150523139144L;

    @JacksonXmlProperty(localName = "S124:NWPreamble")
    private S124NWPreambleDto preamble;

    @JacksonXmlProperty(localName = "S124:NavigationalWarningFeaturePart")
    private S124NWPartDto part;

    @JacksonXmlProperty(localName = "S124:References")
    private S124NWReferencesDto references;
}
