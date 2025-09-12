package fr.shom.ping.controller;

import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.StaticPage;
import fr.shom.ping.model.dto.StaticPageDto;
import fr.shom.ping.repository.StaticPageRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/api/static-pages")
@RequiredArgsConstructor
@Tag(name = "Static page")
public class StaticPageController {
    private final StaticPageRepository repository;

    @GetMapping("/{path}")
    @Operation(summary = "Get static page by path", tags = "Static page")
    public StaticPageDto findByPath(@PathVariable String path) {
        return repository.findOneByPath(path).map(StaticPage::asDto).orElseThrow(NotFoundException::new);
    }
}
