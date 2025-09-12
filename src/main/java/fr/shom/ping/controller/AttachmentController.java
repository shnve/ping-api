package fr.shom.ping.controller;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.enums.NotionType;
import fr.shom.ping.service.AttachmentService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@Slf4j
public class AttachmentController {

    private final AttachmentService service;

    @GetMapping(value = "/{id}", produces = APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getFile(@PathVariable UUID id, @RequestParam NotionType notionType, @RequestParam UUID notionItemId) {
        try {
            return service.getPublicFileByNotionType(id, notionType, notionItemId);
        } catch (ServiceException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
