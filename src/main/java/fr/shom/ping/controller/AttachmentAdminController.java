package fr.shom.ping.controller;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.AttachmentChunk;
import fr.shom.ping.model.dto.AttachmentChunkDto;
import fr.shom.ping.model.dto.AttachmentCreateDto;
import fr.shom.ping.service.AttachmentService;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static fr.shom.ping.model.dto.AttachmentChunkDto.VALIDATION_INVALID_FILENAME_MSG;
import static fr.shom.ping.model.dto.AttachmentChunkDto.VALIDATION_INVALID_RELATIVE_PATH_MSG;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping(value = {"/admin-api/attachments", "/secured-api/attachments"})
@RequiredArgsConstructor
@Slf4j
public class AttachmentAdminController {

    private final AttachmentService service;

    @PostMapping("/chunk")
    public ResponseEntity<Object> uploadChunk(@Valid AttachmentChunkDto attachmentChunkDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            final List<String> errorMessages = bindingResult.getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(toList());

            bindingResult.getAllErrors().forEach(error -> log.debug("Error : objectName = {}, message = {}", error.getObjectName(), error.getDefaultMessage()));
            log.debug("Errors during create : {}", errorMessages);

            // clean messages stack to avoid redundant errors
            if (errorMessages.contains(VALIDATION_INVALID_RELATIVE_PATH_MSG) && errorMessages.contains(VALIDATION_INVALID_FILENAME_MSG)
                && StringUtils.equals(attachmentChunkDto.getFlowRelativePath(), attachmentChunkDto.getFlowFilename())) {
                errorMessages.remove(VALIDATION_INVALID_RELATIVE_PATH_MSG);
            }

            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(errorMessages);
        }

        try {
            service.saveChunk(AttachmentChunk.fromDto(attachmentChunkDto));
        } catch (ServiceException e) {
            throw new BadRequestException();
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/chunk")
    public Object checkChunk(AttachmentChunkDto chunkDto, HttpServletResponse response) {
        if (service.checkChunk(chunkDto.getFlowIdentifier(), chunkDto.getFlowChunkNumber())) {
            response.setStatus(HttpStatus.NOT_MODIFIED.value());
        }

        return chunkDto;
    }

    @GetMapping("/check-file-requirements")
    public String checkFileRequirements(@RequestParam String filename, @RequestParam Long fileSize, @RequestParam(defaultValue = "false") Boolean isBathy,
        @RequestParam(defaultValue = "false") Boolean isSurvey, @RequestParam(defaultValue = "false") Boolean isTechnicalMetadata) {
        return service.checkFileRequirements(filename, fileSize, isBathy, isSurvey, isTechnicalMetadata);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public UUID create(@RequestBody AttachmentCreateDto attachmentCreateDto) {
        try {
            return service.create(attachmentCreateDto);
        } catch (ServiceException | IOException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        try {
            service.deleteFromDiskAndDatabase(id);
        } catch (ServiceException ignored) {
            // Nothing to do as we don't want to notify user
        }
    }

    @GetMapping(value = "/{id}", produces = APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getFile(@PathVariable UUID id) {
        try {
            return service.getFile(id);
        } catch (ServiceException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
