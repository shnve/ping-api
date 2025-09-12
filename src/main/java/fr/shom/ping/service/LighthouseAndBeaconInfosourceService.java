package fr.shom.ping.service;

import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Area;
import fr.shom.ping.model.Feature;
import fr.shom.ping.model.Folder;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.InfosourcePart;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.Notion;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.dto.LighthouseAndBeaconInfosourceMessageDto;
import fr.shom.ping.model.dto.LighthouseAndBeaconInfosourcePartMessageDto;
import fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto;
import fr.shom.ping.model.enums.FolderStatus;
import fr.shom.ping.model.enums.InfosourceNature;
import fr.shom.ping.model.enums.LighthouseAndBeaconMessageDtoProperty;
import fr.shom.ping.model.enums.NotificationType;
import fr.shom.ping.model.enums.WarningStatus;
import fr.shom.ping.repository.InfosourceRepository;
import fr.shom.ping.repository.MaritimeEntityRepository;
import fr.shom.ping.repository.MaritimeServiceRepository;
import fr.shom.ping.repository.WarningRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.model.enums.InfosourceStatus.SENT;
import static fr.shom.ping.model.enums.LighthouseAndBeaconMessageDtoProperty.IDENTIFICATION_NUMBER;
import static fr.shom.ping.model.enums.LighthouseAndBeaconMessageDtoProperty.NOTICE_NUMBER;
import static fr.shom.ping.model.enums.LighthouseAndBeaconMessageDtoProperty.POSITION;
import static fr.shom.ping.model.enums.LighthouseAndBeaconMessageDtoProperty.PREVIOUS_NOTICE_NUMBER;
import static fr.shom.ping.model.enums.LighthouseAndBeaconMessageDtoProperty.TERRITORIAL_DIVISION;
import static fr.shom.ping.model.enums.LighthouseAndBeaconMessageDtoProperty.UTC_SEND_DATE;
import static java.time.Instant.now;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.locationtech.jts.geom.PrecisionModel.FLOATING;

@RequiredArgsConstructor
@Service
@Slf4j
public class LighthouseAndBeaconInfosourceService {
    private final InfosourceRepository repository;
    private final InfosourceRoutingService infosourceRoutingService;
    private final MaritimeServiceService maritimeServiceService;
    private final MaritimeServiceRepository maritimeServiceRepository;
    private final MaritimeEntityRepository maritimeEntityRepository;
    private final InfosourceRecipientService recipientService;
    private final SseNotificationService sseNotificationService;
    private final WarningRepository warningRepository;

    private static final int SRID = 3857;

    @Transactional
    public void create(LighthouseAndBeaconInfosourceMessageDto dto) throws ServiceException {
        Infosource infosource = repository.save(toInfosource(dto));
        final Set<MaritimeServiceInfosourceRecipientDto> recipients = send(infosource);
        processPreviousNoticeNumber(infosource, recipients);
    }

