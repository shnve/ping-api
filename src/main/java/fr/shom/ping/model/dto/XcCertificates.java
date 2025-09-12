package fr.shom.ping.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Builder
public class XcCertificates implements Serializable {
    @Serial
    private static final long serialVersionUID = 5637458970845716637L;

    @JacksonXmlProperty(localName = "S100SE:schemeAdministrator")
    private S100SESchemeAdministrator schemeAdministrator;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S100SE:certificate")
    @Builder.Default
    private List<S100SECertificate> certificates = new ArrayList<>();
}

