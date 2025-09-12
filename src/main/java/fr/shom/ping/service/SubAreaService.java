package fr.shom.ping.service;

import com.querydsl.core.BooleanBuilder;
import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.Area;
import fr.shom.ping.model.ProUser;
import fr.shom.ping.model.QSubArea;
import fr.shom.ping.model.Serie;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.SubArea;
import fr.shom.ping.model.dto.LocalityCreateDto;
import fr.shom.ping.model.dto.SubAreaCreateDto;
import fr.shom.ping.model.dto.SubAreaDetailsDto;
import fr.shom.ping.model.dto.SubAreaSearchDto;
import fr.shom.ping.model.enums.ProUserRole;
import fr.shom.ping.repository.AreaRepository;
import fr.shom.ping.repository.ProUserRepository;
import fr.shom.ping.repository.SerieRepository;
import fr.shom.ping.repository.ServiceUserRepository;
import fr.shom.ping.repository.SubAreaRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.google.common.collect.Sets.newHashSet;
import static org.springframework.util.StringUtils.hasText;

@RequiredArgsConstructor
@Service
public class SubAreaService {

    private final SubAreaRepository repository;
    private final AreaRepository areaRepository;
    private final ProUserRepository proUserRepository;
    private final ServiceUserRepository serviceUserRepository;
    private final SerieRepository serieRepository;

    @Transactional
    public SubAreaDetailsDto create(SubAreaCreateDto dto, String username) {
        checkAccessRights(username);
        Set<SubArea> childSubAreas = Optional.ofNullable(dto.getSubAreas())
            .map(subAreas -> Set.copyOf(findAllById(new ArrayList<>(subAreas))))
            .orElse(new HashSet<>());
        Area area = null;
        if (dto.getAreaId() != null) {
            area = areaRepository.getReferenceById(dto.getAreaId());
        }
        return repository.save(new SubArea(dto, childSubAreas, area)).asDto();
    }

    @Transactional
    public SubAreaDetailsDto update(UUID id, SubAreaCreateDto dto, String username) {
        final SubArea existingSubArea = repository.findById(id).orElseThrow(NotFoundException::new);
        this.checkEditableRights(existingSubArea, username, "update");

        Set<SubArea> childSubAreas = Optional.ofNullable(dto.getSubAreas())
            .map(subAreas -> Set.copyOf(findAllById(new ArrayList<>(subAreas))))
            .orElse(new HashSet<>());
        Area area = null;
        if (dto.getAreaId() != null) {
            area = areaRepository.getReferenceById(dto.getAreaId());
        }
        final Set<Serie> associatedSeries = serieRepository.findAllBySubArea(id);
        SubArea subArea = new SubArea(dto, childSubAreas, area);
        subArea.setId(id);
        subArea.setTechnicalIdentifier(dto.getTechnicalIdentifier());
        subArea.setParents(newHashSet(existingSubArea.getParents()));
        subArea.setSeries(associatedSeries);
        validateChildSubAreas(subArea, childSubAreas);
        return repository.save(subArea).asDto();
    }

    @Transactional
    public void delete(UUID id, String username) {
        final SubArea existingSubArea = repository.findById(id).orElseThrow(NotFoundException::new);
        this.checkEditableRights(existingSubArea, username, "delete");
        repository.delete(existingSubArea);
    }

    private void checkAccessRights(String username) {
        final ProUser proUser = proUserRepository.findOneByEmail(username).orElseThrow(NotFoundException::new);
        if (proUser.getRole() == ProUserRole.SERVICE_USER) {
            final ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(NotFoundException::new);
            if (!serviceUser.getService().isMaritimeNoticeProducer()) {
                throw new ForbiddenException("Only user from maritime notice producer service can create/update/delete a subarea");
            }
        }
    }

    private void checkEditableRights(SubArea subArea, String username, String actionToCheck) throws ForbiddenException {
        if (!this.hasEditableRights(subArea, username)) {
            throw new ForbiddenException("No rights to %s this subarea".formatted(actionToCheck));
        }
    }

