package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@ToString
@EqualsAndHashCode
@Builder
@JsonInclude(NON_NULL)
@JsonPropertyOrder({
    "S100:encodingSpecification", "S100:encodingSpecificationEdition", "S100:productIdentifier", "S100:productEdition",
    "S100:applicationProfile", "S100:datasetFileIdentifier", "S100:datasetTitle", "S100:datasetReferenceDate", "S100:datasetLanguage",
    "S100:datasetAbstract", "S100:datasetTopicCategory", "S100:datasetPurpose", "S100:updateNumber"
})
public class S100DatasetIdentificationInformation implements Serializable {
    @Serial
    private static final long serialVersionUID = -6072172861503013526L;

    // Encoding specification that defines the encoding.
    @Builder.Default
    @JacksonXmlProperty(localName = "S100:encodingSpecification")
    private String encodingSpecification = "S-100 Part 10b";

    // Edition of the encoding specification
    @Builder.Default
    @JacksonXmlProperty(localName = "S100:encodingSpecificationEdition")
    private String encodingSpecificationEdition = "1.0";

    // Unique identifier of the data product as specified in the product specification
    @Builder.Default
    @JacksonXmlProperty(localName = "S100:productIdentifier")
    private String productIdentifier = "S-124";

    // Edition of the product specification
    @Builder.Default
    @JacksonXmlProperty(localName = "S100:productEdition")
    private String productEdition = "1.5.0";

    // Identifier that specifies a profile within the data product
    @Builder.Default
    @JacksonXmlProperty(localName = "S100:applicationProfile")
    private String applicationProfile = "1";

    // The file identifier of the dataset
    @JacksonXmlProperty(localName = "S100:datasetFileIdentifier")
    private String datasetFileIdentifier;

    // The title of the dataset
    @JacksonXmlProperty(localName = "S100:datasetTitle")
    private String datasetTitle;

    // The reference date of the dataset
    @JacksonXmlProperty(localName = "S100:datasetReferenceDate")
    private LocalDate datasetReferenceDate;

    // The (primary) language used in this dataset (ISO-639-2)
    // Must be always "eng" for S-100 datasets
    @Builder.Default
    @JacksonXmlProperty(localName = "S100:datasetLanguage")
    private String datasetLanguage = "eng";

    // The abstract of the dataset
    @JacksonXmlProperty(localName = "S100:datasetAbstract")
    private String datasetAbstract;

    // A set of topic categories
    @JacksonXmlProperty(localName = "S100:datasetTopicCategory")
    private String datasetTopicCategory;

    // base or update
    @JacksonXmlProperty(localName = "S100:datasetPurpose")
    private String datasetPurpose;

    // Update Number, 0 for a Base dataset
    @JacksonXmlProperty(localName = "S100:updateNumber")
    private String updateNumber;
}
