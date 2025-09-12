package fr.shom.ping.service;

import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.dto.SurveyDepositCreateDtoWithServiceUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SurveyTransmissionAdminService {
    private final SurveyAdminService service;

    @Transactional
    public void transmit(UUID id, SurveyDepositCreateDtoWithServiceUser dto, String username) throws ServiceException {
        final ServiceUser serviceUser = service.getUser(username);
        final Survey survey = service.getSurvey(id, serviceUser);
        service.updateDeposit(survey.getId(), dto, username, true);
        service.transmitDepositDraft(survey.getId(), dto.getExternalAnalysisEnabled());
    }
}
