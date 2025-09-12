package fr.shom.ping.service;

import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.StaticLogo;
import fr.shom.ping.model.dto.StaticLogoDto;
import fr.shom.ping.repository.StaticLogoRepository;
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
public class StaticLogoService {
    private final StaticLogoRepository repository;

    /**
     * Check if a static logo doesn't exist for a given key
     *
     * @param key           the key to be checked
     * @param bindingResult validation binding results
     * @throws ServiceException if key already used
     */
    public void checkKeyNotUsedExist(String key, BindingResult bindingResult) throws ServiceException {
        if (StringUtils.isNotBlank(key) && repository.existsStaticLogoByKey(key)) {
            bindingResult.addError(new FieldError("StaticLogo", "key", "validation.create-static-logo.key.already-used"));
            throw new ServiceException("validation.create-static-logo.key.already-used");
        }
    }

    @Transactional
    public StaticLogoDto create(StaticLogoDto dto, BindingResult bindingResult) throws ServiceException {
        this.checkKeyNotUsedExist(dto.getKey(), bindingResult);
        return repository.save(dto.toStaticLogo()).asDto();
    }

    @Transactional
    public StaticLogoDto update(UUID id, StaticLogoDto dto, BindingResult bindingResult) throws ServiceException {
        final StaticLogo existingStaticLogo = repository.findById(id).orElseThrow(NotFoundException::new);
        final boolean updateKey = !StringUtils.equalsIgnoreCase(existingStaticLogo.getKey(), dto.getKey());

        // if try to update locked status, then throw exception
        if (dto.getLocked() != existingStaticLogo.getLocked()) {
            bindingResult.addError(new FieldError("StaticLogo", "locked", "validation.update-static-logo.locked.cannot-change"));
            throw new ServiceException("validation.update-static-logo.locked.cannot-change");
        }

        // if locked and trying to update key, then throw exception
        if (existingStaticLogo.getLocked() && updateKey) {
            bindingResult.addError(new FieldError("StaticLogo", "key", "validation.update-static-logo.key.cannot-change"));
            throw new ServiceException("validation.update-static-logo.key.cannot-change");
        }

        // if not locked and trying to update key to existing value, then throw exception
        if (updateKey) {
            checkKeyNotUsedExist(dto.getKey(), bindingResult);
        }

        final StaticLogo staticLogo = dto.toStaticLogo();
        staticLogo.setId(id);
        return repository.save(staticLogo).asDto();
    }

    public Page<StaticLogoDto> search(Pageable pageable) {
        final Page<StaticLogo> staticLogoPage = repository.findAll(
            PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("description"))
        );
        return new PageImpl<>(staticLogoPage.stream().map(StaticLogo::asDto).collect(Collectors.toList()), pageable,
            staticLogoPage.getTotalElements());
    }

    public StaticLogo findById(UUID id) {
        return repository.findById(id).orElseThrow(NotFoundException::new);
    }

    @Transactional
    public void delete(UUID id) {
        final StaticLogo staticLogo = repository.findById(id).orElseThrow(NotFoundException::new);
        repository.delete(staticLogo);
    }

}