    @Transactional
    public void processPreviousNoticeNumber(Infosource infosource, Set<MaritimeServiceInfosourceRecipientDto> recipients) {
        final String previousNoticeNumber = (String) infosource.getObject().get(PREVIOUS_NOTICE_NUMBER.getTranslateName());
        if (StringUtils.isBlank(previousNoticeNumber)) {
            return;
        }

        final Infosource previousInfosource = repository.findByNumber(previousNoticeNumber).orElseThrow(NotFoundException::new);

        // notify warning to review
        final Set<Warning> linkedWarnings = previousInfosource.getLinkedWarnings();
        linkedWarnings.stream().filter(warning -> WarningStatus.PUBLISHED.equals(warning.getStatus()))
            .forEach(warning -> {
                    sseNotificationService.sendLighthouseAndBeaconPreviousNoticeNumberNotifications(
                        warning.getNameOfSeries() + " " +
                            (warning.getWarningNumber() != null ? warning.getWarningNumber() + "/" + warning.getYear().toString().substring(2) : "") +
                            " - " + previousInfosource.getNumber(),
                        warning.getSerie().getMaritimeService(),
                        warning
                    );
                    warning.setForceToReview(true);
                }
            );

        // update linked warnings to force 'TO_REVIEW' state in table
        if (!linkedWarnings.isEmpty()) {
            warningRepository.saveAll(linkedWarnings);
        }

        // notify folder to review
        final Set<Folder> linkedFolders = previousInfosource.getNotions().stream()
            .map(Notion::getFolder)
            .collect(toSet());
        linkedFolders.stream().filter(folder -> FolderStatus.OPEN.equals(folder.getStatus()))
            .forEach(folder -> sseNotificationService.sendFolderNotificationService(
                    folder.getNumber() + " - " +
                        ofNullable(folder.getIssuingService()).map(MaritimeService::getName).orElse(" ") + " - " +
                        ofNullable(folder.getArea()).map(Area::getName).orElse(" "),
                    NotificationType.LIGHTHOUSE_AND_BEACON_FOLDER_TO_REVIEW,
                    folder.getIssuingService(),
                    folder
                )
            );

        // notify IS
        // send notification to each IS recipient
        final List<MaritimeService> recipientsMaritimeService =
            maritimeServiceRepository.findByIdIn(recipients.stream().map(MaritimeServiceInfosourceRecipientDto::getId).collect(toList()));
        recipientsMaritimeService.forEach(maritimeService -> {
            String title = infosource.getNumber();
            if (maritimeService.isMaritimeNoticeProducer() && !isEmpty(linkedWarnings)) {
                final String warningsAsString = linkedWarnings.stream()
                    .filter(warning -> WarningStatus.PUBLISHED.equals(warning.getStatus()))
                    .map(warning -> (warning.getWarningNumber() != null ? warning.getWarningNumber() + "/" + warning.getYear().toString().substring(2) : ""))
                    .collect(Collectors.joining(" - "));
                title += "{{AN}}" + warningsAsString;
            }

            if (maritimeService.isFolderManager() && !isEmpty(linkedFolders)) {
                final String foldersAsString = linkedFolders.stream()
                    .filter(folder -> FolderStatus.OPEN.equals(folder.getStatus()))
                    .map(Folder::getNumber)
                    .map(String::valueOf)
                    .collect(Collectors.joining(" - "));
                title += "{{FOLDER}}" + foldersAsString;
            }
            sseNotificationService.sendInfosourceNotification(title, maritimeService, infosource);
        });
    }

    @Transactional
    public Set<MaritimeServiceInfosourceRecipientDto> send(Infosource infosource) throws ServiceException {
        infosource = sendInfosource(infosource);
        return sendRecipients(infosource);
    }

    private Infosource toInfosource(LighthouseAndBeaconInfosourceMessageDto lighthouseAndBeaconInfosourceMessageDto) throws ServiceException {
        List<LighthouseAndBeaconInfosourcePartMessageDto> parts = lighthouseAndBeaconInfosourceMessageDto.getParts();
        List<String> maritimeEntitySissyIds = parts.stream()
            .map(LighthouseAndBeaconInfosourcePartMessageDto::getObject)
            .map(item -> item.get(IDENTIFICATION_NUMBER.getKeyName()))
            .map(Object::toString)
            .collect(toList());
        List<MaritimeEntity> maritimeEntities = maritimeEntityRepository.findAllBySissyIds(maritimeEntitySissyIds);
        SortedSet<InfosourcePart> infosourcePartSet = toInfosourcePartSet(parts, maritimeEntities);

        if (infosourcePartSet.isEmpty()) {
            throw new ServiceException("Unable to construct InfosourcePart from Sissy");
        }

        Map<String, Object> object = lighthouseAndBeaconInfosourceMessageDto.getObject();
        MaritimeService maritimeService = maritimeServiceService.findBySissyId(object.get(TERRITORIAL_DIVISION.getKeyName()).toString());

        return Infosource.builder()
            .number(object.get(NOTICE_NUMBER.getKeyName()).toString())
            .nature(InfosourceNature.LIGHTHOUSE_AND_BEACON)
            .timezone(lighthouseAndBeaconInfosourceMessageDto.getTimezone())
            .validationTime(Instant.parse(object.get(UTC_SEND_DATE.getKeyName()).toString()))
            .issuingService(maritimeService)
            .infosourceParts(infosourcePartSet)
            .object(mapLighthouseAndBeaconInfosourceObjectOrPartObjectToInfosourceObjectOrPartObject(object))
            .entities(new HashSet<>(maritimeEntities))
            .build();
    }

