package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.PublicUser;
import fr.shom.ping.model.Survey;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
@Schema(name = "Survey Deposit Create Dto Model with public user")
public class SurveyDepositCreateDtoWithPublicUser extends AbstractSurveyDepositCreateDto {
    @Serial
    private static final long serialVersionUID = 3100748926398672793L;

    public Survey toSurvey(PublicUser user, Survey.SurveyBuilder surveyBuilder) {
        return surveyBuilder.issuingPublicUser(user).build();
    }
}
