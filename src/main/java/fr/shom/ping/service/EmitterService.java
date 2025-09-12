package fr.shom.ping.service;

import fr.shom.ping.repository.EmitterRepository;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Slf4j
public class EmitterService {

    private final long eventsTimeout;
    private final EmitterRepository repository;

    public EmitterService(@Value("${ping.notifications.connection.timeout}") long eventsTimeout, EmitterRepository repository) {
        this.eventsTimeout = eventsTimeout;
        this.repository = repository;
    }

    public SseEmitter createEmitter(String userId) {
        SseEmitter emitter = new SseEmitter(eventsTimeout);
        emitter.onCompletion(() -> repository.remove(userId, emitter));
        emitter.onTimeout(() -> {
            log.warn("SseEmitter timeout for userId: " + userId);
            repository.remove(userId, emitter);
            emitter.complete();
        });
        emitter.onError(e -> {
            log.error("Create SseEmitter exception for userId: " + userId, e);
            repository.remove(userId, emitter);
            emitter.completeWithError(e);
        });
        repository.addEmitter(userId, emitter);
        return emitter;
    }
}
