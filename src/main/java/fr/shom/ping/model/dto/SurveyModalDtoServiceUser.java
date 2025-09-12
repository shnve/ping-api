package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.Survey;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Survey details for modal with service user")
public class SurveyModalDtoServiceUser extends SurveyDetailDto {
    @Serial
    private static final long serialVersionUID = -3289964173278910376L;

    public SurveyModalDtoServiceUser(Survey survey, boolean isBri) {
        super(survey);
        if (survey.getIssuingServiceUser() != null) {
            this.issuingUserLastName = survey.getIssuingServiceUser().getLastname();
            this.issuingUserFirstName = survey.getIssuingServiceUser().getFirstname();
            this.issuingServiceName = Optional.ofNullable(survey.getIssuingService()).map(MaritimeService::getName).orElse("");
            if (isBri) {
                this.briInternalComment = survey.getBriInternalComment();
            }
        } else {
            // if no issuing service but issuing public user then a service user wants to see a survey created from a public user
            this.issuingUserLastName = survey.getIssuingPublicUser().getLastname();
            this.issuingUserFirstName = survey.getIssuingPublicUser().getFirstname();
        }
    }
}
