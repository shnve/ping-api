package fr.shom.ping.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

@ToString
@EqualsAndHashCode
@Builder
@JacksonXmlRootElement(localName = "S100SE:certificate")
public class S100SECertificate implements Serializable {
    @Serial
    private static final long serialVersionUID = 7901583849403444917L;

    @JacksonXmlProperty(isAttribute = true, localName = "id")
    private String id;

    @JacksonXmlProperty(isAttribute = true, localName = "issuer")
    private String issuer;

    @JacksonXmlText
    private String value;
}
