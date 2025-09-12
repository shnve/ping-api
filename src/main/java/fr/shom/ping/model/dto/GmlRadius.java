package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import fr.shom.ping.validation.NoHtml;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@ToString
@EqualsAndHashCode
@Builder
@JsonInclude(NON_NULL)
public class GmlRadius implements Serializable {

    @JacksonXmlProperty(isAttribute = true, localName = "uom")
    @NoHtml
    private String uom;

    @JacksonXmlText
    @NoHtml
    private String value;
}
