package fr.shom.ping.repository;

import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.enums.ServiceRole;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ServiceUserRepository extends JpaRepository<ServiceUser, UUID> {
    @Query("SELECT count(u) FROM ServiceUser u INNER JOIN u.roles r WHERE u.email = :email AND r.serviceRole IN :serviceRoles AND r.serviceId = :serviceId")
    long hasPermission(String email, List<ServiceRole> serviceRoles, UUID serviceId);

    @Query("SELECT count(u) FROM ServiceUser u INNER JOIN u.roles r WHERE u.email = :email AND r.serviceId IN :serviceIds")
    long hasPermission(String email, List<UUID> serviceIds);

    Optional<ServiceUser> findByEmail(String email);

    @Query("SELECT DISTINCT u FROM ServiceUser u INNER JOIN u.roles r WHERE r.serviceId = :serviceId")
    List<ServiceUser> findAllWithRoleOnService(UUID serviceId);

    @Query("SELECT DISTINCT u FROM ServiceUser u INNER JOIN u.roles r WHERE r.serviceId IN :serviceIds")
    Set<ServiceUser> findAllWithRoleOnServices(List<UUID> serviceIds);

    @Query("SELECT DISTINCT u FROM ServiceUser u INNER JOIN u.roles r WHERE r.serviceId IN :serviceIds")
    Page<ServiceUser> findAllWithRoleOnServices(List<UUID> serviceIds, Pageable pageable);
}
