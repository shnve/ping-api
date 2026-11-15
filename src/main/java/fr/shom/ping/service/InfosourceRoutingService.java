package fr.shom.ping.service;

import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Feature;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.InfosourcePart;
import fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto;
import fr.shom.ping.model.enums.MaritimeServiceType;
import fr.shom.ping.properties.PingProperties;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

import static fr.shom.ping.model.enums.MaritimeServiceType.CAPTAINCY;
import static fr.shom.ping.model.enums.MaritimeServiceType.NATIONAL_DELEGATED_COORDINATOR;
import static fr.shom.ping.model.enums.MaritimeServiceType.NAVAREA_COORDINATOR;
import static fr.shom.ping.model.enums.MaritimeServiceType.SEMAPHORE;
import static fr.shom.ping.model.enums.MaritimeServiceType.SHOM_GEOGRAPHIC_AREA;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

/**
 * Destinatarios de las contribuciones (fuentes de información) al enviarlas.
 * <p>
 * PISAC (15-nov-2026): en Venezuela el enrutamiento es nacional, no por intersección geográfica con las zonas de
 * servicio (modelo SHOM, que proponía capitanías, semáforos, VTS...):
 * <ul>
 *   <li>Anomalía en el balizamiento, nuevo peligro, previsión de actividades, profundidades peligrosas y faro y
 *   baliza: solo el coordinador delegado nacional (<b>SHN - AN</b>, productor S-124).</li>
 *   <li>Anomalía en carta náutica, anomalía en publicación náutica e información para futuras actualizaciones:
 *   el área geográfica SHN (<b>SHN - Cartografía</b>) y el coordinador delegado nacional (<b>SHN - AN</b>).</li>
 *   <li>Transmisión de levantamientos: sin cambios (BRI y áreas geográficas que intersecan).</li>
 * </ul>
 * Todos los destinatarios propuestos van marcados por defecto. Se buscan por tipo de servicio, tengan o no zona de
 * servicio dibujada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InfosourceRoutingService {

    /** Contribuciones que recibe solo SHN - AN. */
    private static final List<MaritimeServiceType> NATIONAL_COORDINATION_TYPES = List.of(NATIONAL_DELEGATED_COORDINATOR);

    /** Ajustes a cartas y publicaciones: SHN - Cartografía y SHN - AN. */
    private static final List<MaritimeServiceType> CHART_UPDATE_TYPES = List.of(SHOM_GEOGRAPHIC_AREA, NATIONAL_DELEGATED_COORDINATOR);

    private final MaritimeServiceService maritimeServiceService;
    private final PingProperties pingProperties;

    /**
     * @param shouldGetAdditionalRecipients se conserva por compatibilidad con los llamadores; con el enrutamiento
     *                                      nacional no hay destinatarios adicionales opcionales
     */
    public Set<MaritimeServiceInfosourceRecipientDto> getRecipients(Infosource infosource, boolean shouldGetAdditionalRecipients) throws ServiceException {
        switch (infosource.getNature()) {
            case HAZARD_REPORT:
            case OTHER_REPORT:
            case ACTIVITY_FORECAST:
            case BATHY:
                return getNationalRecipients(infosource, NATIONAL_COORDINATION_TYPES, true);
            case LIGHTHOUSE_AND_BEACON:
                // Mensajes de faro y baliza (también llegan por importación automática): sin destinatario no se rechaza,
                // solo se registra el error, como antes
                return getNationalRecipients(infosource, NATIONAL_COORDINATION_TYPES, false);
            case MAP_ANOMALY:
            case PUBLICATION_ANOMALY:
            case FUTURE_INFORMATION:
                return getNationalRecipients(infosource, CHART_UPDATE_TYPES, true);
            case SURVEY_TRANSMISSION:
                return getRecipientsForSurveyDepositTransmission(infosource);
            default:
                break;
        }
        return Collections.emptySet();
    }

    private Set<MaritimeServiceInfosourceRecipientDto> getNationalRecipients(Infosource infosource, Collection<MaritimeServiceType> maritimeServiceTypes,
        boolean rejectIfEmpty) throws ServiceException {
        final Set<MaritimeServiceInfosourceRecipientDto> recipients = new HashSet<>(maritimeServiceService.getRecipientsByType(maritimeServiceTypes));
        recipients.forEach(recipient -> recipient.setDefaultService(true));

        if (recipients.isEmpty()) {
            log.error("Enrutamiento: no hay servicios de tipo {} para la contribucion {} ({})", maritimeServiceTypes, infosource.getId(),
                infosource.getNature());
            if (rejectIfEmpty) {
                throw new ServiceException("infosource-routing.rejected");
            }
        }
        return recipients;
    }

    private Set<MaritimeServiceInfosourceRecipientDto> getRecipientsForSurveyDepositTransmission(Infosource infosource) throws ServiceException {
        // send to BRI services
        final List<MaritimeServiceInfosourceRecipientDto> defaultRecipients = getRecipientsForMaritimeServiceType(List.of(MaritimeServiceType.BRI));

        // add services which geometries intersects IS deposit bounding box (stored as localisation here)
        final Set<MaritimeServiceInfosourceRecipientDto> additionalRecipients = getRecipientsForMaritimeServiceType(infosource, List.of(SHOM_GEOGRAPHIC_AREA));
        final Set<MaritimeServiceInfosourceRecipientDto> allRecipients = Stream.of(defaultRecipients, additionalRecipients)
            .flatMap(Collection::stream)
            .collect(toSet());
        if (allRecipients.isEmpty()) {
            throw new ServiceException("infosource-routing.rejected");
        }
        return allRecipients;
    }

    private List<MaritimeServiceInfosourceRecipientDto> getRecipientsForMaritimeServiceType(Collection<MaritimeServiceType> maritimeServiceTypes) {
        return maritimeServiceService.getMaritimeServiceByType(maritimeServiceTypes);
    }

    private Set<MaritimeServiceInfosourceRecipientDto> getRecipientsForMaritimeServiceType(Infosource infosource, Collection<MaritimeServiceType> maritimeServiceTypes) {
        List<Geometry> geometries = infosource.getInfosourceParts().stream()
            .map(InfosourcePart::getLocalisation)
            .flatMap(Collection::stream)
            .map(Feature::getGeometry)
            .collect(toList());

        return getRecipientsForMaritimeServiceTypeByGeometries(geometries, maritimeServiceTypes);
    }

    private Set<MaritimeServiceInfosourceRecipientDto> getRecipientsForMaritimeServiceTypeByGeometries(List<Geometry> geometries,
        Collection<MaritimeServiceType> maritimeServiceTypes) {
        Set<MaritimeServiceInfosourceRecipientDto> defaultRecipients = new HashSet<>();
        geometries.forEach(geometry -> defaultRecipients.addAll(maritimeServiceService.getMaritimeServiceByTypeWithIntersection(geometry, maritimeServiceTypes)));
        return defaultRecipients;
    }

    public List<MaritimeServiceInfosourceRecipientDto> getRecipientsForGeoregulationByDetails(UUID issuingServiceId, List<Geometry> geometries,
        List<FixedDateRange> dateRanges, LocalDate signingDate) throws ServiceException {
        Set<MaritimeServiceInfosourceRecipientDto> defaultRecipients = getRecipientsForMaritimeServiceTypeByGeometries(geometries,
            List.of(NATIONAL_DELEGATED_COORDINATOR, SEMAPHORE, CAPTAINCY));

        Set<MaritimeServiceInfosourceRecipientDto> navareaRecipients = new HashSet<>();

        if (defaultRecipients.stream().noneMatch(recipients -> recipients.getType() == NATIONAL_DELEGATED_COORDINATOR)) {
            navareaRecipients = getRecipientsForMaritimeServiceTypeByGeometries(geometries, List.of(NAVAREA_COORDINATOR));
            if (navareaRecipients.isEmpty()) {
                throw new ServiceException("infosource-routing.rejected");
            }
        }

        Optional<LocalDate> olderStartDate = empty();
        Optional<LocalDate> latestEndDate = empty();
        if (isNotEmpty(dateRanges)) {
            olderStartDate = dateRanges.stream().map(FixedDateRange::getDateStart).filter(Objects::nonNull).min(LocalDate::compareTo);
            latestEndDate = dateRanges.stream().map(FixedDateRange::getDateEnd).filter(Objects::nonNull).max(LocalDate::compareTo);
        }

        if (olderStartDate.isEmpty()) {
            olderStartDate = Optional.of(signingDate);
        }

        if (latestEndDate.isEmpty() ||
            DAYS.between(olderStartDate.get(), latestEndDate.get()) >= pingProperties.getGeoregulationMaxGlobalPeriodDurationInDays()) {
            defaultRecipients.addAll(getRecipientsForMaritimeServiceTypeByGeometries(geometries, List.of(SHOM_GEOGRAPHIC_AREA)));
        }

        Set<UUID> excludedRecipientIds = Stream.of(defaultRecipients, navareaRecipients)
            .flatMap(Collection::stream)
            .map(MaritimeServiceInfosourceRecipientDto::getId)
            .collect(toSet());
        List<MaritimeServiceInfosourceRecipientDto> additionalRecipients =
            maritimeServiceService.findRecipientsWithAreasInServiceAreasExcludingIds(issuingServiceId, excludedRecipientIds);
        additionalRecipients.forEach(additionalRecipient -> additionalRecipient.setDefaultService(false));

        return Stream.of(defaultRecipients, navareaRecipients, additionalRecipients).flatMap(Collection::stream).collect(toList());
    }
}
