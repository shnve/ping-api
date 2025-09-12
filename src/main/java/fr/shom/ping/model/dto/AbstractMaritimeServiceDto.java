package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.MaritimeServiceType;
import fr.shom.ping.model.enums.ServiceNetwork;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidCountryCode;
import fr.shom.ping.validation.ValidURN;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.COUNTRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.EMPTY_LIST_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.INFORMATION_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.LOCALIZED_INFORMATION_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.URN_DESCRIPTION;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Maritime Service Dto Model")
public abstract class AbstractMaritimeServiceDto {

    @ValidURN(message = "validation.maritime-service.urn.invalid")
    @Schema(description = URN_DESCRIPTION)
    protected String urn;

    @NotEmpty(message = "validation.maritime-service.name.not-empty")
    @Schema(description = NAME_DESCRIPTION, required = true)
    @NoHtml
    protected String name;

    @Schema(description = INTERNATIONAL_NAME_DESCRIPTION)
    @NoHtml
    protected String internationalName;

    @Schema(description = "Production Agency")
    @NoHtml
    protected String productionAgency;

    @Schema(description = INFORMATION_DESCRIPTION)
    @NoHtml
    protected String information;

    @Schema(description = LOCALIZED_INFORMATION_DESCRIPTION)
    @NoHtml
    protected String localizedInformation;

    @ValidCountryCode(message = "validation.maritime-service.country-code.invalid")
    @Schema(description = COUNTRY_DESCRIPTION)
    @NoHtml
    protected String country;

    @NotNull(message = "validation.maritime-service.type.not-null")
    @Schema(description = "Maritime Service Type", required = true)
    protected MaritimeServiceType type;

    @NoHtml
    protected String authorityCode;

    @NotNull(message = "validation.maritime-service.service-network.not-null")
    @Schema(description = "Service Network", required = true)
    protected ServiceNetwork serviceNetwork;

    @Schema(description = "PING partner")
    protected boolean pingPartner;

    @Schema(description = "Maritime Notice Producer")
    protected boolean maritimeNoticeProducer;

    @Schema(description = "Folder Manager")
    protected boolean folderManager;

    @Schema(description = "Survey owner")
    protected boolean surveyOwner;

    @Schema(description = "List of linked areas", example = EMPTY_LIST_EXAMPLE)
    @ToString.Exclude
    protected List<UUID> areas;

    @Schema(description = "List of linked maritime entities", example = EMPTY_LIST_EXAMPLE)
    @ToString.Exclude
    @NoHtml
    protected Set<String> maritimeEntities;

    @Schema(description = "List of service tags", example = EMPTY_LIST_EXAMPLE)
    @Builder.Default
    protected Set<@Valid TagDto> tags = new HashSet<>();

    @Schema(description = "Sissy ID")
    @NoHtml
    protected String sissyId;

    @Schema(description = "SHOM service")
    protected boolean shomService;

    @Schema(description = "List of observed maritime entities", example = EMPTY_LIST_EXAMPLE)
    @ToString.Exclude
    @NoHtml
    protected Set<String> observedMaritimeEntities;

    @Schema(description = "OHI Producer Code")
    @NoHtml
    protected String ihoProducerCode;

    @Schema(description = "S124 Data Producer")
    @Builder.Default
    protected boolean s124DataProducer = false;
}
