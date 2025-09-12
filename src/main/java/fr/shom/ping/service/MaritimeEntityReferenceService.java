package fr.shom.ping.service;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.MaritimeEntityReference;
import fr.shom.ping.model.dto.MaritimeEntityReferenceCreateDto;
import fr.shom.ping.model.dto.MaritimeEntityReferenceDto;
import fr.shom.ping.repository.AttachmentRepository;
import fr.shom.ping.repository.MaritimeEntityReferenceRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MaritimeEntityReferenceService {

    private final MaritimeEntityReferenceRepository repository;
    private final AttachmentRepository attachmentRepository;
    private final MaritimeEntityService maritimeEntityService;
    private final AttachmentService attachmentService;

    @Transactional
    public MaritimeEntityReferenceDto create(MaritimeEntityReferenceCreateDto dto) {
        checkUrlAndAttachment(dto.getUrl(), dto.getAttachmentId());

        MaritimeEntity maritimeEntity = this.maritimeEntityService.findById(dto.getMaritimeEntityId());

        Attachment attachment = null;
        if (dto.getAttachmentId() != null) {
            attachment = attachmentRepository.findById(dto.getAttachmentId()).get();
        }

        return repository.save(new MaritimeEntityReference(dto, maritimeEntity, attachment)).asDto();
    }

    @Transactional
    public MaritimeEntityReferenceDto update(UUID id, MaritimeEntityReferenceCreateDto dto) throws ServiceException {
        checkUrlAndAttachment(dto.getUrl(), dto.getAttachmentId());

        Attachment attachment = null;
        if (dto.getAttachmentId() != null) {
            attachment = attachmentRepository.findById(dto.getAttachmentId()).orElseThrow(BadRequestException::new);
        }

        MaritimeEntityReference maritimeEntityReference = repository.findById(id).orElseThrow(ForbiddenException::new);

        if (maritimeEntityReference.getAttachment() != null && StringUtils.isNotEmpty(dto.getUrl())
            || (dto.getAttachmentId() != null && maritimeEntityReference.getAttachment() != null && maritimeEntityReference.getAttachment().getId() != dto.getAttachmentId())) {
            this.attachmentService.deleteFromDiskAndDatabase(maritimeEntityReference.getAttachment().getId());
        }

        maritimeEntityReference.setTitle(dto.getTitle());
        maritimeEntityReference.setInternationalTitle(dto.getInternationalTitle());
        maritimeEntityReference.setUrl(dto.getUrl());
        maritimeEntityReference.setPublic(dto.getIsPublic());
        maritimeEntityReference.setAttachment(attachment);

        return repository.save(maritimeEntityReference).asDto();
    }

    @Transactional
    public void delete(UUID id) throws ServiceException {
        MaritimeEntityReference maritimeEntityReference = repository.findById(id).orElseThrow(ForbiddenException::new);
        if (maritimeEntityReference.getAttachment() != null) {
            this.attachmentService.deleteFromDiskAndDatabase(maritimeEntityReference.getAttachment().getId());
        }

        repository.delete(id);
    }

    private static void checkUrlAndAttachment(String url, UUID attachmentId) {
        if ((StringUtils.isNotEmpty(url) && attachmentId != null)
            || (StringUtils.isEmpty(url) && attachmentId == null)) {
            throw new BadRequestException("A maritime entity reference must have either an url or an attachment");
        }
    }
}
