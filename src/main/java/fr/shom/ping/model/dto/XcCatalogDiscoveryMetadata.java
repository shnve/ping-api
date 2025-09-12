package fr.shom.ping.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JacksonXmlRootElement(localName = "XC:catalogueDiscoveryMetadata")
public class XcCatalogDiscoveryMetadata implements Serializable {
    @Serial
    private static final long serialVersionUID = -7424740449797797574L;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "XC:S100_CatalogueDiscoveryMetadata")
    @Builder.Default
    private List<S100CatalogDiscoveryMetadata> catalogDiscoveryMetadata = new ArrayList<>();
}
