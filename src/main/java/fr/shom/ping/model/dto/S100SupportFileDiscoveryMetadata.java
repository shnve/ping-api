package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serial;
import java.io.Serializable;
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
@JacksonXmlRootElement(localName = "XC:S100_SupportFileDiscoveryMetadata")
@JsonPropertyOrder({
    "XC:fileName", "XC:revisionStatus", "XC:editionNumber", "XC:issueDate", "XC:dataType", "XC:otherDataTypeDescription", "XC:comment", "XC:compressionFlag",
    "XC:digitalSignatureReference", "XC:digitalSignatureValue", "XC:defaultLocale", "XC:supportedResource", "XC:resourcePurpose"
})
public class S100SupportFileDiscoveryMetadata extends AbstractS100DiscoveryMetadata {
    @Serial
    private static final long serialVersionUID = -2284645673198446256L;

    @JacksonXmlProperty(localName = "XC:revisionStatus")
    private String revisionStatus;      // S100SupportFileRevisionStatus

    @JacksonXmlProperty(localName = "XC:dataType")
    private String dataType;        // S100SupportFileFormat

    @JacksonXmlProperty(localName = "XC:otherDataTypeDescription")
    private String otherDataTypeDescription;

    @JacksonXmlProperty(localName = "XC:comment")
    private Comment comment;

    @JacksonXmlProperty(localName = "XC:defaultLocale")
    private String defaultLocale;

    @JacksonXmlProperty(localName = "XC:supportedResource")
    @Builder.Default
    private List<String> supportedResource = new ArrayList<>();

    @JacksonXmlProperty(localName = "XC:resourcePurpose")
    private String resourcePurpose;     // S100ResourcePurpose

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class Comment implements Serializable {
        @Serial
        private static final long serialVersionUID = 26673766283983584L;

        @JacksonXmlProperty(localName = "gco:CharacterString")
        private String gcoCharacterString;
    }
}
