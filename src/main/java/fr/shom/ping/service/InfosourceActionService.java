package fr.shom.ping.service;

import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.InfosourceAction;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.enums.ActionType;
import fr.shom.ping.model.enums.UserType;
import fr.shom.ping.repository.InfosourceActionRepository;
import fr.shom.ping.repository.InfosourceRecipientRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static fr.shom.ping.model.enums.ActionType.ARCHIVE;
import static fr.shom.ping.model.enums.ActionType.INIT;
import static fr.shom.ping.model.enums.UserType.PRO;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
public class InfosourceActionService {
    private final InfosourceActionRepository repository;
    private final InfosourceRecipientRepository recipientRepository;

    @Transactional
    public void create(Infosource infosource, ActionType type, UUID userId, UserType userType, MaritimeService service) {
        repository.save(InfosourceAction.builder()
            .infosource(infosource)
            .type(type)
            .userId(userId)
            .userType(userType)
            .service(service)
            .build());
    }

    @Transactional
    public void create(Infosource infosource, ActionType type, UUID userId, UserType userType, List<MaritimeService> services) {
        List<InfosourceAction> infosourceActions = services.stream().map(maritimeService -> InfosourceAction.builder()
            .infosource(infosource)
            .type(type)
            .userId(userId)
            .userType(userType)
            .service(maritimeService)
            .build())
            .collect(toList());
        repository.saveAll(infosourceActions);
    }

    @Transactional
    public void archive(Infosource infosource, String optionalComment, UUID userId) {
        List<MaritimeService> recipientServices = recipientRepository.findServicesByInfosource(infosource);
        repository.saveAll(
            recipientServices.stream()
                .map(recipientService -> InfosourceAction.builder()
                    .infosource(infosource)
                    .type(ARCHIVE)
                    .userId(userId)
                    .userType(PRO)
                    .service(recipientService)
                    .comment(isNotBlank(optionalComment) ? optionalComment : null)
                    .build())
                .collect(toList()));
    }

    @Transactional
    public void transfer(Infosource infosource, UUID userId, List<MaritimeService> servicesToTransferTo) {
        repository.saveAll(
            servicesToTransferTo.stream()
                .map(serviceToTransferTo -> InfosourceAction.builder()
                    .infosource(infosource)
                    .type(INIT)
                    .userId(userId)
                    .userType(PRO)
                    .service(serviceToTransferTo)
                    .build())
                .collect(toList()));
    }
}
