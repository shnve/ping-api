package fr.shom.ping.model.dto;

import fr.shom.ping.model.enums.GeoregulationType;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Subscription Create Dto Model")
public class SubscriptionCreateDto {

    private UUID serviceId;

    @NoHtml
    private String title;

    private UUID areaId;

    private UUID serieId;

    @NoHtml
    private String lang;

    @Builder.Default
    private Set<UUID> subAreaIds = new HashSet<>();

    @Builder.Default
    private Set<WarningHazardTypeGeneral> warningHazardTypeGenerals = new HashSet<>();

    @Builder.Default
    private Set<UUID> maritimeEntityIdsGeographicExtent = new HashSet<>();

    @Builder.Default
    private Set<GeoregulationType> georegulationTypes = new HashSet<>();

    @Builder.Default
    private Set<UUID> maritimeEntityIds = new HashSet<>();

    private boolean sendingMail;
}
