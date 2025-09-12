package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
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
@JacksonXmlRootElement(localName = "XC:S100_DatasetDiscoveryMetadata")
@JsonPropertyOrder({
    "XC:fileName", "XC:compressionFlag", "XC:dataProtection", "XC:digitalSignatureReference", "XC:digitalSignatureValue", "XC:copyright", "XC:notForNavigation",
    "XC:specificUsage", "XC:editionNumber", "XC:issueDate", "XC:productSpecification", "XC:producingAgency", "XC:defaultLocale", "XC:encodingFormat"
})
public class S100DatasetDiscoveryMetadata extends AbstractS100DiscoveryMetadata {

    private static final String ENCODING_FORMAT = "GML";

    private static final String DEFAULT_SPECIFIC_USAGE = "Navigational Warning Service";

    @Serial
    private static final long serialVersionUID = 8017271903653894394L;

    @JacksonXmlProperty(localName = "XC:dataProtection")
    @Builder.Default
    private boolean dataProtection = false;

    @JacksonXmlProperty(localName = "XC:copyright")
    @Builder.Default
    private boolean copyright = false;

    @JacksonXmlProperty(localName = "XC:notForNavigation")
    @Builder.Default
    private boolean notForNavigation = false;

    @JacksonXmlProperty(localName = "XC:specificUsage")
    @Builder.Default
    private S100UsagePropertyType specificUsage = S100UsagePropertyType.builder().build();

    @JacksonXmlProperty(localName = "XC:producingAgency")
    private XcProducingAgency producingAgency;

    @JacksonXmlProperty(localName = "XC:encodingFormat")
    @Builder.Default
    private String encodingFormat = ENCODING_FORMAT;

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class S100UsagePropertyType implements Serializable {

        @Serial
        private static final long serialVersionUID = 8084769606965957169L;

        @JacksonXmlProperty(isAttribute = true, localName = "gco:isoType")
        @Builder.Default
        private String gcoIsoType = DEFAULT_SPECIFIC_USAGE;
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class XcProducingAgency implements Serializable {
        @Serial
        private static final long serialVersionUID = -2027562977139231696L;

        @JacksonXmlProperty(localName = "cit:CI_Responsibility")
        private CitCIResponsibility citCIResponsibility;
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class CitCIResponsibility implements Serializable {
        @Serial
        private static final long serialVersionUID = 6500959298328095720L;

        @JacksonXmlProperty(localName = "cit:role")
        private CitRole citRole;

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "cit:party")
        @Builder.Default
        private List<CitParty> citPartyList = new ArrayList<>();
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class CitRole implements Serializable {
        @Serial
        private static final long serialVersionUID = 5889932891772749949L;

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "cit:CI_RoleCode")
        @Builder.Default
        private List<CitCIRoleCode> roleCodes = new ArrayList<>();
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class CitCIRoleCode implements Serializable {
        @Serial
        private static final long serialVersionUID = -6287119697159213711L;

        @JacksonXmlProperty(isAttribute = true, localName = "codeList")
        @Builder.Default
        private String codeList = "http://standards.iso.org/iso/19115/resources/Codelists/cat/codelists.xml#CI_RoleCode";

        @JacksonXmlProperty(isAttribute = true, localName = "codeListValue")
        @Builder.Default
        private String codeListValue = "owner";

        @JacksonXmlText
        private String value;
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class CitParty implements Serializable {
        @Serial
        private static final long serialVersionUID = -8437364741125644108L;

        @JacksonXmlProperty(localName = "cit:CI_Organisation")
        private CitOrganisation citOrganisation;
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class CitOrganisation implements Serializable {
        @Serial
        private static final long serialVersionUID = -5988579491927355835L;

        @JacksonXmlProperty(localName = "cit:name")
        private GcoCharacterString gcoCharacterString;

        @JacksonXmlProperty(localName = "cit:contactInfo")
        private CitContactInfo citContactInfo;
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class GcoCharacterString implements Serializable {
        @Serial
        private static final long serialVersionUID = -106450308784309246L;

        @JacksonXmlProperty(localName = "gco:CharacterString")
        private String gcoCharacterString;
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class CitContactInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 7968102484278963616L;

        @JacksonXmlProperty(localName = "cit:CI_Contact")
        private CitContact citContact;

        @ToString
        @EqualsAndHashCode
        @Builder
        public static class CitContact implements Serializable {
            @Serial
            private static final long serialVersionUID = 289886071241663742L;

            @JacksonXmlProperty(localName = "cit:address")
            private CitAddressInfo citAddressInfo;

            @JacksonXmlProperty(localName = "cit:onlineResource")
            private CitOnlineResourceInfo citOnlineResourceInfo;
        }
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class CitAddressInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 8555876193533127048L;

        @JacksonXmlProperty(localName = "cit:CI_Address")
        private CitAddress citAddress;

        @ToString
        @EqualsAndHashCode
        @Builder
        public static class CitAddress implements Serializable {
            @Serial
            private static final long serialVersionUID = -5071598532527708198L;

            @JacksonXmlProperty(localName = "cit:administrativeArea")
            private GcoCharacterString citAdministrativeArea;

            @JacksonXmlProperty(localName = "cit:country")
            private GcoCharacterString citCountry;

            @JacksonXmlProperty(localName = "cit:electronicMailAddress")
            private GcoCharacterString citElectronicMailAddress;
        }
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class CitOnlineResourceInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = -4157738563793665013L;

        @JacksonXmlProperty(localName = "cit:CI_OnlineResource")
        private CitOnlineResource citOnlineResource;

        @ToString
        @EqualsAndHashCode
        @Builder
        @JsonPropertyOrder({"cit:linkage", "cit:name", "cit:function"})
        public static class CitOnlineResource implements Serializable {
            @Serial
            private static final long serialVersionUID = 3461847881789152024L;

            @JacksonXmlProperty(localName = "cit:linkage")
            private GcoCharacterString citLinkage;

            @JacksonXmlProperty(localName = "cit:name")
            private GcoCharacterString citName;

            @JacksonXmlProperty(localName = "cit:function")
            private CitOnLineFunctionCodeInfo citFunction;

            @ToString
            @EqualsAndHashCode
            @Builder
            public static class CitOnLineFunctionCodeInfo implements Serializable {
                @Serial
                private static final long serialVersionUID = -1989400158245242644L;

                @JacksonXmlProperty(localName = "cit:CI_OnLineFunctionCode")
                private CitCIRoleCode citOnLineFunctionCode;
            }
        }
    }
}
