package fr.shom.ping.model;

import fr.shom.ping.model.dto.CreateServiceUserDto;
import fr.shom.ping.model.enums.ServiceRole;
import fr.shom.ping.visitor.ProUserConvertVisitor;
import fr.shom.ping.visitor.ProUserEditDtoVisitor;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.io.Serial;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.model.enums.ProUserRole.SERVICE_USER;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_ADMIN;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_OPERATOR;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_VISITOR;
import static jakarta.persistence.FetchType.EAGER;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Entity
@DiscriminatorValue("SERVICE_USER")
public class ServiceUser extends ProUser {
    @Serial
    private static final long serialVersionUID = -4748856300672184096L;

    @OneToOne(optional = false)
    private MaritimeService service;

    @ElementCollection(fetch = EAGER)
    @CollectionTable(
        name = "service_user_role",
        joinColumns = @JoinColumn(name = "service_user_id")
    )
    @EqualsAndHashCode.Exclude
    @Default
    private Set<ServiceUserRole> roles = new HashSet<>();

    public ServiceRole getMainRole() {
        return roles.stream()
            .filter(serviceUserRole -> service.getId().equals(serviceUserRole.getServiceId()))
            .map(ServiceUserRole::getServiceRole)
            .findFirst()
            .orElse(SERVICE_VISITOR);
    }

    public ServiceUser(FunctionalAdmin functionalAdmin) {
        super(functionalAdmin);
        setRole(SERVICE_USER);
        roles = new HashSet<>();
    }

    public ServiceUser(CreateServiceUserDto dto, UUID keycloakId, MaritimeService service) {
        super(dto, keycloakId);
        this.service = service;
        this.roles = newHashSet(ServiceUserRole.builder().serviceRole(dto.getServiceRole()).serviceId(service.getId()).build());
    }

    @Override
    public <T> T toEditDto(ProUserEditDtoVisitor<T> visitor) {
        return visitor.toServiceUserEditDto(this);
    }

    @Override
    public <T> T switchRole(ProUserConvertVisitor<T> visitor) {
        return visitor.toFunctionalAdmin(this);
    }

    public FunctionalAdmin toFunctionalAdmin() {
        return new FunctionalAdmin(this);
    }

    public List<UUID> getServiceIdsWithOperatorOrAdminRole() {
        return roles.stream()
            .filter(serviceUserRole -> asList(SERVICE_OPERATOR, SERVICE_ADMIN).contains(serviceUserRole.getServiceRole()))
            .map(ServiceUserRole::getServiceId)
            .collect(toList());
    }

    public List<UUID> getServiceIdsWithAdminRole() {
        return getServiceIdsByServiceRole(SERVICE_ADMIN);
    }

    public List<UUID> getServiceIdsWithOperatorRole() {
        return getServiceIdsByServiceRole(SERVICE_OPERATOR);
    }

    public List<UUID> getServiceIdsWithVisitorRole() {
        return getServiceIdsByServiceRole(SERVICE_VISITOR);
    }

    private List<UUID> getServiceIdsByServiceRole(ServiceRole serviceRole) {
        return roles.stream()
            .filter(serviceUserRole -> serviceUserRole.getServiceRole() == serviceRole)
            .map(ServiceUserRole::getServiceId)
            .collect(toList());
    }
}
