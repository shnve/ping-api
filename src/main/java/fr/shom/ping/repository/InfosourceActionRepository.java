package fr.shom.ping.repository;

import fr.shom.ping.model.InfosourceAction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface InfosourceActionRepository extends JpaRepository<InfosourceAction, UUID>, QuerydslPredicateExecutor<InfosourceAction> {
    @Query(value = "SELECT infosourceAction.comment FROM InfosourceAction infosourceAction WHERE infosourceAction.infosource.id = :id " +
        "AND infosourceAction.date =  (SELECT MAX(infosourceAction.date) FROM InfosourceAction infosourceAction " +
        "WHERE infosourceAction.infosource.id = :id  AND infosourceAction.type='ARCHIVE' AND infosourceAction.comment IS NOT NULL)")
    Optional<String> findRejectComment(UUID id);
}