    private boolean hasEditableRights(SubArea subArea, String username) {
        final ProUser proUser = proUserRepository.findOneByEmail(username).orElseThrow(NotFoundException::new);
        if (proUser.getRole() == ProUserRole.SERVICE_USER) {
            if (!subArea.getSeries().isEmpty()) {
                // service user can't edit subarea associated with Series (Secteur)
                return false;
            }
            Set<UUID> maritimeServiceIds = subArea.getMaritimeServiceIds();
            final ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(NotFoundException::new);
            if (!serviceUser.getService().isMaritimeNoticeProducer() || maritimeServiceIds.isEmpty() || serviceUser.getServiceIdsWithAdminRole().isEmpty()) {
                return false;
            }
            return !Collections.disjoint(maritimeServiceIds, serviceUser.getServiceIdsWithAdminRole());
        }
        return true;
    }

    private void validateChildSubAreas(SubArea subArea, Set<SubArea> childSubAreas) {
        if (childSubAreas.size() > 0) {
            List<SubArea> parentSubAreas = repository.findAll(QSubArea.subArea.subAreas.any().id.eq(subArea.getId()));
            // On n'autorise que 2 niveaux d'hiérarchie et un descendant ne peut pas être un ascendant
            if (childSubAreas.stream().map(SubArea::getId).collect(Collectors.toList()).contains(subArea.getId()) ||
                parentSubAreas.stream().anyMatch(childSubAreas::contains) ||
                repository.findAll(QSubArea.subArea.subAreas.any().in(parentSubAreas)).size() > 0) {
                throw new ValidationException("validation.subArea.subAreas.invalid");
            }
        }
    }

    public List<SubArea> findAllById(List<UUID> ids) {
        return Optional.ofNullable(ids)
            .map(repository::findAllById)
            .orElse(new ArrayList<>());
    }

    public Page<SubAreaSearchDto> search(Pageable pageable) {
        return repository.findAll(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name"))).map(SubAreaSearchDto::new);
    }

    public Page<SubAreaSearchDto> findAllBySearchCriteria(Pageable pageable, String technicalIdentifier, String name, String serieName, String parentName) {
        final BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (hasText(technicalIdentifier)) {
            booleanBuilder.and(QSubArea.subArea.technicalIdentifier.containsIgnoreCase(technicalIdentifier));
        }

        if (hasText(name)) {
            booleanBuilder.and(QSubArea.subArea.name.containsIgnoreCase(name))
                .or(QSubArea.subArea.internationalName.containsIgnoreCase(name));
        }

        if (hasText(serieName)) {
            booleanBuilder.and(QSubArea.subArea.series.any().name.containsIgnoreCase(serieName))
                .or(QSubArea.subArea.series.any().internationalName.containsIgnoreCase(serieName));
        }

        if (hasText(parentName)) {
            booleanBuilder.and(QSubArea.subArea.parents.any().technicalIdentifier.containsIgnoreCase(parentName))
                .or(QSubArea.subArea.parents.any().name.containsIgnoreCase(parentName))
                .or(QSubArea.subArea.parents.any().internationalName.containsIgnoreCase(parentName));
        }

        return repository.findAll(booleanBuilder, pageable).map(SubAreaSearchDto::new);
    }

    @Transactional
    public SubAreaDetailsDto addLocality(UUID parentSubAreaId, LocalityCreateDto localityCreateDto) {
        if (!parentSubAreaId.equals(localityCreateDto.getParentSubAreaId())) {
            throw new BadRequestException("Parent subarea ids do not match");
        }

        SubArea parentSubArea = repository.findById(parentSubAreaId).orElseThrow(() -> new BadRequestException("Parent subarea does not exist"));

        SubArea childSubArea = repository.save(
            SubArea.builder()
                .name(localityCreateDto.getName())
                .internationalName(localityCreateDto.getInternationalName())
                .build()
        );
        parentSubArea.getSubAreas().add(childSubArea);
        repository.save(parentSubArea);

        return childSubArea.asDto();
    }

    public SubAreaDetailsDto findOne(UUID id) {
        return repository.findById(id)
            .orElseThrow(EntityNotFoundException::new)
            .asDto();
    }

    @Transactional(readOnly = true)
    public List<SubAreaDetailsDto> findAllByAreaId(UUID regionId) {
        return repository.findAllByAreaId(regionId).stream().map(SubArea::asDto).collect(Collectors.toList());
    }

    public SubArea getOne(UUID id) {
        return repository.findById(id)
            .orElseThrow(EntityNotFoundException::new);
    }
}
