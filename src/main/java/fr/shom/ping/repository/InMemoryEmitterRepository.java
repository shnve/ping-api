package fr.shom.ping.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Repository
@RequiredArgsConstructor
@Slf4j
public class InMemoryEmitterRepository implements EmitterRepository {

    private final Map<String, List<SseEmitter>> userEmitterMap = new ConcurrentHashMap<>();

    private final Semaphore mutex = new Semaphore(1);

    @Override
    public void addEmitter(String userId, SseEmitter emitter) {
        try {
            mutex.acquire();
            if (userEmitterMap.containsKey(userId)) {
                userEmitterMap.get(userId).add(emitter);
            } else {
                userEmitterMap.put(userId, new ArrayList<>(List.of(emitter)));
            }
        } catch (InterruptedException e) {
            log.warn("interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            mutex.release();
        }
    }

    @Override
    public void remove(String userId, SseEmitter emitter) {
        try {
            mutex.acquire();
            if (userEmitterMap.containsKey(userId)) {
                userEmitterMap.get(userId).remove(emitter);
                if (userEmitterMap.get(userId).isEmpty()) {
                    userEmitterMap.remove(userId);
                }
            }
        } catch (InterruptedException e) {
            log.warn("interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            mutex.release();
        }
    }

    @Override
    public Optional<List<SseEmitter>> get(String userId) {
        return Optional.ofNullable(userEmitterMap.get(userId));
    }

    @Override
    public Map<String, List<SseEmitter>> getAll() {
        return ImmutableMap.copyOf(userEmitterMap);
    }
}
