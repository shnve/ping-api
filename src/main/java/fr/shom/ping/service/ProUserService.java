package fr.shom.ping.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.KeycloakUserCreationException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.FunctionalAdmin;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.ProUser;
import fr.shom.ping.model.QProUser;
import fr.shom.ping.model.QServiceUser;
import fr.shom.ping.model.QServiceUserRole;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.model.dto.AuthenticatedFunctionalAdminDto;
import fr.shom.ping.model.dto.AuthenticatedProUserDto;
import fr.shom.ping.model.dto.AuthenticatedServiceUserDto;
import fr.shom.ping.model.dto.CreateProUserDto;
import fr.shom.ping.model.dto.ProUserEditDto;
import fr.shom.ping.model.dto.ProUserSearchDto;
import fr.shom.ping.model.enums.MaritimeServiceType;
import fr.shom.ping.repository.ProUserRepository;
import fr.shom.ping.repository.ServiceUserRepository;
import fr.shom.ping.repository.UserRepository;
import fr.shom.ping.utils.InfosourceUtil;
import fr.shom.ping.visitor.ProUserConvertVisitorImpl;
import fr.shom.ping.visitor.ProUserEditDtoVisitorImpl;
import fr.shom.ping.visitor.ProUserVisitorImpl;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import static fr.shom.ping.model.enums.MaritimeServiceType.OLD_USERS;
import static fr.shom.ping.model.enums.ProUserRole.FUNCTIONAL_ADMIN;
import static fr.shom.ping.model.enums.ProUserRole.SERVICE_USER;
import static fr.shom.ping.model.enums.UserStatus.DISABLED;
import static fr.shom.ping.model.enums.UserStatus.ENABLED;
import static fr.shom.ping.service.UserService.USER_MODEL;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.StringUtils.hasText;

@Service
public class ProUserService extends AbstractProUserService {
    private static final String LASTNAME = "lastname";
    private final ServiceUserRepository serviceUserRepository;
    private final UserRepository userRepository;
    private final AreaService areaService;
    private final MaritimeServiceService maritimeServiceService;
    private final EntityManager entityManager;

    public ProUserService(ProUserRepository proUserRepository, KeycloakService keycloakService, ServiceUserRepository serviceUserRepository,
        UserRepository userRepository, AreaService areaService, MaritimeServiceService maritimeServiceService, EntityManager entityManager) {
        super(proUserRepository, keycloakService);
        this.serviceUserRepository = serviceUserRepository;
        this.userRepository = userRepository;
        this.areaService = areaService;
        this.maritimeServiceService = maritimeServiceService;
        this.entityManager = entityManager;
    }

    public AuthenticatedProUserDto me(String email) {
        ProUser proUser = findByEmail(email);
        if (proUser.getRole() == FUNCTIONAL_ADMIN) {
            return AuthenticatedFunctionalAdminDto.fromFunctionalAdmin((FunctionalAdmin) proUser);
        }

        final AuthenticatedServiceUserDto serviceUser = AuthenticatedServiceUserDto.fromServiceUser((ServiceUser) proUser);
        setAttributesForEachServiceRole(serviceUser.getServiceRoles());
        return serviceUser;
    }

    public void setAttributesForEachServiceRole(Set<ServiceUserRole> serviceUserRoles) {
        final List<MaritimeService> services = maritimeServiceService.findAllByIdIn(
            serviceUserRoles.stream().map(ServiceUserRole::getServiceId).collect(toList())
        );

        final Map<UUID, MaritimeServiceType> uuidMaritimeServiceTypeMap = services.stream().collect(
            toMap(MaritimeService::getId, MaritimeService::getType)
        );

        final Map<UUID, Boolean> uuidMaritimeServiceSurveyOwnerMap = services.stream().collect(
            toMap(MaritimeService::getId, MaritimeService::isSurveyOwner)
        );

        final Map<UUID, Boolean> uuidIsShomServiceMap = services.stream().collect(
            toMap(MaritimeService::getId, MaritimeService::isShomService)
        );

        final Map<UUID, Boolean> uuidIsFolderManagerMap = services.stream().collect(
            toMap(MaritimeService::getId, MaritimeService::isFolderManager)
        );

        serviceUserRoles.forEach(serviceUserRole -> {
            serviceUserRole.setType(uuidMaritimeServiceTypeMap.get(serviceUserRole.getServiceId()));
            serviceUserRole.setSurveyOwner(uuidMaritimeServiceSurveyOwnerMap.get(serviceUserRole.getServiceId()));
            serviceUserRole.setShomService(uuidIsShomServiceMap.get(serviceUserRole.getServiceId()));
            serviceUserRole.setFolderManager(uuidIsFolderManagerMap.get(serviceUserRole.getServiceId()));
        });
    }

