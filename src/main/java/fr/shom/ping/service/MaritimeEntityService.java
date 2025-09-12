package fr.shom.ping.service;

import com.querydsl.core.BooleanBuilder;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.Area;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.QMaritimeEntity;
import fr.shom.ping.model.Serie;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.dto.MaritimeEntityCreateDto;
import fr.shom.ping.model.dto.MaritimeEntityDto;
import fr.shom.ping.model.dto.MaritimeEntitySearchResultDto;
import fr.shom.ping.model.dto.MaritimeEntityToDisplayOnMapDto;
import fr.shom.ping.model.enums.MaritimeEntityType;
import fr.shom.ping.repository.MaritimeEntityRepository;
import fr.shom.ping.repository.ServiceUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_ADMIN;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_OPERATOR;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.springframework.util.StringUtils.hasText;

@RequiredArgsConstructor
@Service
public class MaritimeEntityService {
    private final MaritimeEntityRepository repository;
    private final AreaService areaService;
    private final MaritimeServiceService maritimeServiceService;
    protected final EntityManager entityManager;
    private final ServiceUserRepository serviceUserRepository;
    private final SerieService serieService;


    @Transactional
    public MaritimeEntityDto create(MaritimeEntityCreateDto dto) {
        List<Area> areaList = areaService.findAllById(dto.getAreas());
        final List<MaritimeService> additionalServices = maritimeServiceService.findAllByIdIn(dto.getAdditionalServices());
        MaritimeService maritimeService = maritimeServiceService.findById(dto.getMaritimeServiceId());
        return repository.save(new MaritimeEntity(dto, areaList, maritimeService, newHashSet(additionalServices))).asDto();
    }

    @Transactional
    public MaritimeEntityDto update(UUID id, MaritimeEntityCreateDto dto) {
        List<Area> areaList = areaService.findAllById(dto.getAreas());
        final List<MaritimeService> additionalServices = maritimeServiceService.findAllByIdIn(dto.getAdditionalServices());
        MaritimeService maritimeService = maritimeServiceService.findById(dto.getMaritimeServiceId());
        MaritimeEntity entity = new MaritimeEntity(dto, areaList, maritimeService, newHashSet(additionalServices));
        entity.setId(id);
        return repository.save(entity).asDto();
    }

    public Page<MaritimeEntitySearchResultDto> search(Pageable pageable) {
        return repository.findAll(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name")))
            .map(MaritimeEntitySearchResultDto::fromMaritimeEntity);
    }

    public Page<MaritimeEntitySearchResultDto> findAllBySearchCriteria(Pageable pageable, List<MaritimeEntityType> maritimeEntityTypes, List<UUID> areasIds,
        String name, UUID mainServiceId, List<UUID> additionalServiceIds) {
        final BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (hasText(name)) {
            booleanBuilder.and(QMaritimeEntity.maritimeEntity.name.containsIgnoreCase(name));
        }

        if (CollectionUtils.isNotEmpty(areasIds)) {
            booleanBuilder.and(QMaritimeEntity.maritimeEntity.areas.any().id.in(areasIds));
        }

        if (CollectionUtils.isNotEmpty(maritimeEntityTypes)) {
            booleanBuilder.and(QMaritimeEntity.maritimeEntity.type.in(maritimeEntityTypes));
        }

        if (isNotEmpty(mainServiceId)) {
            booleanBuilder.and(QMaritimeEntity.maritimeEntity.maritimeService.id.eq(mainServiceId));
        }

        if (CollectionUtils.isNotEmpty(additionalServiceIds)) {
            booleanBuilder.and(QMaritimeEntity.maritimeEntity.additionalServices.any().id.in(additionalServiceIds));
        }

        return repository.findAll(booleanBuilder, pageable).map(MaritimeEntitySearchResultDto::fromMaritimeEntity);
    }

    public List<MaritimeEntityDto> getAvailableEntitiesToLinkFromSerie(UUID serieId, String username) {
        final ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);

        final Serie serie = serieService.findById(serieId);
        final MaritimeService serieMaritimeService = serie.getMaritimeService();

        final boolean serviceUserHasNoRequiredRoleOnSerieService = serviceUser.getRoles().stream()
            .filter(serviceUserRole -> serieMaritimeService.getId().equals(serviceUserRole.getServiceId()))
            .noneMatch(serviceUserRole -> asList(SERVICE_OPERATOR, SERVICE_ADMIN).contains(serviceUserRole.getServiceRole()));

        if (serviceUserHasNoRequiredRoleOnSerieService) {
            throw new ForbiddenException();
        }

        return Stream.concat(
                repository.findAvailableMaritimeEntitiesToLink(serieMaritimeService.getId()).stream(),
                repository.findObservedEntities(serieMaritimeService.getId()).stream()
            )
            .map(MaritimeEntity::asDto)
            .distinct()
            .collect(toList());
    }

    public MaritimeEntity findById(UUID id) {
        return Optional.ofNullable(id)
            .map(repository::findById)
            .orElseThrow(NotFoundException::new)
            .orElse(null);
    }

    public List<MaritimeEntityDto> findAllByIds(List<UUID> entityIds) {
        return repository.findAllByIdIn(entityIds);
    }

    public List<MaritimeEntityToDisplayOnMapDto> getAllEntitiesManagedOrObservedByServiceToDisplayOnMapView(String username) {
        final ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);

        return repository.findEntitiesManagedOrObservedByService(serviceUser.getService().getId()).stream()
            .map(MaritimeEntityToDisplayOnMapDto::new)
            .collect(toList());
    }
}
