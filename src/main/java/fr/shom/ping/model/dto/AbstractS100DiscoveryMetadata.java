package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public abstract class AbstractS100DiscoveryMetadata implements Serializable {
    @Serial
    private static final long serialVersionUID = -5439616058365039221L;

    protected static final String DATASET_DIGITAL_SIGNATURE_ALGORITHM = "ECDSA-384-SHA2";

    @JacksonXmlProperty(localName = "XC:fileName")
    protected String filename;

    @JacksonXmlProperty(localName = "XC:editionNumber")
    protected Integer editionNumber;

    @JacksonXmlProperty(localName = "XC:issueDate")
    protected LocalDate issueDate;

    @JacksonXmlProperty(localName = "XC:compressionFlag")
    @Builder.Default
    protected boolean compressionFlag = false;

    @JacksonXmlProperty(localName = "XC:digitalSignatureReference")
    @Builder.Default
    protected String digitalSignatureReference = DATASET_DIGITAL_SIGNATURE_ALGORITHM;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "XC:digitalSignatureValue")
    @Builder.Default
    protected List<XcDigitalSignatureValue> digitalSignatureValue = new ArrayList<>();

    @JacksonXmlProperty(localName = "XC:productSpecification")
    protected XcProductSpecification productSpecification;

    @JacksonXmlProperty(localName = "XC:defaultLocale")
    protected String defaultLocale;

    @ToString
    @EqualsAndHashCode
    @Builder
    @JsonInclude(NON_NULL)
    @JsonPropertyOrder({"XC:productIdentifier", "XC:number"})
    public static class XcProductSpecification implements Serializable {
        @Serial
        private static final long serialVersionUID = 6968996340480208849L;

        @JacksonXmlProperty(localName = "XC:productIdentifier")
        @Builder.Default
        private String productIdentifier = "S-124";

        @JacksonXmlProperty(localName = "XC:number")
        @Builder.Default
        private int number = 124;
    }
}
