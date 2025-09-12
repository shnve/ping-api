package fr.shom.ping.controller;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.service.StaticFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/static-file")
@RequiredArgsConstructor
@Tag(name = "Static file")
public class StaticFileController {
    private final StaticFileService service;

    @GetMapping("/{path}")
    @Operation(summary = "Get static file by path", tags = "Static file")
    public ResponseEntity<byte[]> getStaticFileByPath(@PathVariable String path) {
        try {
            return service.getStaticFileByPath(path);
        } catch (ServiceException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
