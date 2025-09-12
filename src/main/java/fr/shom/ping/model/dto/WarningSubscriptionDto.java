package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Warning Subscription Dto Model")
public class WarningSubscriptionDto extends SubscriptionDto {

    @Serial
    private static final long serialVersionUID = -1420235819295369066L;

    private SerieDetailsDto serie;

    private String lang;

    @Builder.Default
    private Set<SubAreaDetailsDto> subAreas = new HashSet<>();

    @Builder.Default
    private Set<WarningHazardTypeGeneral> warningHazardTypeGenerals = new HashSet<>();
}
