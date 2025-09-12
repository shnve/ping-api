package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Survey;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(NON_NULL)
@SuperBuilder
@Schema(name = "Survey details for modal with public user")
public class SurveyModalDtoPublicUser extends SurveyDetailDto {
    @Serial
    private static final long serialVersionUID = -4580884437223305967L;

    public SurveyModalDtoPublicUser(Survey survey) {
        super(survey);
        this.issuingUserLastName = survey.getIssuingPublicUser().getLastname();
        this.issuingUserFirstName = survey.getIssuingPublicUser().getFirstname();
    }
}