    public ProUserEditDto findById(UUID id) {
        ProUser proUser = proUserRepository.findById(id).orElseThrow(NotFoundException::new);
        return proUser.toEditDto(new ProUserEditDtoVisitorImpl());
    }

    @Transactional(readOnly = true)
    public Page<ProUserSearchDto> search(Pageable pageable, String proUserEmail) {
        final ProUser proUser = findByEmail(proUserEmail);

        if (proUser.getRole() == FUNCTIONAL_ADMIN) {
            return proUserRepository.findAllAsProUserSearchDto(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(LASTNAME)));
        }

        final List<UUID> serviceIdsOnWhichUserIsAdmin = getServiceIdsOnWhichUserIsAdmin((ServiceUser) proUser);

        if (serviceIdsOnWhichUserIsAdmin.isEmpty()) {
            return Page.empty();
        }

        final Page<ServiceUser> proUserPage = serviceUserRepository.findAllWithRoleOnServices(
            serviceIdsOnWhichUserIsAdmin, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(LASTNAME)));

        return proUserPage.map(ProUserSearchDto::fromProUser);
    }

    @Transactional(readOnly = true)
    public Page<ProUserSearchDto> findAllBySearchCriteria(Pageable pageable, String lastname, String firstname, String email, String proUserEmail) {
        final ProUser proUser = findByEmail(proUserEmail);

        final BooleanBuilder booleanBuilderWithQProUser = getBooleanBuilderWithQProUser(lastname, firstname, email);

        if (proUser.getRole() == FUNCTIONAL_ADMIN) {
            return proUserRepository.findAll(booleanBuilderWithQProUser, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(LASTNAME)))
                .map(ProUserSearchDto::fromProUser);
        }

        final List<UUID> serviceIdsOnWhichUserIsAdmin = getServiceIdsOnWhichUserIsAdmin((ServiceUser) proUser);

        if (serviceIdsOnWhichUserIsAdmin.isEmpty()) {
            return Page.empty();
        }

        final BooleanBuilder booleanBuilderWithQServiceUser = getBooleanBuilderWithQServiceUser(lastname, firstname, email);

        JPAQuery<ServiceUser> query = new JPAQuery<>(entityManager)
            .select(QServiceUser.serviceUser)
            .from(QServiceUser.serviceUser)
            .leftJoin(QServiceUser.serviceUser.roles, QServiceUserRole.serviceUserRole)
            .where(booleanBuilderWithQServiceUser.and(QServiceUserRole.serviceUserRole.serviceId.in(serviceIdsOnWhichUserIsAdmin)))
            .orderBy(QServiceUser.serviceUser.lastname.asc())
            .distinct();

        return new PageImpl<>(query.fetch().stream().map(ProUserSearchDto::fromProUser).collect(toList()), pageable, query.fetchCount());
    }

    private static BooleanBuilder getBooleanBuilderWithQProUser(String lastname, String firstname, String email) {
        final BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (hasText(lastname)) {
            booleanBuilder.and(QProUser.proUser.lastname.containsIgnoreCase(lastname));
        }

        if (hasText(firstname)) {
            booleanBuilder.and(QProUser.proUser.firstname.containsIgnoreCase(firstname));
        }

        if (hasText(email)) {
            booleanBuilder.and(QProUser.proUser.email.containsIgnoreCase(email));
        }
        return booleanBuilder;
    }

    private static BooleanBuilder getBooleanBuilderWithQServiceUser(String lastname, String firstname, String email) {
        final BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (hasText(lastname)) {
            booleanBuilder.and(QServiceUser.serviceUser.lastname.containsIgnoreCase(lastname));
        }

        if (hasText(firstname)) {
            booleanBuilder.and(QServiceUser.serviceUser.firstname.containsIgnoreCase(firstname));
        }

        if (hasText(email)) {
            booleanBuilder.and(QServiceUser.serviceUser.email.containsIgnoreCase(email));
        }
        return booleanBuilder;
    }

    public ProUser findByEmail(String email) {
        ProUser proUser = proUserRepository.findOneByEmail(email).orElseThrow(NotFoundException::new);

        if (proUser.getRole() == SERVICE_USER) {
            return serviceUserRepository.getReferenceById(proUser.getId());
        }

        return proUser;
    }

    public void create(CreateProUserDto proUserDto, BindingResult bindingResult) throws ServiceException {
        this.areaService.checkAreaExist(proUserDto.getPreferredAreaId(), bindingResult);
        UUID createdKeycloakUserId = createKeycloakUser(proUserDto, bindingResult);
        ProUserVisitorImpl proUserVisitor = new ProUserVisitorImpl(maritimeServiceService, areaService);
        ProUser proUser = proUserDto.toModel(proUserVisitor, createdKeycloakUserId);
        proUser.setStatus(ENABLED);
        if (proUserDto.getPreferredAreaId() != null) {
            proUser.setPreferredArea(this.areaService.getOne(proUserDto.getPreferredAreaId()));
        }

        String identifier = InfosourceUtil.generateIdentifierFromEmail(proUserDto.getEmail());
        long existingEmails = userRepository.countAllExistingUsersWithEmail(proUserDto.getEmail());
        if (existingEmails > 0L) {
            identifier += existingEmails;
        }
        proUser.setIdentifier(identifier);

        this.proUserRepository.save(proUser);
    }

    @Transactional
    public void update(UUID id, CreateProUserDto proUserDto, String email, BindingResult bindingResult) throws ServiceException {
        areaService.checkAreaExist(proUserDto.getPreferredAreaId(), bindingResult);

        ProUser proUser = proUserRepository.findById(id).orElseThrow(NotFoundException::new);
        ProUser currentProUser = proUserRepository.findOneByEmail(email).orElseThrow(BadRequestException::new);

        if (currentProUser.getRole() == SERVICE_USER && !currentProUser.getEmail().equals(email)) {
            throw new ForbiddenException();
        }

        UUID keycloakId = proUser.getKeycloakId();

        if (!proUser.getEmail().equals(proUserDto.getEmail())) {
            keycloakService.updateUserEmail(keycloakId, proUserDto.getEmail());
        }

        if (proUser.getRole() != proUserDto.getRole()) {
            keycloakService.updateUserRole(keycloakId, proUserDto.getRole());
            ProUser newUser = proUser.switchRole(new ProUserConvertVisitorImpl());
            proUserRepository.delete(proUser);
            proUser = newUser;
        }

        if (proUser.hasNotificationsSound() != proUserDto.hasNotificationsSound()) {
            proUser.setHasNotificationsSound(proUserDto.hasNotificationsSound());
        }

        if (proUser.hasSubscriptionEmailNotifications() != proUserDto.hasSubscriptionEmailNotifications()) {
            proUser.setHasSubscriptionEmailNotifications(proUserDto.hasSubscriptionEmailNotifications());
        }

        ProUserVisitorImpl proUserVisitor = new ProUserVisitorImpl(maritimeServiceService, areaService);

        // check if user was an old users and update status if he was an old user
        if (proUser.getRole() == SERVICE_USER) {
            final ServiceUser existingServiceUser = (ServiceUser) proUser;
            ServiceUser serviceUserToUpdate = (ServiceUser) proUserDto.toModel(proUserVisitor, keycloakId);
            final Optional<MaritimeService> oldUsersMaritimeServices =
                    maritimeServiceService.findMaritimeServiceByMaritimeServiceType(OLD_USERS).stream().findFirst();
            final boolean wasOldUser = oldUsersMaritimeServices.isPresent() && existingServiceUser.getRoles().stream().anyMatch(serviceUserRole ->
                oldUsersMaritimeServices.get().getId().equals(serviceUserRole.getServiceId())
                    && !serviceUserToUpdate.getService().getId().equals(oldUsersMaritimeServices.get().getId())
            );

            if (wasOldUser && proUser.getStatus() == DISABLED) {
                proUser.setStatus(ENABLED);
            }
        }

        ProUser updatedProUser = proUserDto.update(proUserVisitor, proUser);
        entityManager.merge(updatedProUser);
    }

    /**
     * Create keycloak user against user dto.
     *
     * @param user          create pro user dto
     * @param bindingResult validation binding results
     * @return newly created keycloak user ID
     * @throws ServiceException if binding result contains errors
     */
    private UUID createKeycloakUser(CreateProUserDto user, BindingResult bindingResult) throws ServiceException {
        UUID createdKeycloakUserId = null;
        try {
            createdKeycloakUserId = keycloakService.createUser(user);
        } catch (KeycloakUserCreationException exception) {
            bindingResult.addError(new FieldError(USER_MODEL, exception.getError().getField(), exception.getError().getI18n()));
        }

        if (bindingResult.hasErrors()) {
            throw new ServiceException();
        }

        return createdKeycloakUserId;
    }

    public void resetPassword(String username) {
        ProUser proUser = findByEmail(username);
        keycloakService.sendUpdatePasswordEmail(proUser.getKeycloakId());
    }
}
