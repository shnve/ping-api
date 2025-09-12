package fr.shom.ping.service;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.StaticPage;
import fr.shom.ping.model.dto.StaticPageDto;
import fr.shom.ping.repository.StaticPageRepository;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

@RequiredArgsConstructor
@Service
public class StaticPageService {

    private final StaticPageRepository repository;

    private final XssSanitizerService xssSanitizerService;

    /**
     * Check if a static page doesn't exist for a given path
     *
     * @param path the path to be checked
     * @param bindingResult   validation binding results
     * @throws ServiceException if path already used
     */
    public void checkPathNotUsedExist(String path, BindingResult bindingResult) throws ServiceException {
        if (StringUtils.isNotBlank(path) && repository.existsStaticPageByPath(path)) {
            bindingResult.addError(new FieldError("StaticPage", "path", "validation.create-static-page.path.already-used"));
            throw new ServiceException("validation.create-static-page.path.already-used");
        }
    }

    public void sanitize(StaticPageDto dto) {
        dto.setContent(xssSanitizerService.sanitize(dto.getContent()));
        dto.setInternationalContent(xssSanitizerService.sanitize(dto.getInternationalContent()));
    }

    @Transactional
    public StaticPageDto create(StaticPageDto dto, BindingResult bindingResult) throws ServiceException {
        this.checkPathNotUsedExist(dto.getPath(), bindingResult);
        sanitize(dto);
        return repository.save(dto.toStaticPage()).asDto();
    }

    @Transactional
    public StaticPageDto update(UUID id, StaticPageDto dto, BindingResult bindingResult) throws ServiceException {
        final StaticPage existingStaticPage = repository.findById(id).orElseThrow(NotFoundException::new);
        final boolean updatePath =  !StringUtils.equalsIgnoreCase(existingStaticPage.getPath(), dto.getPath());

        // if locked and trying to update path, then throw exception
        if (existingStaticPage.getLocked() && updatePath) {
            bindingResult.addError(new FieldError("StaticPage", "path", "validation.update-static-page.path.cannot-change"));
            throw new ServiceException("validation.update-static-page.path.cannot-change");
        }

        // if not locked and trying to update path to existing value, then throw exception
        if (updatePath) {
            this.checkPathNotUsedExist(dto.getPath(), bindingResult);
        }

        // if try to update locked status, then throw exception
        if (dto.getLocked() != existingStaticPage.getLocked()) {
            bindingResult.addError(new FieldError("StaticPage", "locked", "validation.update-static-page.locked.cannot-change"));
            throw new ServiceException("validation.update-static-page.locked.cannot-change");
        }

        sanitize(dto);
        final StaticPage staticPage = dto.toStaticPage();
        staticPage.setId(id);
        return repository.save(staticPage).asDto();
    }

    @Transactional
    public void delete(UUID id) {
        final StaticPage staticPage = repository.findById(id).orElseThrow(NotFoundException::new);
        if (staticPage.getLocked()) {
            throw new BadRequestException("Static page is locked and cannot be deleted");
        }
        repository.delete(staticPage);
    }

    public Page<StaticPageDto> search(Pageable pageable) {
        final Page<StaticPage> staticPagePage = repository.findAll(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("title")));
        return new PageImpl<>(staticPagePage.stream().map(staticPage -> staticPage.asDto()).collect(Collectors.toList()), pageable, staticPagePage.getTotalElements());
    }

    public StaticPage findById(UUID id) {
        return repository.findById(id).orElseThrow(NotFoundException::new);
    }
}
