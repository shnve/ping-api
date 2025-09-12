package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.GeoregulationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import jakarta.validation.Valid;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Georegulation Subscription Dto Model")
public class GeoregulationSubscriptionDto extends SubscriptionDto {

    @Serial
    private static final long serialVersionUID = 6499694505802801373L;

    @Builder.Default
    private Set<GeoregulationType> georegulationTypes = new HashSet<>();

    @Builder.Default
    private Set<@Valid MaritimeEntityDto> maritimeEntities = new HashSet<>();
}
