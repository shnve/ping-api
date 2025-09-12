package fr.shom.ping.service;

import com.querydsl.core.BooleanBuilder;
import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.Area;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.QMaritimeService;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.model.dto.AreaServiceDto;
import fr.shom.ping.model.dto.MaritimeServiceAuthorityDto;
import fr.shom.ping.model.dto.MaritimeServiceCreateDto;
import fr.shom.ping.model.dto.MaritimeServiceDto;
import fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto;
import fr.shom.ping.model.enums.MaritimeServiceType;
import fr.shom.ping.model.enums.ServiceRole;
import fr.shom.ping.repository.MaritimeServiceRepository;
import fr.shom.ping.repository.ServiceUserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static fr.shom.ping.model.enums.MaritimeServiceType.AUTHORITY;
import static fr.shom.ping.model.enums.MaritimeServiceType.OLD_USERS;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.springframework.util.StringUtils.hasText;

@RequiredArgsConstructor
@Service
public class MaritimeServiceService {

    private final MaritimeServiceRepository repository;
    private final AreaService areaService;
    private final ServiceUserService serviceUserService;
    private final ServiceUserRepository serviceUserRepository;

    @Transactional
    public MaritimeServiceDto create(MaritimeServiceCreateDto dto) {
        List<Area> areaList = areaService.findAllById(dto.getAreas());
        return repository.save(new MaritimeService(dto, areaList)).asDto();
    }

    @Transactional
    public MaritimeServiceDto update(UUID id, MaritimeServiceCreateDto dto) {
        List<Area> areaList = areaService.findAllById(dto.getAreas());
        MaritimeService service = new MaritimeService(dto, areaList);
        service.setId(id);
        return repository.save(service).asDto();
    }

    public MaritimeService findById(UUID id) {
        return Optional.ofNullable(id)
            .map(repository::findById)
            .orElseThrow(NotFoundException::new)
            .orElse(null);
    }

    public Set<MaritimeService> findMaritimeServiceByMaritimeServiceType(MaritimeServiceType maritimeServiceType) {
        return repository.findAllByType(maritimeServiceType);
    }

    @Transactional
    public void assignRole(UUID id, UUID userId, ServiceRole role) {
        // Verify if exist
        final MaritimeService serviceToAssignUser = findById(id);

        // Verify if user belong to old user maritime service
        final ServiceUser serviceUser = this.serviceUserService.findServiceUserById(userId);
        final Optional<MaritimeService> oldUsersMaritimeServices = findMaritimeServiceByMaritimeServiceType(OLD_USERS).stream().findFirst();

        final List<ServiceUserRole> serviceUserRoles = serviceUser.getRoles().stream().toList();

        // assign new role
        serviceUserService.assignRole(userId, serviceToAssignUser, role);

        final boolean isOldUser = oldUsersMaritimeServices.isPresent() &&
            serviceUserRoles.stream().anyMatch(serviceUserRole -> oldUsersMaritimeServices.get().getId().equals(serviceUserRole.getServiceId()));

        // remove user from old user maritime service if he is not an old user
        if (isOldUser) {
            removeUserFromOldUsers(userId, serviceUser, serviceToAssignUser, oldUsersMaritimeServices.get());
        }
    }

    private void removeUserFromOldUsers(UUID userId, ServiceUser serviceUser, MaritimeService serviceToAssignUser,
        MaritimeService oldUsersMaritimeServices) {
        serviceUser.setService(serviceToAssignUser);
        serviceUserRepository.save(serviceUser);
        serviceUserService.enable(userId);
        serviceUserService.deleteRole(userId, oldUsersMaritimeServices, ServiceRole.SERVICE_VISITOR);
    }

    @Transactional
    public void deleteRole(UUID id, UUID userId, ServiceRole role) {
        final MaritimeService serviceFromWhichRemoveUser = findById(id);

        final ServiceUser serviceUser = this.serviceUserService.findServiceUserById(userId);

        final List<ServiceUserRole> serviceUserRoles = serviceUser.getRoles().stream().toList();

        final boolean hasNoRole =
            serviceUserRoles.stream().noneMatch(serviceUserRole -> serviceUserRole.getServiceId().equals(serviceFromWhichRemoveUser.getId()));

        if (hasNoRole) {
            throw new BadRequestException();
        }

        // if user has only this role, before removing it, we assign him to old user maritime service. later, only functional admin can remove him
        final boolean hasOnlyThisRole = serviceUserRoles.size() == 1 && serviceUserRoles.getFirst().getServiceRole() == role;

        if (hasOnlyThisRole) {
            assignUserToOldUsers(userId, serviceUser);
        }

        serviceUserService.deleteRole(userId, serviceFromWhichRemoveUser, role);

        if (!hasOnlyThisRole && serviceUser.getService().getId().equals(id)) {
            final List<UUID> serviceIdsOnWhichUserIsAdmin = serviceUser.getServiceIdsWithAdminRole();
            if (isNotEmpty(serviceIdsOnWhichUserIsAdmin)) {
                assignToAnotherServiceAsMainService(id, serviceIdsOnWhichUserIsAdmin, serviceUser);
                return;
            }

            final List<UUID> serviceIdsOnWhichUserIsOperator = serviceUser.getServiceIdsWithOperatorRole();
            if (isNotEmpty(serviceIdsOnWhichUserIsOperator)) {
                assignToAnotherServiceAsMainService(id, serviceIdsOnWhichUserIsOperator, serviceUser);
                return;
            }

            final List<UUID> serviceIdsWithVisitorRole = serviceUser.getServiceIdsWithVisitorRole();
            if (isNotEmpty(serviceIdsWithVisitorRole)) {
                assignToAnotherServiceAsMainService(id, serviceIdsWithVisitorRole, serviceUser);
            }
        }
    }

