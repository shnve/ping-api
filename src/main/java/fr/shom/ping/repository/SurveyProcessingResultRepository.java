package fr.shom.ping.repository;

import fr.shom.ping.model.SurveyProcessingResult;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyProcessingResultRepository extends JpaRepository<SurveyProcessingResult, UUID> {
}
