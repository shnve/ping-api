package fr.shom.ping.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Builder
public class S124Link implements Serializable {
    @Serial
    private static final long serialVersionUID = 247165881281203929L;

    @JacksonXmlProperty(isAttribute = true, localName = "xlink:href")
    private String href;
}

