package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.Survey;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
@Schema(name = "Survey Deposit Create Dto Model with service user")
public class SurveyDepositCreateDtoWithServiceUser extends AbstractSurveyDepositCreateDto {
    @Serial
    private static final long serialVersionUID = -2199952366955628296L;

    @NotNull(message = "validation.survey.deposit.service-id.not-null")
    private UUID serviceId;

    public Survey toSurvey(ServiceUser serviceUser, MaritimeService issuingService, Survey.SurveyBuilder surveyBuilder) {
        return surveyBuilder.issuingServiceUser(serviceUser)
            .issuingService(issuingService)
            .build();
    }
}
