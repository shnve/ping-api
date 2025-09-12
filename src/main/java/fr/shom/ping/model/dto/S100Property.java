package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import fr.shom.ping.validation.NoHtml;
import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.utils.S124v2Util.SRS_NAME;

@ToString
@EqualsAndHashCode
@SuperBuilder
@JsonInclude(NON_NULL)
@Data
public abstract class S100Property implements Serializable {
    @Serial
    private static final long serialVersionUID = -5564623255899003011L;

    @JacksonXmlProperty(isAttribute = true, localName = "gml:id")
    @NoHtml
    protected String gmlId;

    @JacksonXmlProperty(isAttribute = true, localName = "srsName")
    @NoHtml
    @Builder.Default
    protected String srsName = SRS_NAME;
}
