package fr.shom.ping.service;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.Tag;
import fr.shom.ping.model.dto.TagCreateDto;
import fr.shom.ping.model.dto.TagDto;
import fr.shom.ping.repository.TagRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TagService {
    private final TagRepository repository;
    private final MaritimeServiceService maritimeServiceService;

    @Transactional
    public TagDto create(TagCreateDto dto) {
        if (!repository.findByValue(dto.getValue().trim(), dto.getMaritimeServiceId()).isEmpty()) {
            throw new BadRequestException("Tag with same name already exists");
        }
        MaritimeService maritimeService = maritimeServiceService.findById(dto.getMaritimeServiceId());
        return repository.save(new Tag(dto, maritimeService)).asDto();
    }

    @Transactional
    public TagDto update(UUID id, TagCreateDto dto) {
        if (!repository.findByValue(dto.getValue().trim(), dto.getMaritimeServiceId()).isEmpty()) {
            throw new BadRequestException("Tag with same name already exists");
        }

        Tag tag = repository.findById(id).orElseThrow(ForbiddenException::new);
        tag.setValue(dto.getValue());
        return repository.save(tag).asDto();
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(id);
    }

    public Set<Tag> findAllById(List<UUID> ids) {
        return new HashSet<>(Optional.ofNullable(ids)
            .map(repository::findAllById)
            .orElse(new ArrayList<>()));
    }
}


