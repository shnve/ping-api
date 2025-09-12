package fr.shom.ping.service;

import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.Folder;
import fr.shom.ping.model.FolderAttachment;
import fr.shom.ping.model.Notion;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.Study;
import fr.shom.ping.model.StudyReference;
import fr.shom.ping.model.dto.StudyCreateDto;
import fr.shom.ping.model.dto.StudyDto;
import fr.shom.ping.model.enums.StudyStatus;
import fr.shom.ping.repository.FolderRepository;
import fr.shom.ping.repository.ServiceUserRepository;
import fr.shom.ping.repository.StudyRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toSet;


@RequiredArgsConstructor
@Service
public class StudyService {
    private final StudyRepository repository;
    private final FolderRepository folderRepository;
    private final ServiceUserRepository serviceUserRepository;

    @Transactional
    public StudyDto create(StudyCreateDto dto, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Folder folder = folderRepository.findById(dto.getFolderId()).orElseThrow(NotFoundException::new);

        if (!serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(folder.getIssuingService().getId())) {
            throw new ForbiddenException();
        }

        Set<Notion> notions = folder.getNotions().stream().
            filter(notion -> dto.getNotionsIds().contains(notion.getId()))
            .collect(toSet());

        Set<FolderAttachment> attachments = folder.getAttachments().stream()
            .filter(attachment -> dto.getAttachmentsIds().contains(attachment.getId()))
            .collect(toSet());

        return repository.save(new Study(dto, folder, notions, attachments, serviceUser)).asDto(serviceUser.getServiceIdsWithOperatorOrAdminRole());
    }

    @Transactional
    public StudyDto update(UUID id, StudyCreateDto dto, String username) {
        ServiceUser serviceUser = getServiceUser(username);
        Study study = repository.findById(id).orElseThrow(NotFoundException::new);
        Folder folder = folderRepository.findById(study.getFolder().getId()).orElseThrow(NotFoundException::new);

        List<UUID> serviceIdsWithOperatorOrAdminRole = serviceUser.getServiceIdsWithOperatorOrAdminRole();
        if (!serviceIdsWithOperatorOrAdminRole.contains(folder.getIssuingService().getId())) {
            throw new ForbiddenException();
        }

        study.setNumber(dto.getNumber());

        study.getNotions().clear();
        study.setNotions(folder.getNotions().stream()
            .filter(notion -> dto.getNotionsIds().contains(notion.getId()))
            .collect(toSet()));

        study.getAttachments().clear();
        study.setAttachments(folder.getAttachments().stream()
            .filter(attachment -> dto.getAttachmentsIds().contains(attachment.getId()))
            .collect(toSet()));

        study.getReferences().clear();
        study.setReferences(dto.getReferences().stream().map(StudyReference::new).collect(Collectors.toSet()));

        study.setLastUpdate(Instant.now());
        return repository.save(study).asDto(serviceIdsWithOperatorOrAdminRole);
    }

    @Transactional
    public void updateStatus(UUID id, StudyStatus status, String username) {
        ServiceUser serviceUser = getServiceUser(username);
        Study study = repository.findById(id).orElseThrow(NotFoundException::new);
        Folder folder = folderRepository.findById(study.getFolder().getId()).orElseThrow(NotFoundException::new);

        if (!serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(folder.getIssuingService().getId())) {
            throw new ForbiddenException();
        }

        study.setStatus(status);
        study.setLastUpdate(Instant.now());
        repository.save(study);
    }

    @Transactional
    public void delete(UUID studyId, String username) {
        ServiceUser serviceUser = getServiceUser(username);
        Study folderStudy = repository.findById(studyId).orElseThrow(NotFoundException::new);
        Folder folder = folderRepository.findById(folderStudy.getFolder().getId()).orElseThrow(NotFoundException::new);

        if (!serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(folder.getIssuingService().getId())) {
            throw new ForbiddenException();
        }

        folder.getStudies().stream()
            .filter(study -> studyId.equals(study.getId()))
            .findFirst()
            .ifPresent(study -> folder.getStudies().remove(study));

        folderRepository.save(folder);
    }

    public ServiceUser getServiceUser(String username) {
        return serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
    }
}
