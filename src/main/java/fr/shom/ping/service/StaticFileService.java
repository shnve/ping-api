package fr.shom.ping.service;

import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.StaticFile;
import fr.shom.ping.model.dto.StaticFileDto;
import fr.shom.ping.repository.AttachmentRepository;
import fr.shom.ping.repository.StaticFileRepository;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

@RequiredArgsConstructor
@Service
public class StaticFileService {
    private final StaticFileRepository repository;

    private final AttachmentRepository attachmentRepository;

    private final AttachmentService attachmentService;

    /**
     * Check if a static file doesn't exist for a given path
     *
     * @param path          the path to be checked
     * @param bindingResult validation binding results
     * @throws ServiceException if path already used
     */
    public void checkPathNotUsedExist(String path, BindingResult bindingResult) throws ServiceException {
        if (StringUtils.isNotBlank(path) && repository.existsStaticFileByPath(path)) {
            bindingResult.addError(new FieldError("StaticFile", "path", "validation.create-static-file.path.already-used"));
            throw new ServiceException("validation.create-static-file.path.already-used");
        }
    }

    @Transactional
    public StaticFileDto create(StaticFileDto dto, BindingResult bindingResult) throws ServiceException {
        this.checkPathNotUsedExist(dto.getPath(), bindingResult);
        final Attachment attachment = attachmentRepository.findById(dto.getAttachmentId()).orElseThrow(NotFoundException::new);
        return repository.save(dto.toStaticFile(attachment)).asDto();
    }

    public StaticFile findById(UUID id) {
        return repository.findById(id).orElseThrow(NotFoundException::new);
    }

    @Transactional
    public StaticFileDto update(UUID id, StaticFileDto dto, BindingResult bindingResult) throws ServiceException {
        final StaticFile existingStaticFile = repository.findById(id).orElseThrow(NotFoundException::new);
        final boolean updatePath = !StringUtils.equalsIgnoreCase(existingStaticFile.getPath(), dto.getPath());

        // if try to update locked status, then throw exception
        if (dto.getLocked() != existingStaticFile.getLocked()) {
            bindingResult.addError(new FieldError("StaticFile", "locked", "validation.update-static-file.locked.cannot-change"));
            throw new ServiceException("validation.update-static-file.locked.cannot-change");
        }

        // if locked and trying to update path, then throw exception
        if (existingStaticFile.getLocked() && updatePath) {
            bindingResult.addError(new FieldError("StaticFile", "path", "validation.update-static-file.path.cannot-change"));
            throw new ServiceException("validation.update-static-file.path.cannot-change");
        }

        // if not locked and trying to update path to existing value, then throw exception
        if (updatePath) {
            this.checkPathNotUsedExist(dto.getPath(), bindingResult);
        }

        if (!existingStaticFile.getAttachment().getId().equals(dto.getAttachmentId())) {
            attachmentService.deleteFromDiskAndDatabase(existingStaticFile.getAttachment().getId());
        }

        final Attachment attachment = attachmentRepository.findById(dto.getAttachmentId()).orElseThrow(NotFoundException::new);
        final StaticFile staticFile = dto.toStaticFile(attachment);
        staticFile.setId(id);
        return repository.save(staticFile).asDto();
    }

    @Transactional
    public void delete(UUID id) {
        final StaticFile staticFile = repository.findById(id).orElseThrow(NotFoundException::new);
        repository.delete(staticFile);
    }

    public Page<StaticFileDto> search(Pageable pageable) {
        final Page<StaticFile> staticFilePage = repository.findAll(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
            Sort.by("description")));
        return new PageImpl<>(staticFilePage.stream().map(staticFile -> staticFile.asDto()).collect(Collectors.toList()), pageable,
            staticFilePage.getTotalElements());
    }

    public ResponseEntity<byte[]> getStaticFileByPath(String path) throws ServiceException {
        final StaticFile staticFile = repository.findOneByPath(path).orElseThrow(NotFoundException::new);
        return attachmentService.getFile(staticFile.getAttachment().getId());
    }
}
