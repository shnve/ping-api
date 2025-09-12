package fr.shom.ping.service;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.Serie;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.WarningLink;
import fr.shom.ping.model.dto.WarningLinkCreateDto;
import fr.shom.ping.model.dto.WarningLinkDto;
import fr.shom.ping.model.enums.ServiceRole;
import fr.shom.ping.model.enums.WarningStatus;
import fr.shom.ping.repository.ServiceUserRepository;
import fr.shom.ping.repository.WarningLinkRepository;
import fr.shom.ping.repository.WarningRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static fr.shom.ping.utils.UserUtil.checkServiceUserHasNoRequiredRoleOnSerieService;

@RequiredArgsConstructor
@Service
public class WarningLinkService {

    private final WarningLinkRepository repository;
    private final WarningRepository warningRepository;
    private final ServiceUserRepository serviceUserRepository;
    private final SerieService serieService;

    @Transactional
    public WarningLinkDto create(WarningLinkCreateDto dto, String username) {
        checkIfServiceUserHasNoRequiredRoleOnSerieService(dto.getWarningId(), username);

        if (StringUtils.isEmpty(dto.getUrl())) {
            throw new BadRequestException("A warning link must have a url");
        }

        Warning warning = this.warningRepository.findById(dto.getWarningId()).orElseThrow(NotFoundException::new);

        if(warning.getStatus() == WarningStatus.MODEL){
            throw new BadRequestException("A warning model doesn't have link public reference");
        }

        return repository.save(new WarningLink(dto, warning)).asDto();
    }

    @Transactional
    public WarningLinkDto update(UUID id, WarningLinkCreateDto dto, String username) {
        checkIfServiceUserHasNoRequiredRoleOnSerieService(dto.getWarningId(), username);

        if (StringUtils.isEmpty(dto.getUrl())) {
            throw new BadRequestException("A warning link must have a url");
        }

        WarningLink warningLink = repository.findById(id).orElseThrow(NotFoundException::new);

        warningLink.setTitle(dto.getTitle());
        warningLink.setInternationalTitle(dto.getInternationalTitle());
        warningLink.setUrl(dto.getUrl());

        return repository.save(warningLink).asDto();
    }

    @Transactional
    public void delete(UUID id, String username) {
        WarningLink warningLink = repository.findById(id).orElseThrow(NotFoundException::new);
        checkIfServiceUserHasNoRequiredRoleOnSerieService(warningLink.getWarning().getId(), username);

        repository.delete(id);
    }

    private void checkIfServiceUserHasNoRequiredRoleOnSerieService(UUID warningId, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);

        Warning warning = warningRepository.findById(warningId).orElseThrow(NotFoundException::new);

        Serie serie = serieService.findById(warning.getSerie().getId());
        MaritimeService service = serie.getMaritimeService();

        boolean serviceUserHasNoRequiredRoleOnSerieService = checkServiceUserHasNoRequiredRoleOnSerieService(
            serviceUser, service, ServiceRole.SERVICE_OPERATOR, ServiceRole.SERVICE_ADMIN
        );

        if (serviceUserHasNoRequiredRoleOnSerieService) {
            throw new ForbiddenException();
        }
    }
}