    private void assignToAnotherServiceAsMainService(UUID id, List<UUID> serviceIdsOnWhichUserIsOperator, ServiceUser serviceUser) {
        final List<MaritimeService> maritimeServices = findAllByIdIn(serviceIdsOnWhichUserIsOperator);
        final Optional<MaritimeService> maritimeService = maritimeServices.stream().filter(service -> !service.getId().equals(id)).findFirst();
        maritimeService.ifPresent(serviceUser::setService);
        serviceUserRepository.save(serviceUser);
    }

    private void assignUserToOldUsers(UUID userId, ServiceUser serviceUser) {
        final MaritimeService oldUsersMaritimeServices = findMaritimeServiceByMaritimeServiceType(OLD_USERS).iterator().next();
        serviceUserService.assignRole(userId, oldUsersMaritimeServices, ServiceRole.SERVICE_VISITOR);
        serviceUser.setService(oldUsersMaritimeServices);
        serviceUserRepository.save(serviceUser);
        serviceUserService.disable(userId);
    }

    public Page<MaritimeService> search(Pageable pageable) {
        return repository.findAll(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name")));
    }

    @SuppressWarnings("java:S103")
    public List<MaritimeServiceInfosourceRecipientDto> getMaritimeServiceByTypeWithIntersection(Geometry geometry,
        Collection<MaritimeServiceType> maritimeServiceTypes) {
        return repository.findAllByMaritimeServiceTypeInThatIntersectWith(geometry, maritimeServiceTypes);
    }

    public List<MaritimeServiceInfosourceRecipientDto> getMaritimeServiceByType(Collection<MaritimeServiceType> maritimeServiceTypes) {
        return repository.findAllByMaritimeServiceTypeIn(maritimeServiceTypes);
    }

    public List<MaritimeServiceDto> findAllByIds(List<UUID> serviceIds) {
        return repository.findAllByIdIn(serviceIds);
    }

    public List<AreaServiceDto> findAreasByServiceIdsIn(List<UUID> serviceIds) {
        return repository.findAreasByServiceIdIn(serviceIds);
    }

    public List<MaritimeService> findAllByIdIn(Collection<UUID> serviceIds) {
        return repository.findByIdIn(serviceIds);
    }

    public MaritimeService findBySissyId(String sissyId) {
        return repository.findBySissyId(sissyId);
    }

    public List<MaritimeServiceAuthorityDto> getAuthorities(UUID serviceId) {
        final MaritimeService issuingService = repository.findById(serviceId).orElseThrow(BadRequestException::new);

        if (issuingService.getType() != AUTHORITY) {
            throw new ForbiddenException();
        }

        final List<UUID> areaIds = issuingService.getAreas().stream().map(Area::getId).collect(toList());
        return repository.findAuthoritiesByAreaIds(areaIds);
    }

    public List<MaritimeServiceInfosourceRecipientDto> findRecipientsWithAreasInServiceAreasExcludingIds(UUID serviceId, Set<UUID> excludedIds) {
        return repository.findRecipientsWithAreasInServiceAreasExcludingIds(serviceId, excludedIds);
    }

    public Page<MaritimeService> findAllBySearchCriteria(Pageable pageable, List<MaritimeServiceType> maritimeServiceTypes, List<UUID> areaIds, String name) {
        final BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (hasText(name)) {
            booleanBuilder.and(QMaritimeService.maritimeService.name.containsIgnoreCase(name));
        }

        if (isNotEmpty(areaIds)) {
            booleanBuilder.and(QMaritimeService.maritimeService.areas.any().id.in(areaIds));
        }

        if (isNotEmpty(maritimeServiceTypes)) {
            booleanBuilder.and(QMaritimeService.maritimeService.type.in(maritimeServiceTypes));
        }

        return repository.findAll(booleanBuilder, pageable);
    }
}
