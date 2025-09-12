package fr.shom.ping.service;

import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Feature;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.InfosourcePart;
import fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto;
import fr.shom.ping.model.enums.MaritimeServiceType;
import fr.shom.ping.properties.PingProperties;
import fr.shom.ping.repository.InfosourceRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

import static fr.shom.ping.model.enums.InfosourceNature.HAZARD_REPORT;
import static fr.shom.ping.model.enums.LighthouseAndBeaconMessageDtoProperty.NATURE_CODE;
import static fr.shom.ping.model.enums.LighthouseAndBeaconMessageDtoProperty.PREVIOUS_NOTICE_NUMBER;
import static fr.shom.ping.model.enums.LighthouseAndBeaconMessageDtoProperty.RECOVERY_TIME_CODE;
import static fr.shom.ping.model.enums.MaritimeServiceType.CAPTAINCY;
import static fr.shom.ping.model.enums.MaritimeServiceType.CROSS_VTS;
import static fr.shom.ping.model.enums.MaritimeServiceType.LIGHTHOUSES_AND_BEACONS;
import static fr.shom.ping.model.enums.MaritimeServiceType.NATIONAL_DELEGATED_COORDINATOR;
import static fr.shom.ping.model.enums.MaritimeServiceType.NAVAREA_COORDINATOR;
import static fr.shom.ping.model.enums.MaritimeServiceType.PORT_VTS;
import static fr.shom.ping.model.enums.MaritimeServiceType.SEMAPHORE;
import static fr.shom.ping.model.enums.MaritimeServiceType.SHOM_GEOGRAPHIC_AREA;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfosourceRoutingService {

    private final MaritimeServiceService maritimeServiceService;
    private final InfosourceRepository repository;
    private final PingProperties pingProperties;

    public Set<MaritimeServiceInfosourceRecipientDto> getRecipients(Infosource infosource, boolean shouldGetAdditionalRecipients) throws ServiceException {
        switch (infosource.getNature()) {
            case HAZARD_REPORT:
                return getRecipientsForHazardReport(infosource, shouldGetAdditionalRecipients);
            case OTHER_REPORT:
            case ACTIVITY_FORECAST:
            case BATHY:
                return getRecipientsForNewDanger(infosource, shouldGetAdditionalRecipients);
            case MAP_ANOMALY:
            case PUBLICATION_ANOMALY:
            case FUTURE_INFORMATION:
                return getRecipientsForMapOrPublicationUpdate(infosource);
            case LIGHTHOUSE_AND_BEACON:
                return getRecipientsForLighthouseAndBeacon(infosource);
            case SURVEY_TRANSMISSION:
                return getRecipientsForSurveyDepositTransmission(infosource);
            default:
                break;
        }
        return Collections.emptySet();
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

    private Set<MaritimeServiceInfosourceRecipientDto> getRecipientsForHazardReport(Infosource infosource, boolean shouldGetAdditionalRecipients)
        throws ServiceException {

        Set<MaritimeServiceInfosourceRecipientDto> defaultRecipients = getRecipientsForMaritimeServiceType(infosource, List.of(LIGHTHOUSES_AND_BEACONS));

        // no intersection with L&B
        if (defaultRecipients.isEmpty()) {
            return getRecipientsForNewDanger(infosource, shouldGetAdditionalRecipients);
        }

        // intersection with L&B :
        //  - L&B zones services
        //  - intersection with CROSS VTS, VTS PORT, SEMA, CAP
        defaultRecipients = Stream.of(defaultRecipients, getRecipientsForMaritimeServiceType(infosource, List.of(SEMAPHORE, CAPTAINCY, CROSS_VTS, PORT_VTS)))
            .flatMap(Collection::stream)
            .collect(toSet());


        if (!shouldGetAdditionalRecipients) {
            return defaultRecipients;
        }

        Set<MaritimeServiceInfosourceRecipientDto> additionalRecipients = getRecipientsForMaritimeServiceType(infosource,
            List.of(NATIONAL_DELEGATED_COORDINATOR, CAPTAINCY, SHOM_GEOGRAPHIC_AREA, NAVAREA_COORDINATOR));

        additionalRecipients.forEach(additionalRecipient -> additionalRecipient.setDefaultService(false));

        return Stream.of(defaultRecipients, additionalRecipients).flatMap(Collection::stream).collect(toSet());
    }

    private Set<MaritimeServiceInfosourceRecipientDto> getRecipientsForNewDanger(Infosource infosource,
        boolean shouldGetAdditionalRecipients) throws ServiceException {

        Set<MaritimeServiceInfosourceRecipientDto> defaultRecipients = getRecipientsForMaritimeServiceType(infosource,
            List.of(NATIONAL_DELEGATED_COORDINATOR, SEMAPHORE, CAPTAINCY, CROSS_VTS, PORT_VTS));

        Set<MaritimeServiceInfosourceRecipientDto> navareaRecipients = new HashSet<>();
        Set<MaritimeServiceInfosourceRecipientDto> additionalRecipients = new HashSet<>();

        if (defaultRecipients.stream().noneMatch(recipients -> recipients.getType() == NATIONAL_DELEGATED_COORDINATOR)) {
            navareaRecipients = getRecipientsForMaritimeServiceType(infosource, List.of(NAVAREA_COORDINATOR));
            if (navareaRecipients.isEmpty()) {
                throw new ServiceException("infosource-routing.rejected");
            }

            if (shouldGetAdditionalRecipients) {
                additionalRecipients = getRecipientsForMaritimeServiceType(infosource, List.of(SHOM_GEOGRAPHIC_AREA));
            }
        } else if (shouldGetAdditionalRecipients) {
            additionalRecipients = infosource.getNature() == HAZARD_REPORT ?
                getRecipientsForMaritimeServiceType(infosource, List.of(SHOM_GEOGRAPHIC_AREA, NAVAREA_COORDINATOR))
                : getRecipientsForMaritimeServiceType(infosource, List.of(SHOM_GEOGRAPHIC_AREA));
        }

        additionalRecipients.forEach(additionalRecipient -> additionalRecipient.setDefaultService(false));

        return Stream.of(defaultRecipients, navareaRecipients, additionalRecipients).flatMap(Collection::stream).collect(toSet());
    }

    private Set<MaritimeServiceInfosourceRecipientDto> getRecipientsForMapOrPublicationUpdate(Infosource infosource) throws ServiceException {
        Set<MaritimeServiceInfosourceRecipientDto> recipients = getRecipientsForMaritimeServiceType(infosource, List.of(SHOM_GEOGRAPHIC_AREA));
        if (recipients.isEmpty()) {
            throw new ServiceException("infosource-routing.rejected");
        }
        return recipients;
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

    private Set<MaritimeServiceInfosourceRecipientDto> getRecipientsForLighthouseAndBeacon(Infosource infosource) throws ServiceException {
        Set<MaritimeServiceInfosourceRecipientDto> recipients = getRecipientsForMaritimeServiceType(infosource,
            List.of(NATIONAL_DELEGATED_COORDINATOR, SEMAPHORE, CAPTAINCY));

        if (recipients.stream().noneMatch(r -> r.getType() == NATIONAL_DELEGATED_COORDINATOR)) {
            recipients = Stream.of(recipients, getRecipientsForMaritimeServiceType(infosource, List.of(NAVAREA_COORDINATOR)))
                .flatMap(Collection::stream)
                .collect(toSet());
        }

        recipients = getRecipientsForShomGeographicAreaByCode(infosource, RECOVERY_TIME_CODE.getTranslateName(),
            IntStream.rangeClosed(6, 8), recipients);
        recipients = getRecipientsForShomGeographicAreaByCode(infosource, NATURE_CODE.getTranslateName(),
            IntStream.rangeClosed(4, 5), recipients);

        if (recipients.isEmpty() && ofNullable(infosource.getObject().get(PREVIOUS_NOTICE_NUMBER.getTranslateName())).isPresent()) {
            String previousNoticeNumber = infosource.getObject().get(PREVIOUS_NOTICE_NUMBER.getTranslateName()).toString();
            Optional<Infosource> infosourceOptional = repository.findByNumber(previousNoticeNumber);
            if (infosourceOptional.isPresent()) {
                recipients = getRecipientsForLighthouseAndBeacon(infosourceOptional.get());
            }
        }

        if (recipients.isEmpty()) {
            log.error("Global routing recipients failed : no recipients for infosource {}", infosource.getId());
        }
        return recipients;
    }

    private Set<MaritimeServiceInfosourceRecipientDto> getRecipientsForShomGeographicAreaByCode(Infosource infosource, String codeName, IntStream range,
        Set<MaritimeServiceInfosourceRecipientDto> recipients) throws ServiceException {
        log.info("Try to get recipients for shom geographic area with IS {} with {}", infosource.getId(), codeName);
        int size = recipients.size();
        if (ofNullable(infosource.getObject().get(codeName)).isPresent()) {
            try {
                int code = Integer.parseInt(infosource.getObject().get(codeName).toString());
                if (range.anyMatch(i -> i == code)) {
                    recipients = Stream.of(recipients, getRecipientsForMaritimeServiceType(infosource, List.of(SHOM_GEOGRAPHIC_AREA)))
                        .flatMap(Collection::stream)
                        .collect(toSet());
                }
            } catch (NumberFormatException e) {
                throw new ServiceException("Code cannot be parsed : " + e.getMessage());
            }
        }

        if (recipients.size() == size) {
            log.error("Routing recipients for Shom geographic area : no recipients for infosource {}", infosource.getId());
        }
        return recipients;
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
