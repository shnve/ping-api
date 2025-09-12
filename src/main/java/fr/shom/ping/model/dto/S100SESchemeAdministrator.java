package fr.shom.ping.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Builder
@JacksonXmlRootElement(localName = "S100SE:schemeAdministrator")
public class S100SESchemeAdministrator implements Serializable {
    @Serial
    private static final long serialVersionUID = -1325242582794786634L;

    @JacksonXmlProperty(isAttribute = true, localName = "id")
    private String id;
}
