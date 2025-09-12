package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Area;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Maritime Service Dto Model")
public class MaritimeServiceDto extends AbstractMaritimeServiceDto {

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    public MaritimeServiceDto(MaritimeService service) {
        this.id = service.getId();
        this.localizedInformation = service.getLocalizedInformation();
        this.information = service.getInformation();
        this.internationalName = service.getInternationalName();
        this.name = service.getName();
        this.urn = service.getUrn();
        this.country = service.getCountry();
        this.maritimeNoticeProducer = service.isMaritimeNoticeProducer();
        this.pingPartner = service.isPingPartner();
        this.folderManager = service.isFolderManager();
        this.surveyOwner = service.isSurveyOwner();
        this.productionAgency = service.getProductionAgency();
        this.serviceNetwork = service.getServiceNetwork();
        this.type = service.getType();
        this.authorityCode = service.getAuthorityCode();
        this.maritimeEntities = Optional.ofNullable(service.getMaritimeEntities())
            .map(maritimeEntities -> maritimeEntities.stream().map(MaritimeEntity::getName).collect(Collectors.toSet()))
            .orElse(null);
        this.areas = Optional.ofNullable(service.getAreas())
            .map(areas -> areas.stream().map(Area::getId).collect(Collectors.toList()))
            .orElse(null);
        if (isNotEmpty(service.getTags())) {
            this.tags = service.getTags().stream()
                .map(Tag::asDto)
                .sorted(Comparator.comparing(tagDto -> tagDto.value))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        this.sissyId = service.getSissyId();
        this.shomService = service.isShomService();
        this.observedMaritimeEntities = Optional.ofNullable(service.getObservedMaritimeEntities())
            .map(maritimeEntities -> maritimeEntities.stream().map(MaritimeEntity::getName).collect(Collectors.toSet()))
            .orElse(null);
        this.ihoProducerCode = service.getIhoProducerCode();
        this.s124DataProducer = service.isS124DataProducer();
    }
}
