package fr.shom.ping.service;

import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.InfosourceRecipient;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.dto.InfosourceServiceStatusServiceUserDto;
import fr.shom.ping.model.enums.ActionType;
import fr.shom.ping.model.enums.UserType;
import fr.shom.ping.repository.InfosourceRecipientRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static fr.shom.ping.model.enums.ActionType.INIT;
import static fr.shom.ping.model.enums.ActionType.READ;
import static fr.shom.ping.model.enums.UserType.PRO;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class InfosourceRecipientService {

    private final InfosourceRecipientRepository repository;

    private final InfosourceActionService actionService;

    private final SseNotificationService sseNotificationService;

    @Transactional
    public void send(List<MaritimeService> recipients, Infosource infosource, UUID userId, UserType userType, boolean sendNotification) {
        repository.saveAll(recipients.stream()
            .map(recipient -> InfosourceRecipient.builder()
                .infosource(infosource)
                .service(recipient)
                .status(INIT)
                .build())
            .collect(toList()));
        actionService.create(infosource, INIT, userId, userType, recipients);

        if (sendNotification) {
            // send notification to each IS recipient
            recipients.forEach(maritimeService -> sseNotificationService.sendInfosourceNotification(infosource.getNumber(), maritimeService, infosource));
        }
    }

    @Transactional
    public void read(Infosource infosource, ServiceUser serviceUser, List<MaritimeService> recipientServices) {
        repository.updateStatus(infosource.getId(), recipientServices.stream().map(MaritimeService::getId).collect(toList()), READ);
        actionService.create(infosource, READ, serviceUser.getId(), PRO, recipientServices);
    }

    @Transactional
    public void archive(Infosource infosource, ServiceUser serviceUser, String optionalComment) {
        repository.archive(infosource.getId());
        actionService.archive(infosource, optionalComment, serviceUser.getId());
    }

    public List<InfosourceServiceStatusServiceUserDto> findServiceStatusesForServiceUser(UUID infosourceId) {
        return repository.findServiceStatusesForServiceUser(infosourceId);
    }

    public List<InfosourceServiceStatusServiceUserDto> findServiceStatusesForPublicUser(UUID infosourceId, UUID publicUserId) {
        return repository.findServiceStatusesForPublicUser(infosourceId, publicUserId);
    }

    @Transactional
    public void updateStatus(Infosource infosource, ServiceUser serviceUser, MaritimeService service, ActionType status) {
        repository.updateStatus(infosource.getId(), singletonList(service.getId()), status);
        actionService.create(infosource, status, serviceUser.getId(), PRO, service);
    }

    public List<UUID> findRecipientServiceIdsByInfosource(Infosource infosource) {
        return repository.findRecipientServiceIdsByInfosource(infosource);
    }

    @Transactional
    public void transfer(Infosource infosource, ServiceUser serviceUser, List<MaritimeService> servicesToTransferTo) {
        repository.saveAll(servicesToTransferTo.stream()
            .map(serviceToTransferTo -> InfosourceRecipient.builder()
                .infosource(infosource)
                .service(serviceToTransferTo)
                .transferredBy(serviceUser.getService())
                .status(INIT)
                .build())
            .collect(toList()));

        actionService.transfer(infosource, serviceUser.getId(), servicesToTransferTo);


        // send notification to each IS recipient
        servicesToTransferTo.forEach(maritimeService -> sseNotificationService.sendInfosourceNotification(infosource.getNumber(), maritimeService, infosource));
    }
}
