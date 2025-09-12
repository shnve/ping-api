package fr.shom.ping.service;

import com.querydsl.core.BooleanBuilder;
import fr.shom.ping.exception.KeycloakUserCreationException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.PublicUser;
import fr.shom.ping.model.QPublicUser;
import fr.shom.ping.model.dto.AuthenticatedPublicUserDto;
import fr.shom.ping.model.dto.CreatePublicUserDto;
import fr.shom.ping.model.dto.EditPublicUserDto;
import fr.shom.ping.repository.UserRepository;
import io.micrometer.core.instrument.util.StringUtils;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import fr.shom.ping.utils.InfosourceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import static fr.shom.ping.model.enums.UserStatus.DELETED;
import static fr.shom.ping.model.enums.UserStatus.DISABLED;
import static fr.shom.ping.model.enums.UserStatus.ENABLED;
import static org.springframework.util.StringUtils.hasText;

@Service
@RequiredArgsConstructor
public class UserService {
    public static final String USER_MODEL = "user";
    private final KeycloakService keycloakService;
    private final UserRepository repository;
    private final AreaService areaService;

    public PublicUser findById(UUID id) {
        return this.repository.findById(id).orElseThrow(EntityNotFoundException::new);
    }

    public void create(CreatePublicUserDto user, BindingResult bindingResult) throws ServiceException {
        this.areaService.checkAreaExist(user.getPreferredAreaId(), bindingResult);
        this.checkOrganizationForProAccount(user.isProAccount(), user.getOrganization(), bindingResult);

        UUID createdKeycloakUserId = createKeycloakUser(user, bindingResult);

        String identifier = InfosourceUtil.generateIdentifierFromEmail(user.getEmail());
        long existingEmails = repository.countAllExistingUsersWithEmail(user.getEmail());
        if (existingEmails > 0L) {
            identifier += existingEmails;
        }
        PublicUser publicUser = PublicUser.fromCreatePublicUserDto(user, createdKeycloakUserId, identifier);
        if (user.getPreferredAreaId() != null) {
            publicUser.setPreferredArea(areaService.getOne(user.getPreferredAreaId()));
        }

        repository.save(publicUser);
    }

    /**
     * Create keycloak user against user dto.
     *
     * @param user          create public user dto
     * @param bindingResult validation binding results
     * @return newly created keycloak user ID
     * @throws ServiceException if binding result contains errors
     */
    private UUID createKeycloakUser(CreatePublicUserDto user, BindingResult bindingResult) throws ServiceException {
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

    @Transactional
    public void enable(String email) {
        repository.updateUserStatusByEmail(ENABLED, email);
    }

    public AuthenticatedPublicUserDto me(String email) {
        PublicUser publicUser = repository.findOneByEmail(email);
        return AuthenticatedPublicUserDto.fromPublicUser(publicUser);
    }

    public PublicUser edit(UUID id, EditPublicUserDto dto, BindingResult bindingResult) throws ServiceException {
        PublicUser model = this.findById(id);
        PublicUser newModel = editPublicUserInfos(model, dto, bindingResult);
        if (!model.getEmail().equals(dto.getEmail())) {
            keycloakService.updateEmailPublicUser(newModel, dto.getEmail());
            newModel.setEmail(dto.getEmail());
        }
        return this.repository.save(newModel);
    }

    public PublicUser editPublicUser(String email, EditPublicUserDto dto, BindingResult bindingResult) throws ServiceException {
        PublicUser publicUser = repository.findOneByEmail(email);
        if (publicUser == null) {
            throw new ServiceException();
        }
        PublicUser model = editPublicUserInfos(publicUser, dto, bindingResult);
        if (!publicUser.getEmail().equals(dto.getEmail())) {
            keycloakService.updateEmailPublicUser(model, dto.getEmail());
            model.setEmail(dto.getEmail());
        }
        return this.repository.save(model);
    }

    public Page<PublicUser> search(Pageable pageable) {
        return repository.findAll(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("lastname")));
    }

    public Page<PublicUser> findAllBySearchCriteria(Pageable pageable, String lastname, String firstname, String email) {
        final BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (hasText(lastname)) {
            booleanBuilder.and(QPublicUser.publicUser.lastname.containsIgnoreCase(lastname));
        }

        if (hasText(firstname)) {
            booleanBuilder.and(QPublicUser.publicUser.firstname.containsIgnoreCase(firstname));
        }

        if (hasText(email)) {
            booleanBuilder.and(QPublicUser.publicUser.email.containsIgnoreCase(email));
        }

        return repository.findAll(booleanBuilder, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("lastname")));
    }

    @Transactional
    public void delete(String email) {
        Optional<UUID> keycloakId = repository.findKeycloakIdByEmail(email);
        if (keycloakId.isEmpty()) {
            throw new NotFoundException();
        }

        keycloakService.deactivatePublicUser(keycloakId.get());
        repository.updateUserStatusByEmail(DELETED, email);
    }

    public void sendUpdatePasswordEmail(String email) {
        Optional<UUID> keycloakId = repository.findKeycloakIdByEmail(email);
        if (keycloakId.isEmpty()) {
            throw new NotFoundException();
        }
        keycloakService.sendUpdatePasswordEmailPublicUser(keycloakId.get());
    }

    public PublicUser editPublicUserInfos(PublicUser model, EditPublicUserDto dto, BindingResult bindingResult) throws ServiceException {
        this.areaService.checkAreaExist(dto.getPreferredAreaId(), bindingResult);
        this.checkOrganizationForProAccount(dto.isProAccount(), dto.getOrganization(), bindingResult);

        // Merge dto with model
        model.setFirstname(dto.getFirstname());
        model.setLastname(dto.getLastname());
        model.setPhoneNumber(dto.getPhoneNumber());
        model.setMobilePhoneNumber(dto.getMobilePhoneNumber());
        model.setOrganization(dto.getOrganization());
        model.setShipName(dto.getShipName());
        model.setHomePort(dto.getHomePort());
        model.setRegistrationNumber(dto.getRegistrationNumber());
        Optional.ofNullable(dto.getPreferredAreaId()).ifPresentOrElse(
            uuid -> model.setPreferredArea(this.areaService.getOne(uuid)),
            () -> model.setPreferredArea(null)
        );
        model.setHasNotificationsSound(dto.hasNotificationsSound());
        model.setHasSubscriptionEmailNotifications(dto.hasSubscriptionEmailNotifications());
        model.setProAccount(dto.isProAccount());
        return model;
    }

    private void checkOrganizationForProAccount(boolean proAccount, String organization, BindingResult bindingResult) throws ServiceException {
        if (proAccount && StringUtils.isBlank(organization)) {
            bindingResult.addError(new FieldError(USER_MODEL, "organization",
                "validation.edit-public-user.organization.mandatory-if-pro-account"));
            throw new ServiceException();
        }
    }

    @Transactional
    public void disable(UUID id) {
        final PublicUser user = findById(id);
        final UUID keycloakId = repository.findKeycloakIdByEmail(user.getEmail()).orElseThrow(NotFoundException::new);
        keycloakService.deactivatePublicUser(keycloakId);
        repository.updateUserStatusByEmail(DISABLED, user.getEmail());
    }

    @Transactional
    public void enable(UUID id) {
        final PublicUser user = findById(id);
        final UUID keycloakId = repository.findKeycloakIdByEmail(user.getEmail()).orElseThrow(NotFoundException::new);
        keycloakService.activatePublicUser(keycloakId);
        repository.updateUserStatusByEmail(ENABLED, user.getEmail());
    }
}
