package fr.shom.ping.repository;

import fr.shom.ping.model.ProUser;
import fr.shom.ping.model.dto.ProUserSearchDto;
import fr.shom.ping.model.enums.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface ProUserRepository extends JpaRepository<ProUser, UUID>, QuerydslPredicateExecutor<ProUser> {
    Optional<ProUser> findOneByEmail(String email);

    @Query("SELECT new fr.shom.ping.model.dto.ProUserSearchDto(u.id, u.firstname, u.lastname, u.email, u.status) FROM ProUser u")
    Page<ProUserSearchDto> findAllAsProUserSearchDto(Pageable pageable);

    @Query("SELECT u.keycloakId FROM ProUser u WHERE u.email = :email")
    Optional<UUID> findKeycloakIdByEmail(String email);

    @Modifying
    @Query("UPDATE ProUser u SET u.status = :status WHERE u.email = :email")
    int updateUserStatusByEmail(UserStatus status, String email);
}
