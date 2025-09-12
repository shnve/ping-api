package fr.shom.ping.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface EmitterRepository {

    void addEmitter(String memberId, SseEmitter emitter);

    void remove(String memberId, SseEmitter emitter);

    Optional<List<SseEmitter>> get(String memberId);

    Map<String, List<SseEmitter>> getAll();
}
