package fr.shom.ping.repository;

import fr.shom.ping.model.Survey;
import fr.shom.ping.model.enums.SurveyState;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SurveyRepository extends JpaRepository<Survey, UUID> {
    @Query(
        value = "SELECT count(s.id) FROM survey s " +
            "WHERE s.issuing_public_user_id = :userId AND EXTRACT(YEAR FROM s.creation_date) = :year",
        nativeQuery = true
    )
    long countCreatedSurveysByPublicUserWithinYear(UUID userId, int year);

    @Query(
        value = "SELECT count(s.id) FROM survey s " +
            "WHERE s.issuing_service_user_id = :userId AND EXTRACT(YEAR FROM s.creation_date) = :year",
        nativeQuery = true
    )
    long countCreatedSurveysByServiceUserWithinYear(UUID userId, int year);

    @Query("SELECT CASE WHEN count(s.id) > 0 " +
        "THEN false " +
        "ELSE true END " +
        "FROM Survey s " +
        "WHERE s.nameAndLocation = :nameAndLocation")
    boolean isNameAndLocationAvailable(String nameAndLocation);

    Optional<Survey> findSurveyByIdAndStateIsIn(UUID id, Collection<@NotNull(message = "validation.survey.status.not-null") SurveyState> state);
}
