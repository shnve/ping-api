package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Area;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.Survey;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotNull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
@Schema(name = "Survey Test Create Dto Model with service user")
public class SurveyTestCreateDtoWithServiceUser extends AbstractSurveyTestCreateDto {
    @Serial
    private static final long serialVersionUID = -1896208239058894062L;

    @NotNull(message = "validation.survey.test.service-id.not-null")
    private UUID serviceId;

    public Survey toSurvey(ServiceUser serviceUser, MaritimeService issuingService, Survey.SurveyBuilder surveyBuilder) {
        return surveyBuilder.issuingServiceUser(serviceUser)
            .issuingService(issuingService)
            .build();
    }
}
