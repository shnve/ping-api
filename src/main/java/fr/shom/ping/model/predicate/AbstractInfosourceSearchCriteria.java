package fr.shom.ping.model.predicate;

import fr.shom.ping.model.enums.InfosourceState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(name = "Infosource abstract search criteria")
public class AbstractInfosourceSearchCriteria {
    @Schema(description = "State in table (draft, sent, received)", required = true)
    @NotNull
    protected InfosourceState state;
}
