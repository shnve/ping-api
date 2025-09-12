package fr.shom.ping.model.predicate;

import fr.shom.ping.model.enums.SubscriptionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Subscription search criteria")
public class SubscriptionSearchCriteria {

    @Schema(description = "Type in table (warning, georegulation)", required = true)
    protected SubscriptionType type;
}
