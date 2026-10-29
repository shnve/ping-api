package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import fr.shom.ping.model.enums.WarningHazardTypeDetails;
import fr.shom.ping.utils.S124FeatureCatalogue;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@ToString
@EqualsAndHashCode
@Builder
@JsonInclude(NON_NULL)
@JsonPropertyOrder({"S124:information", "S124:navwarnTypeDetails"})
public class S124v2WarningInformation implements Serializable {
    @Serial
    private static final long serialVersionUID = 2946586419546555710L;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S124:navwarnTypeDetails")
    @Default
    private List<S124NavwarnTypeDetails> warningHazardTypeDetails = new ArrayList<>();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S124:information")
    @Default
    private List<S124Information> information = new ArrayList<>();

    @ToString
    @EqualsAndHashCode
    @Builder
    @JsonInclude(NON_NULL)
    public static class S124NavwarnTypeDetails implements Serializable {
        @Serial
        private static final long serialVersionUID = -3502844623219813928L;

        @JacksonXmlProperty(isAttribute = true, localName = "code")
        private int code;

        @JacksonXmlText
        private String value;

        public static S124v2WarningInformation.S124NavwarnTypeDetails from(WarningHazardTypeDetails warningHazardTypeDetails) {
            return S124v2WarningInformation.S124NavwarnTypeDetails.builder()
                .code(warningHazardTypeDetails.getS124Code())
                // the XSD requires the normative Feature Catalogue label as element text
                .value(S124FeatureCatalogue.label(S124FeatureCatalogue.NAVWARN_TYPE_DETAILS, warningHazardTypeDetails.getS124Code()))
                .build();
        }
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    @JsonInclude(NON_NULL)
    @JsonPropertyOrder({"S124:fileLocator", "S124:fileReference", "S124:headline", "S124:language", "S124:text"})
    public static class S124Information implements Serializable {
        @Serial
        private static final long serialVersionUID = 7376008224851549005L;

        @JacksonXmlProperty(localName = "S124:fileLocator")
        private String fileLocator;

        @JacksonXmlProperty(localName = "S124:fileReference")
        private String fileReference;

        @JacksonXmlProperty(localName = "S124:headline")
        private String headline;

        @JacksonXmlProperty(localName = "S124:language")
        private String language;

        @JacksonXmlProperty(localName = "S124:text")
        private String text;
    }
}
