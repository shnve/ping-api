package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Survey metadata Dto Model")
public class SurveyMetadataDto implements Serializable {
    private int epsg;

    @JsonProperty("zUp")
    private boolean zUp;

    private boolean latLon;
}
