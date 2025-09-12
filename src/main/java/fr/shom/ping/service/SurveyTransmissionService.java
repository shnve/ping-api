package fr.shom.ping.service;

import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.PublicUser;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.dto.SurveyDepositCreateDtoWithPublicUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SurveyTransmissionService {
    private final SurveyService service;

    public void transmit(UUID id, SurveyDepositCreateDtoWithPublicUser dto, String username) throws ServiceException {
        final PublicUser user = service.getUser(username);
        final Survey survey = service.getSurvey(id, user);
        service.updateDeposit(survey.getId(), dto, username, true);
        service.transmitDepositDraft(id, dto.getExternalAnalysisEnabled());
    }
}
