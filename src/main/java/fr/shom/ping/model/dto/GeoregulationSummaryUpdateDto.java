package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
@Schema(name = "Update georegulation summary and restriction categories Dto Model")
public class GeoregulationSummaryUpdateDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -7777323425029557134L;

    @NoHtml
    private String summary;

    @NoHtml
    private String internationalSummary;

    private List<GeoregulationPartUpdateDto> parts;
}
