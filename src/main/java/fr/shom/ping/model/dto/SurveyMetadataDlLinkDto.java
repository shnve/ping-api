package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Survey metadata and download link Dto Model")
public class SurveyMetadataDlLinkDto implements Serializable {
    private String dlLink;

    @JsonProperty("isTest")
    private boolean isTest;

    private SurveyMetadataDto metadata;

    @Builder.Default
    private Map<String, String> files = new HashMap<>();
}
