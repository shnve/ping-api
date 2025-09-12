package fr.shom.ping.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ping.survey-external-analysis-mail")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyExternalAnalysisMailProperties {
    private String emailSubject;
    private String emailBodyEndWithCriticalProbes;
    private String emailBodyEndWithoutCriticalProbe;
    private String emailBodyEndWithFailure;
    private String emailAccessSurveyLink;
    private String emailAccessNewContributionLink;
}
