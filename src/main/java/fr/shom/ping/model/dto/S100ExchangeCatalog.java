package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JacksonXmlRootElement(localName = "XC:S100_ExchangeCatalogue")
@JsonPropertyOrder({"XC:certificates", "XC:datasetDiscoveryMetadata", "XC:supportFileDiscoveryMetadata", "XC:catalogueDiscoveryMetadata"})
public class S100ExchangeCatalog implements Serializable {
    @Serial
    private static final long serialVersionUID = 260960099300516442L;

    @Builder.Default
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:cit")
    private String citNs = "http://standards.iso.org/iso/19115/-3/cit/2.0";

    @Builder.Default
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:gco")
    private String gcoNs = "http://standards.iso.org/iso/19115/-3/gco/1.0";

    @Builder.Default
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:XC")
    private String xcNs = "http://www.iho.int/s100/xc/5.0";

    @Builder.Default
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:S100SE")
    private String s100seNs = "http://www.iho.int/s100/se/5.0";

    @JacksonXmlProperty(localName = "XC:certificates")
    private XcCertificates certificates;

    @JacksonXmlProperty(localName = "XC:datasetDiscoveryMetadata")
    private XcDatasetDiscoveryMetadata datasetDiscoveryMetadata;

    @JacksonXmlProperty(localName = "XC:supportFileDiscoveryMetadata")
    private XcSupportFileDiscoveryMetadata supportFileDiscoveryMetadata;

    @JacksonXmlProperty(localName = "XC:catalogueDiscoveryMetadata")
    private XcCatalogDiscoveryMetadata catalogDiscoveryMetadata;
}
