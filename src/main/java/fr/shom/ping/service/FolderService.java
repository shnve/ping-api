package fr.shom.ping.service;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Area;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Folder;
import fr.shom.ping.model.FolderAttachment;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.Notion;
import fr.shom.ping.model.NotionGeoregulation;
import fr.shom.ping.model.NotionInfosource;
import fr.shom.ping.model.NotionWarning;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.model.Study;
import fr.shom.ping.model.SubArea;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.dto.AbstractMaritimeServiceDto;
import fr.shom.ping.model.dto.FolderAttachmentSaveDto;
import fr.shom.ping.model.dto.FolderAvailableForNotionDto;
import fr.shom.ping.model.dto.FolderDto;
import fr.shom.ping.model.dto.FolderReferenceDto;
import fr.shom.ping.model.dto.FolderSaveDto;
import fr.shom.ping.model.dto.FolderTableDto;
import fr.shom.ping.model.dto.MaritimeServiceAvailableToFollowFolderDto;
import fr.shom.ping.model.dto.ReferencesByFolderDto;
import fr.shom.ping.model.dto.StudyReferenceDto;
import fr.shom.ping.model.enums.FolderReviewStatus;
import fr.shom.ping.model.enums.GeoregulationStatus;
import fr.shom.ping.model.enums.InfosourceStatus;
import fr.shom.ping.model.enums.NotionType;
import fr.shom.ping.model.enums.ServiceNetwork;
import fr.shom.ping.model.enums.ServiceRole;
import fr.shom.ping.model.enums.WarningStatus;
import fr.shom.ping.model.predicate.FolderSearchCriteria;
import fr.shom.ping.repository.AttachmentRepository;
import fr.shom.ping.repository.FolderAttachmentRepository;
import fr.shom.ping.repository.FolderRepository;
import fr.shom.ping.repository.GeoregulationRepository;
import fr.shom.ping.repository.InfosourceRepository;
import fr.shom.ping.repository.MaritimeEntityRepository;
import fr.shom.ping.repository.MaritimeServiceRepository;
import fr.shom.ping.repository.ServiceUserRepository;
import fr.shom.ping.repository.WarningRepository;
import fr.shom.ping.utils.search.FolderTableUtil;
import jakarta.persistence.EntityManager;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.model.dto.FolderDto.fromFolder;
import static fr.shom.ping.model.enums.FolderStatus.CLOSED;
import static fr.shom.ping.model.enums.InfosourceNature.LIGHTHOUSE_AND_BEACON;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_ADMIN;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_OPERATOR;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderService {

    protected final EntityManager entityManager;
    private final FolderRepository repository;
    private final AreaService areaService;
    private final SubAreaService subAreaService;
    private final MaritimeEntityRepository maritimeEntityRepository;
    private final ServiceUserRepository serviceUserRepository;
    private final MaritimeServiceRepository maritimeServiceRepository;
    private final InfosourceRepository infosourceRepository;
    private final WarningRepository warningRepository;
    private final GeoregulationRepository georegulationRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AttachmentRepository attachmentRepository;
    private final FolderAttachmentRepository folderAttachmentRepository;
    private final AttachmentService attachmentService;
    private final SissyService sissyService;
    private final SseNotificationService sseNotificationService;

    public PageImpl<FolderTableDto> findAllBySearchCriteria(FolderSearchCriteria searchCriteria, Pageable pageable, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        PageImpl<FolderTableDto> folders = FolderTableUtil.find(entityManager, searchCriteria, pageable, serviceUser);
        folders.stream().forEach(f -> f.setReviewStatus(FolderReviewStatus.getByReviewDate(f.getReviewDate())));
        return folders;
    }

    @Transactional(readOnly = true)
    public FolderDto findById(UUID id, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Folder folder = repository.findById(id).orElseThrow(NotFoundException::new);

        List<UUID> serviceIdsWithOperatorOrAdminRole = serviceUser.getServiceIdsWithOperatorOrAdminRole();
        if (!serviceIdsWithOperatorOrAdminRole.contains(folder.getIssuingService().getId())) {

            if (serviceUser.getService().getServiceNetwork() != ServiceNetwork.GOVERNMENT_SERVICE) {
                throw new ForbiddenException();
            }

            folder.setNotions(folder.getNotions().stream().filter(this::isNotionStatusNotDraft).collect(toSet()));

            for (Study study : folder.getStudies()) {
                study.setNotions(study.getNotions().stream().filter(this::isNotionStatusNotDraft).collect(toSet()));
            }

            if (!serviceUser.getService().isShomService()) {
                folder.getStudies().clear();
            }
        }

        return fromFolder(folder, serviceIdsWithOperatorOrAdminRole);
    }

    private boolean isNotionStatusNotDraft(Notion notion) {
        if (notion.getType() == NotionType.INFOSOURCE) {
            return ((NotionInfosource) notion).getInfosource().getStatus() != InfosourceStatus.DRAFT;
        } else if (notion.getType() == NotionType.WARNING) {
            return ((NotionWarning) notion).getWarning().getStatus() != WarningStatus.DRAFT;
        } else {
            return ((NotionGeoregulation) notion).getGeoregulation().getStatus() != GeoregulationStatus.DRAFT;
        }
    }

    public FolderSaveDto findFolderDetailById(UUID id, String username) {
        Folder folder = getFolderById(id, username);
        return FolderSaveDto.fromFolder(id, folder);
    }

    private Folder getFolderById(UUID id, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Folder folder = repository.findById(id).orElseThrow(NotFoundException::new);

        if (!serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(folder.getIssuingService().getId())) {
            throw new ForbiddenException();
        }
        return folder;
    }

    public Set<ReferencesByFolderDto> findAllReferencesByFolderIds(List<UUID> ids) {
        Set<FolderReferenceDto> folderReferences = repository.findAllReferencesByFolderIds(ids);

        Map<String, ReferencesByFolderDto> referencesByFolderDtoHashMap = new HashMap<>();

        folderReferences.forEach(folderReference -> {
            final StudyReferenceDto studyReferenceDto = StudyReferenceDto.builder()
                .type(folderReference.getType())
                .value(folderReference.getValue())
                .build();

            final String key = folderReference.getFolderId() + "-" + folderReference.getStudyNumber();

            if (referencesByFolderDtoHashMap.containsKey(key)) {
                referencesByFolderDtoHashMap.get(key).getStudyReferences().add(studyReferenceDto);
            } else {
                referencesByFolderDtoHashMap.put(key,
                    ReferencesByFolderDto.builder()
                        .folderId(folderReference.getFolderId())
                        .studyNumber(folderReference.getStudyNumber())
                        .studyReferences(newHashSet(studyReferenceDto))
                        .build());
            }
        });

        return referencesByFolderDtoHashMap.values().stream().collect(toSet());
    }

    public UUID create(FolderSaveDto dto, String username) {
        return saveFolder(null, dto, username);
    }

    public UUID update(UUID id, FolderSaveDto dto, String username) {
        return saveFolder(id, dto, username);
    }

    private UUID saveFolder(UUID id, FolderSaveDto dto, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Folder folder;
        if (id != null) {
            folder = repository.findById(id).orElseThrow(NotFoundException::new);
            if (!serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(folder.getIssuingService().getId())) {
                throw new ForbiddenException();
            }
        } else {
            MaritimeService service = serviceUser.getService();

            boolean noneOfServicesOnWhichUserHasRightsIsFolderManager =
                maritimeServiceRepository.findAllByIdIn(serviceUser.getServiceIdsWithOperatorOrAdminRole()).stream().noneMatch(AbstractMaritimeServiceDto::isFolderManager);

            if (!service.isFolderManager() && noneOfServicesOnWhichUserHasRightsIsFolderManager) {
                throw new ForbiddenException();
            }
            folder = new Folder();
            // Check if reserved number is available
            folder.setNumber(nextAvailableNumber());
            folder.setIssuingService(service);
        }

        Area area = areaService.getOne(dto.getAreaId());
        List<MaritimeEntity> entities = new ArrayList<>();
        if (dto.getConcernedMaritimeEntityIds() != null) {
            entities = maritimeEntityRepository.findAllById(dto.getConcernedMaritimeEntityIds());
        }

        folder.setComment(dto.getComment());
        folder.setObject(dto.getObject());
        folder.setReviewDate(dto.getReviewDate());
        folder.setArea(area);
        folder.setStatus(dto.getStatus());

        SubArea subArea = null;
        if (dto.getLocalityId() != null) {
            subArea = subAreaService.getOne(dto.getLocalityId());
        }

        folder.setLocality(subArea);

        folder.setEntities(new HashSet<>(entities));

        Folder savedFolder = repository.save(folder);

        sendFolderUpdate(savedFolder);

        return savedFolder.getId();
    }

    private void sendFolderUpdate(Folder folder) {
        if (folder.getStatus() != CLOSED) {
            return;
        }

        List<Infosource> infosources = folder.getNotions().stream()
            .filter(notion -> notion.getType() == NotionType.INFOSOURCE)
            .map(notion -> (NotionInfosource) notion)
            .map(NotionInfosource::getInfosource)
            .filter(infosource -> infosource.getNature() == LIGHTHOUSE_AND_BEACON)
            .collect(toList());

        if (isNotEmpty(infosources)) {
            sissyService.sendFolderUpdate(infosources);
        }
    }

    public Integer nextAvailableNumber() {
        Integer number = 1;
        Integer lastReservedNumber = repository.findLastReservedNumberByYear(Year.now(UTC).getValue());
        if (lastReservedNumber != null) {
            number = lastReservedNumber + 1;
        }
        return number;
    }

    @Transactional
    public void delete(UUID id, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Folder folder = repository.findById(id).orElseThrow(NotFoundException::new);
        checkIfServiceUserCanDeleteFolder(serviceUser, folder);

        for (FolderAttachment folderAttachment : folder.getAttachments()) {
            try {
                attachmentService.deleteAttachmentFile(folderAttachment.getAttachment());
            } catch (ServiceException ignored) {
                // Nothing to do as we don't want to notify user
            }
        }

        repository.delete(folder);
    }

    private void checkIfServiceUserCanDeleteFolder(ServiceUser serviceUser, Folder folder) {
        UUID serviceId = folder.getIssuingService().getId();
        if (serviceUser.getRoles().stream()
            .noneMatch(role -> role.getServiceId().equals(serviceId) &&
                asList(SERVICE_ADMIN, SERVICE_OPERATOR).contains(role.getServiceRole()))) {
            throw new ForbiddenException("User has no mandatory role for serviceId " + serviceId);
        }
    }

    public List<FolderAvailableForNotionDto> findAvailableFoldersToAddNotionTo(UUID notionId, NotionType type, String username) {
        List<UUID> issuingServiceIds = maritimeServiceRepository.findFolderManagerByServiceUsernameWithOperatorOrAdminRole(username);

        if (issuingServiceIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<FolderAvailableForNotionDto> folders = findAvailableFoldersByIssuingServiceIds(issuingServiceIds, notionId);
        folders.addAll(findAvailableFoldersByNotionMaritimeEntity(issuingServiceIds, notionId, type));

        return folders;
    }

    private List<FolderAvailableForNotionDto> findAvailableFoldersByIssuingServiceIds(List<UUID> issuingServiceIds, UUID notionId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("issuingServiceIds", issuingServiceIds);
        parameters.addValue("notionId", notionId);

        List<FolderAvailableForNotionDto> folders = jdbcTemplate.query(
            "SELECT DISTINCT f.id, f.number, f.creation_date, ms.name AS area " +
                "FROM folder f " +
                "LEFT JOIN maritime_service ms ON ms.id = f.issuing_service_id " +
                "WHERE f.status = 'OPEN' " +
                "AND f.issuing_service_id IN (:issuingServiceIds) " +
                "AND f.id NOT IN (" +
                "   SELECT f.id " +
                "   FROM folder f " +
                "   LEFT JOIN notion n ON f.id = n.folder_id " +
                "   WHERE n.warning_id = :notionId " +
                "   OR n.georegulation_id = :notionId " +
                "   OR n.infosource_id = :notionId" +
                ") " +
                "ORDER BY f.number",
            parameters,
            new BeanPropertyRowMapper<>(FolderAvailableForNotionDto.class)
        );
        return folders;
    }

    private List<FolderAvailableForNotionDto> findAvailableFoldersByNotionMaritimeEntity(List<UUID> issuingServiceIds, UUID notionId, NotionType type) {
        String notionEntityTable = "georegulation_entity";
        String notionEntityId = "entity_id";

        if (type == NotionType.INFOSOURCE) {
            notionEntityTable = "infosource_maritime_entity";
            notionEntityId = "maritime_entity_id";
        }

        if (type == NotionType.WARNING) {
            notionEntityTable = "warning_entity";
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("issuingServiceIds", issuingServiceIds);
        parameters.addValue("notionId", notionId);

        return jdbcTemplate.query(
            "SELECT DISTINCT f.id, f.number, f.creation_date " +
                "FROM folder f " +
                "LEFT JOIN folder_maritime_entity e ON f.id = e.folder_id " +
                "WHERE f.issuing_service_id IN (:issuingServiceIds) " +
                "AND e.maritime_entity_id IN (SELECT ne." + notionEntityId + " FROM " + notionEntityTable + " ne)" +
                "AND f.id NOT IN (" +
                "   SELECT f.id " +
                "   FROM folder f " +
                "   LEFT JOIN notion n ON f.id = n.folder_id " +
                "   WHERE n.warning_id = :notionId " +
                "   OR n.georegulation_id = :notionId " +
                "   OR n.infosource_id = :notionId" +
                ") " +
                "ORDER BY f.number",
            parameters,
            new BeanPropertyRowMapper<>(FolderAvailableForNotionDto.class));
    }

    @Transactional
    public void addNotionToFolder(UUID folderId, UUID notionId, NotionType type, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Folder folder = repository.findById(folderId).orElseThrow(NotFoundException::new);

        if (!serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(folder.getIssuingService().getId())) {
            throw new ForbiddenException();
        }

        if (type == NotionType.INFOSOURCE) {
            Infosource infosource = infosourceRepository.findById(notionId).orElseThrow(BadRequestException::new);
            if (infosource.getStatus() == InfosourceStatus.DRAFT) {
                throw new ForbiddenException();
            }
            folder.getNotions().add(NotionInfosource.builder().type(NotionType.INFOSOURCE).infosource(infosource).folder(folder).build());
            infosource.getLinkedWarnings().forEach(linkedWarning -> {
                boolean notionAlreadyExistsInFolder = folder.getNotions().stream()
                    .filter(notion -> notion.getType() == NotionType.WARNING)
                    .map(notion -> (NotionWarning) notion)
                    .filter(notion -> notion.getWarning().getStatus() != WarningStatus.DRAFT && notion.getWarning().getStatus() != WarningStatus.MODEL)
                    .anyMatch(notion -> notion.getWarning().getId().equals(linkedWarning.getId()));

                if (!notionAlreadyExistsInFolder) {
                    folder.getNotions().add(NotionWarning.builder()
                        .id(randomUUID())
                        .type(NotionType.WARNING)
                        .warning(linkedWarning)
                        .folder(folder)
                        .build());
                }

                addWarningLinkedNotionsToFolder(linkedWarning, folder);
            });
            repository.save(folder);
            repository.flush();
            sissyService.sendFolderUpdate(singletonList(infosource));
        } else if (type == NotionType.WARNING) {
            Warning warning = warningRepository.findById(notionId).orElseThrow(BadRequestException::new);
            if (warning.getStatus() == WarningStatus.DRAFT || warning.getStatus() == WarningStatus.MODEL) {
                throw new ForbiddenException();
            }
            addWarningToFolder(warning, folder);
            repository.save(folder);
        } else {
            Georegulation georegulation = georegulationRepository.findById(notionId).orElseThrow(BadRequestException::new);
            if (georegulation.getStatus() == GeoregulationStatus.DRAFT) {
                throw new ForbiddenException();
            }
            folder.getNotions().add(NotionGeoregulation.builder().type(NotionType.GEOREGULATION).georegulation(georegulation).folder(folder).build());
            infosourceRepository.findByGeoregulationId(georegulation.getId()).ifPresent(infosource -> {
                boolean notionAlreadyExistsInFolder = folder.getNotions().stream()
                    .filter(notion -> notion.getType() == NotionType.INFOSOURCE)
                    .map(notion -> (NotionInfosource) notion)
                    .anyMatch(notion -> notion.getInfosource().getId().equals(infosource.getId()));

                if (notionAlreadyExistsInFolder || infosource.getStatus() == InfosourceStatus.DRAFT) {
                    return;
                }

                folder.getNotions().add(NotionInfosource.builder()
                    .id(randomUUID())
                    .type(NotionType.INFOSOURCE)
                    .infosource(infosource)
                    .folder(folder)
                    .build());

                infosource.getLinkedWarnings().forEach(warning -> addWarningToFolder(warning, folder));
            });
            repository.save(folder);
        }
    }

    private void addWarningToFolder(Warning warning, Folder folder) {
        folder.getNotions().add(NotionWarning.builder().type(NotionType.WARNING).warning(warning).folder(folder).build());
        addWarningLinkedNotionsToFolder(warning, folder);
    }

    @Transactional
    public void addWarningLinkedNotionsToWarningFolders(Warning warning) {
        List<Folder> folders = repository.findByWarningId(warning.getId());
        if (isNotEmpty(folders)) {
            folders.forEach(folder -> addWarningLinkedNotionsToFolder(warning, folder));
            repository.saveAll(folders);
        }
    }

    private void addWarningLinkedNotionsToFolder(Warning warning, Folder folder) {
        addWarningLinkedWarningsToFolder(warning, folder);
        addWarningLinkedInfosourcesToFolder(warning, folder);
        addWarningLinkedGeoregulationsToFolder(warning, folder);
    }

    private void addWarningLinkedWarningsToFolder(Warning warning, Folder folder) {
        warning.getWarningReferences().forEach(reference -> reference.getWarnings().forEach(warningReference -> {
            boolean notionAlreadyExistsInFolder = folder.getNotions().stream()
                .filter(notion -> notion.getType() == NotionType.WARNING)
                .map(notion -> (NotionWarning) notion)
                .anyMatch(notion -> notion.getWarning().getId().equals(warningReference.getId()));

            if (notionAlreadyExistsInFolder || warningReference.getStatus() == WarningStatus.DRAFT || warningReference.getStatus() == WarningStatus.MODEL) {
                return;
            }

            folder.getNotions().add(NotionWarning.builder()
                .id(randomUUID())
                .type(NotionType.WARNING)
                .warning(warningReference)
                .folder(folder)
                .build());

            addWarningLinkedWarningsToFolder(warningReference, folder);
        }));
    }

    private void addWarningLinkedInfosourcesToFolder(Warning warning, Folder folder) {
        warning.getInfosources().forEach(infosource -> {
            boolean infosourceAlreadyPresentInFolder = folder.getNotions().stream()
                .filter(notion -> notion.getType() == NotionType.INFOSOURCE)
                .map(notion -> (NotionInfosource) notion)
                .anyMatch(notion -> notion.getInfosource().getId().equals(infosource.getId()));

            if (infosourceAlreadyPresentInFolder || infosource.getStatus() == InfosourceStatus.DRAFT) {
                return;
            }

            folder.getNotions().add(NotionInfosource.builder()
                .id(randomUUID())
                .type(NotionType.INFOSOURCE)
                .infosource(infosource)
                .folder(folder)
                .build());

            infosource.getLinkedWarnings().forEach(linkedWarning -> addWarningLinkedWarningsToFolder(linkedWarning, folder));
        });
    }

    private void addWarningLinkedGeoregulationsToFolder(Warning warning, Folder folder) {
        warning.getGeoregulations().forEach(warningGeoregulation -> {
            boolean infosourceAlreadyPresentInFolder = folder.getNotions().stream()
                .filter(notion -> notion.getType() == NotionType.GEOREGULATION)
                .map(notion -> (NotionGeoregulation) notion)
                .anyMatch(notion -> notion.getGeoregulation().getId().equals(warningGeoregulation.getGeoregulation().getId()));

            if (infosourceAlreadyPresentInFolder || warningGeoregulation.getGeoregulation().getStatus() == GeoregulationStatus.DRAFT) {
                return;
            }

            folder.getNotions().add(NotionGeoregulation.builder()
                .id(randomUUID())
                .type(NotionType.GEOREGULATION)
                .georegulation(warningGeoregulation.getGeoregulation())
                .folder(folder)
                .build());
        });
    }

    @Transactional
    public void addWarningToInfosourceFolders(UUID infosourceId, Warning warning) {
        List<Folder> folders = repository.findByInfosourceId(infosourceId);
        folders.forEach(folder -> {
            folder.getNotions().add(NotionWarning.builder()
                .type(NotionType.WARNING)
                .warning(warning)
                .folder(folder)
                .build());
        });

        repository.saveAll(folders);
    }

    @Transactional
    public void addInfosourceToInfosourceFolders(UUID infosourceId, Infosource infosource) {
        List<Folder> folders = repository.findByInfosourceId(infosourceId);
        folders.forEach(folder -> {
            folder.getNotions().add(NotionInfosource.builder()
                .type(NotionType.INFOSOURCE)
                .infosource(infosource)
                .folder(folder)
                .build());
        });

        repository.saveAll(folders);
    }

    @Transactional
    public void addWarningToWarningFolders(UUID warningId, Warning warning) {
        List<Folder> folders = repository.findByWarningId(warningId);
        folders.forEach(folder -> {
            folder.getNotions().add(NotionWarning.builder()
                .type(NotionType.WARNING)
                .warning(warning)
                .folder(folder)
                .build());
        });

        repository.saveAll(folders);
    }

    @Transactional
    public void addInfosourceToGeoregulationFolders(UUID georegulationId, Infosource infosource) {
        List<Folder> folders = repository.findByGeoregulationId(georegulationId);
        folders.forEach(folder -> {
            folder.getNotions().add(NotionInfosource.builder()
                .type(NotionType.INFOSOURCE)
                .infosource(infosource)
                .folder(folder)
                .build());
        });

        repository.saveAll(folders);
    }

    @Transactional
    public void addGeoregulationToGeoregulationFolders(UUID georegulationId, Georegulation georegulation) {
        List<Folder> folders = repository.findByGeoregulationId(georegulationId);
        folders.forEach(folder -> {
            folder.getNotions().add(NotionGeoregulation.builder()
                .type(NotionType.GEOREGULATION)
                .georegulation(georegulation)
                .folder(folder)
                .build());
        });

        repository.saveAll(folders);
    }

    @Transactional
    public void removeNotionFromFolder(UUID folderId, UUID notionId, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Folder folder = repository.findById(folderId).orElseThrow(NotFoundException::new);

        if (!serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(folder.getIssuingService().getId())) {
            throw new ForbiddenException();
        }

        folder.getNotions().stream()
            .filter(notion -> notionId.equals(notion.getId()))
            .findFirst()
            .ifPresent(notion -> {
                folder.getStudies().stream().filter(study -> study.getNotions().stream()
                    .map(Notion::getId)
                    .collect(toSet())
                    .contains(notionId)).forEach(study -> study.getNotions().remove(notion));

                folder.getNotions().remove(notion);
            });

        repository.save(folder);
    }

    @Transactional
    public UUID addAttachmentToFolder(UUID folderId, FolderAttachmentSaveDto dto, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Folder folder = repository.findById(folderId).orElseThrow(NotFoundException::new);

        if (!serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(folder.getIssuingService().getId())) {
            throw new ForbiddenException();
        }

        FolderAttachment folderAttachment = new FolderAttachment();
        folderAttachment.setFolder(folder);

        return saveFolderAttachment(folderAttachment, dto);
    }

    @Transactional
    public UUID updateFolderAttachment(UUID folderAttachmentId, FolderAttachmentSaveDto dto, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        FolderAttachment folderAttachment = folderAttachmentRepository.findById(folderAttachmentId).orElseThrow(NotFoundException::new);
        UUID oldFolderAttachmentId = folderAttachment.getAttachment().getId();

        if (!serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(folderAttachment.getFolder().getIssuingService().getId())) {
            throw new ForbiddenException();
        }

        UUID updatedFolderAttachmentId = saveFolderAttachment(folderAttachment, dto);

        if (!dto.getAttachmentId().equals(oldFolderAttachmentId)) {
            try {
                attachmentService.deleteFromDiskAndDatabase(oldFolderAttachmentId);
            } catch (ServiceException ignored) {
                // Nothing to do as we don't want to notify user
            }
        }

        return updatedFolderAttachmentId;
    }

    private UUID saveFolderAttachment(FolderAttachment folderAttachment, FolderAttachmentSaveDto dto) {
        if (dto.getReceptionDate() != null) {
            folderAttachment.setReceptionDate(dto.getReceptionDate());
        }

        folderAttachment.setComment(dto.getComment());

        MaritimeService maritimeService = null;
        if (dto.getMaritimeServiceId() != null) {
            maritimeService = maritimeServiceRepository.findById(dto.getMaritimeServiceId()).orElseThrow(NotFoundException::new);
        }
        folderAttachment.setMaritimeService(maritimeService);

        Attachment attachment = attachmentRepository.findById(dto.getAttachmentId()).orElseThrow(NotFoundException::new);
        folderAttachment.setAttachment(attachment);

        return folderAttachmentRepository.save(folderAttachment).getId();
    }

    @Transactional
    public void deleteAttachmentFromFolder(UUID folderAttachmentId, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        FolderAttachment folderAttachment = folderAttachmentRepository.findById(folderAttachmentId).orElseThrow(NotFoundException::new);

        if (!serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(folderAttachment.getFolder().getIssuingService().getId())) {
            throw new ForbiddenException();
        }

        try {
            attachmentService.deleteAttachmentFile(folderAttachment.getAttachment());
        } catch (ServiceException ignored) {
            // Nothing to do as we don't want to notify user
        }

        folderAttachmentRepository.delete(folderAttachment);
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    @Transactional
    public UUID clone(UUID folderId, String username) throws ServiceException {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Folder folder = repository.findById(folderId).orElseThrow(NotFoundException::new);

        if (!serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(folder.getIssuingService().getId())) {
            throw new ForbiddenException();
        }

        Folder clonedFolder = Folder.builder()
            .number(nextAvailableNumber())
            .status(folder.getStatus())
            .comment(folder.getComment())
            .object(folder.getObject())
            .reviewDate(folder.getReviewDate())
            .issuingService(folder.getIssuingService())
            .area(folder.getArea())
            .locality(folder.getLocality())
            .entities(new HashSet<>(folder.getEntities()))
            .services(new HashSet<>(folder.getServices()))
            .build();

        Set<Study> clonedStudies = folder.getStudies().stream()
            .map(study -> Study.builder()
                .folder(clonedFolder)
                .number(study.getNumber())
                .lastUpdateUser(serviceUser)
                .status(study.getStatus())
                .build()).collect(toSet());

        Set<Notion> clonedNotions = folder.getNotions().stream()
            .map(notion -> {
                Notion clonedNotion;
                if (notion.getType() == NotionType.INFOSOURCE) {
                    clonedNotion = NotionInfosource.builder()
                        .id(UUID.randomUUID())
                        .type(NotionType.INFOSOURCE)
                        .infosource(((NotionInfosource) notion).getInfosource())
                        .folder(clonedFolder)
                        .build();
                } else if (notion.getType() == NotionType.WARNING) {
                    clonedNotion = NotionWarning.builder()
                        .id(UUID.randomUUID())
                        .type(NotionType.WARNING)
                        .warning(((NotionWarning) notion).getWarning())
                        .folder(clonedFolder)
                        .build();
                } else {
                    clonedNotion = NotionGeoregulation.builder()
                        .id(UUID.randomUUID())
                        .type(NotionType.GEOREGULATION)
                        .georegulation(((NotionGeoregulation) notion).getGeoregulation())
                        .folder(clonedFolder)
                        .build();
                }

                folder.getStudies().stream()
                    .filter(study -> study.getNotions().contains(notion))
                    .forEach(study -> clonedStudies.stream()
                        .filter(clonedStudy -> clonedStudy.getNumber().equals(study.getNumber()))
                        .findFirst()
                        .ifPresent(clonedStudy -> clonedStudy.getNotions().add(clonedNotion)));

                return clonedNotion;
            }).collect(toSet());

        Set<FolderAttachment> clonedFolderAttachments = new HashSet<>();
        for (FolderAttachment folderAttachment : folder.getAttachments()) {
            Attachment clonedAttachment = attachmentService.clone(folderAttachment.getAttachment());

            FolderAttachment clonedFolderAttachment = FolderAttachment.builder()
                .folder(clonedFolder)
                .comment(folderAttachment.getComment())
                .receptionDate(folderAttachment.getReceptionDate())
                .maritimeService(folderAttachment.getMaritimeService())
                .attachment(clonedAttachment)
                .build();

            clonedFolderAttachments.add(clonedFolderAttachment);

            folder.getStudies().stream()
                .filter(study -> study.getAttachments().contains(folderAttachment))
                .forEach(study -> clonedStudies.stream()
                    .filter(clonedStudy -> clonedStudy.getNumber().equals(study.getNumber()))
                    .findFirst()
                    .ifPresent(clonedStudy -> clonedStudy.getAttachments().add(clonedFolderAttachment)));
        }

        clonedFolder.setNotions(clonedNotions);
        clonedFolder.setAttachments(clonedFolderAttachments);
        clonedFolder.setStudies(clonedStudies);

        repository.save(clonedFolder);

        folder.getStudies().forEach(study -> clonedStudies.stream()
            .filter(clonedStudy -> clonedStudy.getNumber().equals(study.getNumber()))
            .findFirst()
            .ifPresent(clonedStudy -> {
                clonedStudy.setCreationDate(study.getCreationDate());
                clonedStudy.setLastUpdate(study.getLastUpdate());
            }));

        return repository.save(clonedFolder).getId();
    }

    public void notify(UUID folderId, Set<UUID> serviceIds, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Folder folder = repository.findById(folderId).orElseThrow(NotFoundException::new);

        if (!serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(folder.getIssuingService().getId())) {
            throw new ForbiddenException();
        }

        List<MaritimeService> services = maritimeServiceRepository.findAllById(serviceIds).stream()
            .filter(service -> service.getId() == folder.getIssuingService().getId() || service.getServiceNetwork() == ServiceNetwork.GOVERNMENT_SERVICE)
            .collect(toList());

        if (services.size() != serviceIds.size()) {
            throw new BadRequestException("At least one service selected to be notified has not rights to read the folder");
        }

        services.forEach(maritimeService -> sseNotificationService.sendFolderNotificationService(
            folder.getNumber() + " - " +
                ofNullable(folder.getIssuingService()).map(MaritimeService::getName).orElse(" ") + " - " +
                ofNullable(folder.getArea()).map(Area::getName).orElse(" "),
            maritimeService,
            folder
        ));
    }

    public List<MaritimeServiceAvailableToFollowFolderDto> findAvailableMaritimeServicesToFollowFolder(UUID folderId, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        List<UUID> serviceIds = serviceUser.getServiceIdsWithOperatorOrAdminRole();

        if (serviceIds.isEmpty()) {
            return new ArrayList<>();
        }

        return findAvailableMaritimeServicesByServiceIds(serviceIds, folderId);
    }

    private List<MaritimeServiceAvailableToFollowFolderDto> findAvailableMaritimeServicesByServiceIds(List<UUID> serviceIds, UUID folderId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("serviceIds", serviceIds);
        parameters.addValue("folderId", folderId);

        List<MaritimeServiceAvailableToFollowFolderDto> services = jdbcTemplate.query(
            "SELECT DISTINCT m.id, m.name, m.international_name " +
                "FROM maritime_service m " +
                "WHERE m.id IN (:serviceIds) " +
                "AND m.service_network = 'GOVERNMENT_SERVICE' " +
                "AND m.id NOT IN (" +
                "SELECT DISTINCT ff.maritime_service_id " +
                "FROM folder_following ff " +
                "WHERE ff.folder_id = :folderId" +
                ")",
            parameters,
            new BeanPropertyRowMapper<>(MaritimeServiceAvailableToFollowFolderDto.class)
        );
        return services;
    }

    private static boolean checkIfUserHasNoRequiredRoleOnService(ServiceUser serviceUser, UUID serviceId, List<ServiceRole> serviceRoles) {
        return serviceUser.getRoles().stream()
            .filter(serviceUserRole -> serviceRoles.contains(serviceUserRole.getServiceRole()))
            .map(ServiceUserRole::getServiceId)
            .noneMatch(uuid -> uuid.equals(serviceId));
    }

    public void follow(UUID folderId, Set<UUID> serviceIds, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Folder folder = repository.findById(folderId).orElseThrow(NotFoundException::new);

        final boolean serviceUserHasNoRequiredRoleOnService = serviceIds.stream().anyMatch(serviceId ->
            checkIfUserHasNoRequiredRoleOnService(serviceUser, serviceId, asList(SERVICE_OPERATOR, SERVICE_ADMIN))
        );

        if (!serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(folder.getIssuingService().getId()) && serviceUserHasNoRequiredRoleOnService) {
            throw new ForbiddenException();
        }

        List<MaritimeService> services = maritimeServiceRepository.findAllById(serviceIds).stream()
            .filter(service -> service.getServiceNetwork() == ServiceNetwork.GOVERNMENT_SERVICE)
            .toList();

        if (services.size() != serviceIds.size()) {
            throw new BadRequestException("At least one service selected has no right to read the folder");
        }

        services.forEach(maritimeService -> folder.getFollowerServices().add(maritimeService));

        repository.save(folder);
    }

    public void unFollow(UUID folderId, UUID serviceId, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Folder folder = repository.findById(folderId).orElseThrow(NotFoundException::new);

        if (!serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(serviceId)) {
            throw new ForbiddenException();
        }

        MaritimeService maritimeServiceWhichUnFollow = maritimeServiceRepository.findById(serviceId).orElseThrow(NotFoundException::new);

        folder.getFollowerServices().remove(maritimeServiceWhichUnFollow);

        repository.save(folder);
    }
}