    private SortedSet<InfosourcePart> toInfosourcePartSet(List<LighthouseAndBeaconInfosourcePartMessageDto> parts, List<MaritimeEntity> maritimeEntities) {
        SortedSet<InfosourcePart> infosourcePartSet = new TreeSet<>();
        AtomicInteger partPosition = new AtomicInteger(1);

        parts.forEach(part -> {
            Map<String, Object> partObject = part.getObject();

            try {
                String sissyId = partObject.get(IDENTIFICATION_NUMBER.getKeyName()).toString();

                InfosourcePart infosourcePart = InfosourcePart.builder()
                    .position(partPosition.getAndIncrement())
                    .object(mapLighthouseAndBeaconInfosourceObjectOrPartObjectToInfosourceObjectOrPartObject(partObject))
                    .description(sissyId)
                    .localisation(newHashSet(convertPositionToFeature(partObject.get(POSITION.getKeyName()).toString())))
                    .build();

                Optional<MaritimeEntity> maritimeEntity = maritimeEntities.stream()
                    .filter(entity -> sissyId.equals(entity.getSissyId()))
                    .findFirst();

                if (maritimeEntity.isPresent()) {
                    infosourcePart.setDescription(maritimeEntity.map(MaritimeEntity::getName).get());
                    infosourcePart.setInternationalDescription(maritimeEntity.map(MaritimeEntity::getInternationalName).orElse(null));
                }

                infosourcePartSet.add(infosourcePart);
            } catch (ServiceException se) {
                log.error(se.getMessage(), se);
            }

        });
        return infosourcePartSet;
    }

    private Feature convertPositionToFeature(String position) throws ServiceException {
        try {
            GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(FLOATING), SRID);
            return Feature.builder()
                .geometry(new WKTReader(geometryFactory).read(position))
                .build();
        } catch (ParseException e) {
            throw new ServiceException("Unable to parse position from Sissy : " + e.getMessage());
        }
    }

    protected Infosource sendInfosource(Infosource infosource) {
        Instant now = now();
        infosource.setLastUpdate(now);
        infosource.setValidationTime(now);
        infosource.setStatus(SENT);
        return repository.save(infosource);
    }

    protected Set<MaritimeServiceInfosourceRecipientDto> sendRecipients(Infosource infosource) throws ServiceException {
        Set<MaritimeServiceInfosourceRecipientDto> recipients = infosourceRoutingService.getRecipients(infosource, false);

        List<MaritimeService> recipientsMaritimeService = maritimeServiceService.findAllByIdIn(
            recipients.stream().map(MaritimeServiceInfosourceRecipientDto::getId).collect(toList())
        );
        recipientService.send(recipientsMaritimeService, infosource, null, null, false);
        return recipients;
    }

    private Map<String, Object> mapLighthouseAndBeaconInfosourceObjectOrPartObjectToInfosourceObjectOrPartObject(
        Map<String, Object> lighthouseAndBeaconInfosourceObjectOrPartObject
    ) {
        return lighthouseAndBeaconInfosourceObjectOrPartObject.entrySet().stream()
            .filter(k -> LighthouseAndBeaconMessageDtoProperty.valueOfToBeSaved(k.getKey()))
            .filter(k -> Objects.nonNull(k.getValue()))
            .collect(Collectors.toMap(
                k -> LighthouseAndBeaconMessageDtoProperty.valueOfTranslateName(k.getKey()),
                Map.Entry::getValue
            ));
    }
}
