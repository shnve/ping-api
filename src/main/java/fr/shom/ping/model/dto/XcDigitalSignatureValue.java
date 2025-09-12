package fr.shom.ping.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JacksonXmlRootElement(localName = "XC:digitalSignatureValue")
public class XcDigitalSignatureValue implements Serializable {
    @Serial
    private static final long serialVersionUID = 3234722048163054536L;

    @JacksonXmlProperty(localName = "S100SE:S100_SE_DigitalSignature")
    private S100SEDigitalSignature s100SEDigitalSignature;

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class S100SEDigitalSignature implements Serializable {
        @Serial
        private static final long serialVersionUID = -1482729740848720960L;

        @JacksonXmlProperty(isAttribute = true, localName = "id")
        private String id;

        @JacksonXmlProperty(isAttribute = true, localName = "certificateRef")
        private String certificateRef;

        @JacksonXmlText
        private String value;
    }
}
