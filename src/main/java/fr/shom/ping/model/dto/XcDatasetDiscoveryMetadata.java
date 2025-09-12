package fr.shom.ping.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JacksonXmlRootElement(localName = "XC:datasetDiscoveryMetadata")
public class XcDatasetDiscoveryMetadata implements Serializable {
    @Serial
    private static final long serialVersionUID = -5095263069353480100L;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "XC:S100_DatasetDiscoveryMetadata")
    @Builder.Default
    private List<S100DatasetDiscoveryMetadata> datasetDiscoveryMetadata = new ArrayList<>();
}
