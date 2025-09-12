package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@JsonInclude(NON_NULL)
public class S124v2MemberDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 6585992849419929494L;

    @JacksonXmlProperty(localName = "S124:NAVWARNPreamble")
    private S124v2NWPreambleDto preamble;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S124:NAVWARNPart")
    @Builder.Default
    private List<S124v2NWPartDto> part = new ArrayList<>();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S124:NAVWARNAreaAffected")
    @Builder.Default
    private List<S124NWAreaAffectedDto> affectedArea = new ArrayList<>();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S124:References")
    @Builder.Default
    private List<S124v2NWReferencesDto> references = new ArrayList<>();
}
