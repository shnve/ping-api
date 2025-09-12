package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serial;
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

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@JacksonXmlRootElement(localName = "XC:S100_CatalogueDiscoveryMetadata")
@JsonPropertyOrder({
    "XC:fileName", "XC:purpose", "XC:editionNumber", "XC:scope", "XC:versionNumber", "XC:issueDate", "XC:productSpecification", "XC:digitalSignatureReference",
    "XC:digitalSignatureValue", "XC:compressionFlag", "XC:defaultLocale", "XC:otherLocale"
})
public class S100CatalogDiscoveryMetadata extends AbstractS100DiscoveryMetadata {
    @Serial
    private static final long serialVersionUID = 5378772028909321577L;

    @JacksonXmlProperty(localName = "XC:purpose")
    private String purpose;     // S100Purpose

    @JacksonXmlProperty(localName = "XC:scope")
    private String scope;       // S100CatalogScope

    @JacksonXmlProperty(localName = "XC:versionNumber")
    private String versionNumber;

    @JacksonXmlProperty(localName = "XC:otherLocale")
    @Builder.Default
    private List<String> otherLocale = new ArrayList<>();
}
