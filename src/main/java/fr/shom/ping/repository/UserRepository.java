package fr.shom.ping.repository;

import fr.shom.ping.model.PublicUser;
import fr.shom.ping.model.enums.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface UserRepository extends JpaRepository<PublicUser, UUID>, QuerydslPredicateExecutor<PublicUser> {
    @Modifying
    @Query("UPDATE PublicUser u SET u.status = :status WHERE u.email = :email")
    int updateUserStatusByEmail(UserStatus status, String email);

    @Query("SELECT u.keycloakId FROM PublicUser u WHERE u.email = :email")
    Optional<UUID> findKeycloakIdByEmail(String email);

    PublicUser findOneByEmail(String email);

    @Query(
        value = "select count(email) " +
            "from (" +
            "select email from pro_user where email = :email " +
            "union all " +
            "select email from public_user where email = :email " +
            ") as total_count;",
        nativeQuery = true
    )
    long countAllExistingUsersWithEmail(String email);
}
