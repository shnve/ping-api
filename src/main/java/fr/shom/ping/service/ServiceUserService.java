package fr.shom.ping.service;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.ProUser;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.model.dto.MaritimeServiceUserDto;
import fr.shom.ping.model.dto.ServiceLinkToUserDto;
import fr.shom.ping.model.enums.ServiceRole;
import fr.shom.ping.repository.ProUserRepository;
import fr.shom.ping.repository.ServiceUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static fr.shom.ping.model.enums.ProUserRole.FUNCTIONAL_ADMIN;
import static fr.shom.ping.model.enums.ProUserRole.SERVICE_USER;

@Service
public class ServiceUserService extends AbstractProUserService {
    private final ServiceUserRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public ServiceUserService(ProUserRepository proUserRepository, KeycloakService keycloakService, ServiceUserRepository serviceUserRepository,
        JdbcTemplate jdbcTemplate) {
        super(proUserRepository, keycloakService);
        this.repository = serviceUserRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MaritimeServiceUserDto> findUsersAssignToService(UUID serviceId) {
        // Prevent N+1 request with jdbcTemplate  (1 request to find ServiceUser + N request to find ServiceUserRole)
        return this.jdbcTemplate.query("SELECT r.service_user_id, r.service_role, u.email, u.service_id  FROM service_user_role as r, pro_user as u " +
            "WHERE r.service_id = ? AND u.id = r.service_user_id", new BeanPropertyRowMapper(MaritimeServiceUserDto.class), serviceId);
    }

    @Transactional(readOnly = true)
    public List<ServiceUser> findAll(UUID serviceId, String proUserMailWhoRequest) {
        final ProUser proUser = findByEmail(proUserMailWhoRequest);

        if (proUser.getRole() == FUNCTIONAL_ADMIN) {
            return this.repository.findAll();
        }

        final List<UUID> serviceIdsOnWhichUserIsAdmin = getServiceIdsOnWhichUserIsAdmin((ServiceUser) proUser);

        return this.repository.findAllWithRoleOnServices(serviceIdsOnWhichUserIsAdmin).stream().toList();
    }

    public ServiceUser findServiceUserById(UUID id) {
        return this.repository.findById(id)
            .orElseThrow(NotFoundException::new);
    }

    public ProUser findByEmail(String email) {
        ProUser proUser = proUserRepository.findOneByEmail(email).orElseThrow(NotFoundException::new);

        if (proUser.getRole() == SERVICE_USER) {
            return this.repository.getReferenceById(proUser.getId());
        }

        return proUser;
    }

    @Transactional
    public void assignRole(UUID userId, MaritimeService service, ServiceRole role) {
        ServiceUser user = this.findServiceUserById(userId);

        final Optional<ServiceUserRole> existingRole = user.getRoles().stream()
            .filter(serviceUserRole -> service.getId().equals(serviceUserRole.getServiceId())).findFirst();

        if (existingRole.isPresent()) {
            existingRole.get().setServiceRole(role);
        } else {
            user.getRoles().add(ServiceUserRole.builder()
                .serviceId(service.getId())
                .serviceRole(role)
                .build());
        }

        this.repository.save(user);
    }

    @Transactional
    public void deleteRole(UUID userId, MaritimeService service, ServiceRole role) {
        ServiceUser user = this.findServiceUserById(userId);
        // it is possible to delete role on main service of user but in this case, we assign him the role SERVICE_VISITOR on OLD USERS maritime service and
        // we disable him
        final boolean hasNoRoleOnService = user.getRoles().stream().noneMatch(serviceUserRole -> service.getId().equals(serviceUserRole.getServiceId()));

        if (hasNoRoleOnService) {
            throw new BadRequestException();
        }

        user.getRoles().removeIf(roleUser -> roleUser.getServiceId().equals(service.getId()) && roleUser.getServiceRole().equals(role));
        this.repository.save(user);
    }

    public List<ServiceLinkToUserDto> findServicesAssignToUser(UUID id) {
        return this.jdbcTemplate.query("SELECT ms.id as id, ms.name as name, ms.international_name as international_name, r.service_role as service_role," +
            "ms.type as type, ms.survey_owner as survey_owner " +
            "FROM service_user_role as r, maritime_service as ms " +
            "WHERE r.service_user_id = ? " +
            "AND ms.id = r.service_id", new BeanPropertyRowMapper(ServiceLinkToUserDto.class), id);
    }
}
