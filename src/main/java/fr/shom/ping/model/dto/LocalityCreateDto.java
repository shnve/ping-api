package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Locality Create Dto Model")
public class LocalityCreateDto {
    private UUID parentSubAreaId;

    @NoHtml
    private String name;

    @NoHtml
    private String internationalName;
}
