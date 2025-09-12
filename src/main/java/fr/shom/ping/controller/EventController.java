package fr.shom.ping.controller;


import fr.shom.ping.service.EmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping(value = {"/admin-api/events", "/secured-api/events"})
@RequiredArgsConstructor
public class EventController {

    private final EmitterService emitterService;

    @GetMapping
    public SseEmitter subscribeToEvents(@RequestParam String userId) {
        return emitterService.createEmitter(userId);
    }
}
