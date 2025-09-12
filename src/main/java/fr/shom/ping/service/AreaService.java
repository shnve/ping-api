package fr.shom.ping.service;

import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Area;
import fr.shom.ping.model.dto.AreaCreateDto;
import fr.shom.ping.model.dto.AreaDto;
import fr.shom.ping.repository.AreaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import static fr.shom.ping.service.UserService.USER_MODEL;

@RequiredArgsConstructor
@Service
public class AreaService {
    private final AreaRepository repository;

    @Transactional
    public AreaDto create(AreaCreateDto dto) {
        List<Area> areaList = findAllById(dto.getBorderAreas());
        return repository.save(new Area(dto, areaList)).asDto();
    }

    @Transactional
    public AreaDto update(UUID id, AreaCreateDto dto) {
        List<Area> areaList = findAllById(dto.getBorderAreas());
        Area area = new Area(dto, areaList);
        area.setId(id);
        return repository.save(area).asDto();
    }

    public List<Area> findAllById(List<UUID> ids) {
        return Optional.ofNullable(ids)
            .map(repository::findAllById)
            .orElse(new ArrayList<>());
    }

    public Page<Area> search(Pageable pageable) {
        return repository.findAll(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name")));
    }

    /**
     * Check if an area exist for a given UUID
     *
     * @param preferredAreaId area id
     * @param bindingResult   validation binding results
     * @throws ServiceException if area doesn't exist
     */
    public void checkAreaExist(UUID preferredAreaId, BindingResult bindingResult) throws ServiceException {
        if (preferredAreaId != null && !repository.existsById(preferredAreaId)) {
            bindingResult.addError(new FieldError(USER_MODEL, "preferredAreaId", "validation.create-public-user.preferred-area-id.unknown"));
            throw new ServiceException();
        }
    }

    public Area getOne(UUID id) {
        return repository.getReferenceById(id);
    }
}
