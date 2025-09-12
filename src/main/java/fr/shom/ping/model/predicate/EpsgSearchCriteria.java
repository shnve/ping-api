package fr.shom.ping.model.predicate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Epsg search criteria")
public class EpsgSearchCriteria {
    private String value;
}
