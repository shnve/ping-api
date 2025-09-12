package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.GeoregulationSubscription;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.SubArea;
import fr.shom.ping.model.Subscription;
import fr.shom.ping.model.WarningSubscription;
import fr.shom.ping.model.enums.SubscriptionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Subscription Dto Model")
public class SubscriptionDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -2447015511003707944L;

    protected UUID id;

    protected String title;

    protected SubscriptionType type;

    protected boolean sendingMail;

    protected MaritimeServiceDto service;

    protected AreaDto area;

    @Builder.Default
    protected Set<MaritimeEntityDto> maritimeEntitiesGeographicExtent = new HashSet<>();

    public static SubscriptionDto fromSubscription(Subscription subscription) {
        SubscriptionDto subscriptionDto;
        if (subscription.getType() == SubscriptionType.WARNING) {
            subscriptionDto = new WarningSubscriptionDto();
            WarningSubscription warningSubscription = (WarningSubscription) subscription;
            WarningSubscriptionDto warningSubscriptionDto = (WarningSubscriptionDto) subscriptionDto;
            warningSubscriptionDto.setSerie(warningSubscription.getSerie().asDto());
            warningSubscriptionDto.setLang(warningSubscription.getLang());
            warningSubscriptionDto.setSubAreas(warningSubscription.getSubAreas().stream()
                .map(SubArea::asDto).collect(Collectors.toSet()));
            warningSubscriptionDto.setWarningHazardTypeGenerals(warningSubscription.getWarningHazardTypeGenerals());
        } else {
            subscriptionDto = new GeoregulationSubscriptionDto();
            GeoregulationSubscription georegulationSubscription = (GeoregulationSubscription) subscription;
            GeoregulationSubscriptionDto georegulationSubscriptionDto = (GeoregulationSubscriptionDto) subscriptionDto;
            georegulationSubscriptionDto.setGeoregulationTypes(georegulationSubscription.getGeoregulationTypes());
            georegulationSubscriptionDto.setMaritimeEntities(georegulationSubscription.getMaritimeEntities().stream()
                .map(MaritimeEntity::asDto).collect(Collectors.toSet()));
        }

        subscriptionDto.setId(subscription.getId());
        subscriptionDto.setTitle(subscription.getTitle());
        subscriptionDto.setType(subscription.getType());
        subscriptionDto.setSendingMail(subscription.isSendingMail());
        subscriptionDto.setService(subscription.getService() != null ? subscription.getService().asDto() : null);
        subscriptionDto.setArea(subscription.getArea().asDto());
        subscriptionDto.setMaritimeEntitiesGeographicExtent(subscription.getMaritimeEntitiesGeographicExtent().stream()
            .map(MaritimeEntity::asDto).collect(Collectors.toSet()));

        return subscriptionDto;
    }
}
