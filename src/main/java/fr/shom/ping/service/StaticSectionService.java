package fr.shom.ping.service;

import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.StaticSection;
import fr.shom.ping.model.dto.StaticSectionDto;
import fr.shom.ping.model.dto.StaticSectionUpdateDto;
import fr.shom.ping.repository.StaticSectionRepository;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static fr.shom.ping.utils.PingStringUtil.stringAsBlob;

@RequiredArgsConstructor
@Service
public class StaticSectionService {
    private final StaticSectionRepository repository;

    @Transactional
    public StaticSectionDto update(UUID id, StaticSectionUpdateDto dto) {
        final StaticSection existingStaticSection = repository.findById(id).orElseThrow(NotFoundException::new);
        existingStaticSection.setContent(stringAsBlob(dto.getContent()));
        existingStaticSection.setInternationalContent(stringAsBlob(dto.getInternationalContent()));
        return repository.save(existingStaticSection).asDto();
    }

    public Page<StaticSectionDto> search(Pageable pageable) {
        final Page<StaticSection> staticSectionPage = repository.findAll(
            PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("location"))
        );
        return new PageImpl<>(staticSectionPage.stream().map(StaticSection::asDto).collect(Collectors.toList()), pageable,
            staticSectionPage.getTotalElements());
    }

    public StaticSection findById(UUID id) {
        return repository.findById(id).orElseThrow(NotFoundException::new);
    }
}
