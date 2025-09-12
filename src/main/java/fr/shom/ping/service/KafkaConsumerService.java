package fr.shom.ping.service;

import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.dto.LighthouseAndBeaconInfosourceMessageDto;
import fr.shom.ping.model.dto.SseNotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {
    private final LighthouseAndBeaconInfosourceService lighthouseAndBeaconInfosourceService;
    private final SseNotificationService sseNotificationService;

    @KafkaListener(topics = "${kafka.topicName}",
        groupId = "${kafka.groupId}",
        containerFactory = "${kafka.containerFactory}")
    public void consume(LighthouseAndBeaconInfosourceMessageDto payload) throws ServiceException {
        log.info("Received payload: {}", payload.toString());
        lighthouseAndBeaconInfosourceService.create(payload);
    }

    // get SSE notification from topic then send event
    @KafkaListener(topics = "${kafka.sse-notification-topic-name}",
        groupId = "${kafka.individualGroupId}",
        containerFactory = "${kafka.sseContainerFactory}")
    public void consumeSseNotification(SseNotificationDto payload) {
        log.info("Received payload: {}", payload.toString());
        sseNotificationService.sendEvent(payload);
    }
}

